package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.EqualizerController
import com.example.ui.theme.LanguageManager

@Composable
fun EqualizerScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isEnabled by EqualizerController.isEnabled.collectAsState()
    val bands by EqualizerController.bands.collectAsState()
    val bandLevels by EqualizerController.bandLevels.collectAsState()
    val bassLevel by EqualizerController.bassLevel.collectAsState()
    val virtualizerLevel by EqualizerController.virtualizerLevel.collectAsState()

    val minVal = if (EqualizerController.minLevel < EqualizerController.maxLevel) EqualizerController.minLevel.toFloat() else -1500f
    val maxVal = if (EqualizerController.minLevel < EqualizerController.maxLevel) EqualizerController.maxLevel.toFloat() else 1500f
    val range = maxVal - minVal

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = LanguageManager.getString("equalizer"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Switch(
                checked = isEnabled,
                onCheckedChange = { EqualizerController.setEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Audio Frequencies block
        Text(
            text = "Fine Acoustic Tuning",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (bands.isEmpty() || bandLevels.isEmpty()) {
                    Text(
                        text = "Connect headphones or play audio to enable customized equalizers.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    bands.forEachIndexed { index, name ->
                        val currentLevel = bandLevels[index] ?: 0
                        val clampedValue = currentLevel.toFloat().coerceIn(minVal, maxVal)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "${if (currentLevel > 0) "+" else ""}${currentLevel / 100} dB",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = clampedValue,
                                onValueChange = {
                                    if (isEnabled) {
                                        EqualizerController.setBandLevel(index, it.toInt())
                                    }
                                },
                                valueRange = minVal..maxVal,
                                enabled = isEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Extra audio enhancements (Bass Boost + 3D Sound)
        Text(
            text = "OxygenOS Core Audio Effects",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Bass Boost Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = LanguageManager.getString("bass"),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$bassLevel%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = bassLevel.toFloat(),
                    onValueChange = {
                        if (isEnabled) {
                            EqualizerController.setBassLevel(it.toInt())
                        }
                    },
                    valueRange = 0f..1000f,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Virtualizer Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = LanguageManager.getString("3d_sound"),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$virtualizerLevel%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = virtualizerLevel.toFloat(),
                    onValueChange = {
                        if (isEnabled) {
                            EqualizerController.setVirtualizerLevel(it.toInt())
                        }
                    },
                    valueRange = 0f..1000f,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Save and Close button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(text = "Apply Acoustics", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
