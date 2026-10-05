package com.example.worldbusiness.data.repository

import com.example.worldbusiness.data.model.CorporateTreeNode
import com.example.worldbusiness.data.model.CorporateTreeStats
import com.example.worldbusiness.data.model.EntityRecord
import java.util.Locale

object CorporateHierarchyEngine {

    /**
     * Builds an interactive corporate hierarchical tree from a flat list of EntityRecord objects.
     */
    fun buildHierarchy(
        entities: List<EntityRecord>,
        collapsedNodeIds: Set<Long> = emptySet(),
        fxRatesToUsd: Map<String, Double> = defaultFxRates
    ): List<CorporateTreeNode> {
        if (entities.isEmpty()) return emptyList()

        // 1. Identify Root / Ultimate Parent Entity (UPE)
        val rootEntity = entities.find {
            it.name.contains("HOLDINGS", ignoreCase = true) ||
                it.name.contains("PARENT", ignoreCase = true) ||
                it.name.contains("GROUP", ignoreCase = true)
        } ?: entities.first()

        // 2. Identify Regional Holding Entities (Tier 1)
        val tier1Entities = entities.filter { it.id != rootEntity.id && isTier1(it) }

        // 3. Remaining entities categorized into Tier 2 / Tier 3 based on jurisdiction & name
        val tier2AndLower = entities.filter { it.id != rootEntity.id && !tier1Entities.contains(it) }

        // Map child entities under their appropriate regional parents
        val emeaHolding = tier1Entities.find { it.name.contains("EMEA", ignoreCase = true) || it.countryCode == "GB" }
        val apacHolding = tier1Entities.find { it.name.contains("APAC", ignoreCase = true) || it.countryCode == "SG" }
        val latamHolding = tier1Entities.find { it.name.contains("LATAM", ignoreCase = true) || it.countryCode == "BR" }

        // Build Tier 1 Nodes with their respective children
        val tier1Nodes = tier1Entities.map { t1Entity ->
            val children = tier2AndLower.filter { sub ->
                resolveParentForSub(sub, emeaHolding, apacHolding, latamHolding)?.id == t1Entity.id
            }.map { subEntity ->
                val capitalUsd = toUsd(subEntity.operatingCapital, subEntity.baseCurrency, fxRatesToUsd)
                CorporateTreeNode(
                    entity = subEntity,
                    parentId = t1Entity.id,
                    parentName = t1Entity.name,
                    ownershipPercentage = 100.0,
                    tierLevel = 2,
                    tierLabel = "Operating Subsidiary (Tier-2)",
                    children = emptyList(),
                    totalSubTreeCapitalUsd = capitalUsd,
                    totalDownstreamCount = 0,
                    isExpanded = !collapsedNodeIds.contains(subEntity.id)
                )
            }

            val ownCapitalUsd = toUsd(t1Entity.operatingCapital, t1Entity.baseCurrency, fxRatesToUsd)
            val subTreeCapital = ownCapitalUsd + children.sumOf { it.totalSubTreeCapitalUsd }

            CorporateTreeNode(
                entity = t1Entity,
                parentId = rootEntity.id,
                parentName = rootEntity.name,
                ownershipPercentage = 100.0,
                tierLevel = 1,
                tierLabel = "Regional Principal Operating Co. (Tier-1)",
                children = children,
                totalSubTreeCapitalUsd = subTreeCapital,
                totalDownstreamCount = children.size,
                isExpanded = !collapsedNodeIds.contains(t1Entity.id)
            )
        }

        // Entities that connect directly to Root (orphan or direct holding subsidiaries)
        val directRootSubs = tier2AndLower.filter { sub ->
            resolveParentForSub(sub, emeaHolding, apacHolding, latamHolding) == null
        }.map { subEntity ->
            val capitalUsd = toUsd(subEntity.operatingCapital, subEntity.baseCurrency, fxRatesToUsd)
            CorporateTreeNode(
                entity = subEntity,
                parentId = rootEntity.id,
                parentName = rootEntity.name,
                ownershipPercentage = 100.0,
                tierLevel = 1,
                tierLabel = "Direct Subsidiary (Tier-1)",
                children = emptyList(),
                totalSubTreeCapitalUsd = capitalUsd,
                totalDownstreamCount = 0,
                isExpanded = !collapsedNodeIds.contains(subEntity.id)
            )
        }

        val allRootChildren = tier1Nodes + directRootSubs
        val rootOwnCapitalUsd = toUsd(rootEntity.operatingCapital, rootEntity.baseCurrency, fxRatesToUsd)
        val rootConsolidatedCapitalUsd = rootOwnCapitalUsd + allRootChildren.sumOf { it.totalSubTreeCapitalUsd }
        val totalDownstream = allRootChildren.size + allRootChildren.sumOf { it.totalDownstreamCount }

        val rootNode = CorporateTreeNode(
            entity = rootEntity,
            parentId = null,
            parentName = null,
            ownershipPercentage = 100.0,
            tierLevel = 0,
            tierLabel = "Ultimate Parent Entity (UPE • Level 0)",
            children = allRootChildren,
            totalSubTreeCapitalUsd = rootConsolidatedCapitalUsd,
            totalDownstreamCount = totalDownstream,
            isExpanded = !collapsedNodeIds.contains(rootEntity.id)
        )

        return listOf(rootNode)
    }

    private fun isTier1(entity: EntityRecord): Boolean {
        val nameUpper = entity.name.uppercase(Locale.US)
        return nameUpper.contains("EMEA") ||
            nameUpper.contains("APAC") ||
            nameUpper.contains("LATAM") ||
            nameUpper.contains("AMERICAS") ||
            nameUpper.contains("HOLDING")
    }

    private fun resolveParentForSub(
        sub: EntityRecord,
        emea: EntityRecord?,
        apac: EntityRecord?,
        latam: EntityRecord?
    ): EntityRecord? {
        val cc = sub.countryCode.uppercase(Locale.US)
        val name = sub.name.uppercase(Locale.US)
        return when {
            cc in listOf("CH", "DE", "FR", "NL", "IT", "ES", "SE", "FI", "AT") || name.contains("DACH") || name.contains("EUROPE") -> emea
            cc in listOf("JP", "HK", "AU", "KR", "IN", "VN", "MY", "ID") || name.contains("ASIA") || name.contains("TOKYO") -> apac
            cc in listOf("BR", "MX", "AR", "CL", "CO") || name.contains("BRAZIL") || name.contains("LATAM") -> latam
            else -> emea ?: apac ?: latam
        }
    }

    private fun toUsd(amount: Double, currency: String, rates: Map<String, Double>): Double {
        val rate = rates[currency.uppercase(Locale.US)] ?: 1.0
        return amount * rate
    }

    val defaultFxRates = mapOf(
        "USD" to 1.0,
        "EUR" to 1.0925,
        "GBP" to 1.3040,
        "SGD" to 0.7710,
        "CHF" to 1.1730,
        "BRL" to 0.1840,
        "JPY" to 0.0068
    )

    fun computeTreeStats(entities: List<EntityRecord>): CorporateTreeStats {
        val goodStanding = entities.count { it.status == "GOOD_STANDING" || it.status == "ACTIVE" }
        val filingDue = entities.count { it.status == "FILING_DUE" || it.status == "PORT_INSPECTION" }
        val avgCompliance = if (entities.isNotEmpty()) entities.map { it.complianceScore }.average().toInt() else 100
        val totalCapitalUsd = entities.sumOf { toUsd(it.operatingCapital, it.baseCurrency, defaultFxRates) }

        return CorporateTreeStats(
            totalEntities = entities.size,
            totalTiers = if (entities.size > 3) 3 else 2,
            consolidatedOperatingCapitalUsd = totalCapitalUsd,
            goodStandingCount = goodStanding,
            filingDueCount = filingDue,
            averageComplianceScore = avgCompliance
        )
    }

    /**
     * Checks if an entity matches a search query by Name, ID, Tax ID, Jurisdiction, or Director.
     */
    fun matchesQuery(entity: EntityRecord, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase(Locale.US)
        val cleanQ = q.removePrefix("#").removePrefix("id:").trim()

        return entity.name.lowercase(Locale.US).contains(q) ||
            entity.id.toString() == cleanQ ||
            "id:${entity.id}" == q ||
            "#${entity.id}" == q ||
            entity.taxId.lowercase(Locale.US).contains(q) ||
            entity.jurisdiction.lowercase(Locale.US).contains(q) ||
            entity.countryCode.lowercase(Locale.US) == cleanQ ||
            entity.entityType.lowercase(Locale.US).contains(q) ||
            entity.localDirector.lowercase(Locale.US).contains(q)
    }

    /**
     * Finds IDs of all ancestor nodes that contain at least one matching descendant.
     * Allows automatic auto-expansion of hierarchical tree branches.
     */
    fun findAncestorsOfMatchingNodes(
        treeNodes: List<CorporateTreeNode>,
        query: String
    ): Set<Long> {
        if (query.isBlank()) return emptySet()
        val expandIds = mutableSetOf<Long>()

        fun checkNode(node: CorporateTreeNode): Boolean {
            val selfMatches = matchesQuery(node.entity, query)
            var descendantMatches = false
            for (child in node.children) {
                if (checkNode(child)) {
                    descendantMatches = true
                }
            }
            if (selfMatches || descendantMatches) {
                if (node.children.isNotEmpty()) {
                    expandIds.add(node.entity.id)
                }
                return true
            }
            return false
        }

        treeNodes.forEach { checkNode(it) }
        return expandIds
    }
}
