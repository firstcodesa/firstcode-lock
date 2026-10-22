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
        ui.header(root, "مرحبًا بعودتك", "افتح لوحة التحكم لإدارة التطبيقات المحمية")
        pinAuth(ui, root, { authenticated = true; dashboard() })
        root.addView(ui.text("لا يوجد رمز افتراضي أو باب خلفي. احتفظ برمزك في مكان آمن.", 13f, ui.muted))
    }
    private fun setup() {
        screenVersion++; firstPin?.fill('\u0000'); firstPin = null
        val ui = Ui(this); val root = ui.root()
        ui.header(root, "خصوصيتك أولًا", "قفل الكود الأول • أنشئ رمزًا من 6 أرقام")
        root.addView(ui.text("تطبيق مستقل، بدون إعلانات، يعمل دون اتصال. ابدأ بإنشاء رمز خاص بك.", 15f, ui.muted))
        val status = ui.text("أدخل الرمز الجديد", 17f); root.addView(status)
        val version = screenVersion
        lateinit var pad: PinPad
        pad = PinPad(ui) { value ->
            if (firstPin == null) { firstPin = value; status.text = "أعد إدخال الرمز للتأكيد" }
            else {
                val same = firstPin!!.contentEquals(value)
                firstPin!!.fill('\u0000'); firstPin = null
                if (!same) { value.fill('\u0000'); status.text = "الرمزان مختلفان. أدخل رمزًا جديدًا" }
                else {
                    pad.enabled = false; status.text = "جارٍ حفظ الرمز…"
                    worker.execute {
                        val saved = runCatching { vault.setPin(value) }.isSuccess
                        value.fill('\u0000')
                        runOnUiThread {
                            if (version == screenVersion && !isFinishing && !isDestroyed) {
                                if (saved) { authenticated = true; dashboard() }
                                else { pad.enabled = true; status.text = "تعذر حفظ الرمز. حاول مجددًا" }
                            }
                        }
                    }
                }
            }
        }
        root.addView(pad.view)
        if (vault.configured) root.addView(ui.button("إلغاء تغيير الرمز") { dashboard() })
        root.addView(ui.text("لن تتمكن من استعادة الرمز من خادم. حذف بيانات التطبيق يلغي إعدادات القفل.", 13f, ui.muted))
    }
    private fun dashboard() {
        if (!authenticated) { login(); return }
        screenVersion++
        val version = screenVersion
        val ui = Ui(this); val root = ui.root()
        ui.header(root, "قفل الكود الأول", "تحكم في تطبيقاتك. احتفظ بخصوصيتك.")
        val live = LockService.instance != null && vault.consent
        val status = ui.column().apply { background = ui.shape(ui.card); setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(12)) }
        status.addView(ui.text(if (live) "● خدمة القفل مفعّلة" else "○ خدمة القفل غير مفعّلة", 19f, if (live) ui.accent else ui.ink))
        val count = ui.text("${vault.protected.size} تطبيقات محددة للحماية", 14f, ui.muted); status.addView(count)
        root.addView(status)
        root.addView(ui.button(if (live) "إدارة صلاحية القفل" else "تفعيل خدمة القفل") { consentDialog() })
        root.addView(ui.button("الإعدادات والخصوصية") { settings() })
        root.addView(ui.text("تطبيقاتك", 23f).apply { typeface = Typeface.DEFAULT_BOLD })
        root.addView(ui.text("اختر التطبيقات التي تريد طلب الرمز عند فتحها.", 14f, ui.muted))
        val search = EditText(this).apply {
            hint = "ابحث عن تطبيق…"; setTextColor(ui.ink); setHintTextColor(ui.muted)
            setSingleLine(); backgroundTintList = android.content.res.ColorStateList.valueOf(ui.accent)
        }
        root.addView(search)
        val list = ui.column(); root.addView(list)
        fun render(query: String) {
            list.removeAllViews()
            catalog.filter { it.name.contains(query, true) || it.pkg.contains(query, true) }.forEach { app ->
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
                    contentDescription = "حماية ${app.name}"
                    setOnCheckedChangeListener { _, checked ->
                        if (authenticated) {
                            val selected = vault.protected.toMutableSet()
                            if (checked) selected.add(app.pkg) else selected.remove(app.pkg)
                            vault.protected = selected
                            count.text = "${selected.size} تطبيقات محددة للحماية"
                        }
                    }
                }
                row.addView(toggle, LinearLayout.LayoutParams(0, -2, 1f)); list.addView(row)
            }
            if (list.childCount == 0) list.addView(ui.text("لا توجد تطبيقات مطابقة", 15f, ui.muted))
        }
        list.addView(ui.text("جارٍ تحميل التطبيقات…", 14f, ui.muted))
        worker.execute {
            val apps = runCatching { AppCatalog.list(this) }.getOrDefault(emptyList())
            runOnUiThread { if (version == screenVersion && !isDestroyed) { catalog = apps; render(search.text.toString()) } }
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { render(s?.toString() ?: "") }
            override fun afterTextChanged(s: Editable?) {}
        })
        root.addView(ui.text("لا تُقفل إعدادات النظام أو الشاشة الرئيسية أو تطبيق الاتصال؛ لتبقى إدارة الجهاز والطوارئ متاحة.", 12f, ui.muted))
    }
    private fun consentDialog() {
        AlertDialog.Builder(this).setTitle("صلاحية رصد التطبيق المفتوح")
            .setMessage("تُستخدم إمكانية الوصول لمعرفة اسم التطبيق الذي تفتحه وعرض شاشة قفله. لا نقرأ نصوص الشاشة أو الرسائل، ولا نسجل ضغطات المفاتيح. لا توجد صلاحية إنترنت أو خدمات إعلانات.\n\nيمكنك تعطيل الخدمة في أي وقت من إعدادات أندرويد. بعد الموافقة اختر «قفل الكود الأول» وفعّل الخدمة.")
            .setPositiveButton("أوافق • فتح الإعدادات") { _, _ ->
                vault.consent = true
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }.setNegativeButton("إلغاء", null).show()
    }
    private fun settings() {
        if (!authenticated) { login(); return }
        screenVersion++
        val ui = Ui(this); val root = ui.root()
        ui.header(root, "الإعدادات", "ضبط الحماية حسب استخدامك")
        fun toggle(title: String, checked: Boolean, action: (Boolean) -> Unit) {
            root.addView(Switch(this).apply {
                text = title; textSize = 17f; setTextColor(ui.ink); minHeight = ui.dp(60)
                isChecked = checked; filterTouchesWhenObscured = true
                setOnCheckedChangeListener { _, value -> action(value) }
            })
        }
        toggle("الوضع الليلي", vault.dark) { vault.dark = it; settings() }
        toggle("السماح ببصمة الجهاز", vault.biometrics) { enabled ->
            if (!enabled) vault.biometrics = false
            else {
                val message = ui.text("أكمل التحقق لتفعيل البصمة", 14f, ui.muted); root.addView(message)
                biometric(message) { vault.biometrics = true; settings() }
                // Keep persisted switch off until Android confirms enrollment/authentication.
                vault.biometrics = false
            }
        }
        root.addView(ui.text("متى يُطلب الرمز مجددًا؟", 19f))
        val choices = arrayOf("عند الانتقال إلى تطبيق آخر", "بعد دقيقة من الفتح", "بعد خمس دقائق من الفتح")
        val values = longArrayOf(0, 60_000, 300_000)
        root.addView(ui.button(choices[values.indexOf(vault.grace).coerceAtLeast(0)]) {
            AlertDialog.Builder(this).setTitle("إعادة القفل").setSingleChoiceItems(choices, values.indexOf(vault.grace)) { dialog, index ->
                vault.grace = values[index]; dialog.dismiss(); settings()
            }.setNegativeButton("إلغاء", null).show()
        })
        root.addView(ui.text("تُلغى جميع جلسات الفتح عند إطفاء الشاشة أو إعادة تشغيل الخدمة.", 13f, ui.muted))
        root.addView(ui.button("تغيير رمز القفل") {
            val gateUi = Ui(this); val gateRoot = gateUi.root()
            gateUi.header(gateRoot, "تأكيد الهوية", "أدخل الرمز الحالي قبل تغييره")
            pinAuth(gateUi, gateRoot, { setup() }, false)
        })
        root.addView(ui.button("إلغاء الموافقة وتعطيل الخدمة") {
            AlertDialog.Builder(this).setTitle("تعطيل الحماية؟").setMessage("لن يطلب التطبيق رمزًا عند فتح التطبيقات المحددة حتى تعيد تفعيل الخدمة.")
                .setPositiveButton("تعطيل") { _, _ -> vault.consent = false; LockService.instance?.disableSelf(); dashboard() }
                .setNegativeButton("إلغاء", null).show()
        })
        root.addView(ui.text("خصوصيتك", 21f))
        root.addView(ui.text("كل البيانات محفوظة على جهازك فقط. يُحفظ الرمز كمشتق PBKDF2 مع ملح عشوائي، وليس كنص واضح. لا إعلانات، لا تتبع، لا حسابات ولا صلاحية إنترنت.", 14f, ui.muted))
        root.addView(ui.text("حدود الحماية", 21f))
        root.addView(ui.text("هذه حماية إضافية وليست بديلًا عن قفل الجهاز. يمكن تجاوزها بإيقاف الخدمة أو حذف التطبيق أو مسح بياناته أو الوضع الآمن. قد يظهر التطبيق للحظة قبل شاشة القفل، ولا يمكن حجب إشعارات التطبيقات أو صورها في قائمة التطبيقات الأخيرة. لا تعتمد عليها لحماية جهاز مشترك عالي الحساسية.", 14f, ui.muted))
        root.addView(ui.text("إذا أوقف الهاتف الخدمة لتوفير البطارية، راجع إعدادات البطارية للتطبيق. بعض أجهزة أندرويد تطلب السماح بالإعدادات المقيّدة للتطبيق المثبت يدويًا قبل تفعيل إمكانية الوصول.", 14f, ui.muted))
        root.addView(ui.text("الإصدار التجريبي 0.1.0 • FIRST CODE", 12f, ui.muted))
        root.addView(ui.button("العودة إلى التطبيقات") { dashboard() })
    }
}
