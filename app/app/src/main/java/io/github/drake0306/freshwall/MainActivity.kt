package io.github.drake0306.freshwall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.drake0306.freshwall.data.ThemeMode
import io.github.drake0306.freshwall.ui.Screen
import io.github.drake0306.freshwall.ui.autorotate.AutoRotateScreen
import io.github.drake0306.freshwall.ui.detail.DetailScreen
import io.github.drake0306.freshwall.ui.donate.DonateScreen
import io.github.drake0306.freshwall.ui.drawer.DrawerItem
import io.github.drake0306.freshwall.ui.favorites.FavoritesScreen
import io.github.drake0306.freshwall.ui.featured.FeaturedViewModel
import io.github.drake0306.freshwall.ui.feedback.FeedbackScreen
import io.github.drake0306.freshwall.ui.home.HomeScreen
import io.github.drake0306.freshwall.ui.onboarding.CategoryEditorScreen
import io.github.drake0306.freshwall.ui.onboarding.OnboardingScreen
import io.github.drake0306.freshwall.ui.search.SearchScreen
import io.github.drake0306.freshwall.ui.settings.AboutScreen
import io.github.drake0306.freshwall.ui.settings.SettingsScreen
import io.github.drake0306.freshwall.ui.settings.ThemeScreen
import io.github.drake0306.freshwall.ui.theme.FreshWallTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called BEFORE super.onCreate.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = applicationContext as FreshWallApplication
            val themeMode by app.themePreferences.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            FreshWallTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    FreshWallApp()
                }
            }
        }
    }
}

private fun Screen.routeKey(): String = when (this) {
    is Screen.Home -> "home"
    is Screen.Search -> "search"
    is Screen.Settings -> "settings"
    is Screen.About -> "about"
    is Screen.Theme -> "theme"
    is Screen.Favorites -> "favorites"
    is Screen.AutoRotate -> "autorotate"
    is Screen.Donate -> "donate"
    is Screen.Feedback -> "feedback"
    is Screen.Onboarding -> "onboarding"
    is Screen.CategoryEditor -> "category-editor"
    is Screen.Detail -> "detail"
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun FreshWallApp() {
    val featuredViewModel: FeaturedViewModel = viewModel()
    val context = androidx.compose.ui.platform.LocalContext.current
    val app = remember(context) { context.applicationContext as FreshWallApplication }

    // First-launch routing: if the user hasn't been through onboarding yet,
    // start them there instead of Home. The screen stack is replaced (not
    // pushed) on onboarding completion so back-nav can't return them to
    // the welcome flow.
    val onboardingComplete = app.categoryPreferences.config.collectAsStateWithLifecycle().value
        .onboardingComplete
    val initialStack = remember(onboardingComplete) {
        if (onboardingComplete) listOf(Screen.Home) else listOf(Screen.Onboarding)
    }
    var screenStack: List<Screen> by remember { mutableStateOf(initialStack) }
    val saveableStateHolder = rememberSaveableStateHolder()
    val currentScreen = screenStack.last()

    val navigate: (Screen) -> Unit = { screenStack = screenStack + it }
    val goBack: () -> Unit = {
        if (screenStack.size > 1) screenStack = screenStack.dropLast(1)
    }

    // Onboarding handles its own back internally (step → previous step;
    // back from the very first step falls through to exit the app).
    // Everywhere else, back pops the screen stack.
    BackHandler(enabled = currentScreen !is Screen.Onboarding && screenStack.size > 1) {
        goBack()
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = currentScreen,
            label = "screen",
            transitionSpec = {
                val initial = initialState
                val target = targetState
                val bottomRight = TransformOrigin(1f, 1f)
                val easing = FastOutSlowInEasing
                val duration = 320
                when {
                    // Home → Search: expand from the bottom-right (where the
                    // search pill icon lives) outward across the screen.
                    initial is Screen.Home && target is Screen.Search -> {
                        (scaleIn(
                            animationSpec = tween(duration, easing = easing),
                            initialScale = 0f,
                            transformOrigin = bottomRight,
                        ) + fadeIn(tween(duration, easing = easing))) togetherWith
                            fadeOut(tween(duration, easing = easing))
                    }
                    // Search → Home: shrink back into the bottom-right corner.
                    initial is Screen.Search && target is Screen.Home -> {
                        fadeIn(tween(duration, easing = easing)) togetherWith
                            (scaleOut(
                                animationSpec = tween(duration, easing = easing),
                                targetScale = 0f,
                                transformOrigin = bottomRight,
                            ) + fadeOut(tween(duration, easing = easing)))
                    }
                    else -> {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    }
                }
            },
        ) { screen ->
            saveableStateHolder.SaveableStateProvider(key = screen.routeKey()) {
                when (screen) {
                    is Screen.Home -> HomeScreen(
                        viewModel = featuredViewModel,
                        onWallpaperClick = { navigate(Screen.Detail(it)) },
                        onSearchClick = { navigate(Screen.Search) },
                        onDrawerItemClick = { item ->
                            when (item) {
                                DrawerItem.FAVORITES -> navigate(Screen.Favorites)
                                DrawerItem.AUTO_ROTATE -> navigate(Screen.AutoRotate)
                                DrawerItem.DONATE -> navigate(Screen.Donate)
                                DrawerItem.SETTINGS -> navigate(Screen.Settings)
                                DrawerItem.ABOUT -> navigate(Screen.About)
                            }
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                    )
                    is Screen.Search -> SearchScreen(
                        featuredViewModel = featuredViewModel,
                        onClose = goBack,
                        onWallpaperClick = { navigate(Screen.Detail(it)) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                    )
                    is Screen.Settings -> SettingsScreen(
                        onBack = goBack,
                        onThemeClick = { navigate(Screen.Theme) },
                        onFeedbackClick = { navigate(Screen.Feedback) },
                        onCategoriesClick = { navigate(Screen.CategoryEditor) },
                    )
                    is Screen.Onboarding -> OnboardingScreen(
                        onFinished = { screenStack = listOf(Screen.Home) },
                    )
                    is Screen.CategoryEditor -> CategoryEditorScreen(
                        onBack = goBack,
                        onDone = goBack,
                    )
                    is Screen.About -> AboutScreen(onBack = goBack)
                    is Screen.Theme -> ThemeScreen(onBack = goBack)
                    is Screen.AutoRotate -> AutoRotateScreen(onBack = goBack)
                    is Screen.Donate -> DonateScreen(onBack = goBack)
                    is Screen.Feedback -> FeedbackScreen(onBack = goBack)
                    is Screen.Favorites -> FavoritesScreen(
                        onBack = goBack,
                        onWallpaperClick = { navigate(Screen.Detail(it)) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                    )
                    is Screen.Detail -> DetailScreen(
                        wallpaper = screen.wallpaper,
                        onBack = goBack,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedContent,
                    )
                }
            }
        }
    }
}
