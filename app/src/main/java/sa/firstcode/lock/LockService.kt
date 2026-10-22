package sa.firstcode.lock

import android.accessibilityservice.AccessibilityService
import android.content.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

class LockService : AccessibilityService() {
    val sessions = UnlockSessions()
    private lateinit var vault: Vault
    private var lastForeground: String? = null
    private var launching: String? = null
    private var launchAt = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var receiverRegistered = false
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                sessions.clear(); launching = null
            } else if (intent.action == Intent.ACTION_USER_PRESENT) {
                handler.postDelayed({ lastForeground?.let { protect(it) } }, 250)
            }
        }
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this; vault = Vault(this)
        val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_USER_PRESENT) }
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        else registerReceiver(screenReceiver, filter)
        receiverRegistered = true
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!::vault.isInitialized || event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // Own unlock UI and System UI are not app switches. No node tree is requested.
        if (pkg == packageName || pkg == "com.android.systemui") return
        // Ignore IME/dialog-only packages; launcher and Settings still revoke sessions.
        if (!AppCatalog.isForegroundApp(this, pkg)) return
        lastForeground = pkg
        sessions.enter(pkg, vault.grace)
        protect(pkg)
    }
    private fun protect(pkg: String) {
        if (!vault.configured || !vault.enabled || !vault.consent || pkg !in vault.protected) return
        if (!(getSystemService(POWER_SERVICE) as PowerManager).isInteractive) return
        if (sessions.allowed(pkg, SystemClock.elapsedRealtime(), vault.grace)) return
        if (launching == pkg && SystemClock.elapsedRealtime() - launchAt < 1500) return
        launching = pkg; launchAt = SystemClock.elapsedRealtime()
        // Accessibility services may start an activity in response to a user app transition.
        runCatching {
            startActivity(Intent(this, GateActivity::class.java).putExtra("target", pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION))
        }.onFailure { launching = null }
    }
    fun unlocked(pkg: String) {
        sessions.enter(pkg, vault.grace)
        sessions.allow(pkg, SystemClock.elapsedRealtime()); launching = null
    }
    fun gateClosed() { launching = null }
    override fun onInterrupt() { sessions.clear() }
    override fun onDestroy() {
        if (receiverRegistered) unregisterReceiver(screenReceiver)
        handler.removeCallbacksAndMessages(null); sessions.clear(); instance = null
        super.onDestroy()
    }
    companion object { var instance: LockService? = null; private set }
}
