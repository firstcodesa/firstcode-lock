package sa.firstcode.lock

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*

class Ui(val activity: Activity) {
    private val dark = Vault(activity).dark
    val bg = Color.parseColor(if (dark) "#0C1422" else "#F1F5FA")
    val card = Color.parseColor(if (dark) "#17253A" else "#FFFFFF")
    val ink = Color.parseColor(if (dark) "#F5F8FC" else "#172433")
    val muted = Color.parseColor(if (dark) "#A7B7C9" else "#526477")
    val accent = Color.parseColor("#26D9B0")
    fun dp(n: Int) = (n * activity.resources.displayMetrics.density).toInt()
    fun shape(color: Int, radius: Int = 18) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    fun text(s: String, size: Float = 16f, color: Int = ink) = TextView(activity).apply {
        text = s; textSize = size; setTextColor(color); setPadding(0, dp(6), 0, dp(6))
        gravity = Gravity.START; textDirection = if (Vault(activity).english) View.TEXT_DIRECTION_LTR else View.TEXT_DIRECTION_RTL
    }
    fun button(s: String, action: () -> Unit) = Button(activity).apply {
        text = s; isAllCaps = false; textSize = 16f; setTextColor(ink)
        background = android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(Color.parseColor("#3326D9B0")), shape(card), null)
        minHeight = dp(54); setPadding(dp(16), dp(10), dp(16), dp(10))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8); bottomMargin = dp(4) }
        setOnClickListener { action() }; filterTouchesWhenObscured = true
    }
    fun column() = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; layoutDirection = if (Vault(activity).english) View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL }
    fun root(): LinearLayout {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val column = column().apply { setPadding(dp(24), dp(18), dp(24), dp(24)) }
        val scroll = ScrollView(activity).apply { setBackgroundColor(bg); isFillViewport = true; addView(column) }
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom); insets
        }
        activity.setContentView(scroll)
        scroll.requestApplyInsets()
        return column
    }
    fun header(root: LinearLayout, title: String, subtitle: String) {
        root.addView(ImageView(activity).apply {
            setImageResource(sa.firstcode.lock.R.drawable.ic_lock)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(52), dp(52)).apply { bottomMargin = dp(12) })
        root.addView(text(activity.tr(R.string.copy_000), 12f, accent))
        root.addView(text(title, 30f).apply { typeface = Typeface.DEFAULT_BOLD })
        root.addView(text(subtitle, 14f, muted).apply { setPadding(0, dp(4), 0, dp(18)) })
    }
}

/** A keypad avoids exposing the PIN to a third-party input method. */
class PinPad(private val ui: Ui, private val submit: (CharArray) -> Unit) {
    val view = ui.column()
    private val chars = StringBuilder()
    private val dots = ui.text("○  ○  ○  ○  ○  ○", 26f).apply { gravity = Gravity.CENTER; textDirection = View.TEXT_DIRECTION_LTR }
    private val keys = mutableListOf<Button>()
    var enabled = true
        set(value) { field = value; keys.forEach { it.isEnabled = value } }
    init {
        view.addView(dots)
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(ui.activity.tr(R.string.copy_001), "0", "⌫")).forEach { row ->
            val line = LinearLayout(ui.activity).apply { layoutDirection = View.LAYOUT_DIRECTION_LTR }
            row.forEach { key ->
                val button = ui.button(key) {
                    if (!enabled) return@button
                    when (key) { ui.activity.tr(R.string.copy_001) -> chars.clear(); "⌫" -> if (chars.isNotEmpty()) chars.deleteCharAt(chars.lastIndex)
                        else -> if (chars.length < 6) chars.append(key) }
                    dots.text = (0..5).joinToString("  ") { if (it < chars.length) "●" else "○" }
                    if (chars.length == 6) { val value = chars.toString().toCharArray(); clear(); submit(value) }
                }
                button.layoutParams = LinearLayout.LayoutParams(0, ui.dp(60), 1f).apply { setMargins(ui.dp(4), ui.dp(4), ui.dp(4), ui.dp(4)) }
                keys.add(button); line.addView(button)
            }
            view.addView(line)
        }
    }
    fun clear() { chars.clear(); dots.text = "○  ○  ○  ○  ○  ○" }
}
