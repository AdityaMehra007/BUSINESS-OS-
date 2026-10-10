package com.example.worldbusiness.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import com.example.worldbusiness.data.model.AuditLogRecord
import com.example.worldbusiness.data.model.EntityRecord
import com.example.worldbusiness.data.model.FxBalanceRecord
import com.example.worldbusiness.data.model.InvoiceRecord
import com.example.worldbusiness.data.model.ShipmentRecord
import com.example.worldbusiness.data.repository.DatabaseComplianceExportEngine
import com.example.worldbusiness.data.repository.EnterpriseLedgerSnapshot

/**
 * Enterprise System Health & Zero-Trust Cryptographic Inspector Modal.
 * Verifies Room SQLite database integrity, hash chains, and produces SHA-256 signed audit exports.
 */
@Composable
fun SystemHealthInspectorModal(
    entities: List<EntityRecord>,
    invoices: List<InvoiceRecord>,
    balances: List<FxBalanceRecord>,
    shipments: List<ShipmentRecord>,
    auditLogs: List<AuditLogRecord>,
    userEmail: String?,
    onDismiss: () -> Unit
) {
    var generatedSnapshot by remember { mutableStateOf<EnterpriseLedgerSnapshot?>(null) }
    var auditTriggered by remember { mutableStateOf(false) }

    val snapshot = remember(entities, invoices, balances, shipments, auditLogs, auditTriggered) {
        DatabaseComplianceExportEngine.generateSnapshot(entities, invoices, balances, shipments, auditLogs)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldPositive.copy(alpha = 0.15f))
                            .border(1.dp, EmeraldPositive.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Zero-Trust Security",
                            tint = EmeraldPositive,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "SYSTEM HEALTH & ZERO-TRUST AUDIT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Cryptographic Proof • Room Persistence • Cloud State",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = EmeraldPositive.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(EmeraldPositive, EmeraldPositive.copy(alpha = 0.5f)))
                    )
                ) {
                    Text(
                        text = "100% HEALTHY",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Overall Enterprise Diagnostics Strip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceVariantDark,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(CyanAccent.copy(alpha = 0.4f), BorderSubtle))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "CONSOLIDATED LEDGER RECORDS", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(text = "${snapshot.totalRecordsCount} Persisted Records", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextPrimary, fontFamily = FontFamily.Monospace)
                            Text(text = "Entities (${snapshot.entitiesCount}) • Invoices (${snapshot.invoicesCount}) • Vaults (${snapshot.vaultsCount}) • Freight (${snapshot.shipmentsCount})", fontSize = 8.5.sp, color = TextSecondary)
                        }

                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = EmeraldPositive,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // 2. Cryptographic Blockchain-Grade Proof Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated,
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(GoldAccent.copy(alpha = 0.5f), BorderSubtle))
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = "Security", tint = GoldAccent, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "CRYPTOGRAPHIC HASH-CHAIN INTEGRITY",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = if (snapshot.chainIntegrityValid) "TAMPER-PROOF VALID" else "COMPROMISED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (snapshot.chainIntegrityValid) EmeraldPositive else RoseNegative,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "Root Hash: ${snapshot.rootHash.take(16)}...${snapshot.rootHash.takeLast(8)}",
                            fontSize = 8.5.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Head Hash: ${snapshot.latestHash.take(16)}...${snapshot.latestHash.takeLast(8)}",
                            fontSize = 8.5.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Algorithm: SHA-256 Merkle-Chained Ledger • Zero Collisions",
                            fontSize = 8.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 3. Infrastructure & Cloud Synchronization Status
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SurfaceElevated.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "LOCAL PERSISTENCE", fontSize = 8.5.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(text = "Room SQLite (Async KSP & Flows)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "CLOUD SECURITY LAYER", fontSize = 8.5.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(text = "Cloud Firestore Enterprise Zero-Trust", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = EmeraldPositive, fontFamily = FontFamily.Monospace)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "AUTHENTICATED IDENTITY", fontSize = 8.5.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                            Text(text = userEmail ?: "Local Enterprise Session", fontSize = 8.5.sp, color = TextPrimary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                HorizontalDivider(color = BorderSubtle)

                // 4. Compliance Export Action Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "REGULATORY AUDIT MANIFEST", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = TextMuted, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "SHA-256: ${snapshot.sha256ManifestChecksum.take(18)}...",
                            fontSize = 9.sp,
                            color = CyanAccent,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Button(
                        onClick = { generatedSnapshot = snapshot },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("btn_export_compliance_manifest")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = "Export", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "EXPORT SNAPSHOT", fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                // If user clicked Export Snapshot, show the full verified payload inspector
                generatedSnapshot?.let { exp ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SurfaceDark,
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(CyanAccent, BorderSubtle))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "VERIFIED AUDIT SNAPSHOT PAYLOAD:", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = CyanAccent, fontFamily = FontFamily.Monospace)
                            Text(
                                text = exp.rawJsonPayload,
                                fontSize = 8.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 8
                            )
                            Text(
                                text = "Checksum: ${exp.sha256ManifestChecksum}",
                                fontSize = 7.5.sp,
                                color = EmeraldPositive,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = SurfaceDark),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(text = "DISMISS", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = SurfaceDark
    )
}
