/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Rediseñado con sistema de diseño OpenTune + Material 3 Expressive
 */

package com.arturo254.opentune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.arturo254.opentune.BuildConfig
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.component.IconButton
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.viewmodels.AboutViewModel
import com.arturo254.opentune.viewmodels.Contributor
import kotlinx.coroutines.delay

// ── Shimmer brush (Material 3 Expressive) ──────────────────────────────────

@Composable
fun shimmerEffect(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.2f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim, y = 0f),
        end = Offset(x = translateAnim + 500f, y = 0f),
    )
}

// ── Data ───────────────────────────────────────────────────────────────────

private data class SocialLink(
    val iconRes: Int,
    val url: String,
    val label: String,
)

// ── Main screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AboutViewModel = viewModel()
) {
    val uriHandler = LocalUriHandler.current
    val contributors by viewModel.contributors.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val playerConnection = LocalPlayerConnection.current
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.about),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = Color.Unspecified
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->

        LazyColumn(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = if (playerConnection?.player?.isPlaying == true) 96.dp else 32.dp,
            ),
        ) {

            // ── Hero card (Expresivo con morphing) ─────────────────────────
            item(key = "hero") {
                HeroCardExpressive(shimmerBrush = shimmerEffect())
            }

            // ── Social card ───────────────────────────────────────────────
            item(key = "social") {
                SocialCardExpressive(
                    links = listOf(
                        SocialLink(
                            R.drawable.github,
                            "https://github.com/Arturo254/OpenTune",
                            "GitHub"
                        ),
                        SocialLink(
                            R.drawable.telegram,
                            "https://t.me/opentune_updates",
                            "Telegram"
                        ),
                        SocialLink(
                            R.drawable.facebook,
                            "https://www.facebook.com/Arturo254",
                            "Facebook"
                        ),
                        SocialLink(R.drawable.paypal, "https://www.paypal.me/OpenTune", "PayPal"),
                        SocialLink(
                            R.drawable.instagram,
                            "https://www.instagram.com/arturocg.dev/",
                            "Instagram"
                        ),
                        SocialLink(
                            R.drawable.resource_public,
                            "https://opentune.netlify.app/",
                            "Website"
                        ),
                    ),
                    onLinkClick = { uriHandler.openUri(it) },
                    columns = if (isTablet) 4 else 3,
                )
            }

            // ── Contributors section header ───────────────────────────────
            item(key = "contributors_header") {
                SectionHeaderExpressive(
                    title = stringResource(R.string.contributors),
                    iconRes = R.drawable.person,
                    iconColor = MaterialTheme.colorScheme.secondaryContainer
                )
            }

            // ── Contributors content ───────────────────────────────────────
            when {
                isLoading -> {
                    items(4, key = { "loading_$it" }) {
                        ContributorShimmerExpressive()
                    }
                }

                error != null -> {
                    item(key = "error") {
                        ErrorCardExpressive(
                            message = error ?: "Unknown error",
                            onRetry = { viewModel.fetchContributorsFromGitHub() }
                        )
                    }
                }

                contributors.isNotEmpty() -> {
                    items(
                        items = contributors,
                        key = { contributor -> contributor.name }
                    ) { contributor ->
                        ContributorCardExpressive(
                            contributor = contributor,
                            onClick = { uriHandler.openUri(contributor.profileUrl) },
                        )
                    }
                }
            }

            // ── License footer ────────────────────────────────────────────
            item(key = "license") {
                LicenseFooterExpressive(
                    onLicenseClick = {
                        uriHandler.openUri("https://github.com/Arturo254/OpenTune/blob/master/LICENSE")
                    }
                )
            }

            // ── Bottom spacer ─────────────────────────────────────────────
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── Section Header (Material 3) ────────────────────────────────────────────

@Composable
private fun SectionHeaderExpressive(
    title: String,
    iconRes: Int,
    iconColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = iconColor,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Hero card (Expresivo con morphing) ─────────────────────────────────────

@Composable
private fun HeroCardExpressive(shimmerBrush: Brush) {
    var isExpanded by remember { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val elevation = remember { Animatable(2f) }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            scale.animateTo(1.02f, spring(stiffness = Spring.StiffnessLow))
            rotation.animateTo(1f, spring(stiffness = Spring.StiffnessVeryLow))
            elevation.animateTo(8f, spring(stiffness = Spring.StiffnessLow))
        } else {
            scale.animateTo(1f, spring(stiffness = Spring.StiffnessLow))
            rotation.animateTo(0f, spring(stiffness = Spring.StiffnessVeryLow))
            elevation.animateTo(2f, spring(stiffness = Spring.StiffnessLow))
        }
    }

    // Contenedor con padding superior para evitar corte al expandir
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isExpanded) 8.dp else 0.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    rotationZ = rotation.value * 2f
                }
                .clickable { isExpanded = !isExpanded },
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            elevation = CardDefaults.elevatedCardElevation(
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // App icon with morphing shape
                Surface(
                    shape = if (isExpanded) CircleShape else RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(if (isExpanded) 100.dp else 84.dp),
                    shadowElevation = if (isExpanded) 12.dp else 4.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.opentune_monochrome),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(
                                MaterialTheme.colorScheme.onPrimaryContainer,
                                BlendMode.SrcIn,
                            ),
                            modifier = Modifier
                                .size(if (isExpanded) 68.dp else 56.dp)
                                .clip(RoundedCornerShape(if (isExpanded) 24.dp else 16.dp)),
                        )
                        // Shimmer overlay (solo visible en estado no expandido)
                        if (!isExpanded) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(shimmerBrush),
                            )
                        }
                    }
                }

                // App name (animated)
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                        initialScale = 0.7f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    ),
                    exit = fadeOut(animationSpec = tween(150)) + scaleOut(
                        targetScale = 0.7f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }

                if (!isExpanded) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Version badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VersionBadgeExpressive(
                        text = "v${BuildConfig.VERSION_NAME}",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    VersionBadgeExpressive(
                        text = "#${BuildConfig.VERSION_CODE}",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    if (BuildConfig.DEBUG) {
                        VersionBadgeExpressive(
                            text = "DEBUG",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )

                // Developer info
                DeveloperInfoExpressive()

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 100)) + scaleIn(
                        initialScale = 0.9f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    ),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Text(
                        text = "Jesus Christ loves you",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

// ── Developer Info Component ───────────────────────────────────────────────

@Composable
private fun DeveloperInfoExpressive() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.size(44.dp),
        ) {
            AsyncImage(
                model = "https://avatars.githubusercontent.com/u/87346871?v=4",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        }
        Column {
            Text(
                text = "Arturo Cervantes",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Lead Developer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Social card (Material 3 Expressive) ────────────────────────────────────

@Composable
private fun SocialCardExpressive(
    links: List<SocialLink>,
    onLinkClick: (String) -> Unit,
    columns: Int = 3,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.link),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.social_links),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Social links FlowRow (responsive)
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                links.forEach { link ->
                    SocialPillExpressive(
                        iconRes = link.iconRes,
                        label = link.label,
                        onClick = { onLinkClick(link.url) },
                    )
                }
            }
        }
    }
}

// ── Contributor Card (Expresivo) ───────────────────────────────────────────

@Composable
private fun ContributorCardExpressive(
    contributor: Contributor,
    onClick: () -> Unit,
) {
    var isHovered by remember { mutableStateOf(false) }
    val elevation by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isHovered) 6.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "elevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = if (isHovered) 1.01f else 1f
                scaleY = if (isHovered) 1.01f else 1f
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHovered)
                MaterialTheme.colorScheme.surfaceContainerHighest
            else
                MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = MaterialShapes.Ghostish.toShape(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(52.dp),
                shadowElevation = if (isHovered) 4.dp else 2.dp,
            ) {
                AsyncImage(
                    model = contributor.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialShapes.Ghostish.toShape()),
                )
            }

            // Name + role
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contributor.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = contributor.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Forward arrow
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = if (isHovered)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── License footer (Material 3 Expressive) ─────────────────────────────────

@Composable
private fun LicenseFooterExpressive(onLicenseClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLicenseClick)
            .graphicsLayer {
                scaleX = if (isPressed) 0.98f else 1f
                scaleY = if (isPressed) 0.98f else 1f
            },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.policy),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GNU General Public License v3.0",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.view_license),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = if (isPressed)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ── Loading shimmer (Expresivo) ────────────────────────────────────────────

@Composable
private fun ContributorShimmerExpressive() {
    val shimmer = shimmerEffect()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Avatar shimmer con forma Ghostish
            Surface(
                shape = MaterialShapes.Ghostish.toShape(),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(52.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(shimmer)
                        .clip(MaterialShapes.Ghostish.toShape()),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(20.dp)
                        .background(shimmer, RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(14.dp)
                        .background(shimmer, RoundedCornerShape(4.dp))
                )
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(shimmer, CircleShape)
            )
        }
    }
}

// ── Error card (Material 3 Expressive) ─────────────────────────────────────

@Composable
private fun ErrorCardExpressive(
    message: String,
    onRetry: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(80.dp),
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.error),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }

            Text(
                text = "Could not load contributors",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ElevatedButton(
                onClick = onRetry,
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Text(
                    text = stringResource(R.string.retry),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

// ── Small helpers ──────────────────────────────────────────────────────────

@Composable
private fun VersionBadgeExpressive(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = containerColor,
        shadowElevation = 1.dp,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SocialPillExpressive(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
) {
    var isHovered by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = if (isHovered) 1.05f else 1f
                scaleY = if (isHovered) 1.05f else 1f
            },
        shape = RoundedCornerShape(40.dp),
        color = if (isHovered)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = if (isHovered) 6.dp else 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = if (isHovered)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (isHovered)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
            )
        }
    }
}