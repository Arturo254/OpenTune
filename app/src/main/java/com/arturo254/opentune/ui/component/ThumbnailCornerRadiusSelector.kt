package com.arturo254.opentune.ui.component

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arturo254.opentune.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


// Extensión de contexto para DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

object AppConfig {
    private val THUMBNAIL_CORNER_RADIUS_KEY = floatPreferencesKey("thumbnail_corner_radius")

    // Guardar el valor de thumbnail corner radius
    suspend fun saveThumbnailCornerRadius(context: Context, radius: Float) {
        context.dataStore.edit { preferences ->
            preferences[THUMBNAIL_CORNER_RADIUS_KEY] = radius
        }
    }

    // Obtener el valor de thumbnail corner radius, o un valor por defecto si no está presente
    suspend fun getThumbnailCornerRadius(context: Context, defaultValue: Float = 16f): Float {
        return context.dataStore.data
            .map { preferences ->
                preferences[THUMBNAIL_CORNER_RADIUS_KEY] ?: defaultValue
            }.first()
    }
}


@Composable
fun ThumbnailCornerRadiusSelectorButton(
    modifier: Modifier = Modifier,
    onRadiusSelected: (Float) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var currentRadius by remember { mutableStateOf(24f) }

    // Cargar el valor guardado al iniciar
    LaunchedEffect(Unit) {
        currentRadius = AppConfig.getThumbnailCornerRadius(context)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .clickable(
                enabled = true,
                onClick = { showDialog = true }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.line_curve),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(
                    id = R.string.customize_thumbnail_corner_radius,
                    currentRadius.roundToInt()
                ),

                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
//    }

    // Modal para seleccionar el radio
    if (showDialog) {
        ThumbnailCornerRadiusModal(
            initialRadius = currentRadius,
            onDismiss = { showDialog = false },
            onRadiusSelected = { newRadius ->
                currentRadius = newRadius
                onRadiusSelected(newRadius)
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThumbnailCornerRadiusModal(
    initialRadius: Float,
    onDismiss: () -> Unit,
    onRadiusSelected: (Float) -> Unit
) {
    val context = LocalContext.current
    var thumbnailCornerRadius by remember { mutableStateOf(initialRadius) }
    val presetValues = listOf(0f, 8f, 16f, 24f, 32f, 40f)
    var customValue by remember { mutableStateOf("") }
    var isCustomSelected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isCustomSelected = !presetValues.contains(initialRadius)
        if (isCustomSelected) {
            customValue = initialRadius.roundToInt().toString()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {

        BoxWithConstraints {
            val screenWidth = maxWidth
            val dialogWidth = when {
                screenWidth > 840.dp -> 0.5f
                screenWidth > 600.dp -> 0.7f
                else -> 0.95f
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth(dialogWidth)
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(30.dp))
                    .padding(16.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(modifier = Modifier.height(16.dp))

                    // Previsualización con imagen de fondo
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(thumbnailCornerRadius.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Imagen de ejemplo con bordes redondeados
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
                        ) {
                            // Usando un painterResource para cargar una imagen local
                            Image(
                                painter = painterResource(id = R.drawable.previewalbum),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Overlay para mostrar el valor del radio
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${thumbnailCornerRadius.roundToInt()}dp",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Chips con Layout mejorado y adaptable usando un enfoque alternativo a FlowRow
                    ChipsGrid(
                        values = presetValues,
                        selectedValue = if (isCustomSelected) null else thumbnailCornerRadius,
                        onValueSelected = { value ->
                            thumbnailCornerRadius = value
                            isCustomSelected = false
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Campo personalizado con mejor disposición
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isCustomSelected,
                            onClick = {
                                isCustomSelected = true
                                if (customValue.isEmpty()) {
                                    customValue = thumbnailCornerRadius.roundToInt().toString()
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(id = R.string.custom),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = customValue,
                            onValueChange = { newValue ->
                                // Solo aceptar números
                                if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                    customValue = newValue
                                    newValue.toIntOrNull()?.let { intValue ->
                                        // Limitar a 45
                                        val limitedValue = minOf(intValue, 45).toFloat()
                                        thumbnailCornerRadius = limitedValue
                                        isCustomSelected = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            enabled = isCustomSelected,
                            label = {
                                Text(
                                    text = stringResource(id = R.string.custom_value),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            trailingIcon = {
                                Text(
                                    text = "dp",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            shape = MaterialTheme.shapes.small
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sección del slider mejorada para más feedback
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(id = R.string.adjust_radius),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Slider(
                                value = thumbnailCornerRadius,
                                onValueChange = { newValue ->
                                    thumbnailCornerRadius = newValue
                                    customValue = newValue.roundToInt().toString()
                                    isCustomSelected = !presetValues.contains(newValue)
                                },
                                valueRange = 0f..45f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary,
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )

                            Text(
                                text = "45",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = stringResource(
                                id = R.string.corner_radius_label,
                                thumbnailCornerRadius.roundToInt().toString()
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Divisor para separar visualmente los botones de acción
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones de acción con mejor UX
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.cancel),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Button(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    AppConfig.saveThumbnailCornerRadius(
                                        context,
                                        thumbnailCornerRadius
                                    )
                                }
                                onRadiusSelected(thumbnailCornerRadius)
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.apply),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipsGrid(
    values: List<Float>,
    selectedValue: Float?,
    onValueSelected: (Float) -> Unit
) {
    val chunkedValues = values.chunked(3) // Divide en filas de 3 elementos

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chunkedValues.forEach { rowValues ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowValues.forEachIndexed { index, value ->
                    FilterChip(
                        selected = selectedValue == value,
                        onClick = { onValueSelected(value) },
                        label = {
                            Text(
                                text = "${value.roundToInt()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    if (index < rowValues.size - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}