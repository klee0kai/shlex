package com.github.klee0kai.shlex

data class ShlexConfig(
    var posix: Boolean = true,
    var whitespaceSplit: Boolean = true,
    var punctuationChars: Boolean = false,


    var tag: String = "shlex",
    var debug: Int = 0,
    )

