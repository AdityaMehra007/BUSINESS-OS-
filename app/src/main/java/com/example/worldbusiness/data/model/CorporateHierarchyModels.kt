package com.example.worldbusiness.data.model

data class CorporateTreeNode(
    val entity: EntityRecord,
    val parentId: Long?,
    val parentName: String?,
    val ownershipPercentage: Double,
    val tierLevel: Int, // 0: Global Ultimate Parent (UPE), 1: Regional Hub, 2: Operating Sub, 3: Local SPV / Branch
    val tierLabel: String,
    val children: List<CorporateTreeNode> = emptyList(),
    val totalSubTreeCapitalUsd: Double = 0.0,
    val totalDownstreamCount: Int = 0,
    val isExpanded: Boolean = true
)

data class CorporateTreeStats(
    val totalEntities: Int,
    val totalTiers: Int,
    val consolidatedOperatingCapitalUsd: Double,
    val goodStandingCount: Int,
    val filingDueCount: Int,
    val averageComplianceScore: Int
)
