package com.robin.libutil

import android.app.Application
import java.lang.reflect.InvocationTargetException

object AppUtil {
    private var sApp: Application? = null

    fun init(app: Application?) {
        if (app == null) {
            LogUtil.e("app is null.")
            return
        }
        if (sApp == null) {
            sApp = app
            return
        }
        if (sApp == app) return
        sApp = app
    }

    fun getApp(): Application {
        if (sApp != null) return sApp!!
        init(getApplicationByReflect())
        if (sApp == null) throw NullPointerException("reflect failed.")
        return sApp!!
    }

    private fun getApplicationByReflect(): Application? {
        try {
            val activityThreadClass =
                Class.forName("android.app.ActivityThread")
            val thread = getActivityThread()
            val app = activityThreadClass.getMethod("getApplication").invoke(thread) ?: return null
            return app as Application
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    private fun getActivityThread(): Any? {
        val activityThread = getActivityThreadInActivityThreadStaticField()
        return activityThread ?: getActivityThreadInActivityThreadStaticMethod()
    }

    private fun getActivityThreadInActivityThreadStaticField(): Any? {
        return try {
            val activityThreadClass =
                Class.forName("android.app.ActivityThread")
            val sCurrentActivityThreadField =
                activityThreadClass.getDeclaredField("sCurrentActivityThread")
            sCurrentActivityThreadField.isAccessible = true
            sCurrentActivityThreadField[null]
        } catch (e: Exception) {
            LogUtil.e("AppUtil", "getActivityThreadInActivityThreadStaticField: ", e)
            null
        }
    }

    private fun getActivityThreadInActivityThreadStaticMethod(): Any? {
        return try {
            val activityThreadClass =
                Class.forName("android.app.ActivityThread")
            activityThreadClass.getMethod("currentActivityThread").invoke(null)
        } catch (e: Exception) {
            LogUtil.e("AppUtil", "getActivityThreadInActivityThreadStaticMethod: ", e)
            null
        }
    }
}