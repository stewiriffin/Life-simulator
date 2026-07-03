package com.maisha.game.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.maisha.game.domain.TestFixtures
import com.maisha.game.ui.theme.MaishaTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PersonCardRecompositionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun personCard_doesNotRecomposeWhenUnrelatedGlobalStateChanges() {
        val person = TestFixtures.person(id = "sibling-1", name = "Sam Sibling")
        val recomposeCount = AtomicInteger(0)

        composeRule.setContent {
            var unrelatedTick by mutableIntStateOf(0)
            MaishaTheme {
                Column {
                    Button(
                        onClick = { unrelatedTick++ },
                        modifier = Modifier.testTag("unrelated_bump")
                    ) {
                        Text("Tick $unrelatedTick")
                    }
                    TrackedPersonCard(
                        recomposeCount = recomposeCount,
                        person = person,
                        relationLabel = "Sibling"
                    )
                }
            }
        }

        composeRule.runOnIdle {
            assertEquals(1, recomposeCount.get())
        }

        composeRule.onNodeWithTag("unrelated_bump").performClick()
        composeRule.onNodeWithTag("unrelated_bump").performClick()

        composeRule.runOnIdle {
            assertEquals(
                "PersonCard should not recompose when only unrelated state changes",
                1,
                recomposeCount.get()
            )
        }
    }
}

@Composable
private fun TrackedPersonCard(
    recomposeCount: AtomicInteger,
    person: com.maisha.game.data.model.Person,
    relationLabel: String
) {
    SideEffect {
        recomposeCount.incrementAndGet()
    }
    PersonCard(
        person = person,
        relationLabel = relationLabel,
        onClick = {},
        modifier = Modifier.testTag("person_card")
    )
}
