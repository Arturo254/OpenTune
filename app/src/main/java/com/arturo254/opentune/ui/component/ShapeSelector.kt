package com.arturo254.opentune.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShapeLine
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.arturo254.opentune.R

data class SmallButtonShapeOption(
    val name: String,
    val shape: RoundedPolygon,
    val displayName: String
)

enum class ShapeType {
    SMALL_BUTTONS,
    PLAY_PAUSE,
    MINIPLAYER_THUMBNAIL
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UnifiedShapeBottomSheet(
    selectedSmallButtonsShape: String,
    selectedMiniPlayerShape: String,
    onSmallButtonsShapeSelected: (String) -> Unit,
    onMiniPlayerShapeSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    initialTab: ShapeType = ShapeType.SMALL_BUTTONS
) {
    var selectedTabIndex by remember { mutableIntStateOf(
        when (initialTab) {
            ShapeType.SMALL_BUTTONS -> 0
            ShapeType.PLAY_PAUSE -> 1
            ShapeType.MINIPLAYER_THUMBNAIL -> 2
        }
    ) }

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

    val tabTitles = listOf("Small Buttons", "Play/Pause", "MiniPlayer")
    val tabIcons = listOf(Icons.Default.ShapeLine, Icons.Default.PlayArrow, Icons.Default.Album)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Shape Selector",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Customize shapes for all buttons",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = tabIcons[index],
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            val currentSelectedShape = when (selectedTabIndex) {
                0 -> selectedSmallButtonsShape
                1 -> selectedMiniPlayerShape
                else -> selectedSmallButtonsShape
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(availableShapes) { shapeOption ->
                    SmallButtonShapeItem(
                        shapeOption = shapeOption,
                        isSelected = shapeOption.name == currentSelectedShape,
                        onClick = {
                            when (selectedTabIndex) {
                                0 -> onSmallButtonsShapeSelected(shapeOption.name)
                                1 -> onMiniPlayerShapeSelected(shapeOption.name)
                            }
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
 * Botón selector unificado para todas las formas (UN SOLO BOTÓN)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UnifiedShapeSelectorButton(
    smallButtonsShape: String,
    miniPlayerShape: String,
    onSmallButtonsShapeSelected: (String) -> Unit,
    onMiniPlayerShapeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // UN SOLO BOTÓN que muestra un resumen
    PreferenceEntry(
        title = { Text("Shape Selector") },
        description = "Customize button shapes",
        icon = {
            Icon(
                painter = painterResource(R.drawable.scatter_plot),
                contentDescription = null
            )
        },
        onClick = {
            showBottomSheet = true
        },
        modifier = modifier
    )

    if (showBottomSheet) {
        UnifiedShapeBottomSheet(
            selectedSmallButtonsShape = smallButtonsShape,
            selectedMiniPlayerShape = miniPlayerShape,
            onSmallButtonsShapeSelected = onSmallButtonsShapeSelected,
            onMiniPlayerShapeSelected = onMiniPlayerShapeSelected,
            onDismiss = { showBottomSheet = false },
            sheetState = sheetState,
            initialTab = ShapeType.SMALL_BUTTONS
        )
    }
}