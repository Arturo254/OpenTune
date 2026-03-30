package com.arturo254.opentune.innertube.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieParsingTest {

    @Test
    fun parseCookieString_handlesWebViewCookieWithoutSpaces() {
        val input = "SID=1;HSID=2;SAPISID=abc123;SSID=4"

        val cookies = parseCookieString(input)

        assertEquals(4, cookies.size)
        assertTrue(cookies.containsKey("SAPISID"))
        assertEquals("abc123", cookies["SAPISID"])
    }

    @Test
    fun parseCookieString_trimsPartsAndIgnoresMalformed() {
        val input = "  SID=1 ;  ; invalid ; SAPISID = token  ; =bad ; SSID=4  "

        val cookies = parseCookieString(input)

        assertEquals("1", cookies["SID"])
        assertEquals("token", cookies["SAPISID"])
        assertEquals("4", cookies["SSID"])
        assertEquals(3, cookies.size)
    }
}
