package org.linphone.testsuites

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.linphone.call.OutgoingCallUITests

@RunWith(Suite::class)
@Suite.SuiteClasses(
    OutgoingCallUITests::class
)
class CallTestSuite
