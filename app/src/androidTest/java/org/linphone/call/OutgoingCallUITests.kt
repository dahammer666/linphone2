package org.linphone.call

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
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

@RunWith(AndroidJUnit4::class)
@LargeTest
class OutgoingCallUITests {

    lateinit var methods: CallViewUITestsMethods

    @get:Rule
    val screenshotsRule = ScreenshotsRule(true)

    @get:Rule
    var mGrantPermissionRule = GrantPermissionRule.grant(*LinphonePermissions.CALL)

    @Before
    fun setUp() {
        UITestsUtils.testAppSetup()
        methods = CallViewUITestsMethods()
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
        methods.endCall()
        takeScreenshot("dialer_view", "declined")
    }

    @Test
    fun testNoAnswer() {
        methods.noAnswerCallFromOutgoingCall()
        takeScreenshot("dialer_view", "no_answer")
    }

    @Test
    fun testToggleMute() {
        Espresso.onView(withId(R.id.microphone)).perform(ViewActions.click())
        takeScreenshot("outgoing_call_view", "mute")
        Espresso.onView(withId(R.id.microphone)).perform(ViewActions.click())
        takeScreenshot("outgoing_call_view")
        methods.endCall()
        takeScreenshot("dialer_view", "declined")
    }

    @Test
    fun testToggleSpeaker() {
        Espresso.onView(withId(R.id.speaker)).perform(ViewActions.click())
        takeScreenshot("outgoing_call_view", "speaker")
        Espresso.onView(withId(R.id.speaker)).perform(ViewActions.click())
        takeScreenshot("outgoing_call_view")
        methods.endCall()
        takeScreenshot("dialer_view", "declined")
    }

    @Test
    fun testCancel() {
        methods.cancelCallFromOutgoingCallView()
        takeScreenshot("dialer_view")
    }
}
