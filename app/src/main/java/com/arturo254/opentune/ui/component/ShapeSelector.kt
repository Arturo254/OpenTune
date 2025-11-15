package com.arturo254.opentune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.graphics.shapes.RoundedPolygon
import com.arturo254.opentune.R

/**
 * Data class que representa una forma disponible para botones pequeños
 */
data class SmallButtonShapeOption(
    val name: String,
    val shape: RoundedPolygon,
    val displayName: String
)

/**
 * Selector de formas para botones pequeños (radio, download, sleep, more)
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SmallButtonShapeSelectorDialog(
    selectedShapeName: String,
    onShapeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Lista COMPLETA de formas apropiadas para botones pequeños
    val availableShapes = remember {
        listOf(
            SmallButtonShapeOption("Pill", MaterialShapes.Pill, "Pill"),
            SmallButtonShapeOption("Circle", MaterialShapes.Circle, "Circle"),
            SmallButtonShapeOption("Square", MaterialShapes.Square, "Square"),
            SmallButtonShapeOption("Diamond", MaterialShapes.Diamond, "Diamond"),
            SmallButtonShapeOption("Pentagon", MaterialShapes.Pentagon, "Pentagon"),
            SmallButtonShapeOption("Heart", MaterialShapes.Heart, "Heart"),
            SmallButtonShapeOption("Oval", MaterialShapes.Oval, "Oval"),
            SmallButtonShapeOption("Arch", MaterialShapes.Arch, "Arch"),
            SmallButtonShapeOption("SemiCircle", MaterialShapes.SemiCircle, "Semicircle"),
            SmallButtonShapeOption("Triangle", MaterialShapes.Triangle, "Triangle"),
            SmallButtonShapeOption("Arrow", MaterialShapes.Arrow, "Arrow"),
            SmallButtonShapeOption("Fan", MaterialShapes.Fan, "Fan"),
            SmallButtonShapeOption("Gem", MaterialShapes.Gem, "Gem"),
            SmallButtonShapeOption("Bun", MaterialShapes.Bun, "Bun"),
            SmallButtonShapeOption("Ghostish", MaterialShapes.Ghostish, "Ghost-ish"),
            SmallButtonShapeOption("Cookie4Sided", MaterialShapes.Cookie4Sided, "Cookie 4"),
            SmallButtonShapeOption("Cookie6Sided", MaterialShapes.Cookie6Sided, "Cookie 6"),
            SmallButtonShapeOption("Cookie7Sided", MaterialShapes.Cookie7Sided, "Cookie 7"),
            SmallButtonShapeOption("Cookie9Sided", MaterialShapes.Cookie9Sided, "Cookie 9"),
            SmallButtonShapeOption("Cookie12Sided", MaterialShapes.Cookie12Sided, "Cookie 12"),
            SmallButtonShapeOption("Clover4Leaf", MaterialShapes.Clover4Leaf, "Clover 4"),
            SmallButtonShapeOption("Clover8Leaf", MaterialShapes.Clover8Leaf, "Clover 8"),
            SmallButtonShapeOption("Sunny", MaterialShapes.Sunny, "Sunny"),
            SmallButtonShapeOption("VerySunny", MaterialShapes.VerySunny, "Very Sunny"),
            SmallButtonShapeOption("Burst", MaterialShapes.Burst, "Burst"),
            SmallButtonShapeOption("SoftBurst", MaterialShapes.SoftBurst, "Soft Burst"),
            SmallButtonShapeOption("Boom", MaterialShapes.Boom, "Boom"),
            SmallButtonShapeOption("SoftBoom", MaterialShapes.SoftBoom, "Soft Boom"),
            SmallButtonShapeOption("Flower", MaterialShapes.Flower, "Flower"),
            SmallButtonShapeOption("PixelCircle", MaterialShapes.PixelCircle, "Pixel Circle"),
            SmallButtonShapeOption("PixelTriangle", MaterialShapes.PixelTriangle, "Pixel Triangle"),
            SmallButtonShapeOption("Puffy", MaterialShapes.Puffy, "Puffy"),
            SmallButtonShapeOption("PuffyDiamond", MaterialShapes.PuffyDiamond, "Puffy Diamond"),
            SmallButtonShapeOption("Slanted", MaterialShapes.Slanted, "Slanted"),
            SmallButtonShapeOption("ClamShell", MaterialShapes.ClamShell, "Clam Shell")
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Select Small Buttons Shape",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    items(availableShapes) { shapeOption ->
                        SmallButtonShapeItem(
                            shapeOption = shapeOption,
                            isSelected = shapeOption.name == selectedShapeName,
                            onClick = {
                                onShapeSelected(shapeOption.name)
                                onDismiss()
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

/**
 * Item individual de forma para botones pequeños
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SmallButtonShapeItem(
    shapeOption: SmallButtonShapeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(200),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "backgroundColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(shapeOption.shape.toShape())
                .background(MaterialTheme.colorScheme.primary)
        )

        Text(
            text = shapeOption.displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Botón para abrir el selector de formas de botones pequeños
 */
@Composable
fun SmallButtonShapeSelectorButton(
    currentShapeName: String,
    onShapeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    PreferenceEntry(
        title = { Text("Small Buttons Shape") },
        description = currentShapeName,
        icon = {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.scatter_plot),
                contentDescription = null
            )
        },
        onClick = { showDialog = true },
        modifier = modifier
    )

    if (showDialog) {
        SmallButtonShapeSelectorDialog(
            selectedShapeName = currentShapeName,
            onShapeSelected = onShapeSelected,
            onDismiss = { showDialog = false }
        )
    }
}