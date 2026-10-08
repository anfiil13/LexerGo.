package com.golexer;

import com.golexer.driver.Driver;
import com.golexer.error.Error;
import com.golexer.error.Error.LexException;
import com.golexer.scanner.Lex;
import com.golexer.scanner.Scanner;
import com.golexer.scanner.Token;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Main {

static class Stats {
long totalTokens = 0;
final Map<Lex, Long> counts = new HashMap<>();
final List<Token> allTokens = new ArrayList<>();
final List<LexException> errors = new ArrayList<>();

void add(Token t) {
totalTokens++;
counts.merge(t.type, 1L, Long::sum);
allTokens.add(t);
}
}

public static void main(String[] args) {
System.out.println("   Лексический анализатор языка Go (GoLexer)");
System.out.println();

String[] files;
if (args.length == 0) {
files = new String[]{"test1.go", "test2.go", "test3.go"};
System.out.println("Аргументы не заданы. Используем файлы по умолчанию:");
} else {
files = args;
System.out.println("Файлы для анализа:");
}
for (String f : files) System.out.println("  - " + f);
System.out.println();

Stats stats = new Stats();
int processedFiles = 0;

for (String fileName : files) {
File f = new File(fileName);
if (!f.exists()) {
System.err.println("[ПРОПУСК] Файл не найден: " + fileName);
continue;
}
processedFiles++;
System.out.println(" Анализ файла: " + fileName);
analyzeFile(fileName, stats);
System.out.println();
}

if (processedFiles == 0) {
System.err.println("Не найдено ни одного файла для анализа.");
System.exit(2);
}

printFinalReport(stats);
}

private static void analyzeFile(String fileName, Stats stats) {
Driver driver = new Driver();
Error error = new Error();

try {
driver.resetText(fileName);
} catch (RuntimeException e) {
System.err.println("  [ОШИБКА] " + e.getMessage());
return;
}

long fileTokens = 0;
try {
Scanner scanner = new Scanner(driver, error);
while (!scanner.isEOT()) {
Token t = scanner.token;
stats.add(t);
fileTokens++;
scanner.nextLex();
}
System.out.println("  Найдено лексем: " + fileTokens);
} catch (LexException e) {
stats.errors.add(e);
System.err.println("  [ЛЕКС. ОШИБКА] " + e);
System.out.println("  Успешно разобрано до ошибки: " + fileTokens + " лексем");
}
}

private static void printFinalReport(Stats stats) {

System.out.println("1. Общее количество лексем: " + stats.totalTokens);
System.out.println();

System.out.println("2. Частота лексем по типам:");
System.out.printf("   %-22s %10s %10s%n", "Тип лексемы", "Абс.", "Отн., %");
System.out.println("   " + repeat('-', 44));

List<Map.Entry<Lex, Long>> entries = new ArrayList<>(stats.counts.entrySet());
entries.sort(Comparator
.comparingLong((Map.Entry<Lex, Long> e) -> e.getValue()).reversed()
.thenComparing(e -> e.getKey().name()));

for (Map.Entry<Lex, Long> e : entries) {
if (e.getKey() == Lex.EOT) continue;
long count = e.getValue();
double pct = stats.totalTokens == 0 ? 0.0 : (count * 100.0 / stats.totalTokens);
System.out.printf("   %-22s %10d %10.2f%n",
e.getKey().name(), count, pct);
}
System.out.println();

System.out.println("3. Перечень идентификаторов в лексикографическом порядке:");
System.out.printf("   %-25s %-30s%n", "Идентификатор", "Расположение (файл:строка)");
System.out.println("   " + repeat('-', 57));

TreeMap<String, List<Token>> identifiers = new TreeMap<>();
for (Token t : stats.allTokens) {
if (t.type == Lex.NAME) {
identifiers.computeIfAbsent(t.value, k -> new ArrayList<>()).add(t);
}
}

if (identifiers.isEmpty()) {
System.out.println("   (идентификаторы не найдены)");
} else {
for (Map.Entry<String, List<Token>> e : identifiers.entrySet()) {
StringBuilder locs = new StringBuilder();
for (int i = 0; i < e.getValue().size(); i++) {
if (i > 0) locs.append(", ");
Token t = e.getValue().get(i);
locs.append(t.file).append(":").append(t.line);
}
System.out.printf("   %-25s %s%n", e.getKey(), locs);
}
}
System.out.println();

if (!stats.errors.isEmpty()) {
System.out.println("4. Обнаруженные лексические ошибки:");
for (LexException err : stats.errors) {
System.out.println("   - " + err);
}
System.out.println();
}
}

private static String repeat(char c, int n) {
StringBuilder sb = new StringBuilder(n);
for (int i = 0; i < n; i++) sb.append(c);
return sb.toString();
}
}