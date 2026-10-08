package com.golexer.driver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Driver {
public static final char chSpace = ' ';
public static final char chTab   = '\t';
public static final char chEOL   = '\n';
public static final char chEOF   = '\0';

private char[] inputChain = new char[0];
private int position;
private int line;
private int column;
private char currentChar;
private String fileName = "";

public Driver() {}

public int getPosition() { return position; }
public int getLine() { return line; }
public int getColumn() { return column; }
public char getCurrentChar() { return currentChar; }
public String getFileName() { return fileName; }

public void resetText(String path) {
try {
byte[] bytes = Files.readAllBytes(Paths.get(path));
String content = new String(bytes, StandardCharsets.UTF_8);
inputChain = content.toCharArray();
position = 0;
line = 1;
column = 1;
fileName = path;
nextChar();
} catch (IOException e) {
throw new RuntimeException("Не удалось открыть файл: " + path, e);
}
}

public void nextChar() {
if (position < inputChain.length) {
currentChar = inputChain[position];
position++;

if (currentChar == '\n') {
line++;
column = 1;
} else if (currentChar == '\t') {
column += 4;
} else {
column++;
}
} else {
currentChar = chEOF;
}
}

public char peek() {
if (position < inputChain.length) {
return inputChain[position];
}
return chEOF;
}

public char peek2() {
if (position + 1 < inputChain.length) {
return inputChain[position + 1];
}
return chEOF;
}
}