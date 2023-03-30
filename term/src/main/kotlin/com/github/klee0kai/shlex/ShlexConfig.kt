package com.github.klee0kai.shlex

import java.io.File

data class ShlexConfig(
    var infile: File? = null,
    var source: String? = null,

    var posix: Boolean = true,
    var comments: Boolean = false,
    var whitespaceSplit: Boolean = true,
    var punctuationChars: Boolean = false,
    var customPunctuationChars: String? = null,

    var tag: String = "shlex",
    var debug: Int = 0,
)

