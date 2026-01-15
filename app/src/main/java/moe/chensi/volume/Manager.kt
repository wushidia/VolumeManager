package moe.chensi.volume

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import moe.chensi.volume.data.App
import moe.chensi.volume.data.AppPreferencesStore
import moe.chensi.volume.data.Player
import moe.chensi.volume.manager.PackageManagerProxy
import org.joor.Reflect
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import java.lang.reflect.Method

@SuppressLint("PrivateApi")
class Manager(context: Context, dataStore: DataStore<Preferences>) {
    companion object {
        private val getClientPidMethod: Method =
            AudioPlaybackConfiguration::class.java.getDeclaredMethod("getClientPid")
        private val getPlayerProxyMethod: Method =
            AudioPlaybackConfiguration::class.java.getDeclaredMethod("getPlayerProxy")
    }

    enum class ShizukuStatus {
        Disconnected, PermissionDenied, Connected
    }

    private var _shizukuStatus by mutableStateOf(ShizukuStatus.Disconnected)
    val shizukuStatus
        get() = _shizukuStatus

    val audioManager = context.getSystemService(AudioManager::class.java)!!.apply {
        Reflect.onClass(AudioManager::class.java).call("getService").get<Any>()
            .apply { ToggleableBinderProxy.wrap(this) }
    }

    val activityManager = context.getSystemService(ActivityManager::class.java)!!.apply {
        Reflect.onClass(ActivityManager::class.java).call("getService").get<Any>()
            .apply { ToggleableBinderProxy.wrap(this) }
    }
    private val packageManager = PackageManagerProxy(context)

    private val appPreferencesStore = AppPreferencesStore(dataStore)

    val apps = mutableStateMapOf<String, App>()

    private fun reloadApps() {
        for (app in packageManager.getInstalledApplicationsForAllUsers()) {
            if (!apps.containsKey(app.packageName)) {
                apps[app.packageName] = App(
                    app.packageName,
                    packageManager.loadLabel(app),
                    packageManager.getDrawable(app.packageName, app.icon, app)
                        ?: packageManager.defaultActivityIcon,
                    appPreferencesStore.getOrCreate(app.packageName),
                    appPreferencesStore::save
                )
            }
        }
    }

    private fun getApp(packageName: String): App? {
        val app = apps[packageName]
        if (app != null) {
            return app
        }

        // Maybe just installed?
        reloadApps()
        return apps[packageName]
    }

    @EnableBinderProxy
    private fun initialize() {
        reloadApps()

        val playbackConfigurations = audioManager.activePlaybackConfigurations
        processAudioPlaybackConfigurations(playbackConfigurations)

        audioManager.registerAudioPlaybackCallback(
            object : AudioManager.AudioPlaybackCallback() {
                override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
                    for (app in apps.values) {
                        app.clearPlayers()
                    }
                    processAudioPlaybackConfigurations(configs)
                }
            }, null
        )
    }

    @SuppressLint("DiscouragedPrivateApi")
    @EnableBinderProxy
    fun processAudioPlaybackConfigurations(configs: List<AudioPlaybackConfiguration>) {
        val runningProcesses = activityManager.runningAppProcesses

        for (config in configs) {
            val playerProxy = getPlayerProxyMethod.invoke(config) ?: continue

            val pid = getClientPidMethod.invoke(config) as Int
            val process = runningProcesses.find { process -> process.pid == pid } ?: continue

            val packageName = process.pkgList[0] ?: continue
            val app = getApp(packageName) ?: continue

            app.addPlayer(Player(config, playerProxy))
        }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky {
            if (Shizuku.isPreV11()) {
                return@addBinderReceivedListenerSticky
            }

            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                _shizukuStatus = ShizukuStatus.Connected
                start()
            } else {
                _shizukuStatus = ShizukuStatus.PermissionDenied
            }
        }

        Shizuku.addBinderDeadListener {
            _shizukuStatus = ShizukuStatus.Disconnected
        }

        Shizuku.addRequestPermissionResultListener { _, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                _shizukuStatus = ShizukuStatus.Connected
                start()
            }
        }

        ShizukuProvider.requestBinderForNonProviderProcess(context)
    }

    private fun start() {
        appPreferencesStore.track { first ->
            for ((packageName, index) in appPreferencesStore.indices) {
                if (!first) {
                    // Replace with new reference
                    getApp(packageName)?.setPreferences(appPreferencesStore.values[index])
                }
            }

            if (first) {
                initialize()
            }
        }
    }
}
