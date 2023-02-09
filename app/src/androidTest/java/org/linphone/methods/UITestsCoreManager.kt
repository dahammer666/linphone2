package org.linphone.methods

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import java.util.*
import kotlinx.coroutines.*
import org.linphone.LinphoneApplication
import org.linphone.core.*
import org.linphone.core.tools.Log
import org.linphone.utils.LinphoneUtils

class UITestsCoreManager {

    var core: Core
    var accountCreator: AccountCreator
    private val factory = Factory.instance()

    var appAccountAuthInfo: AuthInfo
    var ghostAccounts: UITestsGhostAccounts
    val dnsServer = arrayOf("5.135.31.162") // fs-test-3 = 5.135.31.162 and fs-test-4 = 51.255.123.121

    companion object {
        private var mInstance: UITestsCoreManager? = null

        val instance: UITestsCoreManager
            get() {
                if (mInstance == null) mInstance = UITestsCoreManager()
                return mInstance!!
            }
    }

    init {
        factory.loggingService.setLogLevel(LogLevel.Debug)

        // Config account creator for flexiapi
        val config = factory.createConfig(LinphoneApplication.corePreferences.uiTestsConfigPath)
        config.setInt("account_creator", "backend", AccountCreatorBackend.FlexiAPI.ordinal)
        config.setString("account_creator", "url", "http://subscribe.example.org/flexiapi/api/")
        core = factory.createCoreWithConfig(config, getApplicationContext())
        core.setDnsServersApp(dnsServer)
        accountCreator = core.createAccountCreator(null)
        core.start()
        ghostAccounts = UITestsGhostAccounts(::newAccountAuthInfo)
        appAccountAuthInfo = newAccountAuthInfo()
        ghostAccounts.indexOffset++
    }

    private fun newAccountAuthInfo(): AuthInfo {
        val authInfo: AuthInfo
        if (core.authInfoList.count() > ghostAccounts.count) {
            authInfo = core.authInfoList[ghostAccounts.count]
            Log.i("[UITests] Account retrieved (n°${ghostAccounts.count}) {usr: ${authInfo.username}, pwd: ${authInfo.password}, dmn: ${authInfo.domain}}")
        } else {
            authInfo = createAccount()
        }
        return authInfo
    }

    fun createAccount(): AuthInfo {
        accountCreator.username = "uitester_" + (Date().time * 1000).toUInt().toString().subSequence(0, 5)
        accountCreator.password = (1..15).map { accountCreator.username!!.random() }.joinToString("")
        accountCreator.domain = "sip.example.org"
        accountCreator.email = accountCreator.username + "@" + accountCreator.domain
        accountCreator.transport = TransportType.Tcp
        assert(accountCreator.createAccount() == AccountCreator.Status.RequestOk) { "[UITests] Unable to send a request to create an account on server" }
        waitForAccountCreationStatus(AccountCreator.Status.AccountCreated, 5.0)

        val authInfo = factory.createAuthInfo(accountCreator.username!!, "", accountCreator.password, "", "", accountCreator.domain)
        core.addAuthInfo(authInfo)
        Log.i("[UITests] New Account created (n°${core.authInfoList.count()}) {usr: ${authInfo.username}, pwd: ${authInfo.password}, dmn: ${authInfo.domain}}")

        return authInfo
    }

    fun accountsReset() {
        core.clearAllAuthInfo()
        ghostAccounts.reset()
        appAccountAuthInfo = createAccount()
    }

    fun createAddress(authInfo: AuthInfo): Address {
        return factory.createAddress("sip:" + authInfo.username + "@" + authInfo.domain)!!
    }

    fun waitForAccountCreationStatus(wStatus: AccountCreator.Status, timeout: Double) = runBlocking {
        var result = false
        val wait = launch { delay(timeout.toLong() * 1000) }
        val listener = object : AccountCreatorListenerStub() {
            override fun onCreateAccount(
                creator: AccountCreator,
                status: AccountCreator.Status?,
                response: String?
            ) {
                super.onCreateAccount(creator, status, response)
                if (wStatus == status) {
                    result = true
                    wait.cancel()
                }
            }
        }
        accountCreator.addListener(listener)
        wait.join()
        accountCreator.removeListener(listener)
        assert(result) { "[UITests] $wStatus account status still not verified after $timeout seconds" }
    }
}

class UITestsGhostAccounts(authInfoCreationFunction: () -> AuthInfo) {
    private var mCores = mutableListOf<UITestsRegisteredLinphoneCore>()
    var indexOffset = 0
    val count: Int
        get() = mCores.count() + indexOffset

    private var newCore: (() -> AuthInfo)

    init {
        newCore = authInfoCreationFunction
    }

    fun reset() {
        mCores.clear()
    }

    operator fun get(index: Int): UITestsRegisteredLinphoneCore {
        while (index >= mCores.count()) {
            mCores.add(UITestsRegisteredLinphoneCore(newCore()))
        }
        return mCores[index]
    }
}

class UITestsRegisteredLinphoneCore(authInfo: AuthInfo) {
    var mCore: Core
    private val factory = Factory.instance()
    var description: String

    private val manager = UITestsCoreManager.instance

    var mCoreListener: CoreListener private set
    lateinit var mAccount: Account private set
    var mAuthInfo: AuthInfo private set

    var callState = Call.State.Released
        private set
    var registrationState = RegistrationState.Cleared
        private set

    init {
        description = "Ghost Account (" + authInfo.username + ")"
        factory.loggingService.setLogLevel(LogLevel.Debug)

        mCore = factory.createCore("", "", getApplicationContext())
        mCore.setDnsServersApp(manager.dnsServer)

        mCore.isVideoCaptureEnabled = true
        mCore.isVideoDisplayEnabled = true
        mCore.isRecordAwareEnabled = true
        mCore.videoActivationPolicy.automaticallyAccept = true

        mCoreListener = object : CoreListenerStub() {
            override fun onCallStateChanged(
                core: Core,
                call: Call,
                state: Call.State?,
                message: String
            ) {
                callState = state ?: Call.State.Released
                Log.i("[UITests] ${authInfo.username} current call state is $callState")
            }

            override fun onAccountRegistrationStateChanged(
                core: Core,
                account: Account,
                state: RegistrationState?,
                message: String
            ) {
                registrationState = state ?: RegistrationState.Cleared
                Log.i("[UITests] New registration state \"$state\" for user ${account.params.identityAddress?.username}")
            }
        }
        mCore.addListener(mCoreListener)

        mCore.playFile = "sounds/hello8000.wav"
        mCore.useFiles = true

        mCore.start()

        mAuthInfo = authInfo
        login(TransportType.Tcp)
    }

    fun login(transport: TransportType) {
        val accountParams = mCore.createAccountParams()
        val identity = manager.createAddress(mAuthInfo)
        accountParams.setIdentityAddress(identity)
        val address = factory.createAddress("sip:" + mAuthInfo.domain)!!
        address.transport = transport
        accountParams.serverAddress = address
        accountParams.isRegisterEnabled = true
        val account = mCore.createAccount(accountParams)
        mCore.addAuthInfo(mAuthInfo)
        mCore.addAccount(account)
        mAccount = account
        mCore.defaultAccount = mAccount
    }

    fun startCall(address: Address) {
        val params = mCore.createCallParams(null)!!
        params.mediaEncryption = MediaEncryption.None
        params.recordFile = LinphoneUtils.getRecordingFilePathForAddress(address)
        mCore.inviteAddressWithParams(address, params)
    }

    fun terminateCall() {
        if (mCore.callsNb == 0) { return }
        val call = if (mCore.currentCall != null) mCore.currentCall else mCore.calls[0]
        call ?: return
        call.terminate()
    }

    fun acceptCall() {
        mCore.currentCall?.accept()
    }

    fun toggleMicrophone() {
        mCore.isMicEnabled = !mCore.isMicEnabled
    }

    fun toggleSpeaker() {
        val currentAudioDevice = mCore.currentCall?.outputAudioDevice
        val speakerEnabled = currentAudioDevice?.type == AudioDevice.Type.Speaker

        for (audioDevice in mCore.audioDevices) {
            if (speakerEnabled && audioDevice.type == AudioDevice.Type.Earpiece) {
                mCore.currentCall?.outputAudioDevice = audioDevice
                return
            } else if (!speakerEnabled && audioDevice.type == AudioDevice.Type.Speaker) {
                mCore.currentCall?.outputAudioDevice = audioDevice
                return
            } /* If we wanted to route the audio to a bluetooth headset
            else if (audioDevice.type == AudioDevice.Type.Bluetooth) {
                core.currentCall?.outputAudioDevice = audioDevice
            }*/
        }
    }

    fun toggleVideo() {
        if (mCore.callsNb == 0) return
        val call = if (mCore.currentCall != null) mCore.currentCall else mCore.calls[0]
        call ?: return

        val params = mCore.createCallParams(call)
        params?.isVideoEnabled = !call.currentParams.isVideoEnabled
        call.update(params)
    }

    fun toggleCamera() {
        val currentDevice = mCore.videoDevice
        for (camera in mCore.videoDevicesList) {
            if (camera != currentDevice && camera != "StaticImage: Static picture") {
                mCore.videoDevice = camera
                break
            }
        }
    }

    fun pauseCall() {
        if (mCore.callsNb == 0) return
        val call = if (mCore.currentCall != null) mCore.currentCall else mCore.calls[0]
        call ?: return
        call.pause()
    }

    fun resumeCall() {
        if (mCore.callsNb == 0) return
        val call = if (mCore.currentCall != null) mCore.currentCall else mCore.calls[0]
        call ?: return
        call.resume()
    }

    fun startRecording() {
        mCore.currentCall?.startRecording()
    }

    fun stopRecording() {
        mCore.currentCall?.stopRecording()
    }

    fun waitForRegistrationState(registrationState: RegistrationState, timeout: Double) = runBlocking {
        var result = false
        val wait = launch { delay(timeout.toLong() * 1000) }
        val listener = object : AccountListenerStub() {
            override fun onRegistrationStateChanged(
                account: Account,
                state: RegistrationState?,
                message: String
            ) {
                super.onRegistrationStateChanged(account, state, message)
                if (registrationState == state) {
                    result = true
                    wait.cancel()
                }
            }
        }
        mCore.defaultAccount!!.addListener(listener)
        wait.join()
        mCore.defaultAccount!!.removeListener(listener)
        assert(result) { "[UITests] $registrationState registration state still not verified after $timeout seconds" }
    }

    fun waitForCallState(call_state: Call.State, timeout: Double) = runBlocking {
        var result = false
        val wait = launch(Dispatchers.Default) {
            repeat((timeout * 10).toInt()) {
                if (call_state == callState) {
                    result = true
                    cancel()
                }
                delay(100)
            }
        }
        wait.join()
        assert(result) { "[UITests] $call_state call state still not verified after $timeout seconds (last known state: $callState)" }
    }

    fun waitForRecordingState(recording: Boolean, onRemote: Boolean = false, timeout: Double) = runBlocking {
        var result = false
        val wait = launch(Dispatchers.Default) {
            repeat((timeout * 10).toInt()) {
                if (!onRemote && recording == mCore.currentCall?.params?.isRecording) {
                    result = true
                    cancel()
                }
                if (onRemote && recording == mCore.currentCall?.remoteParams?.isRecording) {
                    result = true
                    cancel()
                }
                delay(100)
            }
        }
        val remoteText = if (onRemote) "remote" else ""
        wait.join()
        assert(result) { "[UITests] $remoteText call state still not $recording after ${timeout.toInt()} seconds" }
    }
}
