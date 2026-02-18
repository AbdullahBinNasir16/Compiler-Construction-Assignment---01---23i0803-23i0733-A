
public enum TokenType {

    // ── Literals ──────────────────────────────────────────────────
    INTEGER_LITERAL,        // [+-]?[0-9]+
    FLOAT_LITERAL,          // [+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?
    STRING_LITERAL,         // "( [^"\\\n] | \\["\\ntr] )*"
    CHAR_LITERAL,           // '( [^'\\\n] | \\['\\ntr] )'
    BOOLEAN_LITERAL,        // true | false

    // ── Identifiers & Keywords ────────────────────────────────────
    IDENTIFIER,             // [A-Z][a-z0-9_]{0,30}
    KEYWORD,                // start finish loop condition declare output
                            // input function return break continue else

    // ── Operators ─────────────────────────────────────────────────
    ARITHMETIC_OP,          // + - * / % **
    RELATIONAL_OP,          // == != < > <= >=
    LOGICAL_OP,             // && || !
    ASSIGNMENT_OP,          // = += -= *= /=
    INC_DEC_OP,             // ++ --

    // ── Punctuators ───────────────────────────────────────────────
    PUNCTUATOR,             // ( ) { } [ ] , ; :

    // ── Comments (consumed but not emitted as real tokens) ────────
    SINGLE_LINE_COMMENT,    // ##[^\n]*
    MULTI_LINE_COMMENT,     // #\*...\*#

    // ── Error ─────────────────────────────────────────────────────
    ERROR                   // any unrecognised / malformed lexeme
}
