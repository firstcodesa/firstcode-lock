package sa.firstcode.lock

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Optional, owner-controlled removal protection. No lock/wipe policies requested. */
class RemovalAdmin : DeviceAdminReceiver() {
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        context.tr(R.string.copy_105)
    companion object {
        fun component(context: Context) = ComponentName(context, RemovalAdmin::class.java)
        fun manager(context: Context) = context.getSystemService(DevicePolicyManager::class.java)
        fun active(context: Context) = manager(context)?.isAdminActive(component(context)) == true
        fun request(context: Context) = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component(context))
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.tr(R.string.copy_106))
        }
    }
}
