package com.arturo254.opentune.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arturo254.opentune.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

// Extension property para el DataStore
val Context.avatarDataStore: DataStore<Preferences> by preferencesDataStore(name = "avatar_preferences")

/**
 * Gestor de preferencias para el avatar
 */
class AvatarPreferenceManager(private val context: Context) {
    companion object {
        private val SELECTED_AVATAR_TYPE_KEY = stringPreferencesKey("selected_avatar_type")
        private val CUSTOM_AVATAR_URI_KEY = stringPreferencesKey("custom_avatar_uri")
        private val DICEBEAR_AVATAR_URL_KEY = stringPreferencesKey("dicebear_avatar_url")
    }

    suspend fun saveAvatarSelection(selection: AvatarSelection) {
        context.avatarDataStore.edit { preferences ->
            when (selection) {
                is AvatarSelection.Default -> {
                    preferences[SELECTED_AVATAR_TYPE_KEY] = "default"
                    preferences.remove(CUSTOM_AVATAR_URI_KEY)
                    preferences.remove(DICEBEAR_AVATAR_URL_KEY)
                }
                is AvatarSelection.DiceBear -> {
                    preferences[SELECTED_AVATAR_TYPE_KEY] = "dicebear"
                    preferences[DICEBEAR_AVATAR_URL_KEY] = selection.url
                    preferences.remove(CUSTOM_AVATAR_URI_KEY)
                }
                is AvatarSelection.Custom -> {
                    preferences[SELECTED_AVATAR_TYPE_KEY] = "custom"
                    preferences[CUSTOM_AVATAR_URI_KEY] = selection.uri
                    preferences.remove(DICEBEAR_AVATAR_URL_KEY)
                }
            }
        }
    }

    val getAvatarSelection: Flow<AvatarSelection> = context.avatarDataStore.data
        .map { preferences ->
            val type = preferences[SELECTED_AVATAR_TYPE_KEY] ?: "default"
            when (type) {
                "dicebear" -> {
                    val url = preferences[DICEBEAR_AVATAR_URL_KEY]
                    if (url != null) AvatarSelection.DiceBear(url) else AvatarSelection.Default
                }
                "custom" -> {
                    val uri = preferences[CUSTOM_AVATAR_URI_KEY]
                    if (uri != null) AvatarSelection.Custom(uri) else AvatarSelection.Default
                }
                else -> AvatarSelection.Default
            }
        }
}

/**
 * Tipos de selección de avatar
 */
sealed class AvatarSelection {
    object Default : AvatarSelection()
    data class DiceBear(val url: String) : AvatarSelection()
    data class Custom(val uri: String) : AvatarSelection()
}

/**
 * Estilos disponibles de DiceBear
 */
enum class DiceBearStyle(val value: String, val displayName: String) {
    ADVENTURER("adventurer", "Adventurer"),
    ADVENTURER_NEUTRAL("adventurer-neutral", "Adventurer Neutral"),
    AVATAAARS("avataaars", "Avataaars"),
    AVATAAARS_NEUTRAL("avataaars-neutral", "Avataaars Neutral"),
    BIG_EARS("big-ears", "Big Ears"),
    BIG_EARS_NEUTRAL("big-ears-neutral", "Big Ears Neutral"),
    BIG_SMILE("big-smile", "Big Smile"),
    BOTTTS("bottts", "Bottts"),
    BOTTTS_NEUTRAL("bottts-neutral", "Bottts Neutral"),
    CROODLES("croodles", "Croodles"),
    CROODLES_NEUTRAL("croodles-neutral", "Croodles Neutral"),
    DYLAN("dylan", "Dylan"),
    FUN_EMOJI("fun-emoji", "Fun Emoji"),
    GLASS("glass", "Glass"),
    ICONS("icons", "Icons"),
    IDENTICON("identicon", "Identicon"),
    INITIALS("initials", "Initials"),
    LORELEI("lorelei", "Lorelei"),
    LORELEI_NEUTRAL("lorelei-neutral", "Lorelei Neutral"),
    MICAH("micah", "Micah"),
    MINIAVS("miniavs", "Miniavs"),
    NOTIONISTS("notionists", "Notionists"),
    NOTIONISTS_NEUTRAL("notionists-neutral", "Notionists Neutral"),
    OPEN_PEEPS("open-peeps", "Open Peeps"),
    PERSONAS("personas", "Personas"),
    PIXEL_ART("pixel-art", "Pixel Art"),
    PIXEL_ART_NEUTRAL("pixel-art-neutral", "Pixel Art Neutral"),
    RINGS("rings", "Rings"),
    SHAPES("shapes", "Shapes"),
    THUMBS("thumbs", "Thumbs")
}


/**
 * Generador de URLs de DiceBear
 */
object DiceBearGenerator {
    private const val BASE_URL = "https://api.dicebear.com/7.x"

    fun generateAvatarUrl(
        style: DiceBearStyle = DiceBearStyle.SHAPES,
        seed: String? = null,
        size: Int = 200,
        backgroundColor: String? = null,
        format: String = "png" // 👈 antes estaba "svg"
    ): String {
        val actualSeed = seed ?: generateRandomSeed()
        var url = "$BASE_URL/${style.value}/$format?seed=$actualSeed&size=$size"

        backgroundColor?.let { url += "&backgroundColor=$it" }

        return url
    }


    fun generateRandomSeed(): String {
        return Random.nextLong(0, Long.MAX_VALUE).toString()
    }

    fun getPresetAvatars(style: DiceBearStyle = DiceBearStyle.SHAPES): List<String> {
        val seeds = listOf(
            "Amaya",
            "Destiny",
            "Sarah",
            "Alexander",
            "Jack",
            "Wyatt",
            "Emery",
            "Jameson",
            "Avery",
            "Sara",
            "Aiden",
            "Sophia",
            "Riley",
            "Brian",
            "Jude",
            "Luis",
            "Christian",
            "Eliza",
            "Leo",
            "Vivian"
        )
        return seeds.map { generateAvatarUrl(style, it) }
    }

}

/**
 * Estado de la UI para el avatar
 */
data class AvatarUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentSelection: AvatarSelection = AvatarSelection.Default,
    val showDiceBearDialog: Boolean = false
)

/**
 * Componente principal del selector de avatar
 */
@Composable
fun AvatarSelector(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentSelection by avatarManager.getAvatarSelection.collectAsState(initial = AvatarSelection.Default)

    var uiState by remember {
        mutableStateOf(AvatarUiState(currentSelection = currentSelection))
    }

    // Actualizar el estado cuando cambia la selección
    LaunchedEffect(currentSelection) {
        uiState = uiState.copy(currentSelection = currentSelection)
    }

    val coroutineScope = rememberCoroutineScope()

    // Launcher para seleccionar imagen personalizada
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uiState = uiState.copy(isLoading = true, error = null)

            coroutineScope.launch {
                val result = saveImageToInternalStorage(context, it)
                result.fold(
                    onSuccess = { savedFile ->
                        val savedUri = Uri.fromFile(savedFile)
                        avatarManager.saveAvatarSelection(AvatarSelection.Custom(savedUri.toString()))
                    },
                    onFailure = { exception ->
                        uiState = uiState.copy(
                            error = context.getString(R.string.error_saving_image),
                            isLoading = false
                        )
                        Log.e("AvatarSelector", "Error saving image", exception)
                    }
                )
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    // Mostrar error temporalmente
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            delay(4000)
            uiState = uiState.copy(error = null)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Título
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.avatar_selection),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Avatar actual seleccionado
            CurrentAvatarDisplay(
                selection = uiState.currentSelection,
                isLoading = uiState.isLoading,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botones de acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(id = R.string.custom))
                }

                Button(
                    onClick = {
                        uiState = uiState.copy(showDiceBearDialog = true)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    Icon(
                        painter = painterResource(R.drawable.palette),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Avatars")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Botón para restaurar por defecto
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        cleanupOldAvatars(context)
                        avatarManager.saveAvatarSelection(AvatarSelection.Default)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.currentSelection !is AvatarSelection.Default && !uiState.isLoading
            ) {
                Text(stringResource(id = R.string.restore_default_avatar))
            }

            // Mostrar mensaje de error
            AnimatedVisibility(visible = uiState.error != null) {
                uiState.error?.let { error ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog para seleccionar avatars DiceBear
    if (uiState.showDiceBearDialog) {
        DiceBearAvatarDialog(
            onDismiss = {
                uiState = uiState.copy(showDiceBearDialog = false)
            },
            onAvatarSelected = { url ->
                coroutineScope.launch {
                    avatarManager.saveAvatarSelection(AvatarSelection.DiceBear(url))
                }
                uiState = uiState.copy(showDiceBearDialog = false)
            },
            currentSelection = uiState.currentSelection
        )
    }
}

/**
 * Dialog para seleccionar avatars de DiceBear
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiceBearAvatarDialog(
    onDismiss: () -> Unit,
    onAvatarSelected: (String) -> Unit,
    currentSelection: AvatarSelection
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedStyle by remember { mutableStateOf(DiceBearStyle.SHAPES) }
    var avatarUrls by remember { mutableStateOf(DiceBearGenerator.getPresetAvatars(selectedStyle)) }
    var isLoading by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf(false) }

    // 🔄 Regenera avatares al cambiar estilo
    LaunchedEffect(selectedStyle) {
        isLoading = true
        avatarUrls = DiceBearGenerator.getPresetAvatars(selectedStyle)
        isLoading = false
    }

    // BottomSheet principal
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Título
            Text(
                text = "Selecciona un avatar",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Fila: estilo actual + randomizar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón para abrir selección de estilo
                FilledTonalButton(
                    onClick = { showStyleDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(selectedStyle.displayName)
                }

                // Botón randomizar con tu ícono
                IconButton(
                    onClick = {
                        isLoading = true
                        avatarUrls = DiceBearGenerator.getPresetAvatars(selectedStyle)
                        isLoading = false
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ia_icon),
                        contentDescription = "Randomizar avatares"
                    )
                }
            }

            // Grid de avatares
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxHeight(0.7f)
                ) {
                    items(avatarUrls) { url ->
                        Card(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable {
                                    onAvatarSelected(url)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Avatar DiceBear",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog para seleccionar estilo
    if (showStyleDialog) {
        Dialog(
            onDismissRequest = { showStyleDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(0.7f), // máximo 70% de altura de pantalla
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Elige un estilo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(DiceBearStyle.values()) { style ->
                            TextButton(
                                onClick = {
                                    selectedStyle = style
                                    showStyleDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = style.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (style == selectedStyle) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}



/**
 * Item individual de avatar DiceBear
 */
@Composable
private fun DiceBearAvatarItem(
    url: String,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }

    val borderWidth = if (isSelected) 3.dp else 1.dp

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .border(BorderStroke(borderWidth, borderColor), CircleShape)
            .clickable { onSelected() },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = "Avatar DiceBear",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Indicador de selección
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Muestra el avatar actualmente seleccionado
 */
@Composable
private fun CurrentAvatarDisplay(
    selection: AvatarSelection,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .border(
                width = 3.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            selection is AvatarSelection.Custom -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(Uri.parse(selection.uri))
                        .crossfade(true)
                        .error(R.drawable.person)
                        .placeholder(R.drawable.person)
                        .build(),
                    contentDescription = stringResource(id = R.string.custom_avatar),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            selection is AvatarSelection.DiceBear -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(selection.url)
                        .crossfade(true)
                        .error(R.drawable.person)
                        .placeholder(R.drawable.person)
                        .build(),
                    contentDescription = "Avatar DiceBear",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            else -> {
                DefaultAvatarIcon()
            }
        }
    }
}

/**
 * Icono de avatar por defecto
 */
@Composable
private fun DefaultAvatarIcon() {
    Icon(
        painter = painterResource(id = R.drawable.person),
        contentDescription = stringResource(id = R.string.default_avatar),
        modifier = Modifier.size(40.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// Funciones utilitarias (las mismas del código anterior)
private suspend fun saveImageToInternalStorage(
    context: Context,
    uri: Uri
): Result<File> = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            cleanupOldAvatars(context)

            val fileName = "custom_avatar_${System.currentTimeMillis()}.jpg"
            val outputFile = File(context.filesDir, fileName)

            val bitmap = BitmapFactory.decodeStream(inputStream)
            val compressedBitmap = resizeAndCompressBitmap(bitmap, 500, 500)

            FileOutputStream(outputFile).use { outputStream ->
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }

            if (bitmap != compressedBitmap) {
                bitmap.recycle()
            }
            compressedBitmap.recycle()

            Result.success(outputFile)
        } ?: Result.failure(Exception("No se pudo abrir el archivo"))
    } catch (e: Exception) {
        Log.e("AvatarSelector", "Error saving image to internal storage", e)
        Result.failure(e)
    }
}

private fun resizeAndCompressBitmap(
    bitmap: Bitmap,
    maxWidth: Int,
    maxHeight: Int
): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)

    return if (ratio < 1.0f) {
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    } else {
        bitmap
    }
}

private fun cleanupOldAvatars(context: Context) {
    try {
        context.filesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("custom_avatar_") && file.name.endsWith(".jpg")) {
                val deleted = file.delete()
                Log.d("AvatarSelector", "Deleted old avatar file: ${file.name}, success: $deleted")
            }
        }
    } catch (e: Exception) {
        Log.e("AvatarSelector", "Error cleaning up old avatars", e)
    }
}