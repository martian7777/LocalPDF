package com.localpdf.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FtsQuerySanitizerTest {
    @Test fun stripsFtsOperators() { assertEquals("\"invoice\" AND \"OR\" AND \"4231\"", FtsQuerySanitizer.sanitize("invoice OR 4231*\"")) }
}
