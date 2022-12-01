package org.linphone.methods

import android.content.Intent
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.*
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import java.util.*
import kotlinx.coroutines.*
import org.hamcrest.Matcher
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.linphone.LinphoneApplication
import org.linphone.R
import org.linphone.activities.main.MainActivity
import org.linphone.activities.main.settings.viewmodels.AccountSettingsViewModel
import org.linphone.activities.main.viewmodels.StatusViewModel
import org.linphone.core.Factory
import org.linphone.core.TransportType
import org.linphone.core.tools.Log

class ScreenshotsRule(active: Boolean) : TestWatcher() {

    val screenshotComparison = active

    override fun starting(description: Description) {
        super.starting(description)
        UITestsScreenshots.screenshotComparison = screenshotComparison
        UITestsScreenshots.definePath(description.className, description.methodName, Date().time.toString())
        if (screenshotComparison && !UITestsScreenshots.defaultPath.isDirectory) {
            UITestsScreenshots.defaultPath.mkdirs()
        }
    }
}

object UITestsUtils {

    private var mainActivityIntent = Intent(getApplicationContext(), MainActivity::class.java)
    var activityScenario: ActivityScenario<MainActivity>? = null

    fun testAppSetup() {
        // launch app
        Log.i("[UITests] Launch Linphone app")
        if (!isAppLaunch()) { launchApp() }
        if (!rightAccountConnected() || !accountIsConnected()) {
            removeAllAccounts()
            connectAccount()
            assert(accountIsConnected()) { "registration state on the Status Bar is still not : Connected after 10 seconds" }
        }
        onView(withId(R.id.dialer_layout)).checkWithTimeout(matches(isDisplayed()), 5.0)
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
        LinphoneApplication.corePreferences.dnsServerAddress = manager.dnsServer.first()
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

    fun waitForExistence(matcher: Matcher<View>, timeout: Double) {
        return waitForView(matcher, timeout, true)
    }

    fun waitForNonExistence(matcher: Matcher<View>, timeout: Double) {
        return waitForView(matcher, timeout, false)
    }

    private fun waitForView(matcher: Matcher<View>, timeout: Double, exist: Boolean) = runBlocking {
        var result = false
        val wait = launch(Dispatchers.Default) {
            repeat(timeout.toInt() * 10) {
                try {
                    onView(matcher).check(matches(isDisplayed()))
                    result = true
                    cancel()
                } catch (_: Exception) {
                    // do nothing to retry until timeout
                }
                delay(100)
            }
        }
        wait.join()
        assert(result) { "[UITests] $matcher still ${if (exist) "not " else ""}displayed after $timeout seconds" }
    }

    fun ViewInteraction.checkWithTimeout(viewAssert: ViewAssertion, timeout: Double): ViewInteraction = runBlocking {
        val wait = launch(Dispatchers.Default) {
            repeat(timeout.toInt() * 10) {
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
        check(viewAssert)
    }
}
