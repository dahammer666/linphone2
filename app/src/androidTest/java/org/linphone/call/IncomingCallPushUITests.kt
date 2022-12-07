package org.linphone.call

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.linphone.methods.*
import org.linphone.methods.UITestsScreenshots.takeScreenshot

@RunWith(AndroidJUnit4::class)
class IncomingCallPushUITests {

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
        methods.noAnswerCallFromPush()
        takeScreenshot("dialer_view")
    }

    @Test
    fun testClickOnCallPush() {
        methods.openIncomingCallViewFromPush()
        takeScreenshot("incoming_call_view")
        methods.endCall()
        takeScreenshot("dialer_view")
    }

    @Test
    fun testDeclineCallPush() {
        methods.declineCallFromPush()
        takeScreenshot("dialer_view")
    }

    @Test
    fun testAnswerCallPush() {
        methods.answerCallFromPush()
        takeScreenshot("single_call_view")
        methods.endCall()
        takeScreenshot("dialer_view")
    }
}
