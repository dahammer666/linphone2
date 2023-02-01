package org.linphone.call

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.linphone.R
import org.linphone.methods.*
import org.linphone.methods.UITestsScreenshots.takeScreenshot
import org.linphone.utils.AppUtils.Companion.getString

@RunWith(AndroidJUnit4::class)
class IncomingCallPushUITests {

    val methods = CallViewUITestsMethods

    @get:Rule
    val linphoneUITestRule = LinphoneUITestRule(LinphonePermissions.CALL, true, 2)

    @Before
    fun setUp() {
        UITestsUtils.testAppSetup()
        methods.refreshAccountInfo()
        takeScreenshot("dialer_view")
        methods.startIncomingCall()
        takeScreenshot("dialer_view", "incoming_call_push")
    }

    @After
    fun tearDown() {
        methods.endCall()
    }

    @Test
    fun testPushDisplay() {
        methods.endCall()
        takeScreenshot("dialer_view")
    }

    @Test
    fun testNoAnswer() {
        methods.waitForCallNotification(false, 30.0)
        takeScreenshot("dialer_view")
    }

    @Test
    fun testOnClick() {
        methods.onPushAction(getString(R.string.incoming_call_notification_title), UITestsView.incomingCallView)
        takeScreenshot("incoming_call_view")
        methods.endCall(UITestsView.incomingCallView)
        takeScreenshot("dialer_view")
    }

    @Test
    fun testDecline() {
        methods.onPushAction("Decline", null)
        methods.waitForCallNotification(false, 5.0)
        takeScreenshot("dialer_view")
    }

    @Test
    fun testAnswer() {
        methods.onPushAction(getString(R.string.incoming_call_notification_answer_action_label), UITestsView.singleCallView)
        takeScreenshot("single_call_view")
        methods.endCall(UITestsView.singleCallView)
        takeScreenshot("dialer_view")
    }
}
