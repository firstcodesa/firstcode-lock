package sa.firstcode.lock

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinCrypto {
    const val ITERATIONS = 210_000
    fun salt(): ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) }
    fun derive(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, ITERATIONS, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    fun matches(pin: CharArray, salt: ByteArray, expected: ByteArray): Boolean {
        val candidate = derive(pin, salt)
        return try { MessageDigest.isEqual(candidate, expected) } finally { candidate.fill(0) }
    }
}

/** Uses monotonic time; a device restart loses all sessions, deliberately. */
class UnlockSessions {
    private val times = mutableMapOf<String, Long>()
    private var foreground: String? = null
    fun enter(pkg: String, grace: Long) {
        if (foreground != pkg && grace == 0L) times.clear()
        foreground = pkg
    }
    fun allow(pkg: String, now: Long) { times[pkg] = now }
    fun allowed(pkg: String, now: Long, grace: Long): Boolean {
        val since = times[pkg] ?: return false
        return if (grace == 0L) foreground == pkg else now >= since && now - since < grace
    }
    fun clear() { times.clear(); foreground = null }
}
