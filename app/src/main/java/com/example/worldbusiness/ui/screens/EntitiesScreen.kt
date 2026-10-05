package com.example.worldbusiness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.ui.components.CorporateHierarchyTreeView
import com.example.worldbusiness.ui.components.Formatters
import java.util.Locale

enum class EntityViewMode(val label: String) {
    HIERARCHY_TREE("Hierarchy Tree"),
    DIRECTORY("Entity Directory")
}

@Composable
fun EntitiesScreen(
    entities: List<EntityRecord>,
    onAddEntity: (
        name: String,
        jurisdiction: String,
        countryCode: String,
        entityType: String,
        taxId: String,
        baseCurrency: String,
        capital: Double,
        filingDeadline: String,
        director: String
    ) -> Unit,
    onUpdateEntityStatus: (EntityRecord, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(EntityViewMode.HIERARCHY_TREE) }
    var searchQuery by remember { mutableStateOf("") }
    var showIncorporateDialog by remember { mutableStateOf(false) }

    val filteredEntities = entities.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.jurisdiction.contains(searchQuery, ignoreCase = true) ||
                it.taxId.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("entities_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Screen Top Info & Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CORPORATE STRUCTURING",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyanAccent,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${entities.size} Sovereign Subsidiaries",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
            }

            Button(
                onClick = { showIncorporateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("btn_incorporate_entity")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Incorporate",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "INCORPORATE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // View Mode Selector Tabs (Hierarchy Tree vs Entity Directory)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EntityViewMode.values().forEach { mode ->
                val isSelected = viewMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CyanAccent else SurfaceDark)
                        .border(1.dp, if (isSelected) CyanAccent else BorderSubtle, RoundedCornerShape(8.dp))
                        .clickable { viewMode = mode }
                        .padding(vertical = 9.dp)
                        .testTag("entity_tab_${mode.name.lowercase(Locale.US)}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) SurfaceDark else TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Search bar (for Directory mode; Tree mode has integrated GlobalHierarchySearchBar)
        if (viewMode == EntityViewMode.DIRECTORY) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Filter by jurisdiction, entity name, or tax ID...",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_entities"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = CyanAccent,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (viewMode) {
            EntityViewMode.HIERARCHY_TREE -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        CorporateHierarchyTreeView(
                            entities = entities,
                            searchQuery = searchQuery,
                            onIncorporateClick = { showIncorporateDialog = true },
                            onUpdateEntityStatus = onUpdateEntityStatus
                        )
                    }
                }
            }

            EntityViewMode.DIRECTORY -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredEntities, key = { it.id }) { entity ->
                        EntityCard(entity = entity)
                    }
                }
            }
        }
    }

    if (showIncorporateDialog) {
        IncorporateEntityDialog(
            onDismiss = { showIncorporateDialog = false },
            onConfirm = { name, jurisdiction, code, type, taxId, curr, cap, deadline, dir ->
                onAddEntity(name, jurisdiction, code, type, taxId, curr, cap, deadline, dir)
                showIncorporateDialog = false
            }
        )
    }
}

@Composable
private fun EntityCard(entity: EntityRecord) {
    val statusColor = Formatters.getStatusColor(entity.status)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("entity_card_${entity.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entity.countryCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entity.jurisdiction,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = entity.status.replace("_", " "),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = entity.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "${entity.entityType} • ${entity.taxId}",
                fontSize = 11.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceVariantDark)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "OPERATING CAPITAL", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = Formatters.formatCurrency(entity.operatingCapital, entity.baseCurrency),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column {
                    Text(text = "STATUTORY DEADLINE", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = entity.annualFilingDeadline,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "OECD COMPLIANCE", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    Text(
                        text = "${entity.complianceScore}/100",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Officer: ${entity.localDirector}",
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun IncorporateEntityDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        jurisdiction: String,
        code: String,
        type: String,
        taxId: String,
        currency: String,
        capital: Double,
        deadline: String,
        director: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var jurisdiction by remember { mutableStateOf("Ireland (Dublin)") }
    var code by remember { mutableStateOf("IE") }
    var type by remember { mutableStateOf("Designated Activity Company (DAC)") }
    var taxId by remember { mutableStateOf("IE 9841203T") }
    var currency by remember { mutableStateOf("EUR") }
    var capitalStr by remember { mutableStateOf("500000") }
    var deadline by remember { mutableStateOf("Nov 30, 2026") }
    var director by remember { mutableStateOf("Ciaran Walsh (Local Director)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = {
            Text(
                text = "INCORPORATE NEW ENTITY",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Entity Legal Name", fontSize = 11.sp) },
                    placeholder = { Text("e.g. OmniGlobal Ireland DAC") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = jurisdiction,
                        onValueChange = { jurisdiction = it },
                        label = { Text("Jurisdiction", fontSize = 11.sp) },
                        modifier = Modifier.weight(2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("ISO", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text("Legal Entity Type", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = taxId,
                        onValueChange = { taxId = it },
                        label = { Text("Tax Registration / VAT / EIN", fontSize = 11.sp) },
                        modifier = Modifier.weight(2f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        label = { Text("Currency", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
                OutlinedTextField(
                    value = capitalStr,
                    onValueChange = { capitalStr = it },
                    label = { Text("Operating Capital Amount", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                OutlinedTextField(
                    value = director,
                    onValueChange = { director = it },
                    label = { Text("Resident Director / Officer", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val cap = capitalStr.toDoubleOrNull() ?: 100000.0
                        onConfirm(name, jurisdiction, code, type, taxId, currency, cap, deadline, director)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark)
            ) {
                Text("RATIFY & INCORPORATE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextSecondary)
            }
        }
    )
}
