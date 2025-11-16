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
 * Bottom Sheet selector de formas para botones pequeños (radio, download, sleep, more)
 * Diseño Material 3 Expressive con animaciones sutiles y jerarquía clara
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SmallButtonShapeBottomSheet(
    selectedShapeName: String,
    onShapeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag handle personalizado Material 3
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            // Título con jerarquía clara
            Text(
                text = "Small Buttons Shape",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Grid de formas con espaciado coherente
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 480.dp)
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
        }
    }
}

/**
 * Item individual de forma con animaciones sutiles y feedback visual
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SmallButtonShapeItem(
    shapeOption: SmallButtonShapeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Animación de escala suave
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 300f
        ),
        label = "scale"
    )

    // Transición de color del contenedor
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(250),
        label = "backgroundColor"
    )

    // Color del borde con transición
    val borderColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.primary
        else
            Color.Transparent,
        animationSpec = tween(250),
        label = "borderColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .scale(scale)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        // Preview de la forma
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(shapeOption.shape.toShape())
                .background(
                    if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
        )

        // Nombre de la forma
        Text(
            text = shapeOption.displayName,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            color = if (isSelected)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Botón para abrir el bottom sheet de formas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallButtonShapeSelectorButton(
    currentShapeName: String,
    onShapeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    PreferenceEntry(
        title = { Text(stringResource(R.string.SmallButtonsShape)) },
        description = currentShapeName,
        icon = {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.scatter_plot),
                contentDescription = null
            )
        },
        onClick = { showBottomSheet = true },
        modifier = modifier
    )

    if (showBottomSheet) {
        SmallButtonShapeBottomSheet(
            selectedShapeName = currentShapeName,
            onShapeSelected = onShapeSelected,
            onDismiss = { showBottomSheet = false },
            sheetState = sheetState
        )
    }
}