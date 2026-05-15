package com.example.freshwall.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.freshwall.R
import com.example.freshwall.data.WALLPAPER_CATEGORIES

/** Source brand colors used on the picker headers. */
internal val PexelsBrand = Color(0xFF05A081)
internal val UnsplashBrand = Color(0xFF111111)

private const val MIN_SELECTION = 5
private const val MAX_SELECTION = 15

/**
 * Pool of public mountain photos for the source-customisation headers.
 * Direct CDN URLs (no API key needed). One is picked at random per
 * composition via `remember {}` so each onboarding visit feels different,
 * but the same image stays put for the duration of the visit.
 */
internal val MOUNTAIN_IMAGE_URLS: List<String> = listOf(
    "https://images.pexels.com/photos/1366630/pexels-photo-1366630.jpeg?auto=compress&cs=tinysrgb&w=1200",
    "https://images.pexels.com/photos/417173/pexels-photo-417173.jpeg?auto=compress&cs=tinysrgb&w=1200",
    "https://images.pexels.com/photos/618833/pexels-photo-618833.jpeg?auto=compress&cs=tinysrgb&w=1200",
    "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=1200&q=80",
    "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1200&q=80",
    "https://images.unsplash.com/photo-1519681393784-d120267933ba?w=1200&q=80",
)

/* -------------------------------------------------------------------------- */
/*  Step scaffold (simple steps only — picker has its own layout)             */
/* -------------------------------------------------------------------------- */

@Composable
internal fun OnboardingScaffold(
    gradientFrom: Color,
    gradientTo: Color,
    headerContent: @Composable () -> Unit,
    title: String,
    subtitle: String,
    actions: @Composable () -> Unit,
    backgroundImageUrl: String? = null,
    heroHeight: Dp = 300.dp,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BackgroundHeader(
            gradientFrom = gradientFrom,
            gradientTo = gradientTo,
            imageUrl = backgroundImageUrl,
            height = heroHeight,
        ) { headerContent() }

        Spacer(Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        // Push the action area to the bottom but with a tighter weight ratio
        // so the title/subtitle don't feel marooned mid-screen.
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 24.dp,
                ),
        ) {
            actions()
        }
    }
}

/**
 * Hero panel for every step. Renders a remote mountain photo behind a
 * dark scrim when [imageUrl] is supplied; falls back to a gradient when
 * it's null (used for Welcome / Purpose) or when the image fails to load.
 */
@Composable
private fun BackgroundHeader(
    gradientFrom: Color,
    gradientTo: Color,
    imageUrl: String?,
    height: Dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        // Gradient fallback shown while the image loads
                        // (and if it ever fails). Coil paints over it.
                        brush = Brush.linearGradient(
                            colors = listOf(gradientFrom, gradientTo),
                        ),
                    ),
            )
            // Dark scrim so white-on-image text and brand tags stay legible.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.45f),
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.45f),
                            ),
                        ),
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(gradientFrom, gradientTo),
                        ),
                    ),
            )
        }
        content()
    }
}

/* -------------------------------------------------------------------------- */
/*  Hero icon — Material vector, set into the gradient panel                  */
/* -------------------------------------------------------------------------- */

@Composable
internal fun HeroIcon(
    icon: ImageVector,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.92f),
        modifier = Modifier.size(112.dp),
    )
}

/* -------------------------------------------------------------------------- */
/*  Brand tag + source-specific header accents                                */
/* -------------------------------------------------------------------------- */

/**
 * Vertically-stacked Pexels-tag + plus-icon + Unsplash-tag, used both on
 * the ModeSelect step (where the user picks "same vs different") and on
 * the Combined picker step (where the same categories apply to both).
 */
@Composable
internal fun BrandTagsStack() {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandTag(
            label = "Pexels",
            iconRes = R.drawable.ic_pexels,
            brandColor = PexelsBrand,
        )
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "plus",
            tint = Color.White,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(4.dp),
        )
        BrandTag(
            label = "Unsplash",
            iconRes = R.drawable.ic_unsplash,
            brandColor = UnsplashBrand,
        )
    }
}

internal sealed class TopHeaderAccent {
    abstract val gradientFrom: Color @Composable get
    abstract val gradientTo: Color @Composable get
    abstract val imageUrl: String?
    @Composable abstract fun Header()

    /** "Apply to both" — Pexels + plus + Unsplash tags, on a mountain photo. */
    class Combined(override val imageUrl: String?) : TopHeaderAccent() {
        override val gradientFrom: Color
            @Composable get() = MaterialTheme.colorScheme.primaryContainer
        override val gradientTo: Color
            @Composable get() = MaterialTheme.colorScheme.tertiaryContainer

        @Composable
        override fun Header() = BrandTagsStack()
    }

    /** Single Pexels brand tag, on a mountain photo. */
    class Pexels(override val imageUrl: String?) : TopHeaderAccent() {
        override val gradientFrom: Color
            @Composable get() = MaterialTheme.colorScheme.tertiaryContainer
        override val gradientTo: Color
            @Composable get() = MaterialTheme.colorScheme.surfaceContainer

        @Composable
        override fun Header() {
            BrandTag(
                label = "Pexels",
                iconRes = R.drawable.ic_pexels,
                brandColor = PexelsBrand,
            )
        }
    }

    /** Single Unsplash brand tag, on a mountain photo. */
    class Unsplash(override val imageUrl: String?) : TopHeaderAccent() {
        override val gradientFrom: Color
            @Composable get() = MaterialTheme.colorScheme.secondaryContainer
        override val gradientTo: Color
            @Composable get() = MaterialTheme.colorScheme.surfaceContainer

        @Composable
        override fun Header() {
            BrandTag(
                label = "Unsplash",
                iconRes = R.drawable.ic_unsplash,
                brandColor = UnsplashBrand,
            )
        }
    }
}

@Composable
private fun BrandTag(
    label: String,
    iconRes: Int,
    brandColor: Color,
) {
    Surface(
        shape = CircleShape,
        color = brandColor,
        contentColor = Color.White,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                ),
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/*  Pill button — matches the home pill-menu rhythm                           */
/* -------------------------------------------------------------------------- */

@Composable
internal fun PillButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean,
    enabled: Boolean = true,
) {
    val container = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
        primary -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        primary -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = container,
        contentColor = content,
        // Match the home-screen bottom pill rack: tonal 3 / shadow 4 — the
        // "primary" variant gets a touch more shadow for hierarchy.
        tonalElevation = 3.dp,
        shadowElevation = if (primary) 6.dp else 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier.padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/*  Category picker step                                                      */
/* -------------------------------------------------------------------------- */

@Composable
internal fun CategoryPickerStep(
    title: String,
    subtitle: String,
    headerAccent: TopHeaderAccent,
    initialSelection: Set<String>,
    onNext: (Set<String>) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember(initialSelection) { mutableStateOf(initialSelection) }

    val visible = remember(query) {
        if (query.isBlank()) WALLPAPER_CATEGORIES
        else WALLPAPER_CATEGORIES.filter { it.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val heroHeight = if (headerAccent is TopHeaderAccent.Combined) 280.dp else 220.dp
        BackgroundHeader(
            gradientFrom = headerAccent.gradientFrom,
            gradientTo = headerAccent.gradientTo,
            imageUrl = headerAccent.imageUrl,
            height = heroHeight,
        ) { headerAccent.Header() }

        Spacer(Modifier.height(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("Filter categories") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(12.dp))

        // Bordered + weighted + scrollable chip area: chip rows scroll
        // inside the box; the Next button below it stays visible.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(20.dp),
                )
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                visible.forEach { category ->
                    val isSelected = category in selected
                    CategoryChip(
                        label = category,
                        selected = isSelected,
                        onClick = {
                            selected = if (isSelected) {
                                selected - category
                            } else if (selected.size >= MAX_SELECTION) {
                                selected
                            } else {
                                selected + category
                            }
                        },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 20.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val count = selected.size
            val message = when {
                count < MIN_SELECTION ->
                    "Pick at least ${MIN_SELECTION - count} more " +
                        "($count/$MIN_SELECTION)"
                count >= MAX_SELECTION ->
                    "$MAX_SELECTION selected — that's the max"
                else ->
                    "$count selected"
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PillButton(
                label = "Next",
                onClick = { onNext(selected) },
                primary = true,
                enabled = count in MIN_SELECTION..MAX_SELECTION,
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                       else MaterialTheme.colorScheme.onSurface,
        tonalElevation = if (selected) 0.dp else 1.dp,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
