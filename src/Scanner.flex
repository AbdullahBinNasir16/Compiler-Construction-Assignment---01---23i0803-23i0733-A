/* ================================================================
   Scanner.flex
   JFlex Lexical Specification for CustomLang (.lang)
   CS4031 - Compiler Construction - Assignment 01
   Part 2: JFlex Implementation

   Mirrors the exact priority order of ManualScanner.java:
     1.  Multi-line comments
     2.  Single-line comments
     3.  Multi-character operators  (longest match)
     4.  Keywords
     5.  Boolean literals
     6.  Identifiers
     7.  Floating-point literals
     8.  Integer literals
     9.  String / Character literals
     10. Single-character operators
     11. Punctuators
     12. Whitespace  (skipped)
   ================================================================ */


/* ----------------------------------------------------------------
   SECTION 1 — USER CODE
   Copied verbatim before the generated class declaration.
   ---------------------------------------------------------------- */
import java.io.*;
import java.util.*;


%%


/* ----------------------------------------------------------------
   SECTION 2 — OPTIONS & CLASS DECLARATIONS
   ---------------------------------------------------------------- */
%class      Yylex
%unicode
%line                   /* enables yyline (0-based) */
%column                 /* enables yycolumn (0-based) */
%type       Token       /* yylex() return type */

%{
    /* ── Instance state ─────────────────────────────────────── */
    private final SymbolTable            symTable    = new SymbolTable();
    private final ErrorHandler           errHandler  = new ErrorHandler();
    private final Map<TokenType,Integer> tokenCounts = new LinkedHashMap<>();
    private       int                    commentCount = 0;

    /* Initialise all counts to 0 on construction */
    {
        for (TokenType t : TokenType.values()) tokenCounts.put(t, 0);
    }

    /* ── Helpers ─────────────────────────────────────────────── */

    /** Build a Token, update statistics, register identifiers. */
    private Token tok(TokenType type) {
        String lexeme = yytext();
        int    ln     = yyline  + 1;   /* convert 0-based → 1-based */
        int    col    = yycolumn + 1;

        tokenCounts.merge(type, 1, Integer::sum);

        if (type == TokenType.SINGLE_LINE_COMMENT ||
            type == TokenType.MULTI_LINE_COMMENT) {
            commentCount++;
        }

        if (type == TokenType.IDENTIFIER) {
            symTable.insert(lexeme, ln, col);
        }

        return new Token(type, lexeme, ln, col);
    }

    /** Record a lexical error, then return an ERROR token.
     *  Error recovery: scanning continues after this token. */
    private Token lexError(ErrorHandler.ErrorType et, String reason) {
        String lexeme = yytext();
        int    ln     = yyline  + 1;
        int    col    = yycolumn + 1;
        errHandler.report(et, ln, col, lexeme, reason);
        tokenCounts.merge(TokenType.ERROR, 1, Integer::sum);
        return new Token(TokenType.ERROR, lexeme, ln, col);
    }

    /* ── Public accessors (used by JFlexMain) ─────────────────── */
    public SymbolTable  getSymbolTable()  { return symTable;    }
    public ErrorHandler getErrorHandler() { return errHandler;  }
    public int          getCommentCount() { return commentCount; }

    public void printStatistics(int totalTokens) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║         JFLEX SCANNER STATISTICS             ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.printf( "║  Lines processed      : %-20d║%n", yyline + 1);
        System.out.printf( "║  Total tokens         : %-20d║%n", totalTokens);
        System.out.printf( "║  Comments removed     : %-20d║%n", commentCount);
        System.out.printf( "║  Lexical errors found : %-20d║%n", errHandler.errorCount());
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║  Token counts by type:                       ║");
        tokenCounts.forEach((type, count) -> {
            if (count > 0)
                System.out.printf("║    %-28s : %-5d     ║%n", type, count);
        });
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();
    }
%}


/* ----------------------------------------------------------------
   SECTION 3 — MACRO DEFINITIONS
   ---------------------------------------------------------------- */

/* Basic character classes */
DIGIT        = [0-9]
UPPER        = [A-Z]
LOWER        = [a-z]
LETTER       = [A-Za-z]
UNDERSCORE   = [_]
ID_TAIL_CHAR = ({LOWER}|{DIGIT}|{UNDERSCORE})

/* Number parts */
DIGITS       = {DIGIT}+
SIGN         = [+\-]
EXP_PART     = [eE]{SIGN}?{DIGITS}

/* Escape sequences */
STR_ESC      = \\[\"\\ntr]
CHAR_ESC     = \\[\'\\ntr]

/* Valid body characters (excluding delimiters/backslash/newline) */
STR_BODY     = [^\"\\\n]
CHAR_BODY    = [^\'\\\n]

/* Whitespace */
WS           = [ \t\r\n]+

/* Keywords macro (for reference — rules use literals for priority) */
KEYWORD = "start"|"finish"|"loop"|"condition"|"declare"|"output"|"input"|"function"|"return"|"break"|"continue"|"else"

/* Boolean macro */
BOOL = "true"|"false"


/* ----------------------------------------------------------------
   SECTION 4 — LEXICAL RULES
   Order = priority. JFlex picks the LONGEST match; ties broken
   by whichever rule appears first in this file.
   ---------------------------------------------------------------- */
%%


/* ══ Priority 1: Multi-line comment  #* ... *#  ══════════════════
   The body allows anything except a lone '*' before '#'.
   [^*]      = any char that is not '*'
   \*+[^*#]  = one-or-more stars NOT followed by '#'
   ════════════════════════════════════════════════════════════════ */
"#*"([^*]|\*+[^*#])*\*+"#"
    { return tok(TokenType.MULTI_LINE_COMMENT); }

/* Unclosed multi-line comment — runs to EOF */
"#*"[^]*
    {
        return lexError(
            ErrorHandler.ErrorType.UNCLOSED_COMMENT,
            "Multi-line comment was never closed"
        );
    }

/* ══ Priority 2: Single-line comment  ##...  ══════════════════════ */
"##"[^\n]*
    { return tok(TokenType.SINGLE_LINE_COMMENT); }


/* ══ Priority 3: Multi-character operators (longest match) ════════
   Listed longest/most-specific first within each group.           */

"**"   { return tok(TokenType.ARITHMETIC_OP); }   /* exponentiation */

"=="   { return tok(TokenType.RELATIONAL_OP); }
"!="   { return tok(TokenType.RELATIONAL_OP); }
"<="   { return tok(TokenType.RELATIONAL_OP); }
">="   { return tok(TokenType.RELATIONAL_OP); }

"&&"   { return tok(TokenType.LOGICAL_OP); }
"||"   { return tok(TokenType.LOGICAL_OP); }

"++"   { return tok(TokenType.INC_DEC_OP); }
"--"   { return tok(TokenType.INC_DEC_OP); }

"+="   { return tok(TokenType.ASSIGNMENT_OP); }
"-="   { return tok(TokenType.ASSIGNMENT_OP); }
"*="   { return tok(TokenType.ASSIGNMENT_OP); }
"/="   { return tok(TokenType.ASSIGNMENT_OP); }


/* ══ Priority 4: Keywords ══════════════════════════════════════════
   Must appear before the identifier rule.                         */
"start"     { return tok(TokenType.KEYWORD); }
"finish"    { return tok(TokenType.KEYWORD); }
"loop"      { return tok(TokenType.KEYWORD); }
"condition" { return tok(TokenType.KEYWORD); }
"declare"   { return tok(TokenType.KEYWORD); }
"output"    { return tok(TokenType.KEYWORD); }
"input"     { return tok(TokenType.KEYWORD); }
"function"  { return tok(TokenType.KEYWORD); }
"return"    { return tok(TokenType.KEYWORD); }
"break"     { return tok(TokenType.KEYWORD); }
"continue"  { return tok(TokenType.KEYWORD); }
"else"      { return tok(TokenType.KEYWORD); }


/* ══ Priority 5: Boolean literals ═════════════════════════════════ */
"true"  { return tok(TokenType.BOOLEAN_LITERAL); }
"false" { return tok(TokenType.BOOLEAN_LITERAL); }


/* ══ Priority 6: Identifiers  [A-Z][a-z0-9_]{0,30} ════════════════ */
{UPPER}{ID_TAIL_CHAR}{0,30}
    { return tok(TokenType.IDENTIFIER); }

/* Identifier starting with lowercase (not a keyword or boolean) */
{LOWER}({LETTER}|{DIGIT}|{UNDERSCORE})*
    {
        return lexError(
            ErrorHandler.ErrorType.INVALID_IDENTIFIER,
            "Identifier '" + yytext() + "' must start with an uppercase letter"
        );
    }


/* ══ Priority 7: Floating-point literals ══════════════════════════
   [+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?
   Float MUST be tried before integer (longest match).             */

{SIGN}?{DIGITS}\.{DIGIT}{1,6}{EXP_PART}?
    { return tok(TokenType.FLOAT_LITERAL); }

/* More than 6 decimal places — malformed */
{SIGN}?{DIGITS}\.{DIGIT}{7,}{EXP_PART}?
    {
        return lexError(
            ErrorHandler.ErrorType.MALFORMED_LITERAL,
            "Floating-point literal has more than 6 decimal places"
        );
    }

/* Digit(s) then '.' but no digit after — e.g. "3." */
{SIGN}?{DIGITS}\.
    {
        return lexError(
            ErrorHandler.ErrorType.MALFORMED_LITERAL,
            "Floating-point literal '" + yytext() + "' has no digits after decimal point"
        );
    }

/* Leading '.' with digits — e.g. ".14" */
"\."{DIGITS}
    {
        return lexError(
            ErrorHandler.ErrorType.MALFORMED_LITERAL,
            "Floating-point literal '" + yytext() + "' must have digits before the decimal point"
        );
    }


/* ══ Priority 8: Integer literals ═════════════════════════════════ */
{SIGN}?{DIGITS}
    { return tok(TokenType.INTEGER_LITERAL); }


/* ══ Priority 9a: String literals ═════════════════════════════════ */
\"({STR_BODY}|{STR_ESC})*\"
    { return tok(TokenType.STRING_LITERAL); }

/* Unterminated string — newline before closing quote */
\"({STR_BODY}|{STR_ESC})*\n
    {
        return lexError(
            ErrorHandler.ErrorType.UNTERMINATED_STRING,
            "String literal not terminated before end of line"
        );
    }

/* Unterminated string — EOF before closing quote */
\"({STR_BODY}|{STR_ESC})*<<EOF>>
    {
        return lexError(
            ErrorHandler.ErrorType.UNTERMINATED_STRING,
            "String literal not terminated before end of file"
        );
    }


/* ══ Priority 9b: Character literals ══════════════════════════════ */
\'({CHAR_BODY}|{CHAR_ESC})\'
    { return tok(TokenType.CHAR_LITERAL); }

/* Unterminated char literal */
\'({CHAR_BODY}|{CHAR_ESC})*\n
    {
        return lexError(
            ErrorHandler.ErrorType.UNTERMINATED_CHAR,
            "Character literal not terminated before end of line"
        );
    }

/* Empty char literal '' */
"\'\'"
    {
        return lexError(
            ErrorHandler.ErrorType.MALFORMED_LITERAL,
            "Character literal cannot be empty"
        );
    }


/* ══ Priority 10: Single-character operators ═══════════════════════ */
"+"  { return tok(TokenType.ARITHMETIC_OP); }
"-"  { return tok(TokenType.ARITHMETIC_OP); }
"*"  { return tok(TokenType.ARITHMETIC_OP); }
"/"  { return tok(TokenType.ARITHMETIC_OP); }
"%"  { return tok(TokenType.ARITHMETIC_OP); }
"<"  { return tok(TokenType.RELATIONAL_OP); }
">"  { return tok(TokenType.RELATIONAL_OP); }
"!"  { return tok(TokenType.LOGICAL_OP);    }
"="  { return tok(TokenType.ASSIGNMENT_OP); }


/* ══ Priority 11: Punctuators ══════════════════════════════════════ */
"("  { return tok(TokenType.PUNCTUATOR); }
")"  { return tok(TokenType.PUNCTUATOR); }
"{"  { return tok(TokenType.PUNCTUATOR); }
"}"  { return tok(TokenType.PUNCTUATOR); }
"["  { return tok(TokenType.PUNCTUATOR); }
"]"  { return tok(TokenType.PUNCTUATOR); }
","  { return tok(TokenType.PUNCTUATOR); }
";"  { return tok(TokenType.PUNCTUATOR); }
":"  { return tok(TokenType.PUNCTUATOR); }


/* ══ Priority 12: Whitespace — skip, tracking handled by %line %column */
{WS} { /* intentionally skip — line/col auto-tracked by JFlex */ }


/* ══ Catch-all: any character not matched above ════════════════════ */
.    {
        return lexError(
            ErrorHandler.ErrorType.INVALID_CHARACTER,
            "Character '" + yytext() + "' is not part of the language"
        );
     }
