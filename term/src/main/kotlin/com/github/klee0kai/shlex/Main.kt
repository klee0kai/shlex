package com.github.klee0kai.shlex

fun main(arg: Array<String>) {
    val s = Shlex.split("git clone origin http://fdfd") {
        debug = 3
        whitespaceSplit = false
    }.toList()

    println("hello world kotlin")
}