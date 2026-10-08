package com.golexer.error;

import com.golexer.driver.Driver;

public class Error {

public static class LexException extends RuntimeException {
public final String file;
public final int line;
public final int column;

public LexException(String message, String file, int line, int column) {
super(message);
this.file = file;
this.line = line;
this.column = column;
}

@Override
public String toString() {
return String.format("%s:%d:%d: %s", file, line, column, getMessage());
}
}

public void lexError(String msg, Driver driver, int errLine, int errCol) {
throw new LexException(msg, driver.getFileName(), errLine, errCol);
}

public void lexError(String msg, Driver driver) {
lexError(msg, driver, driver.getLine(), driver.getColumn());
}

public void report(LexException e) {
System.err.println("  [ЛЕКС. ОШИБКА] " + e);
}
}