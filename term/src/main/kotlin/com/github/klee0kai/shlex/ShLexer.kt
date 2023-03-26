package com.github.klee0kai.shlex

import java.io.BufferedReader
import java.io.InputStream
import java.util.*

open class ShLexer(
    protected val reader: BufferedReader,
    protected open val posix: Boolean = true,
    punctuationChars: Boolean = false,
) : Sequence<String>, Iterator<String> {

    constructor(
        input: InputStream, posix: Boolean = true, punctuationChars: Boolean = false,
    ) : this(BufferedReader(input.reader()), posix, punctuationChars)

    constructor(
        input: String, posix: Boolean = true, punctuationChars: Boolean = false,
    ) : this(BufferedReader(input.reader()), posix, punctuationChars)

    protected open val tag = "shlex"

    protected open val debug = 0

    protected open val commenters = "#"
    protected open val whitespace = " \t\r\n"
    protected open val whitespaceSplit = false
    protected open val quotes = "'\""
    protected open val escape = "\\"
    protected open val escapedquotes = "\""
    protected open val wordchars = buildString {
        append("abcdfeghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_")
        if (posix) append("ßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞ")
        if (punctuationChars) {
            // these chars added because allowed in file names, args, wildcards
            append("~-./*?=")
        }
    }
    protected open val punctuationChars = if (punctuationChars) "();<>|&" else ""

    protected open val pushback: Deque<String> = LinkedList()
    protected open val pushbackChars: Deque<Char> = LinkedList()

    protected open var state: Char? = ' '
    protected open var token = ""
    protected open var lineno = 0


    override fun iterator(): Iterator<String> = this

    override fun hasNext(): Boolean = pushback.isNotEmpty()

    override fun next(): String = nextToken()!!

    protected open fun nextToken(): String? {
        if (pushback.isNotEmpty())
            return pushback.pollFirst().also {
                if (debug >= 1) println("${tag}: popping token $it")
            }

        // No pushback.  Get a token.
        val raw = readToken()
        // todo Handle inclusions

        // Maybe we got EOF instead?

        // Neither inclusion nor EOF
        if (debug >= 1) print(raw?.let { "${tag}: token=${raw}" } ?: "shlex: token=EOF")

        return raw
    }

    protected open fun readToken(): String? {
        var quoted = false
        var escapedstate = ' '
        var nextchar: Char

        while (true) {

            nextchar = if (pushbackChars.isNullOrEmpty()) pushbackChars.pollFirst() else reader.read().toChar()

            if (nextchar == '\n') lineno += 1
            if (debug >= 3) print("${tag}: in state $state I see character: $nextchar")

            when {
                state == null -> {
                    token = "" // past end of file
                    break
                }

                state == ' ' -> {
                    when {
                        nextchar == null -> {

                        }

                        nextchar in whitespace -> {
                            if (debug >= 2) println("shlex: I see whitespace in whitespace state")
                            if ((token.isNotEmpty()) || (posix && quoted)) break   // emit current token
                            else continue
                        }

                        nextchar in commenters -> {
                            reader.readLine()
                            lineno += 1
                        }

                        posix && nextchar in escape -> {
                            escapedstate = 'a'
                            state = nextchar
                        }

                        nextchar in wordchars -> {
                            token = nextchar.toString()
                            state = 'a'
                        }

                        nextchar in punctuationChars -> {
                            token = nextchar.toString()
                            state = 'c'
                        }

                        nextchar in quotes -> {
                            if (!posix) token = nextchar.toString()
                            state = nextchar
                        }

                        whitespaceSplit -> {
                            token = nextchar.toString()
                            state = 'a'
                        }

                        else -> {
                            token = nextchar.toString()
                            if (token.isNotEmpty() || (posix && quoted)) break   // emit current token
                            else continue
                        }
                    }
                }

                state!! in quotes -> {
                    quoted = true
                    when {
                        nextchar == null -> {
                            // end of file
                            if (debug >= 2) print("shlex: I see EOF in quotes state")
                            //  XXX what error should be raised here?
                            error("No closing quotation")
                        }

                        nextchar == state -> {
                            if (!posix) {
                                token += nextchar
                                state = ' '
                                break
                            } else {
                                state = 'a'
                            }
                        }

                        posix && nextchar in escape && state!! in escapedquotes -> {
                            escapedstate = state!!
                            state = nextchar
                        }

                        else -> {
                            token += nextchar
                        }
                    }
                }

                state!! in escape -> {
                    if (nextchar == null) {
                        // end of file
                        if (debug >= 2) print("shlex: I see EOF in quotes state")
                        //  XXX what error should be raised here?
                        error("No closing quotation")
                    }
                    if (escapedstate in quotes && nextchar != state && nextchar != escapedstate) {
                        // In posix shells, only the quote itself or the escape
                        // character may be escaped within quotes.
                        token += state
                    }
                    token += nextchar
                    state = escapedstate
                }

                state!! in listOf('a', 'c') -> {
                    when {
                        nextchar == null -> {
                            state = null // end of file
                            break
                        }

                        nextchar in whitespace -> {
                            if (debug >= 2) println("shlex: I see whitespace in word state")
                            state = ' '
                            if (token.isNotEmpty() || (posix && quoted)) break   // emit current token
                            else continue
                        }

                        nextchar in commenters -> {
                            reader.readLine()
                            lineno += 1
                            if (posix) state = ' '
                            if (token.isNotEmpty() || (posix && quoted)) break   // emit current token
                            else continue
                        }

                        state == 'c' -> {
                            if (nextchar in punctuationChars)
                                token += nextchar
                            else {
                                if (nextchar !in whitespace) pushbackChars.add(nextchar)
                                state = ' '
                                break
                            }
                        }

                        posix && nextchar in quotes -> {
                            state = nextchar
                        }

                        posix && nextchar in escape -> {
                            escapedstate = 'a'
                            state = nextchar
                        }

                        nextchar in wordchars || nextchar in quotes
                                || (whitespaceSplit && nextchar !in punctuationChars) -> {
                            token += nextchar
                        }

                        else -> {
                            if (punctuationChars.isNotEmpty()) pushbackChars.addLast(nextchar)
                            else pushback.addFirst(nextchar.toString())
                            if (debug >= 2) println("shlex: I see punctuation in word state")
                            state = ' '

                            if (token.isNotEmpty() || (posix && quoted)) break   // emit current token
                            else continue
                        }
                    }
                }
            }
        }

        var result: String? = token
        token = ""
        if (posix && !quoted && result.isNullOrEmpty()) result = null

        if (debug > 1) println(result?.let { "${tag}: raw token=${result}" } ?: "shlex: raw token=EOF")
        return result
    }


}