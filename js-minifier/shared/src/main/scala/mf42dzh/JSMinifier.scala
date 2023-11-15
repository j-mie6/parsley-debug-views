/*
 * Copyright 2023 Fawwaz Abdullah
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package mf42dzh

// scalastyle:off
import scala.language.implicitConversions

import parsley.*
import Parsley.*
import parsley.character.*
import parsley.combinator.*
import parsley.errors.combinator.*
import parsley.expr.*
import parsley.token.*
import parsley.token.descriptions.*
import parsley.token.predicate.*
// scalastyle:on

/** Performs minification (and light obfuscation) of JS.
  */
object JSMinifier extends ((String, Boolean) => String) {
  // Selecting the lexer.
  implicit private val lex: CoerceToParser = JSLexer

  // The front call for this.
  override def apply(v1: String, strict: Boolean): String = ???

  // The full minifier.
  private lazy val minifier: Parsley[String => String] = ???

  // Lexer and string->parser coerce.
  private trait CoerceToParser {
    def _tokenLift(st: String): Parsley[Unit]
    def _fully[A](p: => Parsley[A]): Parsley[A]
  }

  implicit private def tokenLift[T <: CoerceToParser](str: String)(implicit conv: CoerceToParser): Parsley[Unit] =
    conv._tokenLift(str)

  private def fully[A](p: => Parsley[A])(implicit conv: CoerceToParser): Parsley[A] =
    conv._fully(p)

  private object JSLexer extends CoerceToParser {
    val defaultLang: LexicalDesc = LexicalDesc.plain.copy(
      spaceDesc = SpaceDesc.plain.copy(
        space = Basic(_.isWhitespace),
        commentLine = "//",
        commentStart = "/*",
        commentEnd = "*/",
        commentLineAllowsEOF = true
      ),
      nameDesc = NameDesc.plain.copy(
        identifierStart = Basic(c => "$_".contains(c) || c.isLetter),
        identifierLetter = Basic(c => "$_".contains(c) || c.isLetterOrDigit)
      ),
      symbolDesc = SymbolDesc.plain.copy(
        hardKeywords = Set( // There are some context-sensitive keywords in JS. Because why not.
          "break",
          "case",
          "catch",
          "class",
          "const",
          "continue",
          "debugger",
          "default",
          "delete",
          "do",
          "else",
          "export",
          "extends",
          "false",
          "finally",
          "for",
          "function",
          "if",
          "import",
          "in",
          "instanceof",
          "new",
          "null",
          "undefined", // My extension.
          "return",
          "super",
          "switch",
          "this",
          "throw",
          "true",
          "try",
          "typeof",
          "var",
          "void",
          "while",
          "with",
          "enum", // Why are these just Java keywords?
          "abstract",
          "boolean",
          "byte",
          "char",
          "double",
          "final",
          "float",
          "goto", // Except you. Go away goto.
          "int",
          "long",
          "native",
          "short",
          "synchronized",
          "throws",
          "transient",
          "volatile",
          "let", // A bunch of strict mode identifiers.
          "static",
          "yield",
          "await",
          "async",
          "static",
          "yield",
          "await",
          "implements",
          "interface",
          "package",
          "private",
          "protected",
          "public",
          "arguments",
          "as",
          "eval",
          "from",
          "of",
          "set"
        )
      )
    )

    // The actual processing happens here.
    val lexer = new Lexer(defaultLang)

    def token[A](p: => Parsley[A]): Parsley[A] =
      lexer.lexeme(attempt(p))

    override def _tokenLift(str: String): Parsley[Unit] =
      token(lexer.lexeme.symbol(str))

    override def _fully[A](p: => Parsley[A]): Parsley[A] =
      lexer.space.whiteSpace *> lexer.lexeme(
        p <* (eof <|> unexpected("More input after fully."))
      )
  }
}
