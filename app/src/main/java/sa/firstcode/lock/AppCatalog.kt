package sa.firstcode.lock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppEntry(val pkg: String, val name: String, val icon: Drawable)

object AppCatalog {
    private fun launcherIntent() = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    fun excluded(context: Context): Set<String> {
        val pm = context.packageManager
        val set = mutableSetOf(context.packageName, "com.android.settings", "com.android.systemui",
            "com.android.permissioncontroller", "com.google.android.permissioncontroller",
            "com.android.packageinstaller", "com.google.android.packageinstaller")
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_ALL)
            .forEach { set.add(it.activityInfo.packageName) }
        pm.queryIntentActivities(Intent(Intent.ACTION_DIAL), PackageManager.MATCH_ALL)
            .forEach { set.add(it.activityInfo.packageName) }
        return set
    }
    fun list(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val excluded = excluded(context)
        return pm.queryIntentActivities(launcherIntent(), 0).filter { it.activityInfo.packageName !in excluded }
            .distinctBy { it.activityInfo.packageName }
            .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
            .sortedBy { it.name.lowercase() }
    }
    fun isForegroundApp(context: Context, pkg: String): Boolean =
        pkg == "com.android.settings" || pkg in excluded(context) || context.packageManager.getLaunchIntentForPackage(pkg) != null
    fun label(context: Context, pkg: String): String = runCatching {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}
