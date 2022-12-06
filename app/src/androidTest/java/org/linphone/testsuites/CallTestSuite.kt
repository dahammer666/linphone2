package org.linphone.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.linphone.call.IncomingCallUITests
import org.linphone.call.OutgoingCallUITests

@RunWith(Suite::class)
@Suite.SuiteClasses(
    OutgoingCallUITests::class,
    IncomingCallUITests::class
)
class CallTestSuite
