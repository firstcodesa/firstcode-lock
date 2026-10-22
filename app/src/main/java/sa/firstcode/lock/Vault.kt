package sa.firstcode.lock

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.Base64

class Vault(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("vault", Context.MODE_PRIVATE)
    val configured: Boolean get() = prefs.contains("hash") && prefs.contains("salt")
    var dark: Boolean
        get() = prefs.getBoolean("dark", true)
        set(value) { prefs.edit().putBoolean("dark", value).apply() }
    var biometrics: Boolean
        get() = prefs.getBoolean("bio", false)
        set(value) { prefs.edit().putBoolean("bio", value).apply() }
    var grace: Long
        get() = prefs.getLong("grace", 0)
        set(value) { prefs.edit().putLong("grace", value).apply(); LockService.instance?.sessions?.clear() }
    var consent: Boolean
        get() = prefs.getBoolean("consent", false)
        set(value) { prefs.edit().putBoolean("consent", value).apply() }
    var protected: Set<String>
        get() = prefs.getStringSet("protected", emptySet())!!.toSet()
        set(value) { prefs.edit().putStringSet("protected", value.toSet()).commit(); LockService.instance?.sessions?.clear() }
    fun setPin(pin: CharArray) {
        require(pin.size == 6 && pin.all { it in '0'..'9' })
        val salt = PinCrypto.salt()
        val hash = PinCrypto.derive(pin, salt)
        check(prefs.edit().putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", Base64.encodeToString(hash, Base64.NO_WRAP))
            .putInt("failures", 0).putLong("until", 0).commit())
        hash.fill(0)
        LockService.instance?.sessions?.clear()
    }
    private fun boot(): Int = Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -1)
    @Synchronized fun remaining(): Long {
        val until = prefs.getLong("until", 0)
        if (until == 0L) return 0
        if (prefs.getInt("boot", -2) != boot()) {
            prefs.edit().putLong("until", SystemClock.elapsedRealtime() + 30_000).putInt("boot", boot()).commit()
        }
        return (prefs.getLong("until", 0) - SystemClock.elapsedRealtime()).coerceAtLeast(0)
    }
    @Synchronized fun verify(pin: CharArray): Boolean {
        if (!configured || remaining() > 0) return false
        val valid = runCatching {
            PinCrypto.matches(pin, Base64.decode(prefs.getString("salt", ""), Base64.NO_WRAP),
                Base64.decode(prefs.getString("hash", ""), Base64.NO_WRAP))
        }.getOrDefault(false)
        if (valid) { prefs.edit().putInt("failures", 0).putLong("until", 0).commit(); return true }
        val failures = prefs.getInt("failures", 0) + 1
        val editor = prefs.edit().putInt("failures", failures)
        if (failures >= 5) editor.putLong("until", SystemClock.elapsedRealtime() + 30_000).putInt("boot", boot())
        editor.commit()
        return false
    }
    fun clearFailures() { prefs.edit().putInt("failures", 0).putLong("until", 0).commit() }
}
