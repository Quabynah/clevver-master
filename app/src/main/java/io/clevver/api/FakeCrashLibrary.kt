/*
 * Copyright (c) 2018. Property of Dennis Kwabena Bilson. No unauthorized duplication of this material should be made without prior permission from the developer
 */

package io.clevver.api

import android.util.Log

/** Not a real crash reporting library!  */
class FakeCrashLibrary private constructor() {

    init {
        throw AssertionError("No instances.")
    }

    companion object {
        fun log(priority: Int, tag: String?, message: String) {
            Log.println(priority, tag, message)
        }

        fun logWarning(t: Throwable) {
            Log.getStackTraceString(t)
        }

        fun logError(t: Throwable) {
            Log.getStackTraceString(t)
        }
    }
}
