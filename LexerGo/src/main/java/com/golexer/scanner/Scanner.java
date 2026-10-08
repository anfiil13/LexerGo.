package com.golexer.scanner;

import com.golexer.driver.Driver;
import com.golexer.error.Error;
import com.golexer.error.Error.LexException;

import java.util.HashMap;
import java.util.Map;

public class Scanner {

private static final char SQUOTE    = 39;   // '
private static final char DQUOTE    = '"';
private static final char BACKTICK  = 96;   // `
private static final char BACKSLASH = 92;   // \

public Token token;

public boolean isEOT() {
return token != null && token.type == Lex.EOT;
}

private final Driver driver;
private final Error error;

private static final Map<String, Lex> KEYWORDS = new HashMap<>();
static {
KEYWORDS.put("break", Lex.BREAK);
KEYWORDS.put("case", Lex.CASE);
KEYWORDS.put("chan", Lex.CHAN);
KEYWORDS.put("const", Lex.CONST);
KEYWORDS.put("continue", Lex.CONTINUE);
KEYWORDS.put("default", Lex.DEFAULT);
KEYWORDS.put("defer", Lex.DEFER);
KEYWORDS.put("else", Lex.ELSE);
KEYWORDS.put("fallthrough", Lex.FALLTHROUGH);
KEYWORDS.put("for", Lex.FOR);
KEYWORDS.put("func", Lex.FUNC);
KEYWORDS.put("go", Lex.GO);
KEYWORDS.put("goto", Lex.GOTO);
KEYWORDS.put("if", Lex.IF);
KEYWORDS.put("import", Lex.IMPORT);
KEYWORDS.put("interface", Lex.INTERFACE);
KEYWORDS.put("map", Lex.MAP);
KEYWORDS.put("package", Lex.PACKAGE);
KEYWORDS.put("range", Lex.RANGE);
KEYWORDS.put("return", Lex.RETURN);
KEYWORDS.put("select", Lex.SELECT);
KEYWORDS.put("struct", Lex.STRUCT);
KEYWORDS.put("switch", Lex.SWITCH);
KEYWORDS.put("type", Lex.TYPE);
KEYWORDS.put("var", Lex.VAR);
}

public Scanner(Driver driver, Error error) {
this.driver = driver;
this.error = error;
nextLex();
}

public Driver getDriver() { return driver; }

public void nextLex() {
while (true) {
char ch = driver.getCurrentChar();
if (ch == Driver.chSpace || ch == Driver.chTab
|| ch == '\r' || ch == Driver.chEOL) {
driver.nextChar();
continue;
}
if (ch == '/') {
char nx = driver.peek();
if (nx == '/') { skipLineComment(); continue; }
else if (nx == '*') { skipBlockComment(); continue; }
}
break;
}

int startLine = driver.getLine();
int startCol  = driver.getColumn();
char ch = driver.getCurrentChar();

if (isLetter(ch))            scanName(startLine, startCol);
else if (isDigit(ch))        scanNumber(startLine, startCol);
else if (ch == DQUOTE)       scanString(startLine, startCol);
else if (ch == SQUOTE)       scanChar(startLine, startCol);
else if (ch == BACKTICK)     scanRawString(startLine, startCol);
else if (ch == Driver.chEOF) token = new Token(Lex.EOT, "", driver.getFileName(), startLine, startCol);
else                         scanOperator(startLine, startCol);
}

private void scanName(int line, int col) {
StringBuilder sb = new StringBuilder();
char ch = driver.getCurrentChar();
while (isLetter(ch) || isDigit(ch)) {
sb.append(ch);
driver.nextChar();
ch = driver.getCurrentChar();
}
String text = sb.toString();
Lex type = KEYWORDS.getOrDefault(text, Lex.NAME);
token = new Token(type, text, driver.getFileName(), line, col);
}

private void scanNumber(int line, int col) {
StringBuilder sb = new StringBuilder();
char ch = driver.getCurrentChar();

if (ch == '0' && (driver.peek() == 'x' || driver.peek() == 'X')) {
sb.append(ch); driver.nextChar();
sb.append(driver.getCurrentChar()); driver.nextChar();
if (!isHexDigit(driver.getCurrentChar()))
error.lexError("Некорректный шестнадцатеричный литерал", driver, line, col);
while (isHexDigit(driver.getCurrentChar()) || driver.getCurrentChar() == '*') {
sb.append(driver.getCurrentChar()); driver.nextChar();
}
}
else if (ch == '0' && (driver.peek() == 'b' || driver.peek() == 'B')) {
sb.append(ch); driver.nextChar();
sb.append(driver.getCurrentChar()); driver.nextChar();
if (!isBinDigit(driver.getCurrentChar()))
error.lexError("Некорректный двоичный литерал", driver, line, col);
while (isBinDigit(driver.getCurrentChar()) || driver.getCurrentChar() == '*') {
sb.append(driver.getCurrentChar()); driver.nextChar();
}
}
else if (ch == '0' && (driver.peek() == 'o' || driver.peek() == 'O')) {
sb.append(ch); driver.nextChar();
sb.append(driver.getCurrentChar()); driver.nextChar();
if (!isOctDigit(driver.getCurrentChar()))
error.lexError("Некорректный восьмеричный литерал", driver, line, col);
while (isOctDigit(driver.getCurrentChar()) || driver.getCurrentChar() == '*') {
sb.append(driver.getCurrentChar()); driver.nextChar();
}
}
else {
while (isDigit(driver.getCurrentChar()) || driver.getCurrentChar() == '*') {
sb.append(driver.getCurrentChar()); driver.nextChar();
}
if (driver.getCurrentChar() == '.'
&& isDigit(driver.peek())
&& driver.peek2() != '.') {
sb.append('.');
driver.nextChar();
while (isDigit(driver.getCurrentChar()) || driver.getCurrentChar() == '*') {
sb.append(driver.getCurrentChar()); driver.nextChar();
}
}
char e = driver.getCurrentChar();
if (e == 'e' || e == 'E') {
char nx = driver.peek();
if (isDigit(nx) || ((nx == '+' || nx == '-') && isDigit(driver.peek2()))) {
sb.append(e); driver.nextChar();
char sign = driver.getCurrentChar();
if (sign == '+' || sign == '-') { sb.append(sign); driver.nextChar(); }
if (!isDigit(driver.getCurrentChar()))
error.lexError("Ожидалась цифра в экспоненте", driver, line, col);
while (isDigit(driver.getCurrentChar()) || driver.getCurrentChar() == '*') {
sb.append(driver.getCurrentChar()); driver.nextChar();
}
}
}
}
token = new Token(Lex.NUM, sb.toString(), driver.getFileName(), line, col);
}

private void scanString(int line, int col) {
StringBuilder sb = new StringBuilder();
sb.append(DQUOTE);
driver.nextChar();
char ch = driver.getCurrentChar();
while (ch != DQUOTE && ch != Driver.chEOF && ch != Driver.chEOL) {
if (ch == BACKSLASH) {
sb.append(ch);
driver.nextChar();
ch = driver.getCurrentChar();
if (ch == Driver.chEOF || ch == Driver.chEOL) break;
sb.append(ch);
driver.nextChar();
ch = driver.getCurrentChar();
continue;
}
sb.append(ch);
driver.nextChar();
ch = driver.getCurrentChar();
}
if (ch == DQUOTE) {
sb.append(DQUOTE);
driver.nextChar();
token = new Token(Lex.STRING, sb.toString(), driver.getFileName(), line, col);
} else {
error.lexError("Незакрытый строковый литерал", driver, line, col);
}
}

private void scanRawString(int line, int col) {
StringBuilder sb = new StringBuilder();
sb.append(BACKTICK);
driver.nextChar();
char ch = driver.getCurrentChar();
while (ch != BACKTICK && ch != Driver.chEOF) {
sb.append(ch);
driver.nextChar();
ch = driver.getCurrentChar();
}
if (ch == BACKTICK) {
sb.append(BACKTICK);
driver.nextChar();
token = new Token(Lex.STRING, sb.toString(), driver.getFileName(), line, col);
} else {
error.lexError("Незакрытый сырой строковый литерал", driver, line, col);
}
}

private void scanChar(int line, int col) {
StringBuilder sb = new StringBuilder();
sb.append(SQUOTE);
driver.nextChar();
char ch = driver.getCurrentChar();
if (ch == Driver.chEOF || ch == Driver.chEOL)
error.lexError("Незакрытый символьный литерал", driver, line, col);
if (ch == BACKSLASH) {
sb.append(ch);
driver.nextChar();
ch = driver.getCurrentChar();
if (ch == Driver.chEOF || ch == Driver.chEOL)
error.lexError("Незакрытый символьный литерал", driver, line, col);
sb.append(ch);
driver.nextChar();
} else {
sb.append(ch);
driver.nextChar();
}
ch = driver.getCurrentChar();
if (ch == SQUOTE) {
sb.append(SQUOTE);
driver.nextChar();
token = new Token(Lex.STRING, sb.toString(), driver.getFileName(), line, col);
} else {
error.lexError("Незакрытый символьный литерал", driver, line, col);
}
}

private void skipLineComment() {
driver.nextChar();
driver.nextChar();
while (driver.getCurrentChar() != Driver.chEOL
&& driver.getCurrentChar() != Driver.chEOF) {
driver.nextChar();
}
}

private void skipBlockComment() {
int startLine = driver.getLine();
int startCol  = driver.getColumn();
driver.nextChar();
driver.nextChar();
while (true) {
char ch = driver.getCurrentChar();
if (ch == Driver.chEOF)
error.lexError("Незакрытый многострочный комментарий", driver, startLine, startCol);
if (ch == '*' && driver.peek() == '/') {
driver.nextChar();
driver.nextChar();
return;
}
driver.nextChar();
}
}

private void scanOperator(int line, int col) {
char ch = driver.getCurrentChar();
driver.nextChar();
Lex type;
String value;

switch (ch) {
case '+':
if (driver.getCurrentChar() == '+') { driver.nextChar(); type = Lex.INC; value = "++"; }
else if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.ADD_ASSIGN; value = "+="; }
else { type = Lex.ADD; value = "+"; }
break;
case '-':
if (driver.getCurrentChar() == '-') { driver.nextChar(); type = Lex.DEC; value = "--"; }
else if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.SUB_ASSIGN; value = "-="; }
else { type = Lex.SUB; value = "-"; }
break;
case '*':
if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.MUL_ASSIGN; value = "*="; }
else { type = Lex.MUL; value = "*"; }
break;
case '/':
if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.QUO_ASSIGN; value = "/="; }
else { type = Lex.QUO; value = "/"; }
break;
case '%':
if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.REM_ASSIGN; value = "%="; }
else { type = Lex.REM; value = "%"; }
break;
case '&':
if (driver.getCurrentChar() == '&') { driver.nextChar(); type = Lex.LAND; value = "&&"; }
else if (driver.getCurrentChar() == '^') {
driver.nextChar();
if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.AND_NOT_ASSIGN; value = "&^="; }
else { type = Lex.AND_NOT; value = "&^"; }
}
else if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.AND_ASSIGN; value = "&="; }
else { type = Lex.AND; value = "&"; }
break;
case '|':
if (driver.getCurrentChar() == '|') { driver.nextChar(); type = Lex.LOR; value = "||"; }
else if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.OR_ASSIGN; value = "|="; }
else { type = Lex.OR; value = "|"; }
break;
case '^':
if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.XOR_ASSIGN; value = "^="; }
else { type = Lex.XOR; value = "^"; }
break;
case '<':
if (driver.getCurrentChar() == '<') {
driver.nextChar();
if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.SHL_ASSIGN; value = "<<="; }
else { type = Lex.SHL; value = "<<"; }
}
else if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.LEQ; value = "<="; }
else if (driver.getCurrentChar() == '-') { driver.nextChar(); type = Lex.SEND; value = "<-"; }
else { type = Lex.LSS; value = "<"; }
break;
case '>':
if (driver.getCurrentChar() == '>') {
driver.nextChar();
if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.SHR_ASSIGN; value = ">>="; }
else { type = Lex.SHR; value = ">>"; }
}
else if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.GEQ; value = ">="; }
else { type = Lex.GTR; value = ">"; }
break;
case '=':
if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.EQL; value = "=="; }
else { type = Lex.ASSIGN; value = "="; }
break;
case '!':
if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.NEQ; value = "!="; }
else { type = Lex.NOT; value = "!"; }
break;
case ':':
if (driver.getCurrentChar() == '=') { driver.nextChar(); type = Lex.DEFINE; value = ":="; }
else { type = Lex.COLON; value = ":"; }
break;
case '.':
if (driver.getCurrentChar() == '.' && driver.peek() == '.') {
driver.nextChar();
driver.nextChar();
type = Lex.ELLIPSIS; value = "...";
} else {
type = Lex.DOT; value = ".";
}
break;
case ',': type = Lex.COMMA; value = ","; break;
case ';': type = Lex.SEMICOLON; value = ";"; break;
case '(': type = Lex.LPAREN; value = "("; break;
case ')': type = Lex.RPAREN; value = ")"; break;
case '[': type = Lex.LBRACK; value = "["; break;
case ']': type = Lex.RBRACK; value = "]"; break;
case '{': type = Lex.LBRACE; value = "{"; break;
case '}': type = Lex.RBRACE; value = "}"; break;
default:
error.lexError("Недопустимый символ: '" + ch + "'", driver, line, col);
return;
}
token = new Token(type, value, driver.getFileName(), line, col);
}

private static boolean isLetter(char c) {
return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
}

private static boolean isDigit(char c) {
return c >= '0' && c <= '9';
}

private static boolean isHexDigit(char c) {
return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
}

private static boolean isOctDigit(char c) {
return c >= '0' && c <= '7';
}

private static boolean isBinDigit(char c) {
return c == '0' || c == '1';
}
}