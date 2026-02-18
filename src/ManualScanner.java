import java.util.*;
import java.io.*;
import java.nio.file.*;

/*
   Features:
     A. Token Recognition  - All token types, DFA-based, longest match
     B. Pre-processing     - Whitespace removal, string preservation, line/col tracking
     C. Token Output       - <TYPE, "lexeme", Line: N, Col: N>
     D. Statistics         - Total tokens, per-type counts, lines, comments removed
     E. Symbol Table       - Identifier name, type, first occurrence, frequency
 */
public class ManualScanner {

    // fields

    private final String        source;         // full source code string
    private       int           pos;            // current index into source
    private       int           line;           // current line number
    private       int           col;            // current column number 

    private final List<Token>            tokens       = new ArrayList<>();
    private final SymbolTable            symbolTable  = new SymbolTable();
    private final ErrorHandler           errorHandler = new ErrorHandler();
    private final Map<TokenType,Integer> tokenCounts  = new LinkedHashMap<>();

    // stats counters
    private int commentsRemoved = 0;
    private int linesProcessed  = 0;

    //all 12 required keywords
    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
        "start", "finish", "loop", "condition", "declare",
        "output", "input", "function", "return", "break",
        "continue", "else"
    ));

    // constructor

    public ManualScanner(String source) {
        this.source = source;
        this.pos    = 0;
        this.line   = 1;
        this.col    = 1;
        // Initialize token counts to 0 for all types
        for (TokenType t : TokenType.values()) {
            tokenCounts.put(t, 0);
        }
    }

    // public entry point

    //tokenize entrire string and return list of tokens
    public List<Token> tokenize() {
        while (pos < source.length()) {

            // skip whitespace between tokens
            skipWhitespace();
            if (pos >= source.length()) break;

            // snapshot position before consuming
            int tokLine = line;
            int tokCol  = col;

            Token t = nextToken(tokLine, tokCol);
            if (t == null) continue;

            switch (t.getType()) {
                // comments count but do not add to token list
                case SINGLE_LINE_COMMENT:
                case MULTI_LINE_COMMENT:
                    commentsRemoved++;
                    tokenCounts.merge(t.getType(), 1, Integer::sum);
                    break;

                // errors add to list for reporting but dont count as real tokens
                case ERROR:
                    tokens.add(t);
                    tokenCounts.merge(TokenType.ERROR, 1, Integer::sum);
                    break;

                // all real tokens are added to the list and counted
                default:
                    tokens.add(t);
                    tokenCounts.merge(t.getType(), 1, Integer::sum);
                    // register every identifier in symbil tbl
                    if (t.getType() == TokenType.IDENTIFIER) {
                        symbolTable.insert(t.getLexeme(), t.getLine(), t.getColumn());
                    }
                    break;
            }
        }

        linesProcessed = line;
        return Collections.unmodifiableList(tokens);
    }

    //  Core Dispatcher
    //  Follows the priority order from Section 3.12 of the spec:
    //  1  Multi-line comment
    //  2  Single-line comment
    //  3  Multi-character operators
    //  4  Keywords
    //  5  Boolean literals
    //  6  Identifiers
    //  7  Floating-point literals
    //  8  Integer literals
    //  9  String literals
    //  10 Single character operators
    //  11 Punctuators
    //  12 Whitespace 

    private Token nextToken(int sl, int sc) {

        char c0 = peek(0);
        char c1 = peek(1);

        // Multi-line comment 
        if (c0 == '#' && c1 == '*') {
            return scanMultiLineComment(sl, sc);
        }

        // Single-line comment  
        if (c0 == '#' && c1 == '#') {
            return scanSingleLineComment(sl, sc);
        }

        // Multi character operators 
        Token multiOp = tryMultiCharOperator(sl, sc);
        if (multiOp != null) return multiOp;

        // Words (keyword / boolean / identifier) 
        if (Character.isLetter(c0) || c0 == '_') {
            return scanWord(sl, sc);
        }

        //Numbers (float checked before integer)
        boolean signedNumber = (c0 == '+' || c0 == '-') && isDigit(c1) && !prevTokenIsValue();

        if (isDigit(c0) || signedNumber) {
            return scanNumber(sl, sc);
        }

        // String literal 
        if (c0 == '"') {
            return scanStringLiteral(sl, sc);
        }

        //Character literal 
        if (c0 == '\'') {
            return scanCharLiteral(sl, sc);
        }

        //  Single-character operators 
        Token singleOp = trySingleCharOperator(sl, sc);
        if (singleOp != null) return singleOp;

        // Punctuators
        Token punct = tryPunctuator(sl, sc);
        if (punct != null) return punct;

        //Unknown character 
        char bad = consume();
        errorHandler.report(
            ErrorHandler.ErrorType.INVALID_CHARACTER,
            sl, sc, String.valueOf(bad),
            "Character '" + bad + "' is not part of the language"
        );
        return new Token(TokenType.ERROR, String.valueOf(bad), sl, sc);
    }

    //  PRE-PROCESSING
    // mremoves all whitespace between tokens

    private void skipWhitespace() {
        while (pos < source.length() && isWhitespace(peek(0))) {
            consume();
        }
    }

    // comment scanners

    
    //   Single-line comment: ##[^\n]*
    //   Consume everything up to (but not including) the newline.
     
    private Token scanSingleLineComment(int sl, int sc) {
        StringBuilder sb = new StringBuilder();
        sb.append(consume()); // first  #
        sb.append(consume()); // second #
        while (pos < source.length() && peek(0) != '\n') {
            sb.append(consume());
        }
        return new Token(TokenType.SINGLE_LINE_COMMENT, sb.toString(), sl, sc);
    }

    
    //  Multi-line comment: #\* ... \*#
    //  The closing sequence is one or more '*' followed by '#'.
     
    private Token scanMultiLineComment(int sl, int sc) {
        StringBuilder sb = new StringBuilder();
        sb.append(consume()); // #
        sb.append(consume()); // *

        while (pos < source.length()) {
            if (peek(0) == '*') {
                sb.append(consume()); // consume '*'
                // Absorb any extra stars (e.g. **#  also closes)
                while (pos < source.length() && peek(0) == '*') {
                    sb.append(consume());
                }
                if (pos < source.length() && peek(0) == '#') {
                    sb.append(consume()); // closing #
                    return new Token(TokenType.MULTI_LINE_COMMENT, sb.toString(), sl, sc);
                }
                // Not closed yet 
            } else {
                sb.append(consume());
            }
        }

        // Reached EOF without closing the comment
        errorHandler.report(
            ErrorHandler.ErrorType.UNCLOSED_COMMENT,
            sl, sc, sb.toString(),
            "Multi-line comment was never closed"
        );
        return new Token(TokenType.ERROR, sb.toString(), sl, sc);
    }

    // Word scanner (keywords, boolean literals, identifiers)
    //  DFA states:
    //    q0 (start) --[letter|_]--> q1 (reading word chars)
    //    q1          --[letter|digit|_]--> q1
    //    q1          --[other]--> ACCEPT (classify)

    private Token scanWord(int sl, int sc) {
        StringBuilder sb = new StringBuilder();

        // Consume all identifier valid characters
        while (pos < source.length() && isIdentChar(peek(0))) {
            sb.append(consume());
        }

        String word = sb.toString();

        // check keywords first
        if (KEYWORDS.contains(word)) {
            return new Token(TokenType.KEYWORD, word, sl, sc);
        }

        // boolean literals
        if (word.equals("true") || word.equals("false")) {
            return new Token(TokenType.BOOLEAN_LITERAL, word, sl, sc);
        }

        // identifiers — must start with uppercase A-Z
        if (Character.isUpperCase(word.charAt(0))) {
            // Validate max length (31 characters total)
            if (word.length() > 31) {
                errorHandler.report(
                    ErrorHandler.ErrorType.INVALID_IDENTIFIER,
                    sl, sc, word,
                    "Identifier exceeds maximum length of 31 characters (got " + word.length() + ")"
                );
            }
            return new Token(TokenType.IDENTIFIER, word, sl, sc);
        }

        // Starts with lowercase but not a keyword/boolean — invalid identifier
        errorHandler.report(
            ErrorHandler.ErrorType.INVALID_IDENTIFIER,
            sl, sc, word,
            "Identifier '" + word + "' must start with an uppercase letter"
        );
        return new Token(TokenType.ERROR, word, sl, sc);
    }

    // number scanner (float and integer)
    //
    //  Float DFA:
    //    q0 --[+|-]--> q1 --[digit]--> q2 --[.]--> q3 --[digit(1-6)]--> q4 (ACCEPT)
    //    q4 --[e|E]--> q5 --[+|-]--> q6 --[digit]--> q7 (ACCEPT)
    //    q4 --[e|E]--> q5 --[digit]--> q7 (ACCEPT)
    //
    //  Integer DFA:
    //    q0 --[+|-]--> q1 --[digit]--> q2 (ACCEPT)
    //    q0 --[digit]--> q2 (ACCEPT)
    //    q2 --[digit]--> q2
    //
    //  Float is tried FIRST (longest match principle)

    private Token scanNumber(int sl, int sc) {
        StringBuilder sb = new StringBuilder();

        // Optional leading sign
        if (peek(0) == '+' || peek(0) == '-') {
            sb.append(consume());
        }

        // One or more digits before potential decimal point
        while (pos < source.length() && isDigit(peek(0))) {
            sb.append(consume());
        }

        // Try floating-point branch 
        // Must have '.' followed by at least one digit
        if (peek(0) == '.' && isDigit(peek(1))) {
            sb.append(consume()); // consume '.'

            int decimalCount = 0;
            while (pos < source.length() && isDigit(peek(0))) {
                sb.append(consume());
                decimalCount++;
            }

            // Validate: at most 6 decimal places
            if (decimalCount > 6) {
                errorHandler.report(
                    ErrorHandler.ErrorType.MALFORMED_LITERAL,
                    sl, sc, sb.toString(),
                    "Floating-point literal has " + decimalCount + " decimal places (max 6)"
                );
            }

            // Optional exponent part [eE][+-]?[0-9]+
            if (peek(0) == 'e' || peek(0) == 'E') {
                sb.append(consume()); // e or E
                if (peek(0) == '+' || peek(0) == '-') {
                    sb.append(consume()); // optional sign
                }
                if (!isDigit(peek(0))) {
                    errorHandler.report(
                        ErrorHandler.ErrorType.MALFORMED_LITERAL,
                        sl, sc, sb.toString(),
                        "Exponent in floating-point literal must be followed by digits"
                    );
                }
                while (pos < source.length() && isDigit(peek(0))) {
                    sb.append(consume());
                }
            }

            return new Token(TokenType.FLOAT_LITERAL, sb.toString(), sl, sc);
        }

        // ---- Malformed float: "3." with no digit after ----
        if (peek(0) == '.') {
            sb.append(consume()); // consume the dot for error reporting
            errorHandler.report(
                ErrorHandler.ErrorType.MALFORMED_LITERAL,
                sl, sc, sb.toString(),
                "Floating-point literal '" + sb + "' has no digits after the decimal point"
            );
            return new Token(TokenType.ERROR, sb.toString(), sl, sc);
        }

        // ---- Integer literal ----
        return new Token(TokenType.INTEGER_LITERAL, sb.toString(), sl, sc);
    }

    // ================================================================
    //  SECTION 9 — STRING LITERAL SCANNER
    //  Regex: "([ ^"\\\n]|\\["\\ntr])*"
    //  B. Pre-processing: whitespace inside quotes is preserved raw.
    //
    //  DFA states:
    //    q0 --["]--> q1 (inside string)
    //    q1 --[\\]--> q2 (escape start)
    //    q2 --[",\,n,t,r]--> q1 (valid escape)
    //    q1 --["]--> ACCEPT
    //    q1 --[\n|EOF]--> ERROR (unterminated)
    // ================================================================

    private Token scanStringLiteral(int sl, int sc) {
        StringBuilder sb = new StringBuilder();
        sb.append(consume()); // opening "

        while (pos < source.length()) {
            char c = peek(0);

            if (c == '"') {
                sb.append(consume()); // closing "
                return new Token(TokenType.STRING_LITERAL, sb.toString(), sl, sc);
            }

            if (c == '\n') {
                // Newline before closing quote = unterminated string
                break;
            }

            if (c == '\\') {
                sb.append(consume()); // consume backslash
                if (pos < source.length()) {
                    char esc = peek(0);
                    if (isValidStringEscape(esc)) {
                        sb.append(consume()); // valid escape char
                    } else {
                        // Invalid escape — report but keep going
                        errorHandler.report(
                            ErrorHandler.ErrorType.MALFORMED_LITERAL,
                            line, col, "\\" + esc,
                            "Invalid escape sequence '\\" + esc + "' in string literal"
                        );
                        sb.append(consume()); // consume the bad char anyway
                    }
                }
            } else {
                sb.append(consume()); // regular character (whitespace preserved here)
            }
        }

        // Fell out of loop = unterminated string
        errorHandler.report(
            ErrorHandler.ErrorType.UNTERMINATED_STRING,
            sl, sc, sb.toString(),
            "String literal is not terminated before end of line"
        );
        return new Token(TokenType.ERROR, sb.toString(), sl, sc);
    }

    // ================================================================
    //  SECTION 10 — CHARACTER LITERAL SCANNER
    //  Regex: '([ ^'\\\n]|\\['\\ntr])'
    //
    //  DFA states:
    //    q0 --[']--> q1
    //    q1 --[\\]--> q2 --[',\,n,t,r]--> q3
    //    q1 --[regular char]--> q3
    //    q3 --[']--> ACCEPT
    // ================================================================

    private Token scanCharLiteral(int sl, int sc) {
        StringBuilder sb = new StringBuilder();
        sb.append(consume()); // opening '

        if (pos >= source.length() || peek(0) == '\n') {
            errorHandler.report(
                ErrorHandler.ErrorType.MALFORMED_LITERAL,
                sl, sc, sb.toString(),
                "Empty or unterminated character literal"
            );
            return new Token(TokenType.ERROR, sb.toString(), sl, sc);
        }

        if (peek(0) == '\\') {
            // Escape sequence
            sb.append(consume()); // backslash
            if (pos < source.length()) {
                char esc = peek(0);
                if (isValidCharEscape(esc)) {
                    sb.append(consume());
                } else {
                    errorHandler.report(
                        ErrorHandler.ErrorType.MALFORMED_LITERAL,
                        line, col, "\\" + esc,
                        "Invalid escape sequence '\\" + esc + "' in character literal"
                    );
                    sb.append(consume());
                }
            }
        } else if (peek(0) != '\'') {
            sb.append(consume()); // single regular character
        } else {
            // Empty char literal: ''
            errorHandler.report(
                ErrorHandler.ErrorType.MALFORMED_LITERAL,
                sl, sc, "''",
                "Character literal cannot be empty"
            );
        }

        // Expect closing quote
        if (pos < source.length() && peek(0) == '\'') {
            sb.append(consume());
            return new Token(TokenType.CHAR_LITERAL, sb.toString(), sl, sc);
        }

        errorHandler.report(
            ErrorHandler.ErrorType.UNTERMINATED_CHAR,
            sl, sc, sb.toString(),
            "Character literal is not closed with a single quote"
        );
        return new Token(TokenType.ERROR, sb.toString(), sl, sc);
    }

    // ================================================================
    //  SECTION 11 — MULTI-CHARACTER OPERATOR SCANNER
    //  Longest match: always check 2-char ops before falling through
    //  to single-char ops. Order within here also matters for ** vs *=.
    // ================================================================

    private Token tryMultiCharOperator(int sl, int sc) {
        char c0 = peek(0);
        char c1 = peek(1);

        // Exponentiation **  (must be before *= check)
        if (c0 == '*' && c1 == '*') { consume(); consume(); return new Token(TokenType.ARITHMETIC_OP,  "**", sl, sc); }

        // Relational operators
        if (c0 == '=' && c1 == '=') { consume(); consume(); return new Token(TokenType.RELATIONAL_OP,  "==", sl, sc); }
        if (c0 == '!' && c1 == '=') { consume(); consume(); return new Token(TokenType.RELATIONAL_OP,  "!=", sl, sc); }
        if (c0 == '<' && c1 == '=') { consume(); consume(); return new Token(TokenType.RELATIONAL_OP,  "<=", sl, sc); }
        if (c0 == '>' && c1 == '=') { consume(); consume(); return new Token(TokenType.RELATIONAL_OP,  ">=", sl, sc); }

        // Logical operators
        if (c0 == '&' && c1 == '&') { consume(); consume(); return new Token(TokenType.LOGICAL_OP,     "&&", sl, sc); }
        if (c0 == '|' && c1 == '|') { consume(); consume(); return new Token(TokenType.LOGICAL_OP,     "||", sl, sc); }

        // Increment / Decrement  (before += and -= checks)
        if (c0 == '+' && c1 == '+') { consume(); consume(); return new Token(TokenType.INC_DEC_OP,     "++", sl, sc); }
        if (c0 == '-' && c1 == '-') { consume(); consume(); return new Token(TokenType.INC_DEC_OP,     "--", sl, sc); }

        // Compound assignment
        if (c0 == '+' && c1 == '=') { consume(); consume(); return new Token(TokenType.ASSIGNMENT_OP,  "+=", sl, sc); }
        if (c0 == '-' && c1 == '=') { consume(); consume(); return new Token(TokenType.ASSIGNMENT_OP,  "-=", sl, sc); }
        if (c0 == '*' && c1 == '=') { consume(); consume(); return new Token(TokenType.ASSIGNMENT_OP,  "*=", sl, sc); }
        if (c0 == '/' && c1 == '=') { consume(); consume(); return new Token(TokenType.ASSIGNMENT_OP,  "/=", sl, sc); }

        return null; // no multi-char operator matched
    }

    // ================================================================
    //  SECTION 12 — SINGLE-CHARACTER OPERATOR SCANNER
    // ================================================================

    private Token trySingleCharOperator(int sl, int sc) {
        char c = peek(0);
        switch (c) {
            case '+': case '-': case '*': case '/': case '%':
                consume();
                return new Token(TokenType.ARITHMETIC_OP, String.valueOf(c), sl, sc);
            case '<': case '>':
                consume();
                return new Token(TokenType.RELATIONAL_OP, String.valueOf(c), sl, sc);
            case '!':
                consume();
                return new Token(TokenType.LOGICAL_OP, "!", sl, sc);
            case '=':
                consume();
                return new Token(TokenType.ASSIGNMENT_OP, "=", sl, sc);
            default:
                return null;
        }
    }

    // ================================================================
    //  SECTION 13 — PUNCTUATOR SCANNER
    //  ( ) { } [ ] , ; :
    // ================================================================

    private Token tryPunctuator(int sl, int sc) {
        char c = peek(0);
        if ("(){}[],;:".indexOf(c) >= 0) {
            consume();
            return new Token(TokenType.PUNCTUATOR, String.valueOf(c), sl, sc);
        }
        return null;
    }

    // ================================================================
    //  SECTION 14 — LOW-LEVEL CHARACTER HELPERS
    // ================================================================

    /** Return the character at pos+offset, or '\0' if out of bounds. */
    private char peek(int offset) {
        int i = pos + offset;
        return (i < source.length()) ? source.charAt(i) : '\0';
    }

    /**
     * Consume the current character:
     *  - Advance pos
     *  - Update line/col tracking for B. Pre-processing requirement
     *  - Return the consumed character
     */
    private char consume() {
        char c = source.charAt(pos);
        pos++;
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    /** Is this character whitespace? */
    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    /** Is this character a decimal digit? */
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Valid identifier characters (after the first):
     * lowercase letters, digits, underscore.
     * Uppercase is also allowed here so that the whole word
     * is consumed before we classify it.
     */
    private boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /** Valid escape characters inside a string literal */
    private boolean isValidStringEscape(char c) {
        return c == '"' || c == '\\' || c == 'n' || c == 't' || c == 'r';
    }

    /** Valid escape characters inside a character literal */
    private boolean isValidCharEscape(char c) {
        return c == '\'' || c == '\\' || c == 'n' || c == 't' || c == 'r';
    }

    /**
     * Disambiguation helper for +/- sign vs arithmetic operator.
     * Returns true if the last real token was a value-producing token,
     * meaning a following +/- should be treated as binary operator.
     */
    private boolean prevTokenIsValue() {
        for (int i = tokens.size() - 1; i >= 0; i--) {
            Token prev = tokens.get(i);
            if (prev.getType() == TokenType.ERROR) continue; // skip errors
            TokenType tt = prev.getType();
            if (tt == TokenType.IDENTIFIER     ||
                tt == TokenType.INTEGER_LITERAL ||
                tt == TokenType.FLOAT_LITERAL   ||
                tt == TokenType.STRING_LITERAL  ||
                tt == TokenType.CHAR_LITERAL    ||
                tt == TokenType.BOOLEAN_LITERAL) {
                return true;
            }
            if (tt == TokenType.PUNCTUATOR) {
                String lex = prev.getLexeme();
                return lex.equals(")") || lex.equals("]");
            }
            break;
        }
        return false;
    }

    // ================================================================
    //  SECTION 15 — D. STATISTICS OUTPUT
    // ================================================================

    public void printStatistics() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║            SCANNER STATISTICS                ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.printf( "║  Lines processed      : %-20d║%n", linesProcessed);
        System.out.printf( "║  Total tokens         : %-20d║%n", tokens.size());
        System.out.printf( "║  Comments removed     : %-20d║%n", commentsRemoved);
        System.out.printf( "║  Lexical errors found : %-20d║%n", errorHandler.errorCount());
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.println("║  Token counts by type:                       ║");
        tokenCounts.forEach((type, count) -> {
            if (count > 0) {
                System.out.printf("║    %-28s : %-5d     ║%n", type, count);
            }
        });
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();
    }

    // ================================================================
    //  SECTION 16 — MAIN (command-line entry point)
    // ================================================================

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java ManualScanner <source_file.lang>");
            System.exit(1);
        }

        // Read the entire source file
        String source = new String(Files.readAllBytes(Paths.get(args[0])));
        ManualScanner scanner = new ManualScanner(source);

        // Run the scanner
        List<Token> tokens = scanner.tokenize();

        // C. Token Output
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║               TOKEN STREAM                   ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        for (Token t : tokens) {
            System.out.println("  " + t);
        }
        System.out.println("╚══════════════════════════════════════════════╝");

        // D. Statistics
        scanner.printStatistics();

        // E. Symbol Table
        scanner.symbolTable.print();

        // Error summary
        scanner.errorHandler.printSummary();
    }
}
