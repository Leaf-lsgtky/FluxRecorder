package com.island.recorder.utils

import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object ShizukuHelper {

    private const val TAG = "ShizukuHelper"

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Exception) {
        false
    }

    fun hasPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) {
        false
    }

    fun requestPermission(requestCode: Int) {
        Shizuku.requestPermission(requestCode)
    }

    fun addPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.addRequestPermissionResultListener(listener)
    }

    fun removePermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.removeRequestPermissionResultListener(listener)
    }

    private fun wrapService(serviceName: String, stubClassName: String): Any? {
        return try {
            val originalBinder = SystemServiceHelper.getSystemService(serviceName)
            val stubClass = Class.forName(stubClassName)
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            val original = asInterface.invoke(null, originalBinder)!!
            val binder = original.javaClass.getMethod("asBinder").invoke(original) as IBinder
            val wrapper = ShizukuBinderWrapper(binder)
            asInterface.invoke(null, wrapper)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to wrap service $serviceName: ${e.message}")
            null
        }
    }

    private val wrappedCM: Any? by lazy { wrapService("connectivity", "android.net.IConnectivityManager\$Stub") }

    private fun invokeIfExists(obj: Any, method: String, paramTypes: Array<Class<*>>, args: Array<Any>): Boolean {
        return try {
            obj.javaClass.getMethod(method, *paramTypes).invoke(obj, *args)
            true
        } catch (_: NoSuchMethodException) {
            false
        } catch (e: Throwable) {
            Log.w(TAG, "$method failed: ${e.cause?.message ?: e.message}")
            false
        }
    }

    /**
     * Block network for [packageName] (uid [uid] used as fallback for iptables).
     * Strategy: IConnectivityManager binder hook (HyperOS standard)
     */
    fun blockNetwork(uid: Int, packageName: String): Boolean {
        val cm = wrappedCM
        if (cm != null) {
            val chainOk = invokeIfExists(
                cm, "setFirewallChainEnabled",
                arrayOf(Int::class.javaPrimitiveType!!, Boolean::class.javaPrimitiveType!!),
                arrayOf(9, true),
            )
            if (chainOk) {
                invokeIfExists(
                    cm, "setUidFirewallRule",
                    arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
                    arrayOf(9, uid, 2),
                )
                Log.d(TAG, "Network BLOCKED for $packageName via IConnectivityManager")
                return true
            }
        }
        return false
    }

    /**
     * Unblock network for [packageName] (uid [uid] used as fallback for iptables).
     */
    fun unblockNetwork(uid: Int, packageName: String) {
        val cm = wrappedCM
        if (cm != null) {
            invokeIfExists(
                cm, "setUidFirewallRule",
                arrayOf(Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
                arrayOf(9, uid, 0),
            )
            Log.d(TAG, "Network RESTORED for $packageName via IConnectivityManager")
        }
    }
}
