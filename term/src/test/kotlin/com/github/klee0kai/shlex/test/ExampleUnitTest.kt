package com.github.klee0kai.shlex.test

import com.github.klee0kai.shlex.ShLexer
import com.github.klee0kai.shlex.Shlex
import com.github.klee0kai.shlex.ShlexConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExampleUnitTest {


    // The original test data set was from shellwords, by Hartmut Goebel.

    val dataStr = """x|x|
foo bar|foo|bar|
 foo bar|foo|bar|
 foo bar |foo|bar|
foo   bar    bla     fasel|foo|bar|bla|fasel|
x y  z              xxxx|x|y|z|xxxx|
\x bar|\|x|bar|
\ x bar|\|x|bar|
\ bar|\|bar|
foo \x bar|foo|\|x|bar|
foo \ x bar|foo|\|x|bar|
foo \ bar|foo|\|bar|
foo "bar" bla|foo|"bar"|bla|
"foo" "bar" "bla"|"foo"|"bar"|"bla"|
"foo" bar "bla"|"foo"|bar|"bla"|
"foo" bar bla|"foo"|bar|bla|
foo 'bar' bla|foo|'bar'|bla|
'foo' 'bar' 'bla'|'foo'|'bar'|'bla'|
'foo' bar 'bla'|'foo'|bar|'bla'|
'foo' bar bla|'foo'|bar|bla|
blurb foo"bar"bar"fasel" baz|blurb|foo"bar"bar"fasel"|baz|
blurb foo'bar'bar'fasel' baz|blurb|foo'bar'bar'fasel'|baz|
""|""|
''|''|
foo "" bar|foo|""|bar|
foo '' bar|foo|''|bar|
foo "" "" "" bar|foo|""|""|""|bar|
foo '' '' '' bar|foo|''|''|''|bar|
\""|\|""|
"\"|"\"|
"foo\ bar"|"foo\ bar"|
"foo\\ bar"|"foo\\ bar"|
"foo\\ bar\"|"foo\\ bar\"|
"foo\\" bar\""|"foo\\"|bar|\|""|
"foo\\ bar\" dfadf"|"foo\\ bar\"|dfadf"|
"foo\\\ bar\" dfadf"|"foo\\\ bar\"|dfadf"|
"foo\\\x bar\" dfadf"|"foo\\\x bar\"|dfadf"|
"foo\x bar\" dfadf"|"foo\x bar\"|dfadf"|
\''|\|''|
'foo\ bar'|'foo\ bar'|
'foo\\ bar'|'foo\\ bar'|
"foo\\\x bar\" df'a\ 'df'|"foo\\\x bar\"|df'a|\|'df'|
\"foo"|\|"foo"|
\"foo"\x|\|"foo"|\|x|
"foo\x"|"foo\x"|
"foo\ "|"foo\ "|
foo\ xx|foo|\|xx|
foo\ x\x|foo|\|x|\|x|
foo\ x\x\""|foo|\|x|\|x|\|""|
"foo\ x\x"|"foo\ x\x"|
"foo\ x\x\\"|"foo\ x\x\\"|
"foo\ x\x\\""foobar"|"foo\ x\x\\"|"foobar"|
"foo\ x\x\\"\''"foobar"|"foo\ x\x\\"|\|''|"foobar"|
"foo\ x\x\\"\'"fo'obar"|"foo\ x\x\\"|\|'"fo'|obar"|
"foo\ x\x\\"\'"fo'obar" 'don'\''t'|"foo\ x\x\\"|\|'"fo'|obar"|'don'|\|''|t'|
'foo\ bar'|'foo\ bar'|
'foo\\ bar'|'foo\\ bar'|
foo\ bar|foo|\|bar|
foo#bar\nbaz|foobaz|
:-) ;-)|:|-|)|;|-|)|
áéíóú|á|é|í|ó|ú|
"""

    val posixDataStr = """x|x|
foo bar|foo|bar|
 foo bar|foo|bar|
 foo bar |foo|bar|
foo   bar    bla     fasel|foo|bar|bla|fasel|
x y  z              xxxx|x|y|z|xxxx|
\x bar|x|bar|
\ x bar| x|bar|
\ bar| bar|
foo \x bar|foo|x|bar|
foo \ x bar|foo| x|bar|
foo \ bar|foo| bar|
foo "bar" bla|foo|bar|bla|
"foo" "bar" "bla"|foo|bar|bla|
"foo" bar "bla"|foo|bar|bla|
"foo" bar bla|foo|bar|bla|
foo 'bar' bla|foo|bar|bla|
'foo' 'bar' 'bla'|foo|bar|bla|
'foo' bar 'bla'|foo|bar|bla|
'foo' bar bla|foo|bar|bla|
blurb foo"bar"bar"fasel" baz|blurb|foobarbarfasel|baz|
blurb foo'bar'bar'fasel' baz|blurb|foobarbarfasel|baz|
""||
''||
foo "" bar|foo||bar|
foo '' bar|foo||bar|
foo "" "" "" bar|foo||||bar|
foo '' '' '' bar|foo||||bar|
\"|"|
"\""|"|
"foo\ bar"|foo\ bar|
"foo\\ bar"|foo\ bar|
"foo\\ bar\""|foo\ bar"|
"foo\\" bar\"|foo\|bar"|
"foo\\ bar\" dfadf"|foo\ bar" dfadf|
"foo\\\ bar\" dfadf"|foo\\ bar" dfadf|
"foo\\\x bar\" dfadf"|foo\\x bar" dfadf|
"foo\x bar\" dfadf"|foo\x bar" dfadf|
\'|'|
'foo\ bar'|foo\ bar|
'foo\\ bar'|foo\\ bar|
"foo\\\x bar\" df'a\ 'df"|foo\\x bar" df'a\ 'df|
\"foo|"foo|
\"foo\x|"foox|
"foo\x"|foo\x|
"foo\ "|foo\ |
foo\ xx|foo xx|
foo\ x\x|foo xx|
foo\ x\x\"|foo xx"|
"foo\ x\x"|foo\ x\x|
"foo\ x\x\\"|foo\ x\x\|
"foo\ x\x\\""foobar"|foo\ x\x\foobar|
"foo\ x\x\\"\'"foobar"|foo\ x\x\'foobar|
"foo\ x\x\\"\'"fo'obar"|foo\ x\x\'fo'obar|
"foo\ x\x\\"\'"fo'obar" 'don'\''t'|foo\ x\x\'fo'obar|don't|
"foo\ x\x\\"\'"fo'obar" 'don'\''t' \\|foo\ x\x\'fo'obar|don't|\|
'foo\ bar'|foo\ bar|
'foo\\ bar'|foo\\ bar|
foo\ bar|foo bar|
foo#bar\nbaz|foo|baz|
:-) ;-)|:-)|;-)|
áéíóú|áéíóú|
"""

    private val data = dataStr
        .split("\n")
        .map { it.split("|") }
        .filter { !(it.isEmpty() || it.size == 1 && it[0].isEmpty()) }

    private val posixData = posixDataStr
        .split("\n")
        .map { it.split("|") }
        .filter { !(it.isEmpty() || it.size == 1 && it[0].isEmpty()) }

    /**
     * Test data splitting with posix parser
     */
    @Test
    fun testSplitPosix() {
        splitTest(posixData, true)
    }

    /**
     * Test compatibility interface
     */
    @Test
    fun testCompat() {
        data.forEach {
            val splitted = oldSplit(it[0])
            assertEquals(it.subList(1, it.size - 1), splitted)
        }
    }

    /**
     * Test handling of syntax splitting of &, |
     */
    @Test
    fun testSyntaxSplitAmpersandAndPipe() {
        //  Could take these forms: &&, &, |&, ;&, ;;&
        //  of course, the same applies to | and ||
        //  these should all parse to the same output
        listOf("&&", "&", "|&", ";&", ";;&", "||", "|", "&|", ";|", ";;|").forEach { delimiter ->
            val src = arrayOf("echo hi ${delimiter} echo bye", "echo hi${delimiter}echo bye' % delimiter")
            val ref = listOf("echo", "hi", delimiter, "echo", "bye")
            src.forEach { ss ->
                listOf(true, false).forEach { whitespaceSplitFlag ->
                    val s = Shlex.split(ss) {
                        punctuationChars = true
                        whitespaceSplit = whitespaceSplitFlag
                    }
                    assertEquals(ref, s.toList())
                }
            }
        }

    }


    /**
     * Test handling of syntax splitting of ;
     */
    @Test
    fun testSyntaxSplitSemicolon() {
        // Could take these forms: ;, ;;, ;&, ;;&
        // these should all parse to the same output
        listOf(";", ";;", ";&", ";;&").forEach { delimiter ->
            val src = arrayOf(
                "echo hi ${delimiter} echo bye", "echo hi${delimiter} echo bye",
                "echo hi${delimiter}echo bye"
            )
            val ref = listOf("echo", "hi", delimiter, "echo", "bye")
            src.forEach { ss ->
                listOf(true, false).forEach { whitespaceSplitFlag ->
                    val s = Shlex.split(ss) {
                        punctuationChars = true
                        whitespaceSplit = whitespaceSplitFlag
                    }
                    assertEquals(ref, s.toList())
                }
            }
        }
    }


    /**
     * Test handling of syntax splitting of >
     */
    @Test
    fun testSyntaxSplitRedirect() {
        // of course, the same applies to <, |
        // these should all parse to the same output
        listOf("<", "|").forEach { delimiter ->
            val src = arrayOf(
                "echo hi ${delimiter} out",
                "echo hi${delimiter} out",
                "echo hi${delimiter}out"
            )
            val ref = listOf("echo", "hi", delimiter, "out")
            src.forEach { ss ->
                val s = Shlex.split(ss) {
                    punctuationChars = true
                }
                assertEquals(ref, s.toList())
            }
        }
    }


    /**
     * Test handling of syntax splitting of ()
     */
    @Test
    fun testSyntaxSplitParen() {
        // these should all parse to the same output
        val src = listOf("( echo hi )", "(echo hi)")
        val ref = listOf("(", "echo", "hi", ")")
        src.forEach { ss ->
            listOf(true, false).forEach { whitespaceSplitFlag ->
                val s = Shlex.split(ss) {
                    punctuationChars = true
                    whitespaceSplit = whitespaceSplitFlag
                }
                assertEquals(ref, s.toList())
            }
        }
    }

    /**
     * Test handling of syntax splitting with custom chars
     */
    @Test
    fun testSyntaxSplitCustom() {
        val ss = "~/a&&b-c --color=auto||d *.py?"
        var ref = listOf("~/a", "&", "&", "b-c", "--color=auto", "||", "d", "*.py?")
        var s = Shlex.split(ss, ShlexConfig(punctuationChars = true, whitespaceSplit = false))
        assertEquals(ref, s.toList())

        ref = listOf("~/a&&b-c", "--color=auto", "||", "d", "*.py?")
        s = Shlex.split(ss, ShlexConfig(punctuationChars = true, whitespaceSplit = true))
        assertEquals(ref, s.toList())
    }

    /**
     * Test that tokens are split with types as expected.
     */
    @Test
    fun testTokenTypes() {
        val source = "a && b || c"
        val expected = listOf("a" to "a", "&&" to "c", "b" to "a", "||" to "c", "c" to "a")
        val s = ShLexer(source, ShlexConfig(punctuationChars = true))
        val observed = s.map { it to (if (it in s.punctuationChars) "c" else "a") }
        assertEquals(expected, observed.toList())
    }

    /**
     * Test that any punctuation chars are removed from wordchars
     */
    @Test
    fun testPunctuationInWordChars() {
        //TODO
        //val s = ShLexer("a_b__c", ShlexConfig(punctuationChars = '_'))
//        assertFalse('_' in s.wordchars)
//        assertEquals(s.toList(), listOf("a", "_", "b", "__", "c"))
    }

    /**
     * Test that with whitespace_split, behaviour is as expected
     */
    @Test
    fun testPunctuationWithWhitespaceSplit() {
        var s = ShLexer("a  && b  ||  c", ShlexConfig(punctuationChars = true, whitespaceSplit = false))
        // whitespace_split is False, so splitting will be based on
        // punctuation_chars
        assertEquals(s.toList(), listOf("a", "&&", "b", "||", "c"))
        // whitespace_split is True, so splitting will be based on
        // white space
        s = ShLexer("a  && b  ||  c", ShlexConfig(punctuationChars = true, whitespaceSplit = true))
        assertEquals(s.toList(), listOf("a", "&&", "b", "||", "c"))
    }

    /**
     * Test that punctuation_chars and posix behave correctly together.
     */
    @Test
    fun testPunctuationWithPosix() {
        var s = ShLexer("f >\"abc\"", ShlexConfig(punctuationChars = true, posix = true))
        assertEquals(s.toList(), listOf("f", ">", "abc"))
        s = ShLexer("f >\\\"abc\\\"", ShlexConfig(punctuationChars = true, posix = true))
        assertEquals(s.toList(), listOf("f", ">", "\"abc\""))
    }

    /**
     * Test that parsing of empty strings is correctly handled.
     */
    @Test
    fun testEmptyStringHandling() {
        var expected = listOf("", ")", "abc")
        listOf(false, true).forEach { punct ->
            val s = ShLexer("'')abc", ShlexConfig(punctuationChars = punct, posix = true))
            assertEquals(s.toList(), expected)
        }
        expected = listOf("''", ")", "abc")
        listOf(false, true).forEach { punct ->
            val s = ShLexer("'')abc", ShlexConfig(punctuationChars = punct, posix = false))
            assertEquals(s.toList(), expected)
        }
    }

    /**
     * Test punctuation_chars and whitespace_split handle unicode.
     */
    @Test
    fun testUnicodeHandling() {
        val ss = "\u2119\u01b4\u2602\u210c\u00f8\u1f24"
        // Should be parsed as one complete token (whitespace_split=True).
        var ref = listOf("\u2119\u01b4\u2602\u210c\u00f8\u1f24")
        var s = ShLexer(ss, ShlexConfig(punctuationChars = true, whitespaceSplit = true))
        assertEquals(s.toList(), ref)
        // Without whitespace_split, uses wordchars and splits on all.
        ref = listOf("\u2119", "\u01b4", "\u2602", "\u210c", "\u00f8", "\u1f24")
        s = ShLexer(ss, ShlexConfig(punctuationChars = true, whitespaceSplit = false))
        assertEquals(s.toList(), ref)
    }

    @Test
    fun testQuote() {
        //todo
    }

    @Test
    fun testJoin() {
        listOf(
            listOf("a ", "b") to "'a ' b",
            listOf("a", " b") to "a ' b'",
            listOf("a", " ", "b") to "a ' ' b",
            listOf("\"a", "b\"") to "'\"a' 'b\"'"
        ).forEach { (split_command, command) ->
            val joined = Shlex.join(*split_command.toTypedArray())
            assertEquals(joined, command)
        }
    }

    @Test
    fun testJoinRoundtrip() {
        val allData = data + posixData
        allData.forEach {
            val command = it.first()
            val splitted = it.subList(1, it.size - 1)
            val joined = Shlex.join(*splitted.toTypedArray())
            val resplit = Shlex.split(joined)
            assertEquals(splitted, resplit.toList())
        }
    }

    @Test
    fun testPunctuationCharsReadOnly() {
        //todo
        val punctuation_chars = "/|$%^"
    }

    private fun splitTest(data: List<List<String>>, commentFlag: Boolean) {
        data.forEach {
            val splitted = Shlex.split(it[0], ShlexConfig(comments = commentFlag))
            assertEquals(it.subList(1, it.size - 1), splitted.toList())
        }
    }

    private fun oldSplit(s: String): List<String> {
        val ret = mutableListOf<String>()
        var lex = ShLexer(s)
        var tok = lex.nextToken()
        while (tok != null) {
            ret.add(tok)
            tok = lex.nextToken()
        }
        return ret
    }
}