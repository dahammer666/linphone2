package org.linphone.methods

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import kotlinx.coroutines.*
import org.linphone.R
import org.linphone.core.AuthInfo
import org.linphone.core.Call
import org.linphone.methods.UITestsUtils.activityScenario
import org.linphone.methods.UITestsUtils.checkWithTimeout
import org.linphone.utils.AppUtils.Companion.getString

class CallViewUITestsMethods {

    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val manager = UITestsCoreManager.instance
    val appAccountAuthInfo: AuthInfo = UITestsCoreManager.instance.appAccountAuthInfo
    val ghostAccount: UITestsRegisteredLinphoneCore = UITestsCoreManager.instance.ghostAccounts[0]

    fun startIncomingCall() {
        if (ghostAccount.callState != Call.State.Released) { ghostAccount.terminateCall() }

        ghostAccount.startCall(manager.createAddress(appAccountAuthInfo))
        ghostAccount.waitForCallState(Call.State.OutgoingRinging, 5.0)

        waitForCallNotification(true, 5.0)
    }

    fun startOutgoingCall() {
        if (ghostAccount.callState != Call.State.Released) { ghostAccount.terminateCall() }

        onView(withId(R.id.sip_uri_input)).perform(typeText(ghostAccount.mAuthInfo.username))
        onView(withContentDescription(R.string.content_description_start_call)).perform(click())

        onView(withId(R.id.outgoing_call_layout)).checkWithTimeout(matches(isDisplayed()), 5.0)
    }

    fun endCall() {
        if (ghostAccount.callState == Call.State.Released) { return }

        ghostAccount.terminateCall()
        ghostAccount.waitForCallState(Call.State.Released, 5.0)
        onView(withId(R.id.outgoing_call_layout)).checkWithTimeout(doesNotExist(), 5.0)
        // onView(withId(com.google.android.material.R.id.snackbar_text)).checkWithTimeout(doesNotExist(), 5.0)
    }

    fun noAnswerCallFromPush() {
        waitForCallNotification(false, 30.0)
    }

    fun declineCallFromPush() {
        val declineLabel = getString(R.string.incoming_call_notification_answer_action_label)

        try {
            val decline = device.findObject(By.textContains(declineLabel))
            decline.click()
        } catch (e: java.lang.NullPointerException) {
            throw AssertionError("[UITests] Enable to find the \"$declineLabel\" button in the incoming call notification")
        }
    }

    fun answerCallFromPush() {
        val answerLabel = getString(R.string.incoming_call_notification_answer_action_label)
        try {
            val answer = device.findObject(By.textContains(answerLabel))
            answer.click()
        } catch (e: java.lang.NullPointerException) {
            throw AssertionError("[UITests] Enable to find the \"$answerLabel\" button in the incoming call notification")
        }
        onView(withId(R.id.single_call_layout)).checkWithTimeout(matches(isDisplayed()), 5.0)
    }

    fun openIncomingCallViewFromPush() {
        try {
            val notif = device.findObject(By.textContains(getString(R.string.incoming_call_notification_title)))
            notif.click()
        } catch (e: java.lang.NullPointerException) {
            throw AssertionError("[UITests] Enable to find the incoming call notification")
        }
        onView(withId(R.id.incoming_call_layout)).checkWithTimeout(matches(isDisplayed()), 5.0)
    }

    fun declineCallFromIncomingCallView() {
        onView(withId(R.id.hangup)).checkWithTimeout(matches(isDisplayed()), 5.0)
        onView(withId(R.id.hangup)).perform(click())
        onView(withId(R.id.incoming_call_layout)).checkWithTimeout(doesNotExist(), 5.0)
    }

    fun answerCallFromIncomingCallView() {
        onView(withId(R.id.answer)).checkWithTimeout(matches(isDisplayed()), 5.0)
        onView(withId(R.id.answer)).perform(click())
        onView(withId(R.id.single_call_layout)).checkWithTimeout(matches(isDisplayed()), 5.0)
    }

    fun cancelCallFromOutgoingCallView() {
        onView(withId(R.id.hangup)).checkWithTimeout(matches(isDisplayed()), 5.0)
        onView(withId(R.id.hangup)).perform(click())
        onView(withId(R.id.outgoing_call_layout)).checkWithTimeout(doesNotExist(), 5.0)
    }

    fun noAnswerCallFromIncomingCall() {
        onView(withId(R.id.incoming_call_layout)).checkWithTimeout(doesNotExist(), 30.0)
    }

    fun noAnswerCallFromOutgoingCall() {
        onView(withId(R.id.outgoing_call_layout)).checkWithTimeout(doesNotExist(), 30.0)
        /*
        val snackbar = onView(withId(com.google.android.material.R.id.snackbar_text))
        snackbar.checkWithTimeout(
            matches(
                allOf(
                    withText(containsString("Error")),
                    withText(containsString("Request Timeout"))
                )
            ),
            5.0
        )
        snackbar.checkWithTimeout(doesNotExist(), 5.0)
        */
    }

    private fun waitForCallNotification(exist: Boolean, timeout: Double) = runBlocking {
        var result = false
        val wait = launch(Dispatchers.Default) {
            lateinit var activity: Activity
            activityScenario!!.onActivity { act -> activity = act }
            val manager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            repeat(timeout.toInt() * 10) {
                for (notif in manager.activeNotifications) {
                    if (notif.notification.channelId == getString(R.string.notification_channel_incoming_call_id)) {
                        result = true
                        break
                    }
                    result = false
                }
                if (result == exist) { cancel() }
                delay(100)
            }
        }
        wait.join()
        delay(500)
        assert(result == exist) { "[UITests] Incoming call Notification still ${if (exist) "not " else ""}displayed after $timeout seconds" }
    }
}
