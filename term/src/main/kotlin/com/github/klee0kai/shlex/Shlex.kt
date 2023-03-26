package com.github.klee0kai.shlex

object Shlex {

    /**
     * Split the string *s* using shell-like syntax.
     */
    fun split(cmd: String, comments: Boolean = false, posix: Boolean = true): Sequence<String> =
        ShLexer(input = cmd.byteInputStream(), posix = posix)


    /**
     * Return a shell-escaped string from *split_command*.
     */
    fun join(vararg splitCommand: String): String {
        return splitCommand.joinToString(" ") { quote(it) }
    }

    /**
     * Return a shell-escaped version of the string *s*.
     */
    fun quote(s: String?): String {
        if (s.isNullOrBlank()) return "''";


        val isUnsafe = Regex("[^\\w@%+=:,./-]").find(s) != null
        if (!isUnsafe) return s

        // use single quotes, and put single quotes into double quotes
        // the string $'b is then quoted as '$'"'"'b'
        return "'" + s.replace("'", "'\"'\"'") + "'"
    }

}