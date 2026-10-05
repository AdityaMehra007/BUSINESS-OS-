package com.example.worldbusiness.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.worldbusiness.data.model.RegionalHub
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun InteractiveWorldMap(
    hubs: List<RegionalHub>,
    selectedHub: RegionalHub?,
    onHubSelected: (RegionalHub?) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val waveRadius by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveRadius"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .testTag("interactive_world_map")
    ) {
        // Map Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceVariantDark)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = "Radar",
                    tint = CyanAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "GLOBAL LIQUIDITY & OPERATIONS RADAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "${hubs.size} ACTIVE HUBS",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = EmeraldPositive,
                fontFamily = FontFamily.Monospace
            )
        }

        // Radar Canvas Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(hubs) {
                        detectTapGestures { tapOffset ->
                            val width = size.width
                            val height = size.height

                            // Find closest hub within threshold
                            var closest: RegionalHub? = null
                            var minDistance = Float.MAX_VALUE

                            hubs.forEach { hub ->
                                val hx = hub.coordinatesNormX * width
                                val hy = hub.coordinatesNormY * height
                                val dist = sqrt((tapOffset.x - hx).pow(2) + (tapOffset.y - hy).pow(2))
                                if (dist < 80f && dist < minDistance) {
                                    minDistance = dist
                                    closest = hub
                                }
                            }
                            onHubSelected(closest)
                        }
                    }
            ) {
                val width = size.width
                val height = size.height

                // Draw background radar grid lines (latitude / longitude)
                val gridColor = Color(0x1A00E5FF)
                val numH = 5
                val numV = 8

                for (i in 1..numH) {
                    val y = height * (i.toFloat() / (numH + 1))
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                for (i in 1..numV) {
                    val x = width * (i.toFloat() / (numV + 1))
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // Draw stylized continent outlines / clusters
                val landColor = Color(0x1E3B82F6)
                // North America cluster
                drawOval(
                    color = landColor,
                    topLeft = Offset(width * 0.15f, height * 0.20f),
                    size = androidx.compose.ui.geometry.Size(width * 0.22f, height * 0.32f)
                )
                // South America cluster
                drawOval(
                    color = landColor,
                    topLeft = Offset(width * 0.28f, height * 0.55f),
                    size = androidx.compose.ui.geometry.Size(width * 0.16f, height * 0.35f)
                )
                // Europe / EMEA cluster
                drawOval(
                    color = landColor,
                    topLeft = Offset(width * 0.44f, height * 0.18f),
                    size = androidx.compose.ui.geometry.Size(width * 0.18f, height * 0.30f)
                )
                // Africa cluster
                drawOval(
                    color = landColor,
                    topLeft = Offset(width * 0.46f, height * 0.45f),
                    size = androidx.compose.ui.geometry.Size(width * 0.18f, height * 0.35f)
                )
                // Asia / APAC cluster
                drawOval(
                    color = landColor,
                    topLeft = Offset(width * 0.65f, height * 0.22f),
                    size = androidx.compose.ui.geometry.Size(width * 0.28f, height * 0.42f)
                )

                // Trade corridor connecting arcs
                val arcColor = Color(0x4000E5FF)
                if (hubs.size >= 2) {
                    for (i in 0 until hubs.size - 1) {
                        val h1 = hubs[i]
                        val h2 = hubs[i + 1]
                        val p1 = Offset(h1.coordinatesNormX * width, h1.coordinatesNormY * height)
                        val p2 = Offset(h2.coordinatesNormX * width, h2.coordinatesNormY * height)

                        val midX = (p1.x + p2.x) / 2
                        val midY = ((p1.y + p2.y) / 2) - 30f

                        val path = Path().apply {
                            moveTo(p1.x, p1.y)
                            quadraticBezierTo(midX, midY, p2.x, p2.y)
                        }
                        drawPath(
                            path = path,
                            color = arcColor,
                            style = Stroke(
                                width = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        )
                    }
                }

                // Draw each Hub node
                hubs.forEach { hub ->
                    val cx = hub.coordinatesNormX * width
                    val cy = hub.coordinatesNormY * height
                    val isSelected = selectedHub?.code == hub.code

                    // Pulsing radar wave
                    drawCircle(
                        color = (if (isSelected) GoldAccent else CyanAccent).copy(alpha = waveAlpha),
                        radius = waveRadius + (if (isSelected) 10f else 0f),
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )

                    // Hub node core
                    drawCircle(
                        color = if (isSelected) GoldAccent else CyanAccent,
                        radius = if (isSelected) 8f else 5.5f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = if (isSelected) 3.5f else 2.5f,
                        center = Offset(cx, cy)
                    )
                }
            }

            // Hub pills on bottom of the canvas
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                hubs.forEach { hub ->
                    val isSelected = selectedHub?.code == hub.code
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) GoldAccent.copy(alpha = 0.2f) else SurfaceElevated.copy(alpha = 0.8f))
                            .border(
                                1.dp,
                                if (isSelected) GoldAccent else BorderSubtle,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onHubSelected(hub) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .testTag("hub_pill_${hub.code}")
                    ) {
                        Text(
                            text = "${hub.code} (${hub.revenueContributionPercent}%)",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) GoldAccent else TextSecondary
                        )
                    }
                }
            }
        }

        // Hub Detail Inspection Card
        AnimatedVisibility(visible = selectedHub != null) {
            selectedHub?.let { hub ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceElevated)
                        .padding(12.dp)
                        .testTag("hub_inspector_${hub.code}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(CyanAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${hub.name} // ${hub.city.uppercase()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        IconButton(
                            onClick = { onHubSelected(null) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Inspector",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HubMetric(label = "TIME", value = hub.localTimeStr)
                        HubMetric(label = "HEADCOUNT", value = "${hub.activeHeadcount} Global Staff")
                        HubMetric(label = "ENTITIES", value = "${hub.activeEntityCount} Subsidiaries")
                        HubMetric(label = "REVENUE", value = "${hub.revenueContributionPercent}% Consolidated")
                    }
                }
            }
        }
    }
}

@Composable
private fun HubMetric(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            color = TextMuted,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = CyanAccent,
            fontFamily = FontFamily.Monospace
        )
    }
}
