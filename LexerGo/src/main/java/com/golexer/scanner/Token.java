package com.golexer.scanner;

public class Token {
public final Lex type;
public final String value;
public final String file;
public final int line;
public final int column;

public Token(Lex type, String value, String file, int line, int column) {
this.type = type;
this.value = value;
this.file = file;
this.line = line;
this.column = column;
}

@Override
public String toString() {
return String.format("%s('%s') @ %s:%d:%d", type, value, file, line, column);
}
}