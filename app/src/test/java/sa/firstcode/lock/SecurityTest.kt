package sa.firstcode.lock

import org.junit.Assert.*
import org.junit.Test

class SecurityTest {
    @Test fun pinMatchesAndRejectsWrongValue() {
        val salt = PinCrypto.salt()
        val expected = PinCrypto.derive("739152".toCharArray(), salt)
        assertTrue(PinCrypto.matches("739152".toCharArray(), salt, expected))
        assertFalse(PinCrypto.matches("739153".toCharArray(), salt, expected))
    }
    @Test fun differentSaltsDoNotReuseHashes() {
        val a = PinCrypto.derive("739152".toCharArray(), PinCrypto.salt())
        val b = PinCrypto.derive("739152".toCharArray(), PinCrypto.salt())
        assertFalse(a.contentEquals(b))
    }
    @Test fun immediatePolicyRevokesWhenLeaving() {
        val s = UnlockSessions()
        s.enter("a", 0); s.allow("a", 100)
        assertTrue(s.allowed("a", 101, 0))
        s.enter("home", 0); s.enter("a", 0)
        assertFalse(s.allowed("a", 102, 0))
    }
    @Test fun graceIsPerApplicationAndExpires() {
        val s = UnlockSessions()
        s.enter("a", 60_000); s.allow("a", 100)
        s.enter("b", 60_000)
        assertFalse(s.allowed("b", 101, 60_000))
        assertTrue(s.allowed("a", 101, 60_000))
        assertFalse(s.allowed("a", 60_100, 60_000))
        assertFalse(s.allowed("a", 99, 60_000))
    }
    @Test fun screenOffAndRestartClearEveryGrant() {
        val s = UnlockSessions()
        s.enter("a", 60_000); s.allow("a", 100); s.clear()
        assertFalse(s.allowed("a", 101, 60_000))
        assertFalse(UnlockSessions().allowed("a", 101, 60_000))
    }
    @Test fun biometricSuccessIsConsumedOnlyOnce() {
        val r = AuthRequests(); val t = r.start(1)
        assertTrue(r.consume(t, 1)); assertFalse(r.consume(t, 1))
    }
    @Test fun biometricCannotUnlockAfterCancellationOrScreenChange() {
        val r = AuthRequests(); val t = r.start(1); r.cancel()
        assertFalse(r.consume(t, 1))
        val newer = r.start(2)
        assertFalse(r.consume(t, 2)); assertFalse(r.consume(newer, 3))
        assertTrue(r.consume(newer, 2))
    }
    @Test fun oldBiometricCallbackCannotReplaceNewRequest() {
        val r = AuthRequests(); val old = r.start(1); val fresh = r.start(1)
        assertFalse(r.consume(old, 1)); assertTrue(r.consume(fresh, 1))
    }
}
