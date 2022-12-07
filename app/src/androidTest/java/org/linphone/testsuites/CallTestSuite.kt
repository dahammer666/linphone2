package org.linphone.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.linphone.call.IncomingCallPushUITests
import org.linphone.call.IncomingCallUITests
import org.linphone.call.OutgoingCallUITests

@RunWith(Suite::class)
@Suite.SuiteClasses(
    IncomingCallPushUITests::class,
    IncomingCallUITests::class,
    OutgoingCallUITests::class
)
class CallTestSuite
