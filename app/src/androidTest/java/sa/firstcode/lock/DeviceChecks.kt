package sa.firstcode.lock

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeviceChecks {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun clean() { context.getSharedPreferences("vault", Context.MODE_PRIVATE).edit().clear().commit() }
    private fun texts(view: View): List<TextView> =
        (if (view is TextView) listOf(view) else emptyList()) +
            (if (view is ViewGroup) (0 until view.childCount).flatMap { texts(view.getChildAt(it)) } else emptyList())
    private fun awaitText(s: ActivityScenario<MainActivity>, value: String) {
        val until = System.currentTimeMillis() + 15000
        do {
            var found = false
            s.onActivity { a -> found = texts(a.window.decorView).any { it.text.toString() == value } }
            if (found) return
            Thread.sleep(100)
        } while (System.currentTimeMillis() < until)
        fail("Expected screen text: $value")
    }
    @Test fun pinLockoutPersistsAndCannotBeBypassedByCorrectPin() {
        val v = Vault(context); v.setPin("739152".toCharArray())
        repeat(5) { assertFalse(v.verify("111111".toCharArray())) }
        assertTrue(Vault(context).remaining() > 0)
        assertFalse(Vault(context).verify("739152".toCharArray()))
        v.clearFailures(); assertTrue(v.verify("739152".toCharArray()))
    }
    @Test fun pauseAndResumeKeepSelectionsAndClearSessions() {
        val v = Vault(context); v.protected = setOf("example.one", "example.two")
        v.enabled = false; assertFalse(Vault(context).enabled)
        assertEquals(2, Vault(context).protected.size)
        v.enabled = true; assertTrue(Vault(context).enabled)
        v.protected = emptySet(); assertTrue(Vault(context).protected.isEmpty())
    }
    @Test fun manifestHasNoNetworkOrDangerousDataPermissions() {
        val p = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS)
        assertEquals(setOf("android.permission.USE_BIOMETRIC"), p.requestedPermissions.toSet())
        assertEquals(0, p.applicationInfo!!.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
        assertEquals("android.permission.BIND_ACCESSIBILITY_SERVICE", p.services.single().permission)
        assertEquals("android.permission.BIND_DEVICE_ADMIN", p.receivers.single().permission)
        assertFalse(RemovalAdmin.active(context))
    }
    @Test fun arabicAndEnglishScreensSupportPinLoginAndPause() {
        val v = Vault(context); v.setPin("739152".toCharArray()); v.english = true
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            awaitText(s, "Welcome back")
            s.onActivity { a ->
                assertTrue(a.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
                for (digit in "739152") texts(a.window.decorView).filterIsInstance<Button>().single { it.text.toString() == digit.toString() }.performClick()
            }
            awaitText(s, "Your apps")
            s.onActivity { a -> texts(a.window.decorView).filterIsInstance<Button>().single { it.text.toString() == "Pause app lock" }.performClick() }
            awaitText(s, "Resume app lock")
            assertFalse(v.enabled)
        }
        v.english = false
        ActivityScenario.launch(MainActivity::class.java).use { s -> awaitText(s, "مرحبًا بعودتك") }
    }
    @Test fun unavailableBiometricsAutomaticallyFallsBackToPin() {
        val v = Vault(context); v.setPin("739152".toCharArray()); v.english = true; v.biometrics = true
        // CI emulator has no enrolled biometrics: do not press the retry button.
        ActivityScenario.launch(MainActivity::class.java).use { s ->
            awaitText(s, "Biometric verification failed. Use your PIN.")
            s.onActivity { a ->
                for (digit in "739152") texts(a.window.decorView).filterIsInstance<Button>().single { it.text.toString() == digit.toString() }.performClick()
            }
            awaitText(s, "Your apps")
        }
    }
}
