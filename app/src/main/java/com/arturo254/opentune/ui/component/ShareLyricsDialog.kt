package com.arturo254.opentune.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import com.arturo254.opentune.R
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.utils.ComposeToImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLyricsDialog(
    lyricsText: String,
    songTitle: String,
    artists: String,
    mediaMetadata: MediaMetadata?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )

    var showColorPickerSheet by remember { mutableStateOf(false) }
    var showProgressDialog by remember { mutableStateOf(false) }

    if (!showColorPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.media3_icon_share),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_lyrics),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$songTitle • $artists",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                val songLink = "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
                                putExtra(Intent.EXTRA_TEXT, "\"$lyricsText\"\n\n$songTitle - $artists\n$songLink")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_lyrics)))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.media3_icon_share),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_text),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedButton(
                        onClick = { showColorPickerSheet = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.image),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.share_as_image),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showColorPickerSheet) {
        ShareLyricsImageCustomizationSheet(
            lyricsText = lyricsText,
            songTitle = songTitle,
            artists = artists,
            mediaMetadata = mediaMetadata,
            onDismiss = {
                showColorPickerSheet = false
                onDismiss()
            },
            onBack = { showColorPickerSheet = false },
            showProgressDialog = showProgressDialog,
            onShowProgressDialog = { showProgressDialog = it }
        )
    }

    if (showProgressDialog) {
        ModalBottomSheet(
            onDismissRequest = { },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareLyricsImageCustomizationSheet(
    lyricsText: String,
    songTitle: String,
    artists: String,
    mediaMetadata: MediaMetadata?,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    showProgressDialog: Boolean,
    onShowProgressDialog: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val coverUrl = mediaMetadata?.thumbnailUrl
    val paletteColors = remember { mutableStateListOf<Color>() }

    var selectedCustomization by remember { mutableStateOf(colorPresets[0].customization) }
    var isPresetSelectorExpanded by remember { mutableStateOf(true) }
    var isAdvancedSettingsExpanded by remember { mutableStateOf(false) }
    var isLayoutSettingsExpanded by remember { mutableStateOf(false) }
    var isTextSettingsExpanded by remember { mutableStateOf(false) }
    var isStyleSettingsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(coverUrl) {
        if (coverUrl != null) {
            withContext(Dispatchers.IO) {
                try {
                    val loader = ImageLoader(context)
                    val req = ImageRequest.Builder(context).data(coverUrl).allowHardware(false).build()
                    val drawable = loader.execute(req).drawable
                    if (drawable != null) {
                        val bitmap = drawable.toBitmap()
                        val palette = Palette.from(bitmap).generate()
                        val colors = listOfNotNull(
                            palette.getLightVibrantColor(Color.Black.toArgb()).takeIf { it != Color.Black.toArgb() },
                            palette.getVibrantColor(Color.Black.toArgb()).takeIf { it != Color.Black.toArgb() },
                            palette.getDarkVibrantColor(Color.Black.toArgb()).takeIf { it != Color.Black.toArgb() },
                            palette.getLightMutedColor(Color.Black.toArgb()).takeIf { it != Color.Black.toArgb() },
                            palette.getMutedColor(Color.Black.toArgb()).takeIf { it != Color.Black.toArgb() },
                            palette.getDarkMutedColor(Color.Black.toArgb()).takeIf { it != Color.Black.toArgb() }
                        ).map { Color(it) }.distinct()
                        if (colors.isNotEmpty()) {
                            paletteColors.clear()
                            paletteColors.addAll(colors)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.customize_image),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.close),
                        contentDescription = stringResource(R.string.cancel),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            CustomizationCard(
                title = stringResource(R.string.select_theme),
                isExpanded = isPresetSelectorExpanded,
                onExpandChange = { isPresetSelectorExpanded = it }
            ) {
                if (paletteColors.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.from_cover),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        items(paletteColors) { color ->
                            val customization = ImageCustomization(
                                backgroundColor = color,
                                textColor = if (color.isDark()) Color.White else Color.Black,
                                secondaryTextColor = if (color.isDark()) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                                isDark = color.isDark()
                            )
                            BottomSheetColorCustomizationItem(
                                customization = customization,
                                presetName = stringResource(R.string.cover_color),
                                isSelected = selectedCustomization.backgroundColor == customization.backgroundColor,
                                onClick = { selectedCustomization = customization }
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.presets),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(colorPresets) { preset ->
                        BottomSheetColorCustomizationItem(
                            customization = preset.customization,
                            presetName = preset.name,
                            isSelected = selectedCustomization.backgroundColor == preset.customization.backgroundColor,
                            onClick = { selectedCustomization = preset.customization }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CustomizationCard(
                title = "Style Settings",
                isExpanded = isStyleSettingsExpanded,
                onExpandChange = { isStyleSettingsExpanded = it }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingSection(title = "Cover Art Style") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedCustomization.coverArtStyle == CoverArtStyle.ROUNDED,
                                onClick = { selectedCustomization = selectedCustomization.copy(coverArtStyle = CoverArtStyle.ROUNDED) },
                                label = { Text("Rounded", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCustomization.coverArtStyle == CoverArtStyle.CIRCLE,
                                onClick = { selectedCustomization = selectedCustomization.copy(coverArtStyle = CoverArtStyle.CIRCLE) },
                                label = { Text("Circle", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCustomization.coverArtStyle == CoverArtStyle.SQUARE,
                                onClick = { selectedCustomization = selectedCustomization.copy(coverArtStyle = CoverArtStyle.SQUARE) },
                                label = { Text("Square", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    SettingSection(title = "Lyrics Style") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedCustomization.lyricsStyle == LyricsStyle.NORMAL,
                                onClick = { selectedCustomization = selectedCustomization.copy(lyricsStyle = LyricsStyle.NORMAL) },
                                label = { Text("Normal", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCustomization.lyricsStyle == LyricsStyle.ITALIC,
                                onClick = { selectedCustomization = selectedCustomization.copy(lyricsStyle = LyricsStyle.ITALIC) },
                                label = { Text("Italic", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCustomization.lyricsStyle == LyricsStyle.CONDENSED,
                                onClick = { selectedCustomization = selectedCustomization.copy(lyricsStyle = LyricsStyle.CONDENSED) },
                                label = { Text("Condensed", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    SwitchSetting(
                        title = "Show Accent Line",
                        checked = selectedCustomization.showAccentLine,
                        onCheckedChange = {
                            selectedCustomization = selectedCustomization.copy(
                                showAccentLine = it,
                                accentColor = if (it && selectedCustomization.accentColor == null)
                                    selectedCustomization.textColor else selectedCustomization.accentColor
                            )
                        }
                    )

                    SliderSetting(
                        title = "Line Spacing",
                        value = selectedCustomization.lyricsLineSpacing,
                        valueRange = 1.0f..1.8f,
                        steps = 15,
                        onValueChange = { selectedCustomization = selectedCustomization.copy(lyricsLineSpacing = it) },
                        valueLabel = String.format("%.1f", selectedCustomization.lyricsLineSpacing)
                    )

                    SliderSetting(
                        title = "Element Spacing",
                        value = selectedCustomization.spacingBetweenElements,
                        valueRange = 8f..32f,
                        steps = 23,
                        onValueChange = { selectedCustomization = selectedCustomization.copy(spacingBetweenElements = it) },
                        valueLabel = "${selectedCustomization.spacingBetweenElements.toInt()}dp"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CustomizationCard(
                title = "Background Settings",
                isExpanded = isAdvancedSettingsExpanded,
                onExpandChange = { isAdvancedSettingsExpanded = it }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingSection(title = "Background Style") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedCustomization.backgroundStyle == BackgroundStyle.SOLID,
                                onClick = { selectedCustomization = selectedCustomization.copy(backgroundStyle = BackgroundStyle.SOLID) },
                                label = { Text("Solid", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCustomization.backgroundStyle == BackgroundStyle.GRADIENT,
                                onClick = { selectedCustomization = selectedCustomization.copy(backgroundStyle = BackgroundStyle.GRADIENT) },
                                label = { Text("Gradient", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCustomization.backgroundStyle == BackgroundStyle.PATTERN,
                                onClick = { selectedCustomization = selectedCustomization.copy(backgroundStyle = BackgroundStyle.PATTERN) },
                                label = { Text("Pattern", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    SliderSetting(
                        title = "Corner Radius",
                        value = selectedCustomization.cornerRadius,
                        valueRange = 0f..40f,
                        steps = 39,
                        onValueChange = { selectedCustomization = selectedCustomization.copy(cornerRadius = it) },
                        valueLabel = "${selectedCustomization.cornerRadius.toInt()}dp"
                    )

                    if (selectedCustomization.backgroundStyle == BackgroundStyle.PATTERN) {
                        SliderSetting(
                            title = "Pattern Opacity",
                            value = selectedCustomization.patternOpacity,
                            valueRange = 0.01f..0.15f,
                            steps = 0,
                            onValueChange = { selectedCustomization = selectedCustomization.copy(patternOpacity = it) },
                            valueLabel = "${(selectedCustomization.patternOpacity * 100).toInt()}%"
                        )
                    }

                    SwitchSetting(
                        title = "Border",
                        checked = selectedCustomization.borderEnabled,
                        onCheckedChange = { selectedCustomization = selectedCustomization.copy(borderEnabled = it) }
                    )

                    if (selectedCustomization.borderEnabled) {
                        SliderSetting(
                            title = "Border Width",
                            value = selectedCustomization.borderWidth,
                            valueRange = 1f..8f,
                            steps = 6,
                            onValueChange = { selectedCustomization = selectedCustomization.copy(borderWidth = it) },
                            valueLabel = "${selectedCustomization.borderWidth.toInt()}dp"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CustomizationCard(
                title = "Text Settings",
                isExpanded = isTextSettingsExpanded,
                onExpandChange = { isTextSettingsExpanded = it }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingSection(title = "Font Style") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedCustomization.fontStyle == FontStyle.REGULAR,
                                onClick = { selectedCustomization = selectedCustomization.copy(fontStyle = FontStyle.REGULAR) },
                                label = { Text("Regular", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCustomization.fontStyle == FontStyle.BOLD,
                                onClick = { selectedCustomization = selectedCustomization.copy(fontStyle = FontStyle.BOLD) },
                                label = { Text("Bold", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCustomization.fontStyle == FontStyle.EXTRA_BOLD,
                                onClick = { selectedCustomization = selectedCustomization.copy(fontStyle = FontStyle.EXTRA_BOLD) },
                                label = { Text("Extra Bold", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    SettingSection(title = "Text Alignment") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedCustomization.textAlignment == TextAlignment.LEFT,
                                onClick = { selectedCustomization = selectedCustomization.copy(textAlignment = TextAlignment.LEFT) },
                                label = { Text("Left", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCustomization.textAlignment == TextAlignment.CENTER,
                                onClick = { selectedCustomization = selectedCustomization.copy(textAlignment = TextAlignment.CENTER) },
                                label = { Text("Center", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedCustomization.textAlignment == TextAlignment.RIGHT,
                                onClick = { selectedCustomization = selectedCustomization.copy(textAlignment = TextAlignment.RIGHT) },
                                label = { Text("Right", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    SwitchSetting(
                        title = "Text Shadow",
                        checked = selectedCustomization.textShadowEnabled,
                        onCheckedChange = { selectedCustomization = selectedCustomization.copy(textShadowEnabled = it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CustomizationCard(
                title = "Layout Settings",
                isExpanded = isLayoutSettingsExpanded,
                onExpandChange = { isLayoutSettingsExpanded = it }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SwitchSetting(
                        title = "Show Cover Art",
                        checked = selectedCustomization.showCoverArt,
                        onCheckedChange = { selectedCustomization = selectedCustomization.copy(showCoverArt = it) }
                    )

                    if (selectedCustomization.showCoverArt) {
                        SwitchSetting(
                            title = "Show Song Title",
                            checked = selectedCustomization.showSongTitle,
                            onCheckedChange = { selectedCustomization = selectedCustomization.copy(showSongTitle = it) }
                        )

                        SwitchSetting(
                            title = "Show Artist Name",
                            checked = selectedCustomization.showArtistName,
                            onCheckedChange = { selectedCustomization = selectedCustomization.copy(showArtistName = it) }
                        )
                    }

                    SwitchSetting(
                        title = "Show Logo",
                        checked = selectedCustomization.showLogo,
                        onCheckedChange = { selectedCustomization = selectedCustomization.copy(showLogo = it) }
                    )

                    if (selectedCustomization.showLogo) {
                        SettingSection(title = "Logo Position") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = selectedCustomization.logoPosition == LogoPosition.BOTTOM_LEFT,
                                    onClick = { selectedCustomization = selectedCustomization.copy(logoPosition = LogoPosition.BOTTOM_LEFT) },
                                    label = { Text("Bottom Left", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedCustomization.logoPosition == LogoPosition.BOTTOM_RIGHT,
                                    onClick = { selectedCustomization = selectedCustomization.copy(logoPosition = LogoPosition.BOTTOM_RIGHT) },
                                    label = { Text("Bottom Right", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        SettingSection(title = "Logo Size") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = selectedCustomization.logoSize == LogoSize.SMALL,
                                    onClick = { selectedCustomization = selectedCustomization.copy(logoSize = LogoSize.SMALL) },
                                    label = { Text("Small", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedCustomization.logoSize == LogoSize.MEDIUM,
                                    onClick = { selectedCustomization = selectedCustomization.copy(logoSize = LogoSize.MEDIUM) },
                                    label = { Text("Medium", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedCustomization.logoSize == LogoSize.LARGE,
                                    onClick = { selectedCustomization = selectedCustomization.copy(logoSize = LogoSize.LARGE) },
                                    label = { Text("Large", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    SliderSetting(
                        title = "Padding",
                        value = selectedCustomization.padding,
                        valueRange = 16f..64f,
                        steps = 47,
                        onValueChange = { selectedCustomization = selectedCustomization.copy(padding = it) },
                        valueLabel = "${selectedCustomization.padding.toInt()}dp"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.preview),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (mediaMetadata != null) {
                    val previewSize = remember(maxWidth) { min(maxWidth.value - 48f, 380f).dp }
                    Box(modifier = Modifier.size(previewSize), contentAlignment = Alignment.Center) {
                        LyricsImageCardPreview(
                            lyricText = lyricsText,
                            mediaMetadata = mediaMetadata,
                            customization = selectedCustomization,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.back),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = {
                        scope.launch {
                            onShowProgressDialog(true)
                            try {
                                val screenWidth = configuration.screenWidthDp
                                val screenHeight = configuration.screenHeightDp

                                val image = ComposeToImage.createLyricsImage(
                                    context = context,
                                    coverArtUrl = coverUrl,
                                    songTitle = songTitle,
                                    artistName = artists,
                                    lyrics = lyricsText,
                                    width = (screenWidth * density.density).toInt(),
                                    height = (screenHeight * density.density).toInt(),
                                    backgroundColor = selectedCustomization.backgroundColor.toArgb(),
                                    textColor = selectedCustomization.textColor.toArgb(),
                                    secondaryTextColor = selectedCustomization.secondaryTextColor.toArgb(),
                                    showCoverArt = selectedCustomization.showCoverArt,
                                    showLogo = selectedCustomization.showLogo,
                                    backgroundStyle = selectedCustomization.backgroundStyle.name,
                                    gradientColors = selectedCustomization.gradientColors?.map { it.toArgb() }?.toIntArray(),
                                    fontStyle = selectedCustomization.fontStyle.name,
                                    logoPosition = selectedCustomization.logoPosition.name,
                                    cornerRadius = selectedCustomization.cornerRadius,
                                    patternOpacity = selectedCustomization.patternOpacity,
                                    textAlignment = selectedCustomization.textAlignment.name,
                                    padding = selectedCustomization.padding,
                                    showArtistName = selectedCustomization.showArtistName,
                                    showSongTitle = selectedCustomization.showSongTitle,
                                    textShadowEnabled = selectedCustomization.textShadowEnabled,
                                    borderEnabled = selectedCustomization.borderEnabled,
                                    borderColor = selectedCustomization.borderColor.toArgb(),
                                    borderWidth = selectedCustomization.borderWidth,
                                    logoSize = selectedCustomization.logoSize.name,
                                    coverArtStyle = selectedCustomization.coverArtStyle.name,
                                    lyricsStyle = selectedCustomization.lyricsStyle.name,
                                    showAccentLine = selectedCustomization.showAccentLine,
                                    accentColor = selectedCustomization.accentColor?.toArgb(),
                                    spacingBetweenElements = selectedCustomization.spacingBetweenElements,
                                    lyricsLineSpacing = selectedCustomization.lyricsLineSpacing
                                )

                                val timestamp = System.currentTimeMillis()
                                val safeArtistName = artists.replace("[^a-zA-Z0-9\\s]".toRegex(), "").trim().replace("\\s+".toRegex(), "_")
                                val safeSongTitle = songTitle.replace("[^a-zA-Z0-9\\s]".toRegex(), "").trim().replace("\\s+".toRegex(), "_")

                                val filename = if (safeArtistName.isNotEmpty() && safeSongTitle.isNotEmpty()) {
                                    "lyrics_${safeArtistName}_${safeSongTitle}_$timestamp"
                                } else if (safeSongTitle.isNotEmpty()) {
                                    "lyrics_${safeSongTitle}_$timestamp"
                                } else {
                                    "lyrics_$timestamp"
                                }

                                val uri = ComposeToImage.saveBitmapAsFile(context, image, filename)

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                context.startActivity(Intent.createChooser(shareIntent, "Share Lyrics"))

                                onShowProgressDialog(false)
                                onDismiss()

                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to create image: ${e.message}", Toast.LENGTH_SHORT).show()
                                onShowProgressDialog(false)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = stringResource(R.string.share_lyrics_image),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CustomizationCard(
    title: String,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = { onExpandChange(!isExpanded) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp), content = content)
            }
        }
    }
}

@Composable
private fun SettingSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        content()
    }
}

@Composable
private fun SliderSetting(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    valueLabel: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$title: $valueLabel",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, steps = steps)
    }
}

@Composable
private fun SwitchSetting(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

fun Color.isDark(): Boolean {
    val luminance = 0.299f * this.red + 0.587f * this.green + 0.114f * this.blue
    return luminance < 0.5f
}

@Composable
fun BottomSheetColorCustomizationItem(
    customization: ImageCustomization,
    presetName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Column(
        modifier = modifier.width(60.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(48.dp).scale(scale).clip(RoundedCornerShape(12.dp))
                .background(
                    brush = if (customization.backgroundStyle == BackgroundStyle.GRADIENT && customization.gradientColors != null) {
                        Brush.linearGradient(customization.gradientColors)
                    } else {
                        Brush.linearGradient(listOf(customization.backgroundColor, customization.backgroundColor))
                    }
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Aa", color = customization.textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = presetName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected) 1f else 0.7f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(60.dp)
        )
    }
}