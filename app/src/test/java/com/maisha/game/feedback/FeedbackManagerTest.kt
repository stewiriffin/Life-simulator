package com.maisha.game.feedback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackManagerTest {

    private lateinit var context: Context
    private var manager: FeedbackManager? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        manager?.release()
        manager = null
    }

    @Test
    fun playCue_doesNothingWhenSoundDisabled() = runTest {
        val soundFlow = MutableStateFlow(false)
        manager = FeedbackManager.forTest(context, soundFlow).also { underTest ->
            advanceUntilIdle()

            underTest.resetPlayTrackingForTest()
            underTest.playCue(context, FeedbackCue(sound = SoundEffect.BUTTON_TAP))

            assertTrue(underTest.lastSoundPlayAttempted)
            assertFalse(underTest.lastSoundPlayExecuted)
        }
    }

    @Test
    fun playCue_respectsSoundFlowToggle() = runTest {
        val soundFlow = MutableStateFlow(true)
        manager = FeedbackManager.forTest(context, soundFlow).also { underTest ->
            advanceUntilIdle()

            soundFlow.value = false
            advanceUntilIdle()

            underTest.resetPlayTrackingForTest()
            underTest.playCue(context, FeedbackCue(sound = SoundEffect.BUTTON_TAP))

            assertFalse(underTest.lastSoundPlayExecuted)
        }
    }
}
