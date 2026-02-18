/*
 * Stores:
 *   - TokenType  : the category of the token
 *   - lexeme     : the exact string from the source code
 *   - line       : 1-based line number where the token starts
 *   - column     : 1-based column number where the token starts
 *
 * Output format: <KEYWORD, "start", Line: 1, Col: 1>
 */
public class Token {

    private final TokenType type;
    private final String    lexeme;
    private final int       line;
    private final int       column;

    // ── Constructor ───────────────────────────────────────────────

    public Token(TokenType type, String lexeme, int line, int column) {
        this.type   = type;
        this.lexeme = lexeme;
        this.line   = line;
        this.column = column;
    }

    // ── Getters ───────────────────────────────────────────────────

    public TokenType getType()   { return type;   }
    public String    getLexeme() { return lexeme; }
    public int       getLine()   { return line;   }
    public int       getColumn() { return column; }

    // ── Output ────────────────────────────────────────────────────

    /**
     * Returns the token in the required format:
     *   <KEYWORD, "start", Line: 1, Col: 1>
     */
    @Override
    public String toString() {
        return String.format("<%s, \"%s\", Line: %d, Col: %d>",
                type, lexeme, line, column);
    }
}
