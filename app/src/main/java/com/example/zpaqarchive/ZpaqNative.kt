Warning: ff was declared with const; use let for reassignable variables.
package com.example.zpaqarchive

internal object ZpaqNative {
    init {
        System.loadLibrary("zpaqengine")
    }

    external fun runNative(args: Array<String>, workingDir: String, logPath: String): Int
}
