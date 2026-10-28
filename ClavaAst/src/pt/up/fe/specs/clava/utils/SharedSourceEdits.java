/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clava.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;

/** Retained header ASTs are analysis views. Stage source edits once, preserving the surrounding original text. */
public final class SharedSourceEdits {
    private final App app;
    private final SourceDeclaration.Resolver sourceResolver = new SourceDeclaration.Resolver();
    private final Map<Path, String> originals = new HashMap<>();
    private final Map<Path, Set<String>> sourceUsers = new HashMap<>();
    private final Map<SourceDeclaration, Fragment> fragments = new LinkedHashMap<>();
    private record Fragment(int start, int end, String text) {}
    private record Before(NamedDecl node, SourceDeclaration source, String code) {}
    private record Token(String text, int start, int end) {}
    // Token alignment is used only for a bounded edit, never as a replacement C++ semantic parser.
    private static final Pattern TOKENS = Pattern.compile(
            "//[^\\r\\n]*|/\\*[\\s\\S]*?\\*/|(?:u8|u|U|L)?\"(?:\\\\[\\s\\S]|[^\"\\\\])*\"|(?:u|U|L)?'(?:\\\\[\\s\\S]|[^'\\\\])*'|[\\p{L}_$][\\p{L}\\p{N}_$]*|[0-9][\\p{L}\\p{N}_.']*|\\S");

    public SharedSourceEdits(App app) {
        this.app = app;
        for (var unit : app.getTranslationUnits()) {
            Path path = unit.getFile().toPath().toAbsolutePath().normalize();
            if (Files.isRegularFile(path)) originals.computeIfAbsent(path, SharedSourceEdits::read);
        }
        for (var node : roots()) sourceResolver.of(node).ifPresent(source -> {
            originals.computeIfAbsent(source.file(), SharedSourceEdits::read);
            sourceUsers.computeIfAbsent(source.file(), ignored -> new HashSet<>()).add(DeclarationContext.origin(node));
        });
    }

    private static String read(Path path) {
        try { return Files.readString(path); }
        catch (java.io.IOException e) { throw new RefreshRequiredException("cannot read source " + path); }
    }

    private List<NamedDecl> roots() {
        var result = new ArrayList<NamedDecl>();
        var seen = Collections.newSetFromMap(new IdentityHashMap<ClavaNode, Boolean>());
        for (var unit : app.getTranslationUnits()) for (var declaration : unit.get(TranslationUnit.INCLUDED_DECLARATIONS)) {
            ClavaNode root = declaration;
            while (root.getParent() != null && !(root.getParent() instanceof TranslationUnit)) root = root.getParent();
            if (root instanceof NamedDecl named && !(root instanceof pt.up.fe.specs.clava.ast.decl.ParmVarDecl) && seen.add(root) && !root.get(ClavaNode.IS_IN_SYSTEM_HEADER)
                    && sourceResolver.of(named).isPresent()) result.add(named);
        }
        // Detached implicit instantiations can repeat a written template's inner range. Resolve
        // outer owners with one sorted pass, retaining all TU views of the same written owner.
        record Root(NamedDecl node, SourceDeclaration source, long begin, long end) {}
        var ordered = result.stream().map(node -> {
            var source = sourceResolver.of(node).orElseThrow();
            return new Root(node, source, ((long) source.line() << 32) + source.column(),
                    ((long) source.endLine() << 32) + source.endColumn());
        }).sorted(Comparator.comparing((Root root) -> root.source().file().toString())
                .thenComparingLong(Root::begin).thenComparing(Comparator.comparingLong(Root::end).reversed())
                .thenComparing(root -> !(root.node() instanceof pt.up.fe.specs.clava.ast.decl.TemplateDecl))).toList();
        var owners = new ArrayList<NamedDecl>();
        Root outer = null;
        for (var root : ordered) {
            if (outer != null && outer.source().file().equals(root.source().file()) && outer.end() >= root.end()
                    && (outer.begin() < root.begin() || outer.end() > root.end()
                        || (outer.node() instanceof pt.up.fe.specs.clava.ast.decl.TemplateDecl
                            && !(root.node() instanceof pt.up.fe.specs.clava.ast.decl.TemplateDecl)))) continue;
            owners.add(root.node());
            if (outer == null || !outer.source().file().equals(root.source().file()) || root.end() > outer.end()
                    || root.begin() > outer.end()) outer = root;
        }
        return owners;
    }

    public List<?> before() {
        var result = new ArrayList<Before>();
        var checked = new HashSet<Path>();
        for (var node : roots()) {
            var source = sourceResolver.of(node).orElseThrow();
            String original = originals.computeIfAbsent(source.file(), SharedSourceEdits::read);
            if (checked.add(source.file()) && !original.equals(read(source.file())))
                throw new RefreshRequiredException("source changed since parsing: " + source.file());
            result.add(new Before(node, source, node.getCode()));
        }
        return result;
    }

    /** Called inside App's rollback boundary; commit staged text only after every context agrees. */
    public void after(List<?> snapshots) {
        var changes = new LinkedHashMap<SourceDeclaration, Fragment>();
        for (var entry : snapshots) {
            var before = (Before) entry;
            String after = before.node().getCode();
            if (before.code().equals(after)) continue;
            var source = before.source();
            var previous = fragments.get(source);
            String original = originals.get(source.file());
            int start = previous == null ? offset(original, source.line(), source.column()) : previous.start();
            int end = previous == null ? end(original, source) : previous.end();
            String text = previous == null ? original.substring(start, end) : previous.text();
            String changed = align(text, before.code(), after);
            var fragment = new Fragment(start, end, changed);
            var other = changes.putIfAbsent(source, fragment);
            if (other != null && !other.text().equals(changed))
                throw new RefreshRequiredException("contexts require different edits to " + source.file());
        }
        // Also validate preservation of the including file's preprocessing before committing any edit.
        if (!changes.isEmpty() || !fragments.isEmpty()) for (var unit : app.getTranslationUnits())
            preservePreamble(unit, unit.getCode());
        var combined = new LinkedHashMap<>(fragments);
        combined.putAll(changes);
        for (var left : combined.entrySet()) for (var right : combined.entrySet()) {
            if (left.getKey().equals(right.getKey()) || !left.getKey().file().equals(right.getKey().file())) continue;
            if (left.getValue().start() < right.getValue().end() && right.getValue().start() < left.getValue().end())
                throw new RefreshRequiredException("overlapping source owners in " + left.getKey().file());
        }
        fragments.clear();
        fragments.putAll(combined);
    }

    private static int offset(String text, int line, int byteColumn) {
        int start = 0;
        for (int i = 1; i < line; i++) {
            start = text.indexOf('\n', start);
            if (start < 0) throw new RefreshRequiredException("invalid source line");
            start++;
        }
        int pos = start, bytes = 1;
        while (pos < text.length() && bytes < byteColumn) {
            int count = Character.charCount(text.codePointAt(pos));
            bytes += text.substring(pos, pos + count).getBytes(StandardCharsets.UTF_8).length;
            pos += count;
        }
        if (bytes != byteColumn) throw new RefreshRequiredException("invalid source byte column");
        return pos;
    }

    private static int end(String text, SourceDeclaration source) {
        int pos = offset(text, source.endLine(), source.endColumn());
        var matcher = TOKENS.matcher(text);
        matcher.region(pos, text.length());
        if (!matcher.lookingAt()) throw new RefreshRequiredException("missing source end token");
        int end = matcher.end();
        // Record/variable printers include their terminator, while Clang ranges often end before it.
        if (end < text.length() && text.charAt(end) == ';') end++;
        return end;
    }

    private static List<Token> tokens(String code) {
        // Raw strings need delimiter-aware tokenization; refuse rather than corrupting their contents.
        if (code.matches("(?s).*\\b(?:u8|u|U|L)?R\".*"))
            throw new RefreshRequiredException("source edit crosses unsupported raw-string tokenization");
        var result = new ArrayList<Token>();
        var matcher = TOKENS.matcher(code);
        while (matcher.find()) {
            String token = matcher.group();
            if (!token.startsWith("//") && !token.startsWith("/*")) result.add(new Token(token, matcher.start(), matcher.end()));
        }
        // Clava prints implicit member accesses as this->name. Treat that receiver as optional
        // for alignment, retaining original source positions and any explicitly written receiver.
        for (int i = result.size() - 3; i >= 0; i--)
            if (result.get(i).text().equals("this") && result.get(i + 1).text().equals("-")
                    && result.get(i + 2).text().equals(">")) result.subList(i, i + 3).clear();
        return result;
    }

    private static String align(String source, String oldCode, String newCode) {
        var old = tokens(oldCode); var next = tokens(newCode); var actual = tokens(source);
        int prefix = 0;
        while (prefix < old.size() && prefix < next.size() && old.get(prefix).text().equals(next.get(prefix).text())) prefix++;
        int suffix = 0;
        while (suffix < old.size() - prefix && suffix < next.size() - prefix
                && old.get(old.size() - 1 - suffix).text().equals(next.get(next.size() - 1 - suffix).text())) suffix++;
        if (prefix == old.size() && prefix == next.size()) return source;
        int oldEnd = old.size() - suffix, newEnd = next.size() - suffix;
        if (actual.size() < oldEnd) throw new RefreshRequiredException("source tokens do not match the analyzed edit");
        for (int i = 0; i < oldEnd; i++) if (!old.get(i).text().equals(actual.get(i).text()))
            throw new RefreshRequiredException("edit crosses preprocessing or different source spelling");
        int begin = prefix < actual.size() ? actual.get(prefix).start() : source.length();
        int end = oldEnd > prefix ? actual.get(oldEnd - 1).end() : begin;
        String replacement = newEnd == prefix ? "" : newCode.substring(next.get(prefix).start(), next.get(newEnd - 1).end());
        if (oldEnd == prefix && !replacement.isEmpty()) replacement = " " + replacement + " ";
        return source.substring(0, begin) + replacement + source.substring(end);
    }

    private static final Pattern INCLUDES = Pattern.compile("(?m)^\\h*#\\h*include\\b[^\\r\\n]*");

    private static List<String> includes(String text) {
        return INCLUDES.matcher(text).results().map(match -> match.group().replaceAll("\\s+", "")).toList();
    }

    private String preservePreamble(TranslationUnit unit, String generated) {
        Path file = unit.getFile().toPath().toAbsolutePath().normalize();
        String original = originals.get(file);
        if (original == null) return generated;
        if (!original.equals(read(file))) throw new RefreshRequiredException("source changed before emission: " + file);
        if (original.equals(generated)) return generated;
        if (!Pattern.compile("(?m)^\\h*#\\h*(?!include\\b)\\w+").matcher(original).find()) return generated;
        var first = unit.getChildrenStream().filter(NamedDecl.class::isInstance).map(NamedDecl.class::cast)
                .filter(node -> node.hasValue(NamedDecl.USR) && node.getLocation().isValid()).findFirst();
        if (first.isEmpty()) throw new RefreshRequiredException("cannot preserve preprocessing layout in " + file);
        var node = first.get();
        int begin = offset(original, node.getLocation().getStartLine(), node.getLocation().getStartCol());
        String prefix = original.substring(0, begin);
        int generatedBegin = generated.indexOf(node.getCode());
        if (generatedBegin < 0 || !includes(prefix).equals(includes(generated.substring(0, generatedBegin))))
            throw new RefreshRequiredException("include layout changed in " + file);
        if (!tokens(INCLUDES.matcher(generated.substring(0, generatedBegin)).replaceAll("")).isEmpty())
            throw new RefreshRequiredException("new declarations precede preprocessing in " + file);
        // Conditional regions around source declarations need a compiler-backed source rewrite.
        int depth = 0;
        for (String line : prefix.split("\\R")) {
            String stripped = line.stripLeading();
            if (stripped.matches("#\\h*(if|ifdef|ifndef)\\b.*")) depth++;
            if (stripped.matches("#\\h*endif\\b.*")) depth--;
        }
        if (depth != 0 || Pattern.compile("(?m)^\\h*#\\h*(?!include\\b)\\w+")
                .matcher(original.substring(begin)).find())
            throw new RefreshRequiredException("preprocessing crosses source declarations in " + file);
        return prefix + generated.substring(generatedBegin);
    }

    private Path destination(Path folder, Path source) {
        var mapped = new LinkedHashSet<Path>();
        for (var unit : app.getTranslationUnits()) {
            if (!sourceUsers.getOrDefault(source, Set.of()).contains(unit.getFile().toPath().toAbsolutePath().normalize().toString())) continue;
            Path unitFolder = folder.resolve(unit.get(TranslationUnit.SOURCE_FOLDERNAME).orElse(""))
                    .resolve(unit.getRelativeFolderpath().orElse(""));
            Path relative = unit.getFile().toPath().toAbsolutePath().getParent().relativize(source);
            Path target = unitFolder.resolve(relative).normalize();
            if (target.startsWith(folder)) mapped.add(target);
        }
        if (mapped.size() > 1) throw new RefreshRequiredException("conflicting output roots for " + source);
        if (!mapped.isEmpty()) return mapped.iterator().next();
        Path root = app.getTranslationUnits().get(0).getFile().toPath().toAbsolutePath().normalize().getParent();
        for (var unit : app.getTranslationUnits()) {
            Path path = unit.getFile().toPath().toAbsolutePath().normalize();
            while (root != null && !path.startsWith(root)) root = root.getParent();
        }
        while (root != null && !source.startsWith(root)) root = root.getParent();
        if (root == null) throw new RefreshRequiredException("no common output root for " + source);
        return folder.resolve(root.relativize(source)).normalize();
    }

    public void emit(File destination, Map<File, String> output) {
        if (!fragments.isEmpty()) for (var unit : app.getTranslationUnits()) {
            File target = unit.getDestinationFile(destination);
            output.computeIfPresent(target, (file, code) -> preservePreamble(unit, code));
        }
        var files = new LinkedHashMap<Path, List<Map.Entry<SourceDeclaration, Fragment>>>();
        for (var entry : fragments.entrySet()) files.computeIfAbsent(entry.getKey().file(), ignored -> new ArrayList<>()).add(entry);
        for (var entry : files.entrySet()) {
            Path source = entry.getKey();
            if (app.getTranslationUnits().stream().anyMatch(unit -> unit.getFile().toPath().toAbsolutePath().normalize().equals(source))) continue;
            String original = originals.get(source);
            if (!original.equals(read(source))) throw new RefreshRequiredException("source changed before emission: " + source);
            var ordered = entry.getValue();
            ordered.sort(Comparator.comparingInt((Map.Entry<SourceDeclaration, Fragment> value) -> value.getValue().start()).reversed());
            var text = new StringBuilder(original);
            for (var fragment : ordered) text.replace(fragment.getValue().start(), fragment.getValue().end(), fragment.getValue().text());
            Path target = destination(destination.toPath().toAbsolutePath().normalize(), source);
            if (!target.startsWith(destination.toPath().toAbsolutePath().normalize()))
                throw new RefreshRequiredException("header output would escape destination: " + source);
            if (output.keySet().stream().anyMatch(file -> file.toPath().toAbsolutePath().normalize().equals(target))
                    || output.putIfAbsent(target.toFile(), text.toString()) != null)
                throw new RefreshRequiredException("header output collision: " + target);
        }
    }
}
