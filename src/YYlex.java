import java.io.*;
import java.util.*;


public class JFlexMain {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java JFlexMain <source_file.lang>");
            System.exit(1);
        }

        FileReader reader = new FileReader(args[0]);
        Yylex      lexer  = new Yylex(reader);

        List<Token> tokens = new ArrayList<>();
        Token t;

        /* run scanner n collect all meaningful tokens */
        while ((t = lexer.yylex()) != null) {
            TokenType tt = t.getType();
            //skip comments
            if (tt != TokenType.SINGLE_LINE_COMMENT &&
                tt != TokenType.MULTI_LINE_COMMENT) {
                tokens.add(t);
            }
        }

        // token output
        System.out.println();
        System.out.println("************************************************");
        System.out.println("*           JFLEX TOKEN STREAM                 *");
        System.out.println("************************************************");
        for (Token tok : tokens) {
            System.out.println("  " + tok);
        }
        System.out.println("*************************************************");

        /* Statistics */
        lexer.printStatistics(tokens.size());

        /* Symbol Table */
        lexer.getSymbolTable().print();

        /* Error summary */
        lexer.getErrorHandler().printSummary();
    }
}
