package com.example.freshwall.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import com.example.freshwall.FreshWallApplication
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
 * Transitions are pure forward slide-from-right; the user cannot tap back
 * (system back is intercepted at the host level). On completion, the host
 * persists the selection and navigates to Home.
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        AnimatedContent(
            targetState = step,
            label = "onboardingStep",
            transitionSpec = {
                // Forward-only slide: new content slides in from the right
                // while old content slides out to the left.
                (slideInHorizontally(tween(320)) { it } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(320)) { -it } + fadeOut(tween(280)))
            },
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            when (current) {
                OnboardingStep.Welcome -> WelcomeStep(
                    onNext = { step = OnboardingStep.Purpose },
                )

                OnboardingStep.Purpose -> PurposeStep(
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

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    OnboardingScaffold(
        gradientFrom = MaterialTheme.colorScheme.primaryContainer,
        gradientTo = MaterialTheme.colorScheme.tertiaryContainer,
        headerContent = { HeroIcon(icon = Icons.Outlined.AutoAwesome) },
        title = "Hello — welcome to FreshWall",
        subtitle = "A calmer way to find a wallpaper you'll actually want " +
            "to keep on your screen. Let's get you set up in a minute.",
        actions = {
            PillButton(
                label = "Next",
                onClick = onNext,
                primary = true,
            )
        },
    )
}

/* -------------------------------------------------------------------------- */
/*  STEP 2 — Purpose                                                          */
/* -------------------------------------------------------------------------- */

@Composable
private fun PurposeStep(
    onCustomize: () -> Unit,
    onSkip: () -> Unit,
) {
    OnboardingScaffold(
        gradientFrom = MaterialTheme.colorScheme.tertiaryContainer,
        gradientTo = MaterialTheme.colorScheme.secondaryContainer,
        headerContent = { HeroIcon(icon = Icons.Outlined.Palette) },
        title = "Tell us what you like",
        subtitle = "Pick a few categories and we'll use them to pull " +
            "fresh wallpapers from Pexels and Unsplash whenever you open " +
            "the app. You can change these anytime in Settings.",
        actions = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PillButton(
                    label = "Yes, let's customize",
                    onClick = onCustomize,
                    primary = true,
                )
                PillButton(
                    label = "Skip for now",
                    onClick = onSkip,
                    primary = false,
                )
            }
        },
    )
}

/* -------------------------------------------------------------------------- */
/*  STEP 3 — Mode select                                                      */
/* -------------------------------------------------------------------------- */

@Composable
private fun ModeSelectStep(
    backgroundImageUrl: String,
    onCombined: () -> Unit,
    onSeparate: () -> Unit,
) {
    OnboardingScaffold(
        gradientFrom = MaterialTheme.colorScheme.secondaryContainer,
        gradientTo = MaterialTheme.colorScheme.primaryContainer,
        // Mountain photo backdrop instead of a flat gradient — the brand
        // tags pop against the dark scrim and frame the question visually.
        backgroundImageUrl = backgroundImageUrl,
        heroHeight = 320.dp,
        headerContent = { BrandTagsStack() },
        title = "Same picks for both sources?",
        subtitle = "We can apply your category choices to both Pexels and " +
            "Unsplash together, or let you tune each one separately.",
        actions = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PillButton(
                    label = "Same for both",
                    onClick = onCombined,
                    primary = true,
                )
                PillButton(
                    label = "Customize each",
                    onClick = onSeparate,
                    primary = false,
                )
            }
        },
    )
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        AnimatedContent(
            targetState = step,
            label = "editorStep",
            transitionSpec = {
                (slideInHorizontally(tween(320)) { it } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(320)) { -it } + fadeOut(tween(280)))
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
