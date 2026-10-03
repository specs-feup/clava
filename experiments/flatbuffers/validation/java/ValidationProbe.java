import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.util.SpecsSystem;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/** Release-path checks that run through Clava's ordinary CodeParser. */
public final class ValidationProbe {
    private static final Gson GSON = new Gson();

    private ValidationProbe() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException("usage: ValidationProbe <roundtrip|cross-tu|memory|corpus> <input> <work> [standard] [repeats] [strict-cleanup]");
        }

        SpecsSystem.programStandardInit();
        String mode = args[0];
        Path input = Path.of(args[1]).toAbsolutePath().normalize();
        Path work = Path.of(args[2]).toAbsolutePath().normalize();
        Files.createDirectories(work);

        switch (mode) {
        case "roundtrip" -> roundtrip(input, work, args.length > 3 ? args[3] : inferStandard(input));
        case "cross-tu" -> crossTranslationUnit(input, work);
        case "memory" -> memory(input, work, args.length > 3 ? args[3] : inferStandard(input),
                args.length > 4 ? Integer.parseInt(args[4]) : 20,
                args.length <= 5 || Boolean.parseBoolean(args[5]));
        case "corpus" -> corpus(input, work);
        default -> throw new IllegalArgumentException("unknown mode: " + mode);
        }
    }

    private static void roundtrip(Path source, Path work, String standard) throws IOException {
        Path generated = work.resolve("generated");
        Files.createDirectories(generated);
        App first = parse(List.of(source), work.resolve("first"), standard, List.of(source.getParent()));
        List<File> firstFiles = first.write(generated.toFile());
        Path firstSource = matchingSource(firstFiles, source.getFileName().toString());
        byte[] firstBytes = Files.readAllBytes(firstSource);
        long firstNodes = first.getDescendantsAndSelfStream().count();
        first = null;

        App second = parse(List.of(firstSource), work.resolve("second"), standard, List.of(source.getParent()));
        List<File> secondFiles = second.write(generated.toFile());
        Path secondSource = matchingSource(secondFiles, firstSource.getFileName().toString());
        byte[] secondBytes = Files.readAllBytes(secondSource);
        long secondNodes = second.getDescendantsAndSelfStream().count();
        if (!java.util.Arrays.equals(firstBytes, secondBytes)) {
            throw new IllegalStateException("generated source changed after parse, generate, and reparse: " + source);
        }
        emit("CLAVA_VALIDATION", Map.of(
                "case", "parse-generate-reparse",
                "source", source.toString(),
                "standard", standard,
                "first_nodes", firstNodes,
                "second_nodes", secondNodes,
                "generated_sha256", sha256(secondBytes),
                "generated_bytes", secondBytes.length,
                "source_equal_after_reparse", true));
    }

    private static void crossTranslationUnit(Path fixtureRoot, Path work) throws IOException {
        Path callerSource = fixtureRoot.resolve("caller.cpp");
        Path providerSource = fixtureRoot.resolve("provider.cpp");
        App app = parse(List.of(callerSource, providerSource), work, "c++17", List.of(fixtureRoot));
        List<TranslationUnit> units = app.getTranslationUnits();
        if (units.size() != 2) {
            throw new IllegalStateException("expected two translation units, got " + units.size());
        }

        TranslationUnit caller = unitFor(units, callerSource);
        TranslationUnit provider = unitFor(units, providerSource);
        Optional<FunctionDecl> linkedCallee = app.getDescendantsAndSelfStream()
                .filter(CallExpr.class::isInstance)
                .map(CallExpr.class::cast)
                .filter(call -> call.getAncestorTry(TranslationUnit.class)
                        .map(caller::equals).orElse(false))
                .flatMap(call -> call.getDefinition().stream())
                .filter(callee -> callee.getAncestorTry(TranslationUnit.class)
                        .map(provider::equals).orElse(false))
                .findFirst();
        if (linkedCallee.isEmpty()) {
            throw new IllegalStateException("caller.cpp has no direct callee linked into provider.cpp");
        }

        emit("CLAVA_VALIDATION", Map.of(
                "case", "cross-translation-unit-reference",
                "translation_units", units.size(),
                "caller", caller.getFile().getName(),
                "provider", provider.getFile().getName(),
                "linked_function", linkedCallee.get().getDeclName(),
                "target_is_provider_definition", true));
    }

    private static void memory(Path source, Path work, String standard, int repeats,
            boolean strictCleanup) throws IOException {
        if (repeats < 2) {
            throw new IllegalArgumentException("memory probe needs at least two parse repeats");
        }
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();
        for (int index = 1; index <= repeats; index++) {
            Path iteration = work.resolve("iteration-" + index);
            Files.createDirectories(iteration);
            Trial trial = parseAndRelease(source, iteration, standard);
            boolean collected = awaitCollection(trial.reference());
            long retained = usedHeapAfterGc();
            int mapped = mappedPaths(iteration, tempRoot);
            int temporaryFolders = countClangTempFolders(tempRoot);
            Map<String, Object> row = new HashMap<>();
            row.put("phase", "parse_released");
            row.put("repeat", index);
            row.put("nodes", trial.nodes());
            row.put("live_heap_bytes", trial.liveHeapBytes());
            row.put("retained_heap_bytes", retained);
            row.put("jvm_peak_rss_bytes", peakResidentBytes());
            row.put("app_collected", collected);
            row.put("mapped_paths_under_work", mapped);
            row.put("leftover_clang_temp_folders", temporaryFolders);
            emit("CLAVA_HEAP", row);
            if (strictCleanup && (!collected || mapped != 0 || temporaryFolders != 0)) {
                throw new IllegalStateException("AST release or mapped-file cleanup failed on repeat " + index);
            }
        }
    }

    private static long peakResidentBytes() throws IOException {
        Path status = Path.of("/proc/self/status");
        if (!Files.isRegularFile(status)) return -1;
        for (String line : Files.readAllLines(status)) {
            if (line.startsWith("VmHWM:")) {
                return Math.multiplyExact(Long.parseLong(line.substring(6).trim().split("\\s+")[0]), 1024);
            }
        }
        return -1;
    }

    private static void corpus(Path manifestPath, Path work) throws IOException {
        JsonObject manifest = JsonParser.parseString(Files.readString(manifestPath)).getAsJsonObject();
        JsonArray files = manifest.getAsJsonArray("files");
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("corpus manifest contains no files");
        }
        Path generatedRoot = work.resolve("generated");
        Path parserRoot = work.resolve("parser");
        Path resources = work.resolve("resources");
        Files.createDirectories(generatedRoot);
        Files.createDirectories(parserRoot);
        Files.createDirectories(resources);
        long parsed = 0;
        for (int index = 0; index < files.size(); index++) {
            JsonObject input = files.get(index).getAsJsonObject();
            Path source = Path.of(input.get("source").getAsString()).toAbsolutePath().normalize();
            String relative = input.get("relative").getAsString();
            String standard = input.get("standard").isJsonNull()
                    ? inferStandard(source) : input.get("standard").getAsString();
            List<String> options = new ArrayList<>();
            JsonArray optionValues = input.getAsJsonArray("options");
            if (optionValues != null) {
                for (JsonElement option : optionValues) {
                    options.add(option.getAsString());
                }
            }
            Path caseRoot = parserRoot.resolve(String.format("%05d", index));
            Path generated = generatedRoot.resolve(String.format("%05d", index));
            try {
                App app = parse(List.of(source), caseRoot, standard, options, resources);
                long nodes = app.getDescendantsAndSelfStream().count();
                if (nodes == 0) {
                    throw new IllegalStateException("parser returned an empty AST");
                }
                List<File> written = app.write(generated.toFile());
                String codeHash = generatedCodeHash(written);
                emit("CLAVA_CORPUS", Map.of(
                        "index", index,
                        "relative", relative,
                        "source", source.toString(),
                        "bucket", "CLEAN",
                        "nodes", nodes,
                        "generated_code_sha256", codeHash));
                app = null;
                parsed++;
            } catch (Exception | LinkageError failure) {
                String message = failure.getMessage() == null ? failure.getClass().getName()
                        : failure.getClass().getName() + ": " + failure.getMessage();
                emit("CLAVA_CORPUS", Map.of(
                        "index", index,
                        "relative", relative,
                        "source", source.toString(),
                        "bucket", "CONSUMER_FAIL",
                        "error", message));
            }
            if ((index + 1) % 25 == 0) {
                System.gc();
            }
            if ((index + 1) % 100 == 0) {
                emit("CLAVA_CORPUS_PROGRESS", Map.of("completed", index + 1,
                        "total", files.size(), "parsed", parsed));
            }
        }
        emit("CLAVA_CORPUS_SUMMARY", Map.of("cases", files.size(), "parsed", parsed));
    }

    private static String generatedCodeHash(List<File> files) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
        List<File> ordered = files.stream().sorted(Comparator.comparing(file -> file.getName())).toList();
        if (ordered.isEmpty()) {
            throw new IllegalStateException("code generation returned no output files");
        }
        for (File file : ordered) {
            byte[] name = file.getName().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            byte[] content = Files.readAllBytes(file.toPath());
            digest.update(name);
            digest.update((byte) 0);
            digest.update(content);
            digest.update((byte) 0xff);
        }
        return hex(digest.digest());
    }

    private static Trial parseAndRelease(Path source, Path work, String standard) {
        App app = parse(List.of(source), work, standard, List.of());
        long nodes = app.getDescendantsAndSelfStream().count();
        long liveHeap = usedHeapAfterGc();
        WeakReference<App> reference = new WeakReference<>(app);
        app = null;
        return new Trial(nodes, liveHeap, reference);
    }

    private static App parse(List<Path> sources, Path work, String standard, List<Path> includeDirs) {
        return parse(sources, work, standard, includeDirs.stream().map(path -> "-I" + path).toList(), null);
    }

    private static App parse(List<Path> sources, Path work, String standard,
            List<String> extraOptions, Path resourceRoot) {
        try {
            Files.createDirectories(work);
            Path parseRoot = work.resolve("parse-root");
            Files.createDirectories(parseRoot);
            Path resourceCache = resourceRoot == null ? work.resolve("dumper-resources") : resourceRoot;
            Files.createDirectories(resourceCache);

            CodeParser parser = CodeParser.newInstance();
            parser.set(CodeParser.SHOW_EXEC_INFO, false);
            parser.set(CodeParser.AST_DUMP_CACHE, false);
            parser.set(CodeParser.DUMPER_FOLDER, resourceCache.toFile());
            parser.set(CodeParser.GENERATED_PARSE_ROOT, parseRoot.toFile());
            parser.set(ParallelCodeParser.PARALLEL_PARSING, false);

            List<String> options = new ArrayList<>();
            options.add("-std=" + standard);
            options.addAll(extraOptions);
            return parser.parse(sources.stream().map(Path::toFile).toList(), options);
        } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    private static Path matchingSource(List<File> files, String filename) {
        return files.stream().map(File::toPath)
                .filter(path -> path.getFileName().toString().equals(filename))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("generation did not produce " + filename));
    }

    private static TranslationUnit unitFor(List<TranslationUnit> units, Path source) {
        Path expected = source.toAbsolutePath().normalize();
        return units.stream()
                .filter(unit -> unit.getFile().toPath().toAbsolutePath().normalize().equals(expected))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("translation unit missing for " + source));
    }

    private static boolean awaitCollection(WeakReference<App> reference) {
        // Use the same GC and waiting budget for every transport, even when
        // a historical control keeps an AST reachable.
        for (int attempt = 0; attempt < 3; attempt++) {
            System.gc();
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return reference.get() == null;
    }

    private static long usedHeapAfterGc() {
        for (int attempt = 0; attempt < 3; attempt++) {
            System.gc();
        }
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static int mappedPaths(Path work, Path tempRoot) throws IOException {
        Path maps = Path.of("/proc/self/maps");
        if (!Files.isRegularFile(maps)) {
            return -1;
        }
        String workText = work.toAbsolutePath().normalize().toString();
        String tempText = tempRoot.toString();
        try (Stream<String> lines = Files.lines(maps)) {
            return (int) lines.filter(line -> line.contains(workText) || line.contains(tempText + "/clava_ast_"))
                    .count();
        }
    }

    private static int countClangTempFolders(Path tempRoot) throws IOException {
        if (!Files.isDirectory(tempRoot)) {
            return 0;
        }
        try (Stream<Path> children = Files.list(tempRoot)) {
            return (int) children.filter(path -> path.getFileName().toString().startsWith("clava_ast_")).count();
        }
    }

    private static String sha256(byte[] value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value);
            return hex(hash);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }

    private static String hex(byte[] hash) {
        StringBuilder output = new StringBuilder(hash.length * 2);
        for (byte current : hash) {
            output.append(String.format("%02x", current & 0xff));
        }
        return output.toString();
    }

    private static String inferStandard(Path source) {
        return source.getFileName().toString().toLowerCase().endsWith(".c") ? "c11" : "c++17";
    }

    private static void emit(String prefix, Map<String, ?> row) {
        System.out.println(prefix + " " + GSON.toJson(row));
        System.out.flush();
    }

    private record Trial(long nodes, long liveHeapBytes, WeakReference<App> reference) {
    }
}
