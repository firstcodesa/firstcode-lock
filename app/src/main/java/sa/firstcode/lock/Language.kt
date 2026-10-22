package sa.firstcode.lock

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

fun Context.tr(id: Int, vararg args: Any): String {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(Locale(if (Vault(this).english) "en" else "ar"))
    return createConfigurationContext(configuration).getString(id, *args)
}
