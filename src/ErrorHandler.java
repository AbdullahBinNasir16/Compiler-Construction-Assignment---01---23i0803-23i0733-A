import java.util.*;

/*
 * Error types detected:
 *   INVALID_CHARACTER   — character not in the language (@, $, etc.)
 *   MALFORMED_LITERAL   — bad float, bad escape sequence, etc.
 *   INVALID_IDENTIFIER  — wrong start char, exceeds 31 characters
 *   UNCLOSED_COMMENT    — multi-line comment never closed
 *   UNTERMINATED_STRING — string literal has no closing quote
 *   UNTERMINATED_CHAR   — char literal has no closing quote
 *
 * Error recovery: each error is logged and scanning continues
 * from the next character — all errors in the file are reported.
 */
public class ErrorHandler {


    public enum ErrorType {
        INVALID_CHARACTER,
        MALFORMED_LITERAL,
        INVALID_IDENTIFIER,
        UNCLOSED_COMMENT,
        UNTERMINATED_STRING,
        UNTERMINATED_CHAR
    }

    // record

    private static class LexicalError {
        final ErrorType type;
        final int       line;
        final int       column;
        final String    lexeme;
        final String    reason;

        LexicalError(ErrorType type, int line, int col, String lexeme, String reason) {
            this.type   = type;
            this.line   = line;
            this.column = col;
            this.lexeme = lexeme;
            this.reason = reason;
        }

        @Override
        public String toString() {
            return String.format(
                "[LEXICAL ERROR] %-20s | Line: %-4d | Col: %-4d | Lexeme: \"%-15s\" | %s",
                type, line, column, lexeme, reason
            );
        }
    }

    // storage

    private final List<LexicalError> errors = new ArrayList<>();


    //record and print the lexical error
    public void report(ErrorType type, int line, int col, String lexeme, String reason) {
        LexicalError e = new LexicalError(type, line, col, lexeme, reason);
        errors.add(e);
        System.err.println(e);
    }

    /*return num of errors*/
    public int errorCount() { return errors.size(); }

    /* return true if error recorded */
    public boolean hasErrors() { return !errors.isEmpty(); }

    // print error summary

    public void printSummary() {
        System.out.println();
        if (errors.isEmpty()) {
            System.out.println("************************************************");
            System.out.println("*  No lexical errors found. File is clean.     *");
            System.out.println("************************************************");
        } else {
            System.out.println("************************************************");
            System.out.printf( "*  LEXICAL ERROR SUMMARY  (%d error(s) found)%n", errors.size());
            System.out.println("************************************************");
            for (int i = 0; i < errors.size(); i++) {
                System.out.printf("|  #%-3d %s%n", (i + 1), errors.get(i));
            }
            System.out.println("************************************************");
        }
        System.out.println();
    }
}
