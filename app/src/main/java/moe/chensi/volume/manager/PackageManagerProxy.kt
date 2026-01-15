package moe.chensi.volume.manager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.IBinder
import org.joor.Reflect
import rikka.shizuku.ShizukuBinderWrapper

class PackageManagerProxy(context: Context) {
    private val userManager = UserManagerProxy(context)

    private val packageManager = context.packageManager
    private val reflect = Reflect.on(packageManager)

    init {
        val service = Reflect.onClass("android.app.ActivityThread").call("getPackageManager")
        val remote = service.get<IBinder>("mRemote")
        val wrapper = remote as? ShizukuBinderWrapper ?: ShizukuBinderWrapper(remote)
        service.set("mRemote", wrapper)
    }

    val defaultActivityIcon
        get() = packageManager.defaultActivityIcon

    fun getInstalledApplicationsForAllUsers(): List<ApplicationInfo> {
        val apps = mutableMapOf<String, ApplicationInfo>()

        for (userId in userManager.getUserIds()) {
            for (app in reflect.call("getInstalledApplicationsAsUser", 0, userId)
                .get<List<ApplicationInfo>>()) {
                apps[app.packageName] = app
            }
        }

        return apps.values.toList()
    }

    fun getDrawable(packageName: String, resId: Int, appInfo: ApplicationInfo): Drawable? {
        return packageManager.getDrawable(packageName, resId, appInfo)
    }

    fun loadLabel(appInfo: ApplicationInfo): String {
        return appInfo.loadLabel(packageManager).toString()
    }
}