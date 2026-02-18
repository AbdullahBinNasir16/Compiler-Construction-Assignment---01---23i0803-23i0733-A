import java.util.*;

/*
 * For each identifier, stores:
 *   - name           : the identifier string
 *   - type           : token type (always "IDENTIFIER" at lex phase)
 *   - firstLine      : line where it first appeared
 *   - firstColumn    : column where it first appeared
 *   - frequency      : how many times it appears in the source
 */
public class SymbolTable {

    // ── Inner record ──────────────────────────────────────────────

    private static class Entry {
        final String name;
        final String type;
        final int    firstLine;
        final int    firstColumn;
        int          frequency;

        Entry(String name, int line, int col) {
            this.name        = name;
            this.type        = "IDENTIFIER";
            this.firstLine   = line;
            this.firstColumn = col;
            this.frequency   = 1;
        }
    }

    // ── Storage: insertion-ordered map ───────────────────────────

    private final Map<String, Entry> table = new LinkedHashMap<>();

    // ── Public API ────────────────────────────────────────────────

    /**
     * Insert a new identifier or increment its frequency if already present.
     *
     * @param name   identifier lexeme
     * @param line   line of this occurrence
     * @param col    column of this occurrence
     */
    public void insert(String name, int line, int col) {
        Entry e = table.get(name);
        if (e == null) {
            table.put(name, new Entry(name, line, col));
        } else {
            e.frequency++;
        }
    }

    /**
     * Look up an identifier by name.
     * Returns a formatted string, or null if not found.
     */
    public String lookup(String name) {
        Entry e = table.get(name);
        if (e == null) return null;
        return formatEntry(e);
    }

    /** Returns the number of unique identifiers. */
    public int size() { return table.size(); }

    // ── E. Print the full symbol table ────────────────────────────

    public void print() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                         SYMBOL TABLE                                ║");
        System.out.println("╠══════════════╦════════════╦══════════════╦════════╦═════════════════╣");
        System.out.printf( "║ %-12s ║ %-10s ║ %-12s ║ %-6s ║ %-15s ║%n",
                           "Name", "Type", "First Line", "Col", "Frequency");
        System.out.println("╠══════════════╬════════════╬══════════════╬════════╬═════════════════╣");

        if (table.isEmpty()) {
            System.out.println("║  (no identifiers found)                                             ║");
        } else {
            for (Entry e : table.values()) {
                System.out.printf("║ %-12s ║ %-10s ║ %-12d ║ %-6d ║ %-15d ║%n",
                        e.name, e.type, e.firstLine, e.firstColumn, e.frequency);
            }
        }

        System.out.println("╚══════════════╩════════════╩══════════════╩════════╩═════════════════╝");
        System.out.printf( "  Total unique identifiers: %d%n%n", table.size());
    }

    // ── Private helpers ───────────────────────────────────────────

    private String formatEntry(Entry e) {
        return String.format("Name: %-15s | Type: %-10s | Line: %-4d | Col: %-4d | Frequency: %d",
                e.name, e.type, e.firstLine, e.firstColumn, e.frequency);
    }
}
