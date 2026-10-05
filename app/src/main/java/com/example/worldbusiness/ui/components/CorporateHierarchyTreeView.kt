package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPositive
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoseNegative
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.worldbusiness.data.model.CorporateTreeNode
import com.example.worldbusiness.data.model.CorporateTreeStats
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.repository.CorporateHierarchyEngine
import java.text.NumberFormat
import java.util.Locale

/**
 * Interactive Hierarchical Tree Visualization representing multinational corporate entities,
 * subsidiaries, ownership equity chains, and operational status.
 */
@Composable
fun CorporateHierarchyTreeView(
    entities: List<EntityRecord>,
    searchQuery: String = "",
    onIncorporateClick: () -> Unit = {},
    onUpdateEntityStatus: (EntityRecord, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var internalSearchQuery by remember { mutableStateOf(searchQuery) }
    var collapsedNodeIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedEntityForDossier by remember { mutableStateOf<EntityRecord?>(null) }
    var statusFilter by remember { mutableStateOf("ALL") }

    val activeSearchQuery = if (internalSearchQuery.isNotEmpty()) internalSearchQuery else searchQuery

    val treeStats = remember(entities) {
        CorporateHierarchyEngine.computeTreeStats(entities)
    }

    val treeNodes = remember(entities, collapsedNodeIds) {
        CorporateHierarchyEngine.buildHierarchy(entities, collapsedNodeIds)
    }

    // Auto-expand any ancestors containing matching subsidiaries
    val autoExpandedAncestors = remember(treeNodes, activeSearchQuery) {
        if (activeSearchQuery.isNotBlank()) {
            CorporateHierarchyEngine.findAncestorsOfMatchingNodes(treeNodes, activeSearchQuery)
        } else emptySet()
    }

    val activeCollapsedNodeIds = remember(collapsedNodeIds, autoExpandedAncestors, activeSearchQuery) {
        if (activeSearchQuery.isNotBlank()) {
            collapsedNodeIds - autoExpandedAncestors
        } else {
            collapsedNodeIds
        }
    }

    val matchingEntities = remember(entities, activeSearchQuery) {
        if (activeSearchQuery.isBlank()) entities else {
            entities.filter { CorporateHierarchyEngine.matchesQuery(it, activeSearchQuery) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("corporate_hierarchy_tree_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Executive Corporate Hierarchy Overview Card
        CorporateTreeSummaryCard(
            stats = treeStats,
            onExpandAll = { collapsedNodeIds = emptySet() },
            onCollapseAll = {
                // Collapse all tier 1 and tier 0 nodes
                collapsedNodeIds = entities.map { it.id }.toSet()
            },
            onIncorporateClick = onIncorporateClick
        )

        // Global Search Bar within the Hierarchical Entity Visualization
        GlobalHierarchySearchBar(
            searchQuery = activeSearchQuery,
            onSearchQueryChange = { internalSearchQuery = it },
            matchCount = matchingEntities.size,
            totalCount = entities.size,
            onQuickSelect = { internalSearchQuery = it }
        )

        // Direct Subsidiary Locator Ribbon (shown when active search matches entities)
        if (activeSearchQuery.isNotBlank() && matchingEntities.isNotEmpty()) {
            LocateSubsidiariesRibbon(
                matchingEntities = matchingEntities,
                onLocateEntity = { selectedEntityForDossier = it }
            )
        }

        // Status Filter Chips Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                "ALL" to "All Tiers (${entities.size})",
                "GOOD_STANDING" to "Good Standing (${treeStats.goodStandingCount})",
                "FILING_DUE" to "Filing Due (${treeStats.filingDueCount})"
            ).forEach { (filterKey, label) ->
                val isSelected = statusFilter == filterKey
                Box(
                    modifier = Modifier
                        .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(8.dp))
                        .clickable { statusFilter = filterKey }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) SurfaceDark else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Render Hierarchical Tree Nodes
        if (treeNodes.isNotEmpty()) {
            treeNodes.forEach { rootNode ->
                CorporateNodeBranch(
                    node = rootNode,
                    collapsedNodeIds = activeCollapsedNodeIds,
                    searchQuery = activeSearchQuery,
                    statusFilter = statusFilter,
                    onToggleExpand = { id ->
                        collapsedNodeIds = if (collapsedNodeIds.contains(id)) {
                            collapsedNodeIds - id
                        } else {
                            collapsedNodeIds + id
                        }
                    },
                    onSelectEntity = { selectedEntityForDossier = it }
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Text(
                    text = "No corporate entities incorporated yet.",
                    modifier = Modifier.padding(20.dp),
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    // Entity Statutory Dossier Dialog
    selectedEntityForDossier?.let { entity ->
        EntityDossierDialog(
            entity = entity,
            onDismiss = { selectedEntityForDossier = null },
            onUpdateStatus = { newStatus ->
                onUpdateEntityStatus(entity, newStatus)
                selectedEntityForDossier = null
            }
        )
    }
}

/**
 * Global Search Bar within the Hierarchical Entity Visualization
 */
@Composable
fun GlobalHierarchySearchBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    matchCount: Int,
    totalCount: Int,
    onQuickSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (searchQuery.isNotEmpty()) listOf(CyanAccent.copy(alpha = 0.6f), GoldAccent.copy(alpha = 0.4f))
                else listOf(BorderSubtle, BorderSubtle)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search Input Row with Match Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("hierarchy_search_bar"),
                    placeholder = {
                        Text(
                            text = "Search subsidiaries by name or ID (#1, DACH)...",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated,
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (searchQuery.isNotEmpty() && matchCount > 0) EmeraldPositive.copy(alpha = 0.15f)
                    else SurfaceElevated,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            if (searchQuery.isNotEmpty() && matchCount > 0) listOf(EmeraldPositive, EmeraldPositive.copy(alpha = 0.4f))
                            else listOf(BorderSubtle, BorderSubtle)
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isEmpty()) "$totalCount UNITS" else "$matchCount FOUND",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (searchQuery.isNotEmpty() && matchCount > 0) EmeraldPositive else TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Quick Filter Chips for One-Tap Locating
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOCATE:",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )

                listOf(
                    "#1" to "🇺🇸 Holdings (#1)",
                    "#2" to "🇨🇭 DACH (#2)",
                    "#3" to "🇸🇬 Singapore (#3)",
                    "#4" to "🇬🇧 UK (#4)",
                    "DE" to "🇩🇪 Germany",
                    "BR" to "🇧🇷 Brazil"
                ).forEach { (queryKey, label) ->
                    val isSelected = searchQuery.equals(queryKey, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) CyanAccent else SurfaceElevated, RoundedCornerShape(6.dp))
                            .border(0.5.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(6.dp))
                            .clickable {
                                if (isSelected) onSearchQueryChange("") else onQuickSelect(queryKey)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("quick_search_chip_$queryKey")
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) SurfaceDark else TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * Direct Subsidiary Locator Ribbon (shown when active search matches entities)
 */
@Composable
fun LocateSubsidiariesRibbon(
    matchingEntities: List<EntityRecord>,
    onLocateEntity: (EntityRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Locate",
                    tint = CyanAccent,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "DIRECT SUBSIDIARY LOCATOR (${matchingEntities.size})",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "Tap to open statutory dossier",
                fontSize = 8.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            matchingEntities.forEach { entity ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onLocateEntity(entity) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .testTag("locate_subsidiary_${entity.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = countryCodeToFlag(entity.countryCode), fontSize = 14.sp)
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(CyanAccent.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "#${entity.id}",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CyanAccent,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = entity.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = "${entity.jurisdiction} • ${entity.taxId}",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Recursive Tree Branch Component with Branch Connector Lines & Expand/Collapse Animations
 */
@Composable
private fun CorporateNodeBranch(
    node: CorporateTreeNode,
    collapsedNodeIds: Set<Long>,
    searchQuery: String,
    statusFilter: String,
    onToggleExpand: (Long) -> Unit,
    onSelectEntity: (EntityRecord) -> Unit
) {
    val isCollapsed = collapsedNodeIds.contains(node.entity.id)
    val hasChildren = node.children.isNotEmpty()
    val isMatchesSearch = searchQuery.isNotBlank() && (
        CorporateHierarchyEngine.matchesQuery(node.entity, searchQuery)
    )

    val isMatchesStatus = when (statusFilter) {
        "GOOD_STANDING" -> node.entity.status == "GOOD_STANDING" || node.entity.status == "ACTIVE"
        "FILING_DUE" -> node.entity.status == "FILING_DUE"
        else -> true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (node.tierLevel * 16).dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Connector Visual line if not Root
        if (node.tierLevel > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(14.dp)
                        .background(BorderSubtle.copy(alpha = 0.6f))
                )
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(2.dp)
                        .background(BorderSubtle.copy(alpha = 0.6f))
                )
                Text(
                    text = " ${node.ownershipPercentage.toInt()}% Equity Ownership",
                    fontSize = 9.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Entity Node Card
        CorporateEntityNodeCard(
            node = node,
            isCollapsed = isCollapsed,
            isHighlighted = isMatchesSearch,
            isDimmed = !isMatchesStatus,
            onToggleExpand = { onToggleExpand(node.entity.id) },
            onClick = { onSelectEntity(node.entity) }
        )

        // Render Children if Expanded
        AnimatedVisibility(
            visible = !isCollapsed && hasChildren,
            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                node.children.forEach { childNode ->
                    CorporateNodeBranch(
                        node = childNode,
                        collapsedNodeIds = collapsedNodeIds,
                        searchQuery = searchQuery,
                        statusFilter = statusFilter,
                        onToggleExpand = onToggleExpand,
                        onSelectEntity = onSelectEntity
                    )
                }
            }
        }
    }
}

/**
 * Individual Node Card representing a Corporate Entity with status telemetry.
 */
@Composable
private fun CorporateEntityNodeCard(
    node: CorporateTreeNode,
    isCollapsed: Boolean,
    isHighlighted: Boolean,
    isDimmed: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit
) {
    val entity = node.entity
    val tierColor = when (node.tierLevel) {
        0 -> GoldAccent // Ultimate Parent
        1 -> CyanAccent // Regional Hub
        else -> EmeraldPositive // Downstream Operating Sub
    }

    val statusColor = when (entity.status) {
        "GOOD_STANDING", "ACTIVE" -> EmeraldPositive
        "FILING_DUE" -> GoldAccent
        else -> RoseNegative
    }

    val formattedCapital = NumberFormat.getNumberInstance(Locale.US).format(entity.operatingCapital)
    val formattedSubTreeUsd = NumberFormat.getNumberInstance(Locale.US).format(node.totalSubTreeCapitalUsd)

    val chevronRotation by animateFloatAsState(
        targetValue = if (isCollapsed) 0f else 90f,
        label = "chevron_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("entity_node_${entity.countryCode.lowercase(Locale.US)}_${entity.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDimmed) SurfaceDark.copy(alpha = 0.5f) else SurfaceDark
        ),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
            if (isHighlighted) {
                listOf(CyanAccent, GoldAccent)
            } else if (node.tierLevel == 0) {
                listOf(GoldAccent.copy(alpha = 0.7f), BorderSubtle)
            } else {
                listOf(tierColor.copy(alpha = 0.45f), BorderSubtle)
            }
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Tier Badge, Country Flag, Operational Status, and Expand Caret
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Flag Icon or Tier Badge
                    Box(
                        modifier = Modifier
                            .background(tierColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(0.8.dp, tierColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${countryCodeToFlag(entity.countryCode)} ${entity.countryCode}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Tier Label Tag
                    Text(
                        text = node.tierLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tierColor,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Operational Status Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(
                            listOf(statusColor.copy(alpha = 0.6f), statusColor.copy(alpha = 0.3f))
                        ))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                            Text(
                                text = entity.status.replace("_", " "),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Expand / Collapse Chevron if it has downstream subsidiaries
                    if (node.children.isNotEmpty()) {
                        IconButton(
                            onClick = onToggleExpand,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = if (isCollapsed) "Expand" else "Collapse",
                                tint = tierColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(chevronRotation)
                            )
                        }
                    }
                }
            }

            // Entity Legal Name & Jurisdiction
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(0.5.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#${entity.id}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = entity.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }

                    if (isHighlighted) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CyanAccent.copy(alpha = 0.2f),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CyanAccent, EmeraldPositive)))
                        ) {
                            Text(
                                text = "SEARCH MATCH",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = CyanAccent,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${entity.jurisdiction} • ${entity.entityType}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = entity.taxId,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

            // Operational Telemetry Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Capital Metrics
                Column {
                    Text(
                        text = if (node.tierLevel == 0) "CONSOLIDATED GROUP CAPITAL" else "OPERATING CAPITAL",
                        fontSize = 8.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${entity.baseCurrency} $formattedCapital",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                    if (node.tierLevel <= 1 && node.children.isNotEmpty()) {
                        Text(
                            text = "Incl. Subs: $$formattedSubTreeUsd USD",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Resident Director & Compliance Health
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "RESIDENT DIRECTOR",
                        fontSize = 8.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = entity.localDirector,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Compliance: ${entity.complianceScore}/100",
                            fontSize = 9.sp,
                            color = if (entity.complianceScore >= 95) EmeraldPositive else GoldAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Downstream Subsidiaries Count Tag (if any)
            if (node.children.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SurfaceElevated.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hierarchy: ${node.totalDownstreamCount} Downstream Operating Entities",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isCollapsed) "Tap to Expand ▼" else "Tap to Collapse ▲",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = tierColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

/**
 * Corporate Tree Overview Summary Card with Tiers and Global Consolidation
 */
@Composable
private fun CorporateTreeSummaryCard(
    stats: CorporateTreeStats,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
    onIncorporateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            listOf(CyanAccent.copy(alpha = 0.4f), BorderSubtle)
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(CyanAccent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CorporateFare,
                            contentDescription = "Corporate Tree",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "CORPORATE HIERARCHY TREE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${stats.totalEntities} Subsidiaries Across ${stats.totalTiers} Tiers",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Button(
                    onClick = onIncorporateClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_tree_incorporate")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "INCORPORATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            HorizontalDivider(color = BorderSubtle.copy(alpha = 0.5f))

            // KPI Grid: Consolidated Capital, Average Compliance, Operational Statuses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "CONSOLIDATED CAPITAL", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "$${NumberFormat.getNumberInstance(Locale.US).format(stats.consolidatedOperatingCapitalUsd.toLong())} USD",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "OPERATIONAL HEALTH", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${stats.goodStandingCount} Active • ${stats.filingDueCount} Action Due",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stats.filingDueCount == 0) EmeraldPositive else GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "AVG COMPLIANCE", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${stats.averageComplianceScore}/100",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Tree Toggles: Expand All / Collapse All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onExpandAll,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.UnfoldMore, contentDescription = "Expand", modifier = Modifier.size(12.dp), tint = TextSecondary)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "Expand All", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(
                    onClick = onCollapseAll,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(imageVector = Icons.Default.UnfoldLess, contentDescription = "Collapse", modifier = Modifier.size(12.dp), tint = TextSecondary)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = "Collapse All", fontSize = 9.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

/**
 * Detailed Statutory Dossier Dialog for Focused Entity
 */
@Composable
private fun EntityDossierDialog(
    entity: EntityRecord,
    onDismiss: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    var selectedNewStatus by remember { mutableStateOf(entity.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${countryCodeToFlag(entity.countryCode)} ${entity.name}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "CORPORATE STATUTORY DOSSIER",
                        fontSize = 10.sp,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Statutory Governance Details Grid
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceDark,
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DossierRow("Jurisdiction", entity.jurisdiction)
                        DossierRow("Corporate Form", entity.entityType)
                        DossierRow("Tax Identification", entity.taxId)
                        DossierRow("Resident Director", entity.localDirector)
                        DossierRow("Operating Capital", "${entity.baseCurrency} ${NumberFormat.getNumberInstance(Locale.US).format(entity.operatingCapital)}")
                        DossierRow("Annual Filing Deadline", entity.annualFilingDeadline)
                        DossierRow("Compliance Score", "${entity.complianceScore}/100 • Sovereign Audit Passed")
                    }
                }

                // Operational Status Switcher
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "SET OPERATIONAL STATUS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "GOOD_STANDING" to "Good Standing",
                            "ACTIVE" to "Active",
                            "FILING_DUE" to "Filing Due"
                        ).forEach { (statKey, label) ->
                            val isSel = selectedNewStatus == statKey
                            val color = if (statKey == "GOOD_STANDING" || statKey == "ACTIVE") EmeraldPositive else GoldAccent
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) color.copy(alpha = 0.25f) else SurfaceElevated)
                                    .border(1.dp, if (isSel) color else BorderSubtle, RoundedCornerShape(6.dp))
                                    .clickable { selectedNewStatus = statKey }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) color else TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUpdateStatus(selectedNewStatus) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SAVE DOSSIER", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("DISMISS", color = TextMuted)
            }
        },
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun DossierRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
        Text(text = value, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

private fun countryCodeToFlag(countryCode: String): String {
    return when (countryCode.uppercase(Locale.US)) {
        "US" -> "🇺🇸"
        "GB" -> "🇬🇧"
        "SG" -> "🇸🇬"
        "CH" -> "🇨🇭"
        "BR" -> "🇧🇷"
        "DE" -> "🇩🇪"
        "JP" -> "🇯🇵"
        "NL" -> "🇳🇱"
        "FR" -> "🇫🇷"
        "AU" -> "🇦🇺"
        else -> "🌐"
    }
}
