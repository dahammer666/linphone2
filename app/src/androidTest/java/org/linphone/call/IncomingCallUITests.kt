package org.linphone.call

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.linphone.R
import org.linphone.methods.*
import org.linphone.methods.UITestsScreenshots.takeScreenshot
import org.linphone.methods.UITestsUtils.checkWithTimeout
import org.linphone.utils.AppUtils.Companion.getString

@RunWith(AndroidJUnit4::class)
class IncomingCallUITests {

    val methods = CallViewUITestsMethods

    @get:Rule
    val linphoneUITestRule = LinphoneUITestRule(LinphonePermissions.CALL, true, 2)

    @Before
    fun setUp() {
        UITestsUtils.testAppSetup()
        methods.refreshAccountInfo()
        takeScreenshot("dialer_view")
        methods.startIncomingCall()
        methods.onPushAction(getString(R.string.incoming_call_notification_title), UITestsView.incomingCallView)
        takeScreenshot("incoming_call_view")
    }

    @After
    fun tearDown() {
        methods.endCall()
    }

    @Test
    fun testViewDisplay() {
        methods.checkCallTime(onView(ViewMatchers.withId(R.id.incoming_call_timer)), methods.startCallTime)
        methods.endCall(UITestsView.incomingCallView)
        takeScreenshot("dialer_view")
    }

    @Test
    fun testNoAnswer() {
        UITestsView.incomingCallView.checkWithTimeout(doesNotExist(), 30.0)
        takeScreenshot("dialer_view")
    }

    @Test
    fun testDecline() {
        methods.onCallAction(R.id.hangup, UITestsView.incomingCallView, doesNotExist())
        takeScreenshot("dialer_view")
    }

    @Test
    fun testAccept() {
        methods.onCallAction(R.id.answer, UITestsView.singleCallView, matches(isDisplayed()))
        takeScreenshot("single_call_view")
        methods.endCall(UITestsView.singleCallView)
        takeScreenshot("dialer_view")
    }
}
