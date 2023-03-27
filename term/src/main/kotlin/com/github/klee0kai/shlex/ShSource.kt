package com.github.klee0kai.shlex

import java.io.BufferedReader
import java.io.File

data class ShSource(
    val input: BufferedReader,
    val file: File? = null,
    var lineno: Int = 0,
)
