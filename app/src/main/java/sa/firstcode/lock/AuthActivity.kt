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
        val status: TextView = ui.text("أدخل رمزك المكون من 6 أرقام", 14f, ui.muted)
        root.addView(status)
        lateinit var pad: PinPad
        pad = PinPad(ui) { pin ->
            val remaining = vault.remaining()
            if (remaining > 0) {
                pin.fill('\u0000'); status.text = "حاول بعد ${(remaining + 999) / 1000} ثانية"
            } else {
                pad.enabled = false; status.text = "جارٍ التحقق…"
                worker.execute {
                    val valid = try { vault.verify(pin) } finally { pin.fill('\u0000') }
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed && version == screenVersion) {
                            pad.enabled = true
                            if (valid) success() else status.text = "الرمز غير صحيح. بعد 5 محاولات يتم الانتظار 30 ثانية."
                        }
                    }
                }
            }
        }
        root.addView(pad.view)
        if (allowBiometric && vault.biometrics) root.addView(ui.button("فتح بالبصمة") {
            biometric(status) { if (version == screenVersion && !isFinishing) { vault.clearFailures(); success() } }
        })
    }
    protected fun biometric(status: TextView, success: () -> Unit) {
        if (biometricVisible) return
        biometricVisible = true
        val signal = CancellationSignal(); cancellation = signal
        BiometricPrompt.Builder(this).setTitle("قفل الكود الأول")
            .setSubtitle("تحقق من هويتك بالبصمة المسجلة على الجهاز")
            .setNegativeButton("استخدام الرمز", mainExecutor) { _, _ -> biometricVisible = false }
            .build().authenticate(signal, mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    biometricVisible = false; cancellation = null; success()
                }
                override fun onAuthenticationError(code: Int, message: CharSequence) {
                    biometricVisible = false; cancellation = null
                    if (!isFinishing) status.text = "تعذر التحقق بالبصمة. استخدم الرمز."
                }
                override fun onAuthenticationFailed() { status.text = "لم يتم التعرف على البصمة" }
            })
    }
    override fun onStop() {
        screenVersion++
        cancellation?.cancel(); cancellation = null; biometricVisible = false
        super.onStop()
    }
    override fun onDestroy() { worker.shutdownNow(); super.onDestroy() }
}
