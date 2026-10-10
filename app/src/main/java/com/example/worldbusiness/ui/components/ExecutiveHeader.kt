package com.example.worldbusiness.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ExecutiveHeader(
    consolidatedCashUsd: Double,
    activeEntitiesCount: Int,
    userEmail: String? = null,
    onTriggerCloudSync: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
    onOpenSystemHealth: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alphaPulse"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .border(width = 1.dp, color = BorderSubtle)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("executive_header")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, CyanAccent.copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Global Globe",
                        tint = CyanAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "WORLD OS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(EmeraldPositive.copy(alpha = alphaPulse))
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Cloud Firestore Sync Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, if (userEmail != null) CyanAccent.copy(alpha = 0.5f) else BorderSubtle, RoundedCornerShape(4.dp))
                        .clickable(enabled = onTriggerCloudSync != null) { onTriggerCloudSync?.invoke() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag("cloud_firestore_sync_chip"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (userEmail != null) Icons.Default.CloudDone else Icons.Default.CloudSync,
                        contentDescription = "Cloud Sync Status",
                        tint = if (userEmail != null) CyanAccent else GoldAccent,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (userEmail != null) "SYNC" else "LOCAL",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (userEmail != null) CyanAccent else GoldAccent,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Zero-Trust Security & System Health Inspector Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, EmeraldPositive.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .clickable(enabled = onOpenSystemHealth != null) { onOpenSystemHealth?.invoke() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                        .testTag("btn_system_health_audit"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = "Zero-Trust Integrity",
                        tint = EmeraldPositive,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "100%",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPositive,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Multi-Market Clocks ticker row (streamlined compact bar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MarketClock(city = "NYC", time = "10:24", isOpen = true)
            MarketClock(city = "LDN", time = "15:24", isOpen = true)
            MarketClock(city = "ZUR", time = "16:24", isOpen = true)
            MarketClock(city = "SGP", time = "22:24", isOpen = false)
            MarketClock(city = "TYO", time = "23:24", isOpen = false)
        }
    }
}

@Composable
private fun MarketClock(city: String, time: String, isOpen: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (isOpen) EmeraldPositive else GoldAccent)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "$city $time",
            fontSize = 8.5.sp,
            color = if (isOpen) TextSecondary else TextMuted,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isOpen) FontWeight.Medium else FontWeight.Normal
        )
    }
}
