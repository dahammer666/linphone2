package org.linphone.call

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.linphone.R
import org.linphone.methods.*
import org.linphone.methods.UITestsScreenshots.takeScreenshot
import org.linphone.methods.UITestsUtils.checkWithTimeout

@RunWith(AndroidJUnit4::class)
@LargeTest
class OutgoingCallUITests {

    val methods = CallViewUITestsMethods

    @get:Rule
    val screenshotsRule = ScreenshotsRule(true)

    @get:Rule
    var mGrantPermissionRule = GrantPermissionRule.grant(*LinphonePermissions.CALL)

    @Before
    fun setUp() {
        UITestsUtils.testAppSetup()
        methods.refreshAccountInfo()
        takeScreenshot("dialer_view")
        methods.startOutgoingCall()
        takeScreenshot("outgoing_call_view")
    }

    @After
    fun tearDown() {
        methods.endCall()
    }

    @Test
    fun testViewDisplay() {
        methods.endCall(UITestsView.outgoingCallView)
        takeScreenshot("dialer_view", "declined")
    }

    @Test
    fun testNoAnswer() {
        UITestsView.outgoingCallView.checkWithTimeout(doesNotExist(), 30.0)
        takeScreenshot("dialer_view", "no_answer")
    }

    @Test
    fun testToggleMute() {
        onView(withId(R.id.microphone)).perform(click())
        takeScreenshot("outgoing_call_view", "mute")
        onView(withId(R.id.microphone)).perform(click())
        takeScreenshot("outgoing_call_view")
        methods.endCall()
        takeScreenshot("dialer_view", "declined")
    }

    @Test
    fun testToggleSpeaker() {
        onView(withId(R.id.speaker)).perform(click())
        takeScreenshot("outgoing_call_view", "speaker")
        onView(withId(R.id.speaker)).perform(click())
        takeScreenshot("outgoing_call_view")
        methods.endCall()
        takeScreenshot("dialer_view", "declined")
    }

    @Test
    fun testCancel() {
        methods.onCallAction(R.id.hangup, UITestsView.outgoingCallView, doesNotExist())
        takeScreenshot("dialer_view")
    }
}
