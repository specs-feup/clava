import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.LibcMode;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ClavaNode;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Runs captured source inputs through CodeParser without starting a test suite. */
public final class StandaloneParseRunner {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String METRICS_PROPERTY = "clava.astWireMetrics";
    private static final String METRICS_DEBUG_PROPERTY = "clava.astWireMetrics.debugArgs";
    private static final String WIRE_PROPERTY = "clava.astAbWire";
    private static final String COMPRESSION_PROPERTY = "clava.astWireBenchmarkDisableCompression";
    private static final String WORKING_DIRECTORY_PROPERTY = "clava.astWireBenchmarkWorkingDirectory";
    private static final String PARSE_ID_PROPERTY = "clava.astWireBenchmarkParseId";
    private static final String STANDARD_PROPERTY = "clava.astWireBenchmarkStandard";
    private static final PrintStream QUIET = new PrintStream(java.io.OutputStream.nullOutputStream(), false,
            StandardCharsets.UTF_8);

    private StandaloneParseRunner() {
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> arguments = parseArgs(args);
        Path schedulePath = Path.of(required(arguments, "schedule")).toAbsolutePath().normalize();
        Path outputPath = Path.of(required(arguments, "output")).toAbsolutePath().normalize();
        Files.createDirectories(outputPath.getParent());
        System.setProperty(COMPRESSION_PROPERTY, "true");
        System.setProperty(METRICS_PROPERTY, "false");

        try (BufferedReader schedule = Files.newBufferedReader(schedulePath, StandardCharsets.UTF_8);
                var output = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            String line;
            long scheduleLine = 0;
            while ((line = schedule.readLine()) != null) {
                scheduleLine++;
                if (line.isBlank()) {
                    continue;
                }

                JsonObject operation = JsonParser.parseString(line).getAsJsonObject();
                String operationType = string(operation, "operation", "parse");
                JsonObject result;
                if (operationType.startsWith("ccache_")) {
                    result = runCcacheOperation(operation, scheduleLine);
                } else {
                    result = runParse(operation, scheduleLine);
                }
                output.write(GSON.toJson(result));
                output.newLine();
                output.flush();
                if (result.has("valid") && !result.get("valid").getAsBoolean()) {
                    throw new IllegalStateException("standalone operation failed at schedule line " + scheduleLine
                            + ": " + string(result, "error", string(result, "validity_reason", "invalid result")));
                }
            }
        }
    }

    private static JsonObject runParse(JsonObject operation, long scheduleLine) {
        String phase = requiredString(operation, "phase");
        String protocol = requiredString(operation, "protocol");
        String cacheMode = requiredString(operation, "cache_mode");
        String suite = requiredString(operation, "suite");
        String inputId = requiredString(operation, "input_id");
        String eventId = requiredString(operation, "event_id");
        String sourceSha256 = requiredString(operation, "source_sha256");
        String argsSha256 = requiredString(operation, "args_sha256");
        String optionsSha256 = string(operation, "options_sha256", "");
        Path source = Path.of(requiredString(operation, "source_path")).toAbsolutePath().normalize();
        Path replayCwd = Path.of(requiredString(operation, "replay_cwd")).toAbsolutePath().normalize();
        Path generatedParseRoot = null;
        if (operation.has("generated_parse_root") && !operation.get("generated_parse_root").isJsonNull()) {
            generatedParseRoot = Path.of(operation.get("generated_parse_root").getAsString())
                    .toAbsolutePath().normalize();
        }
        Path dumperFolder = Path.of(requiredString(operation, "dumper_folder")).toAbsolutePath().normalize();
        List<String> compilerOptions = stringList(operation.getAsJsonArray("compiler_options"));
        JsonObject parserConfig = operation.has("parser_config") && operation.get("parser_config").isJsonObject()
                ? operation.getAsJsonObject("parser_config") : new JsonObject();

        JsonObject result = identity(operation, scheduleLine);
        result.addProperty("record_type", phase.equals("diagnostic") ? "diagnostic"
                : phase.equals("fidelity") ? "fidelity" : "parse");
        result.addProperty("source_sha256", sourceSha256);
        result.addProperty("args_sha256", argsSha256);
        result.addProperty("options_sha256", optionsSha256);
        result.addProperty("show_exec_info", false);
        result.addProperty("compression", false);

        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        ByteArrayOutputStream diagnostics = phase.equals("diagnostic") ? new ByteArrayOutputStream() : null;
        PrintStream diagnosticStream = diagnostics == null ? null
                : new PrintStream(diagnostics, true, StandardCharsets.UTF_8);
        boolean success = false;
        long elapsedNanos = 0L;
        String error = null;
        try {
            if (!Files.isRegularFile(source)) {
                throw new IOException("source does not exist: " + source);
            }
            String observedDigest = sha256(source);
            if (!sourceSha256.equals(observedDigest)) {
                throw new IOException("source SHA-256 mismatch for " + source + ": expected "
                        + sourceSha256 + ", got " + observedDigest);
            }
            if (!Files.isDirectory(replayCwd)) {
                throw new IOException("replay working directory does not exist: " + replayCwd);
            }
            if (generatedParseRoot != null && !Files.isDirectory(generatedParseRoot)) {
                throw new IOException("generated parse root does not exist: " + generatedParseRoot);
            }

            System.setProperty(WIRE_PROPERTY, protocol);
            System.setProperty(WORKING_DIRECTORY_PROPERTY, replayCwd.toString());
            System.setProperty(PARSE_ID_PROPERTY, string(operation, "parse_id", ""));
            System.setProperty(STANDARD_PROPERTY, string(operation, "standard", ""));
            System.setProperty(METRICS_PROPERTY, Boolean.toString(phase.equals("diagnostic")));
            System.setProperty(METRICS_DEBUG_PROPERTY, Boolean.toString(phase.equals("diagnostic")));
            System.setOut(QUIET);
            System.setErr(diagnosticStream == null ? QUIET : diagnosticStream);

            CodeParser parser = CodeParser.newInstance();
            configureParser(parser, operation, parserConfig, dumperFolder, generatedParseRoot, cacheMode);

            if (phase.equals("measure")) {
                long start = System.nanoTime();
                App app = parser.parse(List.of(source.toFile()), compilerOptions);
                elapsedNanos = System.nanoTime() - start;
                if (app == null) {
                    throw new IllegalStateException("CodeParser returned no App");
                }
            } else {
                App app = parser.parse(List.of(source.toFile()), compilerOptions);
                if (app == null) {
                    throw new IllegalStateException("CodeParser returned no App");
                }
                if (phase.equals("fidelity")) {
                    addAstFidelity(result, app);
                }
            }
            success = true;
        } catch (Throwable throwable) {
            error = throwable.getClass().getName() + ": " + String.valueOf(throwable.getMessage());
        } finally {
            System.setOut(oldOut);
            System.setErr(oldErr);
            if (diagnosticStream != null) {
                diagnosticStream.flush();
                diagnosticStream.close();
            }
        }

        result.addProperty("valid", success);
        result.addProperty("elapsed_ms", phase.equals("measure") ? elapsedNanos / 1_000_000.0 : 0.0);
        result.addProperty("error", error);
        if (diagnostics != null) {
            JsonArray metrics = extractMetrics(diagnostics.toString(StandardCharsets.UTF_8));
            for (JsonElement metric : metrics) {
                metric.getAsJsonObject().remove("explicit_gc_disabled");
            }
            result.add("metrics", metrics);
            if (metrics.size() != 1) {
                result.addProperty("valid", false);
                result.addProperty("validity_reason", "expected one per-source parser metric, found " + metrics.size());
            } else if (operation.has("expected_native_argv")) {
                List<String> expectedArgv = stringList(operation.getAsJsonArray("expected_native_argv"));
                List<String> actualArgv = normalizedDiagnosticArgv(metrics.get(0).getAsJsonObject(), protocol);
                result.addProperty("argv_match", expectedArgv.equals(actualArgv));
                if (!expectedArgv.equals(actualArgv)) {
                    result.addProperty("valid", false);
                    result.addProperty("validity_reason", "effective native compiler argv differs from capture");
                    result.add("expected_native_argv", GSON.toJsonTree(expectedArgv));
                    result.add("actual_native_argv", GSON.toJsonTree(actualArgv));
                }
            }
        }
        return result;
    }

    private static void configureParser(CodeParser parser, JsonObject operation, JsonObject config,
            Path dumperFolder, Path generatedParseRoot, String cacheMode) {
        parser.set(CodeParser.DUMPER_FOLDER, dumperFolder.toFile());
        parser.set(CodeParser.AST_DUMP_CACHE, cacheMode.equals("warm"));
        if (generatedParseRoot != null) {
            parser.set(CodeParser.GENERATED_PARSE_ROOT, generatedParseRoot.toFile());
        }

        // This guard encloses the legacy heap-utilization call to
        // SpecsSystem.getUsedMemory(true), which explicitly requests GC.
        parser.set(CodeParser.SHOW_EXEC_INFO, false);
        parser.set(CodeParser.SHOW_CLANG_DUMP, false);
        parser.set(CodeParser.SHOW_CLAVA_AST, false);
        parser.set(CodeParser.SHOW_CODE, false);
        parser.set(ParallelCodeParser.PARALLEL_PARSING, false);
        parser.set(ParallelCodeParser.PARSING_NUM_THREADS, 1);

        setBoolean(config, "use_custom_resources", parser, CodeParser.USE_CUSTOM_RESOURCES);
        setString(config, "cuda_gpu_arch", parser, CodeParser.CUDA_GPU_ARCH);
        setString(config, "cuda_path", parser, CodeParser.CUDA_PATH);
        setInteger(config, "system_includes_threshold", parser, ParallelCodeParser.SYSTEM_INCLUDES_THRESHOLD);
        setBoolean(config, "continue_on_parsing_errors", parser, ParallelCodeParser.CONTINUE_ON_PARSING_ERRORS);
        setBoolean(config, "clean", parser, CodeParser.CLEAN);
        setBoolean(config, "syntax_only", parser, ParallelCodeParser.SYNTAX_ONLY);

        String libcMode = string(operation, "effective_libc_mode",
                string(config, "libc_cxx_mode", "BUILTIN_AND_LIBC"));
        parser.set(ClangAstKeys.LIBC_CXX_MODE, LibcMode.valueOf(libcMode));
    }

    private static void setBoolean(JsonObject values, String key, CodeParser parser,
            org.suikasoft.jOptions.Datakey.DataKey<Boolean> dataKey) {
        if (values.has(key) && !values.get(key).isJsonNull()) {
            parser.set(dataKey, values.get(key).getAsBoolean());
        }
    }

    private static void setString(JsonObject values, String key, CodeParser parser,
            org.suikasoft.jOptions.Datakey.DataKey<String> dataKey) {
        if (values.has(key) && !values.get(key).isJsonNull()) {
            parser.set(dataKey, values.get(key).getAsString());
        }
    }

    private static void setInteger(JsonObject values, String key, CodeParser parser,
            org.suikasoft.jOptions.Datakey.DataKey<Integer> dataKey) {
        if (values.has(key) && !values.get(key).isJsonNull()) {
            parser.set(dataKey, values.get(key).getAsInt());
        }
    }

    private static JsonObject runCcacheOperation(JsonObject operation, long scheduleLine) {
        JsonObject result = identity(operation, scheduleLine);
        result.addProperty("record_type", "ccache");
        String operationName = requiredString(operation, "operation");
        String cacheDirectory = requiredString(operation, "cache_directory");
        List<String> command = new ArrayList<>(List.of("ccache", "-d", cacheDirectory,
                operationName.equals("ccache_zero") ? "--zero-stats" : "--show-stats"));
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().put("LC_ALL", "C");
            Process process = builder.redirectErrorStream(true).start();
            String output;
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                output = reader.lines().reduce("", (left, right) -> left + right + "\n");
            }
            int status = process.waitFor();
            result.addProperty("valid", status == 0);
            result.addProperty("command_status", status);
            result.addProperty("output", output);
        } catch (Exception exception) {
            result.addProperty("valid", false);
            result.addProperty("error", exception.getClass().getName() + ": " + exception.getMessage());
        }
        return result;
    }

    private static JsonArray extractMetrics(String output) {
        JsonArray metrics = new JsonArray();
        for (String line : output.split("\\R")) {
            int position = line.indexOf("CLAVA_AST_METRIC ");
            if (position < 0) {
                position = line.indexOf("PROTOBUF_METRIC ");
            }
            if (position < 0) {
                continue;
            }
            int start = line.indexOf('{', position);
            if (start < 0) {
                continue;
            }
            try {
                JsonElement metric = JsonParser.parseString(line.substring(start));
                if (metric.isJsonObject()) {
                    metrics.add(metric);
                }
            } catch (RuntimeException ignored) {
                // Keep invalid metrics out of a valid diagnostic row.
            }
        }
        return metrics;
    }

    private static List<String> normalizedDiagnosticArgv(JsonObject metric, String protocol) {
        JsonArray values = metric.getAsJsonArray("native_argv_debug");
        if (values == null) {
            throw new IllegalArgumentException("diagnostic metric lacks native_argv_debug");
        }

        List<String> normalized = new ArrayList<>();
        boolean foundOutput = false;
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i).getAsString();
            if (value.equals("-o")) {
                if (i + 1 >= values.size()) {
                    throw new IllegalArgumentException("native argv has -o without an output path");
                }
                i++;
                foundOutput = true;
                continue;
            }
            if (protocol.equals("text") && value.equals("-ast-dump-format=text")) {
                continue;
            }
            normalized.add(value);
        }
        if (!foundOutput) {
            throw new IllegalArgumentException("native argv does not contain -o output path");
        }
        return normalized;
    }

    private static void addAstFidelity(JsonObject result, App app) throws Exception {
        String generatedCode = app.getCode();
        byte[] codeBytes = generatedCode.getBytes(StandardCharsets.UTF_8);
        result.addProperty("generated_code_sha256", sha256(codeBytes));
        result.addProperty("generated_code_bytes", codeBytes.length);

        List<TranslationUnit> units = app.getTranslationUnits();
        if (units.size() != 1) {
            throw new IllegalStateException("expected one translation unit, got " + units.size());
        }

        Map<String, Integer> kindCounts = new TreeMap<>();
        Set<ClavaNode> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        ArrayDeque<ClavaNode> pending = new ArrayDeque<>();
        pending.add(units.get(0));
        while (!pending.isEmpty()) {
            ClavaNode node = pending.removeLast();
            if (!visited.add(node)) {
                continue;
            }
            kindCounts.merge(node.getClass().getName(), 1, Integer::sum);
            pending.addAll(node.getChildren());
        }

        JsonObject kinds = new JsonObject();
        kindCounts.forEach(kinds::addProperty);
        result.addProperty("ast_root_kind", units.get(0).getClass().getName());
        result.addProperty("ast_node_count", visited.size());
        result.add("ast_node_kind_counts", kinds);
        result.addProperty("ast_node_kind_counts_sha256", sha256(GSON.toJson(kinds).getBytes(StandardCharsets.UTF_8)));
    }

    private static JsonObject identity(JsonObject operation, long scheduleLine) {
        JsonObject result = new JsonObject();
        result.addProperty("schedule_line", scheduleLine);
        for (String field : List.of("phase", "suite", "input_id", "source_label", "event_id", "protocol",
                "cache_mode", "parse_id", "standard", "options_sha256")) {
            if (operation.has(field)) {
                result.add(field, operation.get(field).deepCopy());
            }
        }
        if (operation.has("repeat")) {
            result.add("repeat", operation.get("repeat").deepCopy());
        }
        return result;
    }

    private static List<String> stringList(JsonArray values) {
        if (values == null) {
            throw new IllegalArgumentException("schedule operation needs compiler_options");
        }
        List<String> result = new ArrayList<>(values.size());
        for (JsonElement value : values) {
            result.add(value.getAsString());
        }
        return result;
    }

    private static Map<String, String> parseArgs(String[] args) {
        if (args.length % 2 != 0) {
            throw new IllegalArgumentException("expected --name value pairs");
        }
        Map<String, String> result = new java.util.LinkedHashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            if (!args[i].startsWith("--")) {
                throw new IllegalArgumentException("invalid argument: " + args[i]);
            }
            result.put(args[i].substring(2), args[i + 1]);
        }
        return result;
    }

    private static String required(Map<String, String> args, String name) {
        String value = args.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing --" + name);
        }
        return value;
    }

    private static String requiredString(JsonObject object, String name) {
        String value = string(object, name, "");
        if (value.isBlank()) {
            throw new IllegalArgumentException("missing " + name);
        }
        return value;
    }

    private static String string(JsonObject object, String name, String fallback) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }

    private static String sha256(Path path) throws Exception {
        return sha256(Files.readAllBytes(path));
    }

    private static String sha256(byte[] contents) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(contents);
        return HexFormat.of().formatHex(digest);
    }
}
