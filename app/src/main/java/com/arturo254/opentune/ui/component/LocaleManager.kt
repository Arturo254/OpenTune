@file:Suppress("DEPRECATION")

package com.arturo254.opentune.ui.component

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.LocaleList
import android.os.Looper
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import com.arturo254.opentune.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import timber.log.Timber

/** Modelo de datos para representar un idioma */
data class LanguageItem(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val completionStatus: CompletionStatus = CompletionStatus.COMPLETE,
    val isSystemDefault: Boolean = false,
    val flag: String = ""
)

/** Estado de completitud de las traducciones */
enum class CompletionStatus(val label: String, val color: @Composable () -> Color) {
    COMPLETE("", { Color.Transparent }),
    INCOMPLETE("Incompleta", { MaterialTheme.colorScheme.tertiary }),
    BETA("Beta", { MaterialTheme.colorScheme.primary }),
    EXPERIMENTAL("Exp", { MaterialTheme.colorScheme.secondary })
}

/** Estados de la operación de cambio de idioma */
sealed class LanguageChangeState {
    object Idle : LanguageChangeState()
    object Changing : LanguageChangeState()
    object Success : LanguageChangeState()
    data class Error(val message: String) : LanguageChangeState()
}
class LocaleManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "LocaleManager"
        private const val PREF_NAME = "locale_preferences"
        private const val PREF_LANGUAGE_KEY = "selected_language"
        private const val SYSTEM_DEFAULT = "system_default"
        private const val RESTART_DELAY = 800L
        private const val ANIMATION_DELAY = 200L

        @Volatile
        private var instance: LocaleManager? = null

        fun getInstance(context: Context): LocaleManager {
            return instance ?: synchronized(this) {
                instance ?: LocaleManager(context.applicationContext).also { instance = it }
            }
        }

        // Mapeo de banderas y estados de traducción
        private val LANGUAGE_METADATA = mapOf(
            "en" to LanguageMetadata("🇺🇸", CompletionStatus.COMPLETE),
            "es" to LanguageMetadata("🇪🇸", CompletionStatus.COMPLETE),
            "fr" to LanguageMetadata("🇫🇷", CompletionStatus.COMPLETE),
            "de" to LanguageMetadata("🇩🇪", CompletionStatus.COMPLETE),
            "it" to LanguageMetadata("🇮🇹", CompletionStatus.COMPLETE),
            "pt-rBR" to LanguageMetadata("🇧🇷", CompletionStatus.COMPLETE),
            "pt" to LanguageMetadata("🇵🇹", CompletionStatus.COMPLETE),
            "ru" to LanguageMetadata("🇷🇺", CompletionStatus.COMPLETE),
            "zh-rCN" to LanguageMetadata("🇨🇳", CompletionStatus.COMPLETE),
            "zh-rTW" to LanguageMetadata("🇹🇼", CompletionStatus.COMPLETE),
            "ja" to LanguageMetadata("🇯🇵", CompletionStatus.COMPLETE),
            "ko" to LanguageMetadata("🇰🇷", CompletionStatus.COMPLETE),
            "ar" to LanguageMetadata("🇸🇦", CompletionStatus.BETA),
            "hi" to LanguageMetadata("🇮🇳", CompletionStatus.BETA),
            "th" to LanguageMetadata("🇹🇭", CompletionStatus.INCOMPLETE),
            "vi" to LanguageMetadata("🇻🇳", CompletionStatus.INCOMPLETE),
            "tr" to LanguageMetadata("🇹🇷", CompletionStatus.BETA),
            "pl" to LanguageMetadata("🇵🇱", CompletionStatus.INCOMPLETE),
            "nl" to LanguageMetadata("🇳🇱", CompletionStatus.INCOMPLETE),
            "id" to LanguageMetadata("🇮🇩", CompletionStatus.BETA),
            "uk" to LanguageMetadata("🇺🇦", CompletionStatus.BETA),
            "he" to LanguageMetadata("🇮🇱", CompletionStatus.BETA)
        )

        private data class LanguageMetadata(
            val flag: String,
            val completionStatus: CompletionStatus
        )
    }

    private val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val _currentLanguage = MutableStateFlow(getSelectedLanguageCode())
    private val _changeState = MutableStateFlow<LanguageChangeState>(LanguageChangeState.Idle)

    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()
    val changeState: StateFlow<LanguageChangeState> = _changeState.asStateFlow()

    private var _cachedLanguages: List<LanguageItem>? = null
    private var _cachedSystemLanguage: String? = null

    fun getSelectedLanguageCode(): String {
        return sharedPreferences.getString(PREF_LANGUAGE_KEY, SYSTEM_DEFAULT) ?: SYSTEM_DEFAULT
    }

    fun getEffectiveLanguageCode(): String {
        val saved = getSelectedLanguageCode()
        return if (saved == SYSTEM_DEFAULT) getSystemLanguageCode() else saved
    }

    private fun getSystemLanguageCode(): String {
        return _cachedSystemLanguage ?: run {
            val systemCode = try {
                val localeList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ConfigurationCompat.getLocales(Resources.getSystem().configuration)
                } else {
                    LocaleListCompat.create(Locale.getDefault())
                }

                val systemLocale = if (localeList.isEmpty) Locale.getDefault() else localeList[0]
                    ?: Locale.getDefault()

                formatLocaleCode(systemLocale)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error obteniendo idioma del sistema")
                "en"
            }
            _cachedSystemLanguage = systemCode
            systemCode
        }
    }
    private fun detectAvailableLanguages(): List<String> {
        val availableLocales = mutableSetOf<String>()

        try {
            val assetManager = context.assets
            val locales = assetManager.locales

            locales.forEach { localeString ->
                if (localeString.isNotEmpty()) {
                    availableLocales.add(localeString)
                }
            }

            if (availableLocales.isEmpty()) {
                val pm = context.packageManager
                val res = pm.getResourcesForApplication(context.packageName)

                // Intentar detectar mediante configuraciones disponibles
                val configs = res.assets.locales
                configs.forEach { locale ->
                    if (locale.isNotEmpty()) {
                        availableLocales.add(locale)
                    }
                }
            }

            if (availableLocales.isEmpty()) {
                val commonLocales = listOf(
                    "en", "es", "fr", "de", "it", "pt", "pt-rBR",
                    "ru", "zh-rCN", "zh-rTW", "ja", "ko", "ar",
                    "hi", "th", "vi", "tr", "pl", "nl", "id", "uk", "he"
                )

                commonLocales.forEach { localeCode ->
                    if (hasTranslationsForLocale(localeCode)) {
                        availableLocales.add(localeCode)
                    }
                }
            }

            Timber.tag(TAG).d("Idiomas detectados: $availableLocales")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error detectando idiomas disponibles")
            // Fallback al idioma por defecto
            availableLocales.add("en")
        }

        return availableLocales.toList()
    }

    /**
     * Verifica si existen traducciones para un locale específico
     */
    private fun hasTranslationsForLocale(localeCode: String): Boolean {
        return try {
            val locale = parseLocaleCode(localeCode)
            val config = Configuration(context.resources.configuration)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale)
            } else {
                config.locale = locale
            }

            val localizedContext = context.createConfigurationContext(config)
            val localizedResources = localizedContext.resources

            // Intentar obtener un string básico para verificar
            try {
                val appName = localizedResources.getString(R.string.app_name)
                true
            } catch (e: Resources.NotFoundException) {
                false
            }
        } catch (e: Exception) {
            false
        }
    }


    private fun formatLocaleCode(locale: Locale): String {
        val language = locale.language
        val country = locale.country

        return when {
            language == "zh" && country.isNotEmpty() -> {
                when (country) {
                    "CN" -> "zh-rCN"
                    "TW", "HK" -> "zh-rTW"
                    else -> "zh-rCN"
                }
            }
            language == "pt" && country == "BR" -> "pt-rBR"
            country.isNotEmpty() -> "$language-r$country"
            else -> language
        }
    }

    /**
     * Convierte código de locale a Locale
     */
    private fun parseLocaleCode(code: String): Locale {
        return when {
            code == "zh-rCN" || code == "zh-CN" -> Locale.SIMPLIFIED_CHINESE
            code == "zh-rTW" || code == "zh-TW" -> Locale.TRADITIONAL_CHINESE
            code.contains("-r") -> {
                val parts = code.split("-r")
                Locale(parts[0], parts[1])
            }
            code.contains("-") -> {
                val parts = code.split("-")
                Locale(parts[0], parts[1])
            }
            else -> Locale(code)
        }
    }

    fun getAvailableLanguages(): List<LanguageItem> {
        return _cachedLanguages ?: run {
            val systemLanguageCode = getSystemLanguageCode()
            val availableLocaleCodes = detectAvailableLanguages()

            val languages = mutableListOf<LanguageItem>()

            // Agregar opción de sistema
            val systemDisplayName = try {
                val locale = parseLocaleCode(systemLanguageCode)
                locale.displayLanguage.replaceFirstChar { it.uppercase() }
            } catch (e: Exception) {
                systemLanguageCode
            }

            languages.add(
                LanguageItem(
                    code = SYSTEM_DEFAULT,
                    displayName = "Sistema ($systemDisplayName)",
                    nativeName = systemDisplayName,
                    completionStatus = CompletionStatus.COMPLETE,
                    isSystemDefault = true,
                    flag = "🌐"
                )
            )

            // Agregar idiomas detectados
            availableLocaleCodes.forEach { localeCode ->
                try {
                    val locale = parseLocaleCode(localeCode)
                    val displayName = locale.getDisplayLanguage(Locale.ENGLISH)
                        .replaceFirstChar { it.uppercase() }
                    val nativeName = locale.getDisplayLanguage(locale)
                        .replaceFirstChar { it.uppercase() }

                    // Obtener metadata (bandera y estado)
                    val metadata = LANGUAGE_METADATA[localeCode]
                        ?: LanguageMetadata("🌍", CompletionStatus.COMPLETE)

                    languages.add(
                        LanguageItem(
                            code = localeCode,
                            displayName = displayName,
                            nativeName = nativeName,
                            completionStatus = metadata.completionStatus,
                            isSystemDefault = false,
                            flag = metadata.flag
                        )
                    )
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Error procesando locale: $localeCode")
                }
            }

            // Ordenar por: sistema primero, luego completos, luego alfabéticamente
            val sorted = languages.sortedWith(
                compareBy<LanguageItem> { !it.isSystemDefault }
                    .thenBy { it.completionStatus.ordinal }
                    .thenBy { it.displayName }
            )

            _cachedLanguages = sorted
            sorted
        }
    }

    suspend fun updateLanguage(languageCode: String): Boolean {
        if (_changeState.value is LanguageChangeState.Changing) {
            return false
        }

        return try {
            _changeState.value = LanguageChangeState.Changing
            Timber.tag(TAG).d("Cambiando idioma a: $languageCode")

            delay(ANIMATION_DELAY)

            val editor = sharedPreferences.edit()
            editor.putString(PREF_LANGUAGE_KEY, languageCode)
            val saved = editor.commit()

            if (!saved) {
                throw Exception("No se pudo guardar la preferencia")
            }

            _currentLanguage.value = languageCode

            val effectiveLanguageCode = if (languageCode == SYSTEM_DEFAULT) {
                getSystemLanguageCode()
            } else {
                languageCode
            }

            val locale = parseLocaleCode(effectiveLanguageCode)
            applyLocaleToApp(locale)

            _changeState.value = LanguageChangeState.Success

            Timber.tag(TAG)
                .d("Idioma actualizado: $languageCode (efectivo: $effectiveLanguageCode)")
            true
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error actualizando idioma a $languageCode")
            _changeState.value = LanguageChangeState.Error(e.message ?: "Error desconocido")
            false
        }
    }

    fun clearCache() {
        _cachedLanguages = null
        _cachedSystemLanguage = null
    }

    private fun applyLocaleToApp(locale: Locale) {
        try {
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val localeList = LocaleList(locale)
                LocaleList.setDefault(localeList)
                config.setLocales(localeList)
                config.setLocale(locale)
            } else {
                config.locale = locale
            }

            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error aplicando configuración de idioma")
        }
    }

    fun applyLocaleToContext(baseContext: Context): Context {
        return try {
            val languageCode = getEffectiveLanguageCode()
            val locale = parseLocaleCode(languageCode)

            Locale.setDefault(locale)
            val config = Configuration(baseContext.resources.configuration)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocale(locale)
                val localeList = LocaleList(locale)
                LocaleList.setDefault(localeList)
                config.setLocales(localeList)
                baseContext.createConfigurationContext(config)
            } else {
                config.locale = locale
                @Suppress("DEPRECATION")
                baseContext.resources.updateConfiguration(
                    config,
                    baseContext.resources.displayMetrics
                )
                baseContext
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error aplicando idioma al contexto")
            baseContext
        }
    }

    fun restartApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                Handler(Looper.getMainLooper()).postDelayed({
                    context.startActivity(it)
                    if (context is Activity) {
                        context.finish()
                        context.overridePendingTransition(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        )
                    }
                }, RESTART_DELAY)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error reiniciando aplicación")
        }
    }

    fun resetChangeState() {
        _changeState.value = LanguageChangeState.Idle
    }
}

// Los composables permanecen igual...
// (LanguageSelector, SearchBar, ChangeStateIndicator, etc.)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelector(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val localeManager = remember { LocaleManager.getInstance(context) }
    val hapticFeedback = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val currentLanguage by localeManager.currentLanguage.collectAsState()
    val changeState by localeManager.changeState.collectAsState()
    val availableLanguages by remember { derivedStateOf { localeManager.getAvailableLanguages() } }

    var selectedLanguageCode by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    val filteredLanguages = remember(availableLanguages, searchQuery) {
        if (searchQuery.isBlank()) {
            availableLanguages
        } else {
            availableLanguages.filter { language ->
                language.displayName.contains(searchQuery, ignoreCase = true) ||
                        language.nativeName.contains(searchQuery, ignoreCase = true) ||
                        language.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(selectedLanguageCode) {
        selectedLanguageCode?.let { languageCode ->
            if (localeManager.updateLanguage(languageCode)) {
                localeManager.restartApp(context)
            }
            selectedLanguageCode = null
        }
    }

    LaunchedEffect(filteredLanguages, currentLanguage) {
        val selectedIndex = filteredLanguages.indexOfFirst { it.code == currentLanguage }
        if (selectedIndex != -1) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            localeManager.resetChangeState()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.language),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = {
                    searchQuery = ""
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
                focusRequester = focusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            )

            AnimatedVisibility(
                visible = changeState is LanguageChangeState.Changing ||
                        changeState is LanguageChangeState.Success,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ChangeStateIndicator(
                    isChanging = changeState is LanguageChangeState.Changing
                )
            }

            if (filteredLanguages.isEmpty()) {
                EmptySearchResult(modifier = Modifier.padding(vertical = 32.dp))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = filteredLanguages,
                        key = { it.code }
                    ) { language ->
                        val isSelected = language.code == currentLanguage
                        val isEnabled = changeState !is LanguageChangeState.Changing

                        LanguageItem(
                            language = language,
                            isSelected = isSelected,
                            isEnabled = isEnabled,
                            onClick = {
                                if (isEnabled && !isSelected) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    selectedLanguageCode = language.code
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                ),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Buscar idioma...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )

            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpiar búsqueda",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangeStateIndicator(
    isChanging: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isChanging) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Aplicando...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Reiniciando...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySearchResult(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sin resultados",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Prueba con otro término",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun LanguageItem(
    language: LanguageItem,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "elevation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .selectable(
                selected = isSelected,
                enabled = isEnabled,
                role = Role.RadioButton,
                onClick = onClick
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bandera
            Text(
                text = language.flag,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.size(28.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Información
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (language.nativeName.isNotEmpty() &&
                    language.nativeName != language.displayName &&
                    !language.isSystemDefault
                ) {
                    Text(
                        text = language.nativeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Badge de estado
            if (language.completionStatus != CompletionStatus.COMPLETE) {
                val statusColor = language.completionStatus.color()

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = language.completionStatus.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Radio button
            RadioButton(
                selected = isSelected,
                onClick = null,
                enabled = isEnabled,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun LanguagePreference(
    modifier: Modifier = Modifier
) {
    var showLanguageSelector by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val localeManager = remember { LocaleManager.getInstance(context) }
    val currentLanguage by localeManager.currentLanguage.collectAsState()
    val changeState by localeManager.changeState.collectAsState()

    val currentLanguageDisplay = remember(currentLanguage) {
        val selectedCode = localeManager.getSelectedLanguageCode()
        localeManager.getAvailableLanguages()
            .find { it.code == selectedCode }
            ?.let { language ->
                if (language.isSystemDefault) {
                    language.nativeName
                } else {
                    "${language.nativeName} ${language.flag}".trim()
                }
            } ?: selectedCode
    }

    val isChanging = changeState is LanguageChangeState.Changing

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isChanging) {
                showLanguageSelector = true
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
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
                    painterResource(R.drawable.translate),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isChanging) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = if (isChanging) {
                        stringResource(R.string.changing_language)
                    } else {
                        currentLanguageDisplay
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isChanging) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (isChanging) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = stringResource(R.string.configure_app_language),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showLanguageSelector) {
        LanguageSelector(
            onDismiss = { showLanguageSelector = false }
        )
    }
}


abstract class LocaleAwareApplication : android.app.Application() {

    private val localeManager by lazy { LocaleManager.getInstance(this) }

    override fun attachBaseContext(base: Context) {
        try {
            val updatedContext = LocaleManager.getInstance(base).applyLocaleToContext(base)
            super.attachBaseContext(updatedContext)
        } catch (e: Exception) {
            Timber.tag("LocaleAwareApplication").e(e, "Error aplicando idioma")
            super.attachBaseContext(base)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            localeManager
            Timber.tag("LocaleAwareApplication").d("LocaleManager inicializado")
        } catch (e: Exception) {
            Timber.tag("LocaleAwareApplication").e(e, "Error inicializando LocaleManager")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeManager.clearCache()
    }
}

