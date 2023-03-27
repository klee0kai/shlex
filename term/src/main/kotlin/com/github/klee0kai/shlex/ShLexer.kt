package com.github.klee0kai.shlex

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.util.*

open class ShLexer(
    reader: BufferedReader,
    conf: ShlexConfig = ShlexConfig()
) : Sequence<String>, Iterator<String> {

    constructor(
        input: InputStream, conf: ShlexConfig = ShlexConfig()
    ) : this(BufferedReader(input.reader()), conf)

    constructor(
        input: String, conf: ShlexConfig = ShlexConfig()
    ) : this(BufferedReader(input.reader()), conf)


    protected open val conf = conf.copy()

    protected open val commenters = "#"
    protected open val whitespace = " \t\r\n"
    protected open val quotes = "'\""
    protected open val escape = "\\"
    protected open val escapedquotes = "\""
    protected open val wordchars = buildString {
        append("abcdfeghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_")
        if (conf.posix) append("ßàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞ")
        if (conf.punctuationChars) {
            // these chars added because allowed in file names, args, wildcards
            append("~-./*?=")
        }
    }
    protected open val punctuationChars = if (conf.punctuationChars) "();<>|&" else ""

    protected open val pushback: Deque<String> = LinkedList()
    protected open val pushbackChars: Deque<Char> = LinkedList()
    protected open val fileStack: Deque<ShSource> = LinkedList()

    protected open var state: Char? = ' '
    protected open var token = ""
    protected open var source = ShSource(input = reader, file = null, lineno = 1)

    private val posix get() = conf.posix
    private val whitespaceSplit get() = conf.whitespaceSplit
    private val tag get() = conf.tag
    private val debug: Int = conf.debug

    override fun iterator(): Iterator<String> = this

    override fun hasNext(): Boolean = nextToken()?.also { pushToken(it) } != null

    override fun next(): String = nextToken()!!

    /**
     * Push an input source onto the lexer's input source stack.
     */
    open fun pushSource(input: BufferedReader, file: File? = null) {
        fileStack.addFirst(source)
        source = ShSource(input, file = file, lineno = 1)
        if (debug >= 1) println("${tag}: pushing source ${source.file?.name ?: input}")
    }

    /**
     * Pop the input source stack.
     */
    open fun popSource() {
        source.input.close()
        source = fileStack.pollFirst()
        if (debug >= 1) println("${tag}: popping source ${source.file?.name ?: source.input}")
        state = ' '
    }

    /**
     * Push a token onto the stack popped by the get_token method
     */
    open fun pushToken(tok: String) {
        if (debug >= 1) println("${tag}: pushing token $tok")
        pushback.addFirst(tok)
    }

    /**
     * Get a token from the input stream (or from stack if it's nonempty)
     */
    open fun nextToken(): String? {
        if (pushback.isNotEmpty())
            return pushback.pollFirst().also {
                if (debug >= 1) println("${tag}: popping token $it")
            }

        // No pushback.  Get a token.
        var raw = readToken()
        // Handle inclusions
        if (conf.source != null) {
            while (raw == conf.source) {
                val spec = readToken()?.let { sourceHook(it) }
                if (spec != null) pushSource(BufferedReader(spec.reader()), file = spec)
                raw = readToken()
            }
        }

        // Maybe we got EOF instead?
        if (raw == null) {
            if (fileStack.isNotEmpty()) {
                popSource()
                raw = nextToken()
            } else {
                state = null
                return null
            }
        }

        // Neither inclusion nor EOF
        if (debug >= 1) print(raw?.let { "${tag}: token=${raw}" } ?: "$tag: token=EOF")
        return raw
    }

    open fun readToken(): String? {
        var quoted = false
        var escapedstate = ' '
        var nextchar: Char?

        while (true) {

            nextchar = if (pushbackChars.isNotEmpty()) pushbackChars.pollLast()
            else source.input.read().let { if (it < 0) null else it.toChar() }

            if (nextchar == '\n') source.lineno += 1
            if (debug >= 3) print("${tag}: in state $state I see character: $nextchar")

            when {
                state == null -> {
                    token = "" // past end of file
                    break
                }

                state == ' ' -> {
                    when {
                        nextchar == null -> {
                            state = null
                            break
                        }

                        nextchar in whitespace -> {
                            if (debug >= 2) println("$tag: I see whitespace in whitespace state")
                            if (token.isNotEmpty() || (posix && quoted)) break   // emit current token
                            else continue
                        }

                        nextchar in commenters -> {
                            source.input.readLine()
                            source.lineno += 1
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
                            if (debug >= 2) print("$tag: I see EOF in quotes state")
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
                        if (debug >= 2) print("$tag: I see EOF in quotes state")
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
                            if (debug >= 2) println("$tag: I see whitespace in word state")
                            state = ' '
                            if (token.isNotEmpty() || (posix && quoted)) break   // emit current token
                            else continue
                        }

                        nextchar in commenters -> {
                            source.input.readLine()
                            source.lineno += 1
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
                            if (debug >= 2) println("$tag: I see punctuation in word state")
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

        if (debug > 1) println(result?.let { "${tag}: raw token=${result}" } ?: "$tag: raw token=EOF")
        return result
    }

    /**
     * Hook called on a filename to be sourced.
     */
    open fun sourceHook(flName: String): File {
        var file = File(if (flName.startsWith("\"")) flName.substring(1, flName.length - 1) else flName)
        // this implements cpp-like semantics for relative-path inclusion.
        if (!file.isAbsolute) file = File(source.file?.parentFile, file.path)
        return file
    }

    /**
     * Emit a C-compiler-like, Emacs-friendly error-message leader.
     */
    open fun errorLeader(infile: File? = null, lineno: Int? = null): String {
        val filename = infile?.absolutePath ?: source.file?.absolutePath ?: ""
        val line = lineno ?: source.lineno ?: 0
        return "\"${filename}\", line ${line}: "
    }

}