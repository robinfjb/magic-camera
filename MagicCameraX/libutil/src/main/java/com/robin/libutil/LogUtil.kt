package com.robin.libutil

import android.util.Log

object LogUtil {
    val V = Log.VERBOSE
    val D = Log.DEBUG
    val I = Log.INFO
    val W = Log.WARN
    val E = Log.ERROR
    val A = Log.ASSERT

    private val LOG_MAXLENGTH = 2000
    private val MAX_LOG_LINE = 200
    private val DEBUG: Boolean = BuildConfig.DEBUG
    private val DEFAULT_TAG = "LogUtil"

    fun v(contents: String?) {
        log(V, DEFAULT_TAG, contents)
    }

    fun v(tag: String, contents: String?) {
        log(V, tag, contents)
    }

    fun d(contents: String?) {
        log(D, DEFAULT_TAG, contents)
    }

    fun d(tag: String, contents: String?) {
        log(D, tag, contents)
    }

    fun i(contents: String?) {
        log(I, DEFAULT_TAG, contents)
    }

    fun i(tag: String, contents: String?) {
        log(I, tag, contents)
    }

    fun w(contents: String?) {
        log(W, DEFAULT_TAG, contents)
    }

    fun w(tag: String, contents: String?) {
        log(W, tag, contents)
    }

    fun e(contents: String?) {
        log(E, DEFAULT_TAG, contents)
    }

    fun e(tag: String, contents: String?) {
        log(E, tag, contents)
    }

    fun e(msg: String, tr: Throwable?) {
        e(DEFAULT_TAG, msg, tr)
    }

    fun e(tag: String, msg: String, tr: Throwable?) {
        log(E, tag, "$msg\n${Log.getStackTraceString(tr)}")
    }

    fun a(contents: String?) {
        log(A, DEFAULT_TAG, contents)
    }

    fun a(tag: String, contents: String?) {
        log(A, tag, contents)
    }

    /**
     * 一行最多2000字，最多200行
     * @param type
     * @param TAG
     * @param msg
     */
    private fun log(type: Int, TAG: String, msg: String?) {
        if (!DEBUG) return
        if (msg == null) return
        val strLength = msg.length.toLong()
        var start = 0
        var end = LOG_MAXLENGTH
        for (i in 0 until MAX_LOG_LINE) {
            if (strLength > end) {
                Log.println(type, TAG, msg.substring(start, end))
                start = end
                end += LOG_MAXLENGTH
            } else {
                Log.println(type, TAG, msg.substring(start))
                break
            }
        }
    }

}