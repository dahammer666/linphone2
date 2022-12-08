package org.linphone.call

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import java.util.*
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
@LargeTest
class IncomingCallPushUITests {

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
        takeScreenshot("dialer_view", "incoming_call_push")
    }

    @After
    fun tearDown() {
        methods.endCall()
    }

    @Test
    fun testDisplayCallPush() {
        methods.endCall()
        takeScreenshot("dialer_view")
    }

    @Test
    fun testNoAnswerCallPush() {
        methods.waitForCallNotification(false, 30.0)
        takeScreenshot("dialer_view")
    }

    @Test
    fun testClickOnCallPush() {
        val time = Date().time
        methods.onPushAction(getString(R.string.incoming_call_notification_title), UITestsView.incomingCallView)
        methods.checkCallTime(onView(withId(R.id.incoming_call_timer)), time)
        takeScreenshot("incoming_call_view")
        methods.endCall(UITestsView.incomingCallView)
        takeScreenshot("dialer_view")
    }

    @Test
    fun testDeclineCallPush() {
        methods.onPushAction("Decline", null)
        methods.waitForCallNotification(false, 5.0)
        takeScreenshot("dialer_view")
    }

    @Test
    fun testAnswerCallPush() {
        val time = Date().time
        methods.onPushAction(getString(R.string.incoming_call_notification_answer_action_label), UITestsView.singleCallView)
        methods.checkCallTime(onView(withId(R.id.active_call_timer)), time)
        takeScreenshot("single_call_view")
        methods.endCall(UITestsView.singleCallView)
        takeScreenshot("dialer_view")
    }
}
