package sa.firstcode.lock

import android.app.Activity
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors

abstract class AuthActivity : Activity() {
    protected val worker = Executors.newSingleThreadExecutor()
    protected var screenVersion = 0
    private var cancellation: CancellationSignal? = null
    protected var biometricVisible = false
    protected fun pinAuth(ui: Ui, root: LinearLayout, success: () -> Unit, allowBiometric: Boolean = true) {
        val version = ++screenVersion
        val vault = Vault(this)
        val status: TextView = ui.text(this@AuthActivity.tr(R.string.copy_096), 14f, ui.muted)
        root.addView(status)
        lateinit var pad: PinPad
        pad = PinPad(ui) { pin ->
            val remaining = vault.remaining()
            if (remaining > 0) {
                pin.fill('\u0000'); status.text = this@AuthActivity.tr(R.string.copy_097 , (remaining + 999) / 1000)
            } else {
                pad.enabled = false; status.text = this@AuthActivity.tr(R.string.copy_098)
                worker.execute {
                    val valid = try { vault.verify(pin) } finally { pin.fill('\u0000') }
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed && version == screenVersion) {
                            pad.enabled = true
                            if (valid) success() else status.text = this@AuthActivity.tr(R.string.copy_099)
                        }
                    }
                }
            }
        }
        root.addView(pad.view)
        if (allowBiometric && vault.biometrics) root.addView(ui.button(this@AuthActivity.tr(R.string.copy_100)) {
            biometric(status) { if (version == screenVersion && !isFinishing) { vault.clearFailures(); success() } }
        })
    }
    protected fun biometric(status: TextView, success: () -> Unit) {
        if (biometricVisible) return
        biometricVisible = true
        val signal = CancellationSignal(); cancellation = signal
        BiometricPrompt.Builder(this).setTitle(this@AuthActivity.tr(R.string.copy_000))
            .setSubtitle(this@AuthActivity.tr(R.string.copy_101))
            .setNegativeButton(this@AuthActivity.tr(R.string.copy_102), mainExecutor) { _, _ -> biometricVisible = false }
            .build().authenticate(signal, mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    biometricVisible = false; cancellation = null; success()
                }
                override fun onAuthenticationError(code: Int, message: CharSequence) {
                    biometricVisible = false; cancellation = null
                    if (!isFinishing) status.text = this@AuthActivity.tr(R.string.copy_103)
                }
                override fun onAuthenticationFailed() { status.text = this@AuthActivity.tr(R.string.copy_104) }
            })
    }
    override fun onStop() {
        screenVersion++
        cancellation?.cancel(); cancellation = null; biometricVisible = false
        super.onStop()
    }
    override fun onDestroy() { worker.shutdownNow(); super.onDestroy() }
}
