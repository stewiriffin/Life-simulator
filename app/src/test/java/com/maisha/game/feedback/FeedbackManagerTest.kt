package com.maisha.game.feedback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun playCue_doesNothingWhenSoundDisabled() = runTest {
        val soundFlow = MutableStateFlow(false)
        val manager = FeedbackManager.forTest(context, soundFlow)
        advanceUntilIdle()

        manager.resetPlayTrackingForTest()
        manager.playCue(context, FeedbackCue(sound = SoundEffect.BUTTON_TAP))

        assertTrue(manager.lastSoundPlayAttempted)
        assertFalse(manager.lastSoundPlayExecuted)
    }

    @Test
    fun playCue_respectsSoundFlowToggle() = runTest {
        val soundFlow = MutableStateFlow(true)
        val manager = FeedbackManager.forTest(context, soundFlow)
        advanceUntilIdle()

        soundFlow.value = false
        advanceUntilIdle()

        manager.resetPlayTrackingForTest()
        manager.playCue(context, FeedbackCue(sound = SoundEffect.BUTTON_TAP))

        assertFalse(manager.lastSoundPlayExecuted)
    }
}
