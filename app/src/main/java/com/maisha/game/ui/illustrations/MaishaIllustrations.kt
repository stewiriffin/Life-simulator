// app/src/main/java/com/maisha/game/ui/illustrations/MaishaIllustrations.kt
package com.maisha.game.ui.illustrations

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.maisha.game.data.IllustrationCatalog
import com.maisha.game.ui.components.AssetHeroImage
import com.maisha.game.ui.components.IllustrationImage

enum class OnboardingIllustration {
    WELCOME,
    AGE_UP,
    CHOICES,
    WORLD,
    READY
}

enum class EmptyStateIllustration {
    FAMILY,
    ASSETS,
    ACTIONS,
    ACHIEVEMENTS,
    RETIRED
}

private fun onboardingResource(type: OnboardingIllustration): String = when (type) {
    OnboardingIllustration.WELCOME -> "img_onboarding_welcome"
    OnboardingIllustration.AGE_UP -> "img_onboarding_age_up"
    OnboardingIllustration.CHOICES -> "img_onboarding_choices"
    OnboardingIllustration.WORLD -> "img_onboarding_world"
    OnboardingIllustration.READY -> "img_onboarding_ready"
}

private fun emptyStateResource(type: EmptyStateIllustration): String = when (type) {
    EmptyStateIllustration.FAMILY -> "img_empty_family"
    EmptyStateIllustration.ASSETS -> "img_empty_assets"
    EmptyStateIllustration.ACTIONS -> "img_empty_actions"
    EmptyStateIllustration.ACHIEVEMENTS -> "img_empty_achievements"
    EmptyStateIllustration.RETIRED -> "img_empty_retired"
}

@Composable
fun OnboardingIllustrationView(
    type: OnboardingIllustration,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    AssetHeroImage(
        ref = IllustrationCatalog.rasterNamed(onboardingResource(type)),
        modifier = modifier,
        height = size,
        cornerRadius = 16.dp
    )
}

@Composable
fun EmptyStateIllustrationView(
    type: EmptyStateIllustration,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    IllustrationImage(
        ref = IllustrationCatalog.rasterNamed(emptyStateResource(type)),
        size = size,
        modifier = modifier
    )
}
