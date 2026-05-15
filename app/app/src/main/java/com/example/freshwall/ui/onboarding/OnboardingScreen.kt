package com.example.freshwall.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import com.example.freshwall.FreshWallApplication
import com.example.freshwall.R
import com.example.freshwall.data.CategoryConfig
import com.example.freshwall.data.CustomizationMode

/** Steps the onboarding flow walks through, in order. */
private enum class OnboardingStep {
    Welcome,           // hello + name + Next
    Purpose,           // "this helps us pick" + Customize / Skip
    ModeSelect,        // combined vs separate
    CombinedPicker,    // one picker covering both sources
    PexelsPicker,      // separate-mode: Pexels first
    UnsplashPicker,    // separate-mode: Unsplash second
}

/**
 * Multi-step first-launch onboarding. The user lands here on first install
 * (or any time `CategoryPreferences.onboardingComplete == false`) and is
 * walked through:
 *
 *   Welcome → Purpose → ModeSelect → one or two CategoryPicker screens
 *
 * Transitions slide horizontally — forward (right→left) when advancing,
 * reverse (left→right) on gesture-back. System back walks one step toward
 * Welcome; from Welcome it falls through and exits the app. On completion,
 * the host persists the selection and navigates to Home.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as FreshWallApplication }

    var step by remember { mutableStateOf(OnboardingStep.Welcome) }
    var mode by remember { mutableStateOf(CustomizationMode.COMBINED) }
    var combinedSelection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pexelsSelection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var unsplashSelection by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Local-asset mosaic that sits behind both Welcome and Purpose. Built
    // once per onboarding visit; Purpose re-shuffles the same pool so the
    // two intro screens feel related but distinct. No network — see
    // [buildWelcomeTiles] / `assets/welcome/`.
    val tiles = remember { buildWelcomeTiles() }
    val purposeTiles = remember(tiles) { tiles.shuffled() }

    // Pick mountain backgrounds once per onboarding session so the user
    // sees consistent imagery across the mode-select / picker steps but
    // a different scene on their next install / re-edit.
    val modeSelectBg = remember { MOUNTAIN_IMAGE_URLS.random() }
    val combinedBg = remember { MOUNTAIN_IMAGE_URLS.random() }
    val pexelsBg = remember { MOUNTAIN_IMAGE_URLS.random() }
    val unsplashBg = remember { MOUNTAIN_IMAGE_URLS.random() }

    fun finish() {
        app.categoryPreferences.finishOnboarding(
            mode = mode,
            combined = combinedSelection.toList(),
            pexels = pexelsSelection.toList(),
            unsplash = unsplashSelection.toList(),
        )
        onFinished()
    }

    BackHandler(enabled = step != OnboardingStep.Welcome) {
        step = when (step) {
            OnboardingStep.Welcome -> step
            OnboardingStep.Purpose -> OnboardingStep.Welcome
            OnboardingStep.ModeSelect -> OnboardingStep.Purpose
            OnboardingStep.CombinedPicker -> OnboardingStep.ModeSelect
            OnboardingStep.PexelsPicker -> OnboardingStep.ModeSelect
            OnboardingStep.UnsplashPicker -> OnboardingStep.PexelsPicker
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        AnimatedContent(
            targetState = step,
            label = "onboardingStep",
            transitionSpec = {
                // Slide the new step in from the side we're heading toward;
                // ordinal compare gives us forward vs. backward direction.
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally(tween(320)) { w -> direction * w } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(320)) { w -> -direction * w } + fadeOut(tween(280)))
            },
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            when (current) {
                OnboardingStep.Welcome -> WelcomeStep(
                    tiles = tiles,
                    onNext = { step = OnboardingStep.Purpose },
                )

                OnboardingStep.Purpose -> PurposeStep(
                    tiles = purposeTiles,
                    onCustomize = { step = OnboardingStep.ModeSelect },
                    onSkip = ::finish,
                )

                OnboardingStep.ModeSelect -> ModeSelectStep(
                    backgroundImageUrl = modeSelectBg,
                    onCombined = {
                        mode = CustomizationMode.COMBINED
                        step = OnboardingStep.CombinedPicker
                    },
                    onSeparate = {
                        mode = CustomizationMode.SEPARATE
                        step = OnboardingStep.PexelsPicker
                    },
                )

                OnboardingStep.CombinedPicker -> CategoryPickerStep(
                    title = "Pick what you like",
                    subtitle = "These categories drive both Pexels and Unsplash. " +
                        "Choose 5 to 15.",
                    headerAccent = TopHeaderAccent.Combined(imageUrl = combinedBg),
                    initialSelection = combinedSelection,
                    onNext = { selection ->
                        combinedSelection = selection
                        finish()
                    },
                )

                OnboardingStep.PexelsPicker -> CategoryPickerStep(
                    title = "Pexels picks",
                    subtitle = "What should we pull from Pexels? Choose 5 to 15.",
                    headerAccent = TopHeaderAccent.Pexels(imageUrl = pexelsBg),
                    initialSelection = pexelsSelection,
                    onNext = { selection ->
                        pexelsSelection = selection
                        step = OnboardingStep.UnsplashPicker
                    },
                )

                OnboardingStep.UnsplashPicker -> CategoryPickerStep(
                    title = "Unsplash picks",
                    subtitle = "And what should we pull from Unsplash? Choose 5 to 15.",
                    headerAccent = TopHeaderAccent.Unsplash(imageUrl = unsplashBg),
                    initialSelection = unsplashSelection,
                    onNext = { selection ->
                        unsplashSelection = selection
                        finish()
                    },
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*  STEP 1 — Welcome                                                          */
/* -------------------------------------------------------------------------- */

/**
 * Welcome step. A faded grid of Pexels images for mountains / bridges /
 * skies tiles the whole screen, with a few colored Pexels/Unsplash chips
 * dropped in so the source brands surface at a glance. A soft vertical
 * scrim keeps the centred logo + title and the bottom pill legible.
 */
@Composable
private fun WelcomeStep(
    tiles: List<WelcomeBackgroundTile>,
    onNext: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        OnboardingBackdrop(tiles = tiles)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            WelcomeBrandLogo()
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Welcome",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "to FreshWall",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1.25f))
            ArrowPillButton(
                label = "Get started",
                onClick = onNext,
            )
            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 36.dp,
                ),
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/*  STEP 2 — Purpose                                                          */
/* -------------------------------------------------------------------------- */

/**
 * Purpose step — same backdrop / scrim / centred-logo treatment as Welcome
 * but with a different icon, a short pitch, and two stacked pill actions:
 * primary `Customize` (with arrow) over secondary `Skip for now`.
 */
@Composable
private fun PurposeStep(
    tiles: List<WelcomeBackgroundTile>,
    onCustomize: () -> Unit,
    onSkip: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        OnboardingBackdrop(tiles = tiles)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            OnboardingLogo(icon = Icons.Outlined.Palette)
            Spacer(Modifier.height(28.dp))
            Text(
                text = "Tell us what you like",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Pick a few categories. We'll pull wallpapers from " +
                    "Pexels and Unsplash that match.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.weight(1.25f))
            ArrowPillButton(
                label = "Customize",
                onClick = onCustomize,
            )
            Spacer(Modifier.height(12.dp))
            CompactPillButton(
                label = "Skip for now",
                onClick = onSkip,
                primary = false,
            )
            Spacer(
                Modifier.height(
                    WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 32.dp,
                ),
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/*  Shared intro chrome (Welcome + Purpose)                                   */
/* -------------------------------------------------------------------------- */

/**
 * Backdrop used behind both intro steps — faded image grid plus a soft
 * vertical scrim so the centred foreground reads cleanly against busy
 * imagery. Designed to be the first child of the host `Box`; the caller
 * stacks its own content on top.
 */
@Composable
private fun OnboardingBackdrop(tiles: List<WelcomeBackgroundTile>) {
    WelcomeBackgroundGrid(
        tiles = tiles,
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.42f),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    0f to MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                    0.30f to MaterialTheme.colorScheme.background.copy(alpha = 0.05f),
                    0.65f to MaterialTheme.colorScheme.background.copy(alpha = 0.20f),
                    1f to MaterialTheme.colorScheme.background.copy(alpha = 0.80f),
                ),
            ),
    )
}

/** Rounded-square card with a single Material icon inside — placeholder logo. */
@Composable
private fun OnboardingLogo(icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 10.dp,
        tonalElevation = 4.dp,
        modifier = Modifier.size(128.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
            )
        }
    }
}

/**
 * Welcome-step variant of [OnboardingLogo] that paints the real FreshWall
 * logo bitmap instead of a Material icon. The Surface only exists to cast a
 * soft drop shadow under the logo — the bitmap already carries its own
 * rounded-square corners, so the Surface's clip uses a matching radius and
 * a transparent fill (the bitmap fills the box edge-to-edge with no halo).
 */
@Composable
private fun WelcomeBrandLogo() {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp,
        modifier = Modifier.size(128.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = "FreshWall logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/* -------------------------------------------------------------------------- */
/*  STEP 3 — Mode select                                                      */
/* -------------------------------------------------------------------------- */

/**
 * ModeSelect — smaller hero, larger title text, and discrete floating
 * pill actions. Three sections (hero, copy, pills) are spread vertically
 * with weighted spacers so the page feels distributed rather than top-heavy.
 */
@Composable
private fun ModeSelectStep(
    backgroundImageUrl: String,
    onCombined: () -> Unit,
    onSeparate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ModeSelectHero(imageUrl = backgroundImageUrl)

        Spacer(Modifier.weight(0.6f))

        Text(
            text = "Same picks for both sources?",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Apply your category choices to both Pexels and Unsplash, " +
                "or tune each source separately.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier.padding(
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 32.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CompactPillButton(
                label = "Same for both",
                onClick = onCombined,
                primary = true,
            )
            CompactPillButton(
                label = "Customize each",
                onClick = onSeparate,
                primary = false,
            )
        }
    }
}

/**
 * Hero panel for ModeSelect — same mountain backdrop + brand-tags stack
 * as the picker pages, but smaller so the screen has room to breathe below.
 */
@Composable
private fun ModeSelectHero(imageUrl: String) {
    BackgroundHeader(
        gradientFrom = MaterialTheme.colorScheme.primaryContainer,
        gradientTo = MaterialTheme.colorScheme.tertiaryContainer,
        imageUrl = imageUrl,
        height = 260.dp,
    ) {
        BrandTagsStack()
    }
}

/* -------------------------------------------------------------------------- */
/*  STEP 4 — Category picker (reused for combined / pexels / unsplash)        */
/* -------------------------------------------------------------------------- */

/**
 * Re-entry point for editing categories from Settings. Shown after
 * onboarding completion. Reuses the picker UI but skips the welcome
 * and purpose intros — just mode select + picker(s).
 */
@Composable
fun CategoryEditorScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as FreshWallApplication }
    val current: CategoryConfig = app.categoryPreferences.config.value

    // Mini state machine: ModeSelect → Combined OR (Pexels → Unsplash).
    var step by remember {
        mutableStateOf(EditorStep.ModeSelect)
    }
    var mode by remember { mutableStateOf(current.mode) }
    var combinedSelection by remember {
        mutableStateOf(current.combinedCategories.toSet())
    }
    var pexelsSelection by remember {
        mutableStateOf(current.pexelsCategories.toSet())
    }
    var unsplashSelection by remember {
        mutableStateOf(current.unsplashCategories.toSet())
    }

    val modeSelectBg = remember { MOUNTAIN_IMAGE_URLS.random() }
    val combinedBg = remember { MOUNTAIN_IMAGE_URLS.random() }
    val pexelsBg = remember { MOUNTAIN_IMAGE_URLS.random() }
    val unsplashBg = remember { MOUNTAIN_IMAGE_URLS.random() }

    fun commit() {
        app.categoryPreferences.update {
            it.copy(
                mode = mode,
                combinedCategories = combinedSelection.toList(),
                pexelsCategories = pexelsSelection.toList(),
                unsplashCategories = unsplashSelection.toList(),
            )
        }
        onDone()
    }

    // From the first step, fall through so the host pops the editor off
    // the screen stack. From any later step, walk back internally.
    BackHandler(enabled = step != EditorStep.ModeSelect) {
        step = when (step) {
            EditorStep.ModeSelect -> step
            EditorStep.CombinedPicker -> EditorStep.ModeSelect
            EditorStep.PexelsPicker -> EditorStep.ModeSelect
            EditorStep.UnsplashPicker -> EditorStep.PexelsPicker
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        AnimatedContent(
            targetState = step,
            label = "editorStep",
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally(tween(320)) { w -> direction * w } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(320)) { w -> -direction * w } + fadeOut(tween(280)))
            },
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            when (current) {
                EditorStep.ModeSelect -> ModeSelectStep(
                    backgroundImageUrl = modeSelectBg,
                    onCombined = {
                        mode = CustomizationMode.COMBINED
                        step = EditorStep.CombinedPicker
                    },
                    onSeparate = {
                        mode = CustomizationMode.SEPARATE
                        step = EditorStep.PexelsPicker
                    },
                )

                EditorStep.CombinedPicker -> CategoryPickerStep(
                    title = "Pick what you like",
                    subtitle = "These drive both Pexels and Unsplash. Pick 5 to 15.",
                    headerAccent = TopHeaderAccent.Combined(imageUrl = combinedBg),
                    initialSelection = combinedSelection,
                    onNext = { selection ->
                        combinedSelection = selection
                        commit()
                    },
                )

                EditorStep.PexelsPicker -> CategoryPickerStep(
                    title = "Pexels picks",
                    subtitle = "What should we pull from Pexels? Pick 5 to 15.",
                    headerAccent = TopHeaderAccent.Pexels(imageUrl = pexelsBg),
                    initialSelection = pexelsSelection,
                    onNext = { selection ->
                        pexelsSelection = selection
                        step = EditorStep.UnsplashPicker
                    },
                )

                EditorStep.UnsplashPicker -> CategoryPickerStep(
                    title = "Unsplash picks",
                    subtitle = "And from Unsplash? Pick 5 to 15.",
                    headerAccent = TopHeaderAccent.Unsplash(imageUrl = unsplashBg),
                    initialSelection = unsplashSelection,
                    onNext = { selection ->
                        unsplashSelection = selection
                        commit()
                    },
                )
            }
        }
    }
}

private enum class EditorStep {
    ModeSelect, CombinedPicker, PexelsPicker, UnsplashPicker,
}
