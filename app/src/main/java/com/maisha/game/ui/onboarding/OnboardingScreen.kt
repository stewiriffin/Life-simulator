// app/src/main/java/com/maisha/game/ui/onboarding/OnboardingScreen.kt (modified — Maisha illustrations)
package com.maisha.game.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.maisha.game.R
import com.maisha.game.ui.illustrations.OnboardingIllustration
import com.maisha.game.ui.illustrations.OnboardingIllustrationView
import com.maisha.game.ui.theme.AppIcons
import com.maisha.game.ui.theme.GoldAccent
import com.maisha.game.ui.theme.MaishaRadius
import com.maisha.game.ui.theme.MaishaSpacing
import com.maisha.game.ui.theme.NavyDeep
import com.maisha.game.ui.theme.TealPrimary
import kotlinx.coroutines.launch

private const val SLIDE_COUNT = 5

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { SLIDE_COUNT })
    val scope = rememberCoroutineScope()
    val isLastSlide = pagerState.currentPage == SLIDE_COUNT - 1

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (!isLastSlide) {
                    TextButton(onClick = onSkip) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = GoldAccent
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> WelcomeSlide()
                    1 -> AgeUpSlide()
                    2 -> ChoicesSlide()
                    3 -> WorldIdentitySlide()
                    else -> StartLifeSlide()
                }
            }

            PagerDots(
                pageCount = SLIDE_COUNT,
                currentPage = pagerState.currentPage
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLastSlide) {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaishaRadius.buttonShape,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 2.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = NavyDeep
                    )
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_start_first_life),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaishaRadius.cardShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.btn_continue),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WelcomeSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OnboardingIllustrationView(
            type = OnboardingIllustration.WELCOME,
            modifier = Modifier.padding(bottom = MaishaSpacing.sm)
        )
        Spacer(modifier = Modifier.height(MaishaSpacing.sm))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = TealPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.tagline),
            style = MaterialTheme.typography.titleMedium,
            color = GoldAccent,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun AgeUpSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OnboardingIllustrationView(
            type = OnboardingIllustration.AGE_UP,
            size = 140.dp,
            modifier = Modifier.padding(bottom = MaishaSpacing.md)
        )
        Text(
            text = stringResource(R.string.onboarding_age_up_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_age_up_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        AgeUpMockup()
    }
}

@Composable
private fun AgeUpMockup() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaishaRadius.cardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.label_years),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "18",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = TealPrimary
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GoldAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.btn_age_up),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyDeep
                )
            }
        }
    }
}

@Composable
private fun ChoicesSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OnboardingIllustrationView(
            type = OnboardingIllustration.CHOICES,
            size = 120.dp,
            modifier = Modifier.padding(bottom = MaishaSpacing.md)
        )
        Text(
            text = stringResource(R.string.onboarding_choices_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_choices_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                Triple(R.string.nav_family, AppIcons.NavFamily, TealPrimary),
                Triple(R.string.nav_career, AppIcons.NavCareer, GoldAccent),
                Triple(R.string.label_net_worth, AppIcons.Money, GoldAccent)
            ).forEach { (labelRes, icon, tint) ->
                Card(
                    modifier = Modifier.weight(1f),
                    shape = MaishaRadius.buttonShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldAccent,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldIdentitySlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OnboardingIllustrationView(
            type = OnboardingIllustration.WORLD,
            modifier = Modifier.padding(bottom = MaishaSpacing.sm)
        )
        Spacer(modifier = Modifier.height(MaishaSpacing.sm))
        Text(
            text = stringResource(R.string.onboarding_world_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_world_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StartLifeSlide() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OnboardingIllustrationView(
            type = OnboardingIllustration.READY,
            modifier = Modifier.padding(bottom = MaishaSpacing.sm)
        )
        Spacer(modifier = Modifier.height(MaishaSpacing.sm))
        Text(
            text = stringResource(R.string.onboarding_ready_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_ready_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PagerDots(pageCount: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (selected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) TealPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    )
            )
        }
    }
}
