package sa.firstcode.lock

import android.app.Activity
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors

abstract class AuthActivity : Activity() {
    protected val worker = Executors.newSingleThreadExecutor()
    protected var screenVersion = 0
    private var cancellation: CancellationSignal? = null
    protected var biometricVisible = false
    private val requests = AuthRequests()
    private var automaticPrompt: (() -> Unit)? = null

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) showAutomaticPrompt()
    }
    private fun showAutomaticPrompt() {
        if (!hasWindowFocus() || isFinishing || isDestroyed) return
        val action = automaticPrompt ?: return
        automaticPrompt = null // Once per lock screen; cancellation must leave the PIN available.
        action()
    }
    protected fun pinAuth(ui: Ui, root: LinearLayout, success: () -> Unit, allowBiometric: Boolean = true) {
        val version = ++screenVersion
        automaticPrompt = null
        val vault = Vault(this)
        val status = ui.text(tr(R.string.copy_096), 14f, ui.muted)
        root.addView(status)
        lateinit var pad: PinPad
        pad = PinPad(ui) { pin ->
            val remaining = vault.remaining()
            if (remaining > 0) {
                pin.fill('\u0000'); status.text = tr(R.string.copy_097, (remaining + 999) / 1000)
            } else {
                pad.enabled = false; status.text = tr(R.string.copy_098)
                worker.execute {
                    val valid = try { runCatching { vault.verify(pin) }.getOrDefault(false) }
                        finally { pin.fill('\u0000') }
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed && version == screenVersion) {
                            pad.enabled = true
                            if (valid) { cancelBiometric(); success() }
                            else status.text = tr(R.string.copy_099)
                        }
                    }
                }
            }
        }
        root.addView(pad.view)
        if (allowBiometric && vault.biometrics) {
            fun startBiometric() {
                if (version != screenVersion || isFinishing || isDestroyed) return
                biometric(status) {
                    if (version == screenVersion) { vault.clearFailures(); success() }
                }
            }
            root.addView(ui.button(tr(R.string.copy_100)) { startBiometric() })
            automaticPrompt = { startBiometric() }
            root.post { if (version == screenVersion) showAutomaticPrompt() }
        }
    }
    protected fun biometric(status: TextView, onFallback: () -> Unit = {}, success: () -> Unit) {
        if (biometricVisible || isFinishing || isDestroyed) return
        val ticket = requests.start(screenVersion)
        val signal = CancellationSignal()
        cancellation = signal; biometricVisible = true
        fun fallback() {
            if (!requests.consume(ticket, screenVersion)) return
            cancellation = null; biometricVisible = false
            if (!isFinishing && !isDestroyed) { status.text = tr(R.string.copy_103); onFallback() }
        }
        val builder = BiometricPrompt.Builder(this).setTitle(tr(R.string.copy_000))
            .setSubtitle(tr(R.string.copy_101))
            .setNegativeButton(tr(R.string.copy_102), mainExecutor) { _, _ -> fallback() }
        if (Build.VERSION.SDK_INT >= 30) builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        runCatching {
            builder.build().authenticate(signal, mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (!requests.consume(ticket, screenVersion)) return
                    cancellation = null; biometricVisible = false
                    if (!isFinishing && !isDestroyed) success()
                }
                override fun onAuthenticationError(code: Int, message: CharSequence) { fallback() }
                override fun onAuthenticationFailed() {
                    if (requests.current(ticket, screenVersion)) status.text = tr(R.string.copy_104)
                }
            })
        }.onFailure { fallback() }
    }
    private fun cancelBiometric() {
        requests.cancel()
        cancellation?.cancel(); cancellation = null; biometricVisible = false
        automaticPrompt = null
    }
    override fun onStop() {
        screenVersion++; cancelBiometric()
        super.onStop()
    }
    override fun onDestroy() { cancelBiometric(); worker.shutdownNow(); super.onDestroy() }
}
