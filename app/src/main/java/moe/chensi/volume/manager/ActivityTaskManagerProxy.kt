package moe.chensi.volume.manager

import android.app.ActivityManager
import android.app.ActivityTaskManager
import android.content.Context
import android.os.IBinder
import org.joor.Reflect
import rikka.shizuku.ShizukuBinderWrapper

class ActivityTaskManagerProxy(context: Context) {
    val activityTaskManager: Reflect =
        context.getSystemService(ActivityTaskManager::class.java).run(Reflect::on)

    init {
        val service = activityTaskManager.call("getService")
        val remote = service.get<IBinder>("mRemote")
        val wrapper = remote as? ShizukuBinderWrapper ?: ShizukuBinderWrapper(remote)
        service.set("mRemote", wrapper)
    }

    fun getForegroundTask(): String? {
        return activityTaskManager.call("getTasks", 1)
            .get<List<ActivityManager.RunningTaskInfo>>()[0].topActivityInfo?.packageName
    }
}