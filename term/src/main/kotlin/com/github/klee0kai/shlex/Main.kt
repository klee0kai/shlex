package com.github.klee0kai.shlex

fun main(arg: Array<String>) {
    val s = Shlex.split("git clone origin http://fdfd").toList()

    println("hello world kotlin")
}