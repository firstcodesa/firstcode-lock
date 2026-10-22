package sa.firstcode.lock

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*

class MainActivity : AuthActivity() {
    private lateinit var vault: Vault
    private var authenticated = false
    private var firstPin: CharArray? = null
    private var onlyProtected = false
    private var catalog = emptyList<AppEntry>()
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); vault = Vault(this) }
    override fun onResume() {
        super.onResume()
        if (!vault.configured) setup() else if (!authenticated) login() else dashboard()
    }
    override fun onStop() {
        authenticated = false; firstPin?.fill('\u0000'); firstPin = null
        super.onStop()
    }
    private fun login() {
        val ui = Ui(this); val root = ui.root()
        ui.header(root, this@MainActivity.tr(R.string.copy_006), this@MainActivity.tr(R.string.copy_007))
        pinAuth(ui, root, { authenticated = true; dashboard() })
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_008), 13f, ui.muted))
    }
    private fun setup() {
        screenVersion++; firstPin?.fill('\u0000'); firstPin = null
        val ui = Ui(this); val root = ui.root()
        ui.header(root, this@MainActivity.tr(R.string.copy_009), this@MainActivity.tr(R.string.copy_010))
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_011), 15f, ui.muted))
        val status = ui.text(tr(R.string.copy_012), 17f); root.addView(status)
        val version = screenVersion
        lateinit var pad: PinPad
        pad = PinPad(ui) { value ->
            if (firstPin == null) { firstPin = value; status.text = this@MainActivity.tr(R.string.copy_015) }
            else {
                val same = firstPin!!.contentEquals(value)
                firstPin!!.fill('\u0000'); firstPin = null
                if (!same) { value.fill('\u0000'); status.text = this@MainActivity.tr(R.string.copy_016) }
                else {
                    pad.enabled = false; status.text = this@MainActivity.tr(R.string.copy_017)
                    worker.execute {
                        val saved = runCatching { vault.setPin(value) }.isSuccess
                        value.fill('\u0000')
                        runOnUiThread {
                            if (version == screenVersion && !isFinishing && !isDestroyed) {
                                if (saved) { authenticated = true; dashboard() }
                                else { pad.enabled = true; status.text = this@MainActivity.tr(R.string.copy_018) }
                            }
                        }
                    }
                }
            }
        }
        root.addView(pad.view)
        if (vault.configured) root.addView(ui.button(this@MainActivity.tr(R.string.copy_019)) { dashboard() })
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_020), 13f, ui.muted))
    }
    private fun dashboard() {
        if (!authenticated) { login(); return }
        screenVersion++
        val version = screenVersion
        val ui = Ui(this); val root = ui.root()
        ui.header(root, this@MainActivity.tr(R.string.copy_000), this@MainActivity.tr(R.string.copy_021))
        val live = LockService.instance != null && vault.consent && vault.enabled
        val status = ui.column().apply { background = ui.shape(ui.card); setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(12)) }
        status.addView(ui.text(if (live) this@MainActivity.tr(R.string.copy_022) else this@MainActivity.tr(R.string.copy_023), 19f, if (live) ui.accent else ui.ink))
        val count = ui.text(this@MainActivity.tr(R.string.copy_024 , vault.protected.size), 14f, ui.muted); status.addView(count)
        status.addView(ui.text(if (RemovalAdmin.active(this)) this@MainActivity.tr(R.string.copy_013) else this@MainActivity.tr(R.string.copy_014), 13f, ui.muted))
        root.addView(status)
        root.addView(ui.button(if (vault.enabled) this@MainActivity.tr(R.string.copy_025) else this@MainActivity.tr(R.string.copy_026)) {
            vault.enabled = !vault.enabled; dashboard()
        })
        root.addView(ui.button(if (live) this@MainActivity.tr(R.string.copy_027) else this@MainActivity.tr(R.string.copy_028)) { consentDialog() })
        root.addView(ui.button(this@MainActivity.tr(R.string.copy_029)) { settings() })
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_030), 23f).apply { typeface = Typeface.DEFAULT_BOLD })
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_031), 14f, ui.muted))
        val search = EditText(this).apply {
            hint = this@MainActivity.tr(R.string.copy_032); setTextColor(ui.ink); setHintTextColor(ui.muted)
            setSingleLine(); backgroundTintList = android.content.res.ColorStateList.valueOf(ui.accent)
        }
        root.addView(search)
        val list = ui.column(); root.addView(list)
        fun render(query: String) {
            list.removeAllViews()
            catalog.filter { (!onlyProtected || it.pkg in vault.protected) && (it.name.contains(query, true) || it.pkg.contains(query, true)) }
                .sortedWith(compareBy<AppEntry> { if (vault.protectedFirst && it.pkg in vault.protected) 0 else 1 }.thenBy { it.name })
                .forEach { app ->
                val row = LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL; setPadding(ui.dp(12), ui.dp(6), ui.dp(12), ui.dp(6))
                    background = ui.shape(ui.card)
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = ui.dp(8) }
                }
                row.addView(ImageView(this).apply { setImageDrawable(app.icon); importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO }, LinearLayout.LayoutParams(ui.dp(38), ui.dp(38)))
                val toggle = Switch(this).apply {
                    text = app.name; setTextColor(ui.ink); textSize = 15f; minHeight = ui.dp(56)
                    setPadding(ui.dp(10), 0, ui.dp(10), 0)
                    isChecked = app.pkg in vault.protected; filterTouchesWhenObscured = true
                    contentDescription = this@MainActivity.tr(R.string.copy_033 , app.name)
                    setOnCheckedChangeListener { _, checked ->
                        if (authenticated) {
                            val selected = vault.protected.toMutableSet()
                            if (checked) selected.add(app.pkg) else selected.remove(app.pkg)
                            vault.protected = selected
                            count.text = this@MainActivity.tr(R.string.copy_034 , selected.size)
                            row.post { if (authenticated) render(search.text.toString()) }
                        }
                    }
                }
                row.addView(toggle, LinearLayout.LayoutParams(0, -2, 1f)); list.addView(row)
            }
            if (list.childCount == 0) list.addView(ui.text(this@MainActivity.tr(R.string.copy_035), 15f, ui.muted))
        }
        val bulk = ui.column()
        root.addView(bulk, root.indexOfChild(search))
        bulk.addView(ui.button(this@MainActivity.tr(R.string.copy_036)) {
            if (catalog.isNotEmpty()) AlertDialog.Builder(this).setTitle(this@MainActivity.tr(R.string.copy_037))
                .setMessage(this@MainActivity.tr(R.string.copy_038))
                .setPositiveButton(this@MainActivity.tr(R.string.copy_039)) { _, _ ->
                    vault.protected = catalog.map { it.pkg }.toSet()
                    count.text = this@MainActivity.tr(R.string.copy_024 , vault.protected.size); render(search.text.toString())
                }.setNegativeButton(this@MainActivity.tr(R.string.copy_040), null).show()
        })
        bulk.addView(ui.button(this@MainActivity.tr(R.string.copy_041)) {
            AlertDialog.Builder(this).setTitle(this@MainActivity.tr(R.string.copy_042))
                .setMessage(this@MainActivity.tr(R.string.copy_043))
                .setPositiveButton(this@MainActivity.tr(R.string.copy_044)) { _, _ ->
                    vault.protected = emptySet(); count.text = this@MainActivity.tr(R.string.copy_045); render(search.text.toString())
                }.setNegativeButton(this@MainActivity.tr(R.string.copy_046), null).show()
        })
        bulk.addView(Switch(this).apply {
            text = this@MainActivity.tr(R.string.copy_047); setTextColor(ui.ink); minHeight = ui.dp(48)
            isChecked = onlyProtected
            setOnCheckedChangeListener { _, checked -> onlyProtected = checked; render(search.text.toString()) }
        })
        list.addView(ui.text(this@MainActivity.tr(R.string.copy_048), 14f, ui.muted))
        worker.execute {
            val apps = runCatching { AppCatalog.list(this) }.getOrDefault(emptyList())
            runOnUiThread { if (version == screenVersion && !isDestroyed) { catalog = apps; render(search.text.toString()) } }
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString() ?: "") }
            override fun afterTextChanged(s: Editable?) {}
        })
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_049), 12f, ui.muted))
    }
    private fun consentDialog() {
        AlertDialog.Builder(this).setTitle(this@MainActivity.tr(R.string.copy_050))
            .setMessage(this@MainActivity.tr(R.string.copy_051))
            .setPositiveButton(this@MainActivity.tr(R.string.copy_052)) { _, _ ->
                vault.consent = true
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }.setNegativeButton(this@MainActivity.tr(R.string.copy_040), null).show()
    }
    private fun settings() {
        if (!authenticated) { login(); return }
        screenVersion++
        val ui = Ui(this); val root = ui.root()
        ui.header(root, this@MainActivity.tr(R.string.copy_053), this@MainActivity.tr(R.string.copy_054))
        fun toggle(title: String, checked: Boolean, action: (Boolean) -> Unit) {
            root.addView(Switch(this).apply {
                text = title; textSize = 17f; setTextColor(ui.ink); minHeight = ui.dp(60)
                isChecked = checked; filterTouchesWhenObscured = true
                setOnCheckedChangeListener { _, value -> action(value) }
            })
        }
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_055), 21f))
        root.addView(ui.text(if (RemovalAdmin.active(this)) this@MainActivity.tr(R.string.copy_056) else this@MainActivity.tr(R.string.copy_057), 14f, ui.muted))
        root.addView(ui.button(if (RemovalAdmin.active(this)) this@MainActivity.tr(R.string.copy_058) else this@MainActivity.tr(R.string.copy_059)) {
            if (RemovalAdmin.active(this)) {
                val authUi = Ui(this); val authRoot = authUi.root()
                authUi.header(authRoot, this@MainActivity.tr(R.string.copy_060), this@MainActivity.tr(R.string.copy_061))
                pinAuth(authUi, authRoot, {
                    RemovalAdmin.manager(this)?.removeActiveAdmin(RemovalAdmin.component(this))
                    settings()
                }, false)
                authRoot.addView(authUi.button(this@MainActivity.tr(R.string.copy_040)) { settings() })
            } else {
                AlertDialog.Builder(this).setTitle(this@MainActivity.tr(R.string.copy_055))
                    .setMessage(this@MainActivity.tr(R.string.copy_062))
                    .setPositiveButton(this@MainActivity.tr(R.string.copy_063)) { _, _ ->
                        runCatching { startActivity(RemovalAdmin.request(this)) }.onFailure {
                            Toast.makeText(this, this@MainActivity.tr(R.string.copy_064), Toast.LENGTH_LONG).show()
                        }
                    }.setNegativeButton(this@MainActivity.tr(R.string.copy_040), null).show()
            }
        })
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_065), 21f))
        root.addView(ui.button(this@MainActivity.tr(R.string.copy_066)) {
            AlertDialog.Builder(this).setTitle(this@MainActivity.tr(R.string.copy_066))
                .setSingleChoiceItems(arrayOf(this@MainActivity.tr(R.string.copy_067), "English"), if (vault.english) 1 else 0) { dialog, index ->
                    vault.english = index == 1; dialog.dismiss(); settings()
                }.setNegativeButton(this@MainActivity.tr(R.string.copy_040), null).show()
        })
        toggle(this@MainActivity.tr(R.string.copy_068), vault.protectedFirst) { vault.protectedFirst = it }
        toggle(this@MainActivity.tr(R.string.copy_069), vault.showAppName) { vault.showAppName = it }
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_070), 21f))
        toggle(this@MainActivity.tr(R.string.copy_071), vault.dark) { vault.dark = it; settings() }
        toggle(this@MainActivity.tr(R.string.copy_072), vault.biometrics) { enabled ->
            if (!enabled) vault.biometrics = false
            else {
                val message = ui.text(this@MainActivity.tr(R.string.copy_073), 14f, ui.muted); root.addView(message)
                biometric(message) { vault.biometrics = true; settings() }
                // Keep persisted switch off until Android confirms enrollment/authentication.
                vault.biometrics = false
            }
        }
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_074), 19f))
        val choices = arrayOf(this@MainActivity.tr(R.string.copy_075), this@MainActivity.tr(R.string.copy_076), this@MainActivity.tr(R.string.copy_077))
        val values = longArrayOf(0, 60_000, 300_000)
        root.addView(ui.button(choices[values.indexOf(vault.grace).coerceAtLeast(0)]) {
            AlertDialog.Builder(this).setTitle(this@MainActivity.tr(R.string.copy_078)).setSingleChoiceItems(choices, values.indexOf(vault.grace)) { dialog, index ->
                vault.grace = values[index]; dialog.dismiss(); settings()
            }.setNegativeButton(this@MainActivity.tr(R.string.copy_040), null).show()
        })
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_079), 13f, ui.muted))
        root.addView(ui.button(this@MainActivity.tr(R.string.copy_080)) {
            val gateUi = Ui(this); val gateRoot = gateUi.root()
            gateUi.header(gateRoot, this@MainActivity.tr(R.string.copy_081), this@MainActivity.tr(R.string.copy_082))
            pinAuth(gateUi, gateRoot, { setup() }, false)
        })
        root.addView(ui.button(this@MainActivity.tr(R.string.copy_083)) {
            AlertDialog.Builder(this).setTitle(this@MainActivity.tr(R.string.copy_084)).setMessage(this@MainActivity.tr(R.string.copy_085))
                .setPositiveButton(this@MainActivity.tr(R.string.copy_086)) { _, _ -> vault.consent = false; LockService.instance?.disableSelf(); dashboard() }
                .setNegativeButton(this@MainActivity.tr(R.string.copy_040), null).show()
        })
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_087), 21f))
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_088), 14f, ui.muted))
        root.addView(ui.button(this@MainActivity.tr(R.string.copy_089)) {
            AlertDialog.Builder(this).setTitle(this@MainActivity.tr(R.string.copy_090))
                .setMessage(this@MainActivity.tr(R.string.copy_091))
                .setPositiveButton(this@MainActivity.tr(R.string.copy_092), null).show()
        })
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_093), 16f, ui.accent))
        root.addView(ui.text(this@MainActivity.tr(R.string.copy_094), 12f, ui.muted))
        root.addView(ui.button(this@MainActivity.tr(R.string.copy_095)) { dashboard() })
    }
}
