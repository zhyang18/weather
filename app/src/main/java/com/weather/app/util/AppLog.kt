package com.weather.app.util

import android.util.Log

/**
 * 全环境安全日志工具类
 *
 * 统一封装 Android 原生 [Log] 调用，并在纯 JVM 单元测试环境下自动安全回退至控制台标准输出，
 * 避免 "Method d in android.util.Log not mocked" 异常。
 */
object AppLog {

    /**
     * 输出调试级别 Debug 日志
     *
     * @param tag 日志标签
     * @param msg 日志消息文本
     */
    fun d(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (_: Throwable) {
            println("DEBUG/[$tag]: $msg")
        }
    }

    /**
     * 输出信息级别 Info 日志
     *
     * @param tag 日志标签
     * @param msg 日志消息文本
     */
    fun i(tag: String, msg: String) {
        try {
            Log.i(tag, msg)
        } catch (_: Throwable) {
            println("INFO/[$tag]: $msg")
        }
    }

    /**
     * 输出警告级别 Warn 日志
     *
     * @param tag 日志标签
     * @param msg 日志消息文本
     */
    fun w(tag: String, msg: String) {
        try {
            Log.w(tag, msg)
        } catch (_: Throwable) {
            System.err.println("WARN/[$tag]: $msg")
        }
    }

    /**
     * 输出错误级别 Error 日志
     *
     * @param tag 日志标签
     * @param msg 日志消息文本
     * @param tr 关联的异常对象（可选）
     */
    fun e(tag: String, msg: String, tr: Throwable? = null) {
        try {
            if (tr != null) {
                Log.e(tag, msg, tr)
            } else {
                Log.e(tag, msg)
            }
        } catch (_: Throwable) {
            System.err.println("ERROR/[$tag]: $msg")
            tr?.printStackTrace()
        }
    }
}
