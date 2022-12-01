package org.linphone.call

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.linphone.methods.CallViewUITestsMethods
import org.linphone.methods.UITestsUtils

@RunWith(AndroidJUnit4::class)
class IncomingCallUITests {

    lateinit var methods: CallViewUITestsMethods

    @Before
    fun setUp() {
        UITestsUtils.testAppSetup()
        methods = CallViewUITestsMethods()
    }

    // notification tests
    @Test
    fun testDisplayCallPush() {
        methods.startIncomingCall()
        methods.endCall()
    }

    @Test
    fun testNoAnswerCallPush() {
        methods.startIncomingCall()
        methods.noAnswerCallFromPush()
    }

    @Test
    fun testDeclineCallPush() {
        methods.startIncomingCall()
        methods.declineCallFromPush()
    }

    @Test
    fun testAnswerCallPush() {
        methods.startIncomingCall()
        methods.answerCallFromPush()
        methods.endCall()
    }

    // incoming call view tests
    @Test
    fun testOpenIncomingCallView() {
        methods.startIncomingCall()
        methods.openIncomingCallViewFromPush()
        methods.endCall()
    }

    @Test
    fun testNoAnswerIncomingCallView() {
        methods.startIncomingCall()
        methods.openIncomingCallViewFromPush()
        methods.noAnswerCallFromIncomingCall()
    }

    @Test
    fun testDeclineIncomingCallView() {
        methods.startIncomingCall()
        methods.openIncomingCallViewFromPush()
        methods.declineCallFromIncomingCallView()
        methods.endCall()
    }

    @Test
    fun testAcceptIncomingCallView() {
        methods.startIncomingCall()
        methods.openIncomingCallViewFromPush()
        methods.answerCallFromIncomingCallView()
        methods.endCall()
    }
}
