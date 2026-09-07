package com.fixvol.app.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppMetadata(
    val packageName: String,
    val appName: String,
    val icon: Drawable? = null,
    val uid: Int? = null
)

class AppResolver(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val cache = mutableMapOf<String, AppMetadata>()

    fun resolve(packageName: String): AppMetadata {
        cache[packageName]?.let { return it }

        val metadata = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val icon = packageManager.getApplicationIcon(appInfo)
            AppMetadata(
                packageName = packageName,
                appName = appName,
                icon = icon,
                uid = appInfo.uid
            )
        } catch (e: Exception) {
            AppMetadata(
                packageName = packageName,
                appName = packageName.substringAfterLast('.').capitalizeFirstChar()
            )
        }

        cache[packageName] = metadata
        return metadata
    }

    fun getInstalledMediaApps(): List<AppMetadata> {
        val installedApps = try {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList()
        }

        return installedApps
            .filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { appInfo ->
                val pkgName = appInfo.packageName
                resolve(pkgName)
            }
            .sortedBy { it.appName.lowercase() }
    }

    private fun String.capitalizeFirstChar(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
