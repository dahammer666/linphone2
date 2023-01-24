package org.linphone.methods

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import java.util.*
import kotlinx.coroutines.*
import org.junit.AssumptionViolatedException
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.MultipleFailureException
import org.junit.runners.model.Statement
import org.linphone.LinphoneApplication
import org.linphone.R
import org.linphone.activities.main.MainActivity
import org.linphone.activities.main.settings.viewmodels.AccountSettingsViewModel
import org.linphone.activities.main.viewmodels.StatusViewModel
import org.linphone.core.Factory
import org.linphone.core.TransportType
import org.linphone.core.tools.Log

class LinphoneUITestRule(
    private val permissions: Array<String>,
    private val screenshots: Boolean,
    private val maxAttempts: Int
) : TestRule {

    // @get: Rule
    // var grantPermissionRule = GrantPermissionRule.grant(*permissions)

    private var attemptNumber = 1

    fun onStart(description: Description) {
        UITestsScreenshots.screenshotComparison = screenshots
        UITestsScreenshots.definePath(description.className, description.methodName, Date().time.toString())
        if (screenshots && !UITestsScreenshots.defaultPath.isDirectory) {
            UITestsScreenshots.defaultPath.mkdirs()
        }
    }

    fun onFailure(
        base: Statement,
        description: Description,
        e: Throwable,
        errors: MutableList<Throwable>
    ) {
        if (attemptNumber <= maxAttempts) {
            Log.e("[UITests] ${description.displayName} attempt $attemptNumber failed")
            Log.e("[UITests] ${description.displayName} $e")
            Log.e("[UITests] ${description.displayName} launch of an attempt ${++attemptNumber} ")
            onStart(description)
            base.evaluate()
        } else {
            errors.add(e)
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                val errors: MutableList<Throwable> = ArrayList()
                onStart(description)
                try {
                    base.evaluate()
                    // on succeed
                } catch (e: AssumptionViolatedException) {
                    errors.add(e)
                    // on skip
                } catch (e: Throwable) {
                    onFailure(base, description, e, errors)
                } finally {
                    // on finish
                }

                MultipleFailureException.assertEmpty(errors)
            }
        }
    }
}

object LinphonePermissions {

    val LAUNCH = arrayListOf(
        "android.permission.READ_PHONE_NUMBERS",
        "android.permission.MANAGE_OWN_CALLS",
        "android.permission.POST_NOTIFICATIONS",
        "android.permission.READ_PHONE_STATE"
    ).toTypedArray()

    val CALL = LAUNCH + arrayListOf(
        "android.permission.BLUETOOTH_CONNECT",
        "android.permission.RECORD_AUDIO"
    ).toTypedArray()
}

object UITestsView {
    val dialerView = onView(withId(R.id.dialer_layout))
    val incomingCallView = onView(withId(R.id.incoming_call_layout))
    val outgoingCallView = onView(withId(R.id.outgoing_call_layout))
    val singleCallView = onView(withId(R.id.single_call_layout))
}

object UITestsUtils {

    private var mainActivityIntent = Intent(getApplicationContext(), MainActivity::class.java)
    var activityScenario: ActivityScenario<MainActivity>? = null

    fun testAppSetup() {
        // launch app
        Log.i("[UITests] Launch Linphone app")
        launchApp()
        try {
            onView(withId(R.id.assistant_welcome_layout)).check(doesNotExist())
        } catch (e: Throwable) {
            onView(withId(R.id.back)).perform(click())
        }
        if (!rightAccountConnected() || !accountIsConnected()) {
            removeAllAccounts()
            connectAccount()
            assert(accountIsConnected()) { "registration state on the Status Bar is still not : Connected after 10 seconds" }
        }
        UITestsView.dialerView.checkWithTimeout(matches(isDisplayed()), 5.0)
    }

    fun launchApp() {
        if (isAppLaunch()) activityScenario?.close()
        activityScenario = ActivityScenario.launch(mainActivityIntent)
    }

    fun isAppLaunch(): Boolean {
        if (activityScenario != null) return activityScenario!!.state != Lifecycle.State.DESTROYED
        return false
    }

    fun accountIsConnected(): Boolean {
        var result = false
        runBlocking {
            val wait = launch { delay(5000) }
            val observer = Observer<Int> {
                if (it == R.string.status_connected) {
                    result = true
                    wait.cancel()
                }
            }
            lateinit var viewModel: StatusViewModel
            getInstrumentation().runOnMainSync {
                viewModel = StatusViewModel()
                viewModel.registrationStatusText.observeForever(observer)
            }
            wait.join()
            getInstrumentation().runOnMainSync { viewModel.registrationStatusText.removeObserver(observer) }
        }
        return result
    }

    fun rightAccountConnected(): Boolean {
        val realAccount = LinphoneApplication.coreContext.core.defaultAccount?.findAuthInfo()?.username
        val expectedAccount = UITestsCoreManager.instance.appAccountAuthInfo.username
        return realAccount == expectedAccount
    }

    fun connectAccount() {
        val manager = UITestsCoreManager.instance
        manager.accountsReset()
        Log.i("[UITests] Connect ${manager.appAccountAuthInfo.username} user to Linphone app")
        val core = LinphoneApplication.coreContext.core
        LinphoneApplication.corePreferences.useDnsServer = true
        LinphoneApplication.corePreferences.dnsServerAddress = manager.dnsServer.last()
        core.setDnsServersApp(manager.dnsServer)
        val accountParams = core.createAccountParams()
        val identity = manager.createAddress(manager.appAccountAuthInfo)
        accountParams.identityAddress = identity
        val address = Factory.instance().createAddress("sip:" + manager.appAccountAuthInfo.domain)!!
        address.transport = TransportType.Tcp
        accountParams.serverAddress = address
        accountParams.isRegisterEnabled = true
        val account = core.createAccount(accountParams)
        core.addAuthInfo(manager.appAccountAuthInfo)
        core.addAccount(account)
        core.defaultAccount = account
    }

    fun removeAllAccounts() {
        Log.i("[UITests] Remove all accounts from the Linphone app core")
        for (account in LinphoneApplication.coreContext.core.accountList) {
            getInstrumentation().runOnMainSync {
                val viewModel = AccountSettingsViewModel(account)
                viewModel.deleteListener.onClicked()
            }
        }
    }

    fun ViewInteraction.checkWithTimeout(viewAssert: ViewAssertion, timeout: Double): ViewInteraction = runBlocking {
        val wait = launch(Dispatchers.Default) {
            repeat((timeout * 10).toInt()) { i ->
                try {
                    check(viewAssert)
                    cancel()
                } catch (e: Throwable) {
                    // do nothing to retry until timeout
                }
                delay(100)
            }
        }
        wait.join()
        check(viewAssert).withFailureHandler { error, viewMatcher -> throw Exception("[UITests] $error") }
    }
}
