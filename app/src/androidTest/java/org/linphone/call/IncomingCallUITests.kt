package org.linphone.call

import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
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
import org.linphone.utils.AppUtils.Companion.getString

@RunWith(AndroidJUnit4::class)
class IncomingCallUITests {

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
        methods.startIncomingCall()
        methods.onPushAction(getString(R.string.incoming_call_notification_title), UITestsView.incomingCallView)
        takeScreenshot("incoming_call_view")
    }

    @After
    fun tearDown() {
        methods.endCall()
    }

    @Test
    fun testOpenIncomingCallView() {
        methods.endCall(UITestsView.incomingCallView)
        takeScreenshot("dialer_view")
    }

    @Test
    fun testNoAnswerIncomingCallView() {
        UITestsView.incomingCallView.checkWithTimeout(doesNotExist(), 30.0)
        takeScreenshot("dialer_view")
    }

    @Test
    fun testDeclineIncomingCallView() {
        methods.onCallAction(R.id.hangup, UITestsView.incomingCallView, doesNotExist())
        takeScreenshot("dialer_view")
    }

    @Test
    fun testAcceptIncomingCallView() {
        methods.onCallAction(R.id.answer, UITestsView.singleCallView, matches(isDisplayed()))
        takeScreenshot("single_call_view")
        methods.endCall(UITestsView.singleCallView)
        takeScreenshot("dialer_view")
    }
}
