package sa.firstcode.lock

import android.content.Intent
import android.os.Bundle

class GateActivity : AuthActivity() {
    private var target = ""
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); showGate() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); showGate() }
    override fun onRestart() { super.onRestart(); showGate() }
    private fun showGate() {
        target = intent.getStringExtra("target") ?: ""
        val vault = Vault(this)
        if (target.isBlank() || target !in vault.protected || !vault.configured || !vault.enabled || !vault.consent) { finish(); return }
        val ui = Ui(this); val root = ui.root()
        ui.header(root, this@GateActivity.tr(R.string.copy_002), if (vault.showAppName) AppCatalog.label(this, target) else this@GateActivity.tr(R.string.copy_003))
        pinAuth(ui, root, {
            val service = LockService.instance
            if (service != null) { service.unlocked(target); finish() }
            else { home() }
        })
        root.addView(ui.button(this@GateActivity.tr(R.string.copy_004)) { home() })
        root.addView(ui.text(this@GateActivity.tr(R.string.copy_005), 12f, ui.muted))
    }
    private fun home() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
    @Deprecated("Back must not reveal a locked app")
    override fun onBackPressed() { home() }
    override fun onDestroy() { LockService.instance?.gateClosed(); super.onDestroy() }
}
