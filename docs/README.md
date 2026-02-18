# CustomLang — Lexical Analyzer
### CS4031 Compiler Construction — Assignment 01

---

## Team Members

| Roll Number | Name               |
|-------------|--------------------|
| 23i-0733    | Abbas Raza         |
| 23i-0803    | Abdullah Bin Nasir |
| Section     | A                  |


## Language Overview

| Property        | Value         |
|-----------------|---------------|
| Language Name   | CustomLang    |
| File Extension  | `.lang`       |
| Case Sensitive  | Yes           |
| Paradigm        | Imperative    |

---

## Keyword List

| Keyword     | Meaning                                  |
|-------------|------------------------------------------|
| `start`     | Begin a program or block                 |
| `finish`    | End a program, block, loop, or function  |
| `loop`      | Begin a loop construct                   |
| `condition` | Begin an if/conditional construct        |
| `declare`   | Declare a variable                       |
| `output`    | Print a value to standard output         |
| `input`     | Read a value from standard input         |
| `function`  | Define a function                        |
| `return`    | Return a value from a function           |
| `break`     | Exit the current loop                    |
| `continue`  | Skip to next iteration of loop           |
| `else`      | Alternative branch of a condition        |

---

## Identifier Rules

- Must **start with an uppercase letter** (A–Z)
- Followed by any mix of: lowercase letters (a–z), digits (0–9), underscores (`_`)
- **Maximum total length: 31 characters**
- Case-sensitive

### Valid Examples
```
Count         X             My_variable
Total_sum     Result2024    A
```

### Invalid Examples
```
count         # starts with lowercase
2Count        # starts with digit
myVar         # starts with lowercase
VeryLongIdentifierNameThatExceedsThirtyOne  # too long
```

---

## Literal Formats

### Integer Literals
- Pattern: `[+-]?[0-9]+`
- Examples: `42`, `+100`, `-567`, `0`
- Invalid: `1,000` (no commas), `12.34` (that is a float)

### Floating-Point Literals
- Pattern: `[+-]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?`
- Examples: `3.14`, `+2.5`, `-0.123456`, `1.5e10`, `2.0E-3`
- Invalid: `3.` (no digits after decimal), `.14` (no digits before), `1.2345678` (> 6 decimal places)

### String Literals
- Pattern: `"([^"\\\n] | \\["\\ntr])*"`
- Examples: `"Hello"`, `"Line\nTwo"`, `"Say \"hi\""`, `"Tab\there"`
- Escape sequences: `\"` `\\` `\n` `\t` `\r`

### Character Literals
- Pattern: `'([^'\\\n] | \\['\\ntr])'`
- Examples: `'A'`, `'\n'`, `'\t'`, `'\\'`, `'\''`

### Boolean Literals
- Values: `true`, `false` (exact, lowercase)

---

## Operator List with Precedence

| Precedence | Operator(s)        | Category              | Associativity |
|------------|--------------------|-----------------------|---------------|
| 1 (high)   | `++` `--`          | Increment / Decrement | Left          |
| 2          | `**`               | Exponentiation        | Right         |
| 3          | `*` `/` `%`        | Multiplicative        | Left          |
| 4          | `+` `-`            | Additive              | Left          |
| 5          | `<` `>` `<=` `>=`  | Relational            | Left          |
| 6          | `==` `!=`          | Equality              | Left          |
| 7          | `!`                | Logical NOT           | Right         |
| 8          | `&&`               | Logical AND           | Left          |
| 9          | `\|\|`             | Logical OR            | Left          |
| 10 (low)   | `=` `+=` `-=` `*=` `/=` | Assignment      | Right         |

---

## Comment Syntax

### Single-line Comment
```
## This entire line is a comment
Count = 5  ## inline comment after code
```

### Multi-line Comment
```
#*
   This spans
   multiple lines
*#
```

---

## Punctuators

| Symbol | Name            |
|--------|-----------------|
| `(`    | Left paren      |
| `)`    | Right paren     |
| `{`    | Left brace      |
| `}`    | Right brace     |
| `[`    | Left bracket    |
| `]`    | Right bracket   |
| `,`    | Comma           |
| `;`    | Semicolon       |
| `:`    | Colon           |

---

## Sample Programs

### Program 1 — Hello World
```
start
    declare Msg
    Msg = "Hello, World!"
    output Msg
finish
```

### Program 2 — Counting Loop
```
start
    declare Count
    Count = 1

    loop condition Count <= 10
        output Count
        Count++
    finish
finish
```

### Program 3 — Function with Conditional
```
start
    declare Score
    declare Grade

    Score = 85

    condition Score >= 90
        Grade = "A"
    else
        condition Score >= 80
            Grade = "B"
        else
            Grade = "C"
        finish
    finish

    output Grade
finish
```

### Program 4 — Fibonacci Sequence
```
start
    declare A
    declare B
    declare Temp
    declare N

    A = 0
    B = 1
    N = 10

    loop condition N > 0
        output A
        Temp = A + B
        A = B
        B = Temp
        N--
    finish
finish
```

---

## Compilation & Execution Instructions

### Prerequisites
- Java JDK 8 or higher
- JFlex tool: download `jflex.jar` from https://jflex.de/

### Compile Manual Scanner
```bash
cd src/
javac TokenType.java Token.java SymbolTable.java ErrorHandler.java ManualScanner.java
```

### Run Manual Scanner
```bash
java ManualScanner ../tests/test1.lang
java ManualScanner ../tests/test2.lang
java ManualScanner ../tests/test3.lang
java ManualScanner ../tests/test4.lang
java ManualScanner ../tests/test5.lang
```

### Generate & Compile JFlex Scanner
```bash
# Step 1: Generate Yylex.java from the .flex specification
java -jar jflex.jar Scanner.flex

# Step 2: Compile everything
javac TokenType.java Token.java SymbolTable.java ErrorHandler.java Yylex.java JFlexMain.java

# Step 3: Run
java JFlexMain ../tests/test1.lang
```

### Run Both and Compare
```bash
# Manual scanner
java ManualScanner ../tests/test1.lang > ../tests/manual_out.txt

# JFlex scanner
java JFlexMain ../tests/test1.lang > ../tests/jflex_out.txt

# Diff
diff ../tests/manual_out.txt ../tests/jflex_out.txt
```

---

## Project Structure

```
i23-0744-i23-0509-A/
├── src/
│   ├── ManualScanner.java   # DFA-based hand-written scanner
│   ├── Token.java           # Token data class
│   ├── TokenType.java       # Token type enum
│   ├── SymbolTable.java     # Symbol table for identifiers
│   ├── ErrorHandler.java    # Lexical error detection & reporting
│   ├── Scanner.flex         # JFlex specification
│   ├── Yylex.java           # Generated by JFlex (generate first)
│   └── JFlexMain.java       # Driver for JFlex scanner
├── docs/
│   ├── Automata_Design.pdf  # NFA/DFA diagrams and transition tables
│   ├── LanguageGrammar.txt  # Formal grammar specification
│   ├── Comparison.pdf       # Output comparison report
│   └── README.md            # This file
└── tests/
    ├── test1.lang           # All valid tokens
    ├── test2.lang           # Complex expressions & operators
    ├── test3.lang           # String/char with escape sequences
    ├── test4.lang           # Lexical errors
    ├── test5.lang           # Comment handling
    └── TestResults.txt      # Captured output from both scanners
```

---

## Token Output Format

Each token is printed as:
```
<TOKEN_TYPE, "lexeme", Line: N, Col: N>
```

Example:
```
<KEYWORD, "start", Line: 1, Col: 1>
<KEYWORD, "declare", Line: 2, Col: 5>
<IDENTIFIER, "Count", Line: 2, Col: 13>
<ASSIGNMENT_OP, "=", Line: 3, Col: 11>
<INTEGER_LITERAL, "42", Line: 3, Col: 13>
```
