/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.dumper;

import org.suikasoft.jOptions.Interfaces.DataStore;
import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.language.Standard;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Opt-in snapshots of the inputs and effective settings used by parser calls. */
public final class ClangAstCorpusCapture {

    static final String ROOT_ENV = "CLAVA_AST_CORPUS_CAPTURE_DIR";
    static final String SUITE_ENV = "CLAVA_AST_CORPUS_SUITE";
    private static final String ROOTFS = "rootfs";
    private static final List<String> CAPTURED_ENVIRONMENT = List.of(
            "PATH", "HOME", "TMPDIR", "TMP", "TEMP", "XDG_CACHE_HOME", "LANG", "LC_ALL",
            "CPATH", "C_INCLUDE_PATH", "CPLUS_INCLUDE_PATH", "OBJC_INCLUDE_PATH", "SDKROOT",
            "CUDA_HOME", "CUDA_PATH", "INCLUDE", "LIB", "LIBRARY_PATH", "CCACHE_DIR",
            "CCACHE_BASEDIR", "CCACHE_COMPILERTYPE", "CCACHE_DEPEND", "CCACHE_NOHASHDIR",
            "CCACHE_NOCOMPRESS", "CCACHE_CONFIGPATH", "CCACHE_CPP2", "CCACHE_SLOPPINESS");

    private ClangAstCorpusCapture() {
    }

    public static boolean isEnabled() {
        String value = System.getenv(ROOT_ENV);
        return value != null && !value.isBlank();
    }

    public static boolean isCorpusCollectionRun() {
        return isEnabled();
    }

    public static String beginCodeParserCall(List<File> inputSources, List<File> sources,
            List<String> compilerOptions, CodeParser parser, String effectiveLibcMode,
            File clangExecutable, List<String> builtinIncludes, File systemResourceDir) {
        if (!isEnabled()) {
            return null;
        }

        String callId = UUID.randomUUID().toString();
        Map<String, Object> call = new LinkedHashMap<>();
        call.put("event_id", callId);
        call.put("event_type", "code_parser_call");
        call.put("captured_at_epoch_ms", System.currentTimeMillis());
        call.put("suite", System.getenv().getOrDefault(SUITE_ENV, "unknown"));
        call.put("original_cwd", Path.of("").toAbsolutePath().normalize().toString());
        call.put("input_sources", inputSources.stream().map(file -> file.getAbsoluteFile().toPath()
                .normalize().toString()).toList());
        call.put("sources", sources.stream().map(ClangAstCorpusCapture::captureFile).toList());
        call.put("compiler_options", new ArrayList<>(compilerOptions));
        call.put("code_parser_options", codeParserOptions(parser));
        call.put("effective_libc_mode", effectiveLibcMode);
        Map<String, Object> resources = new LinkedHashMap<>();
        resources.put("clang_executable", absolutePath(clangExecutable));
        resources.put("builtin_includes", new ArrayList<>(builtinIncludes));
        resources.put("system_resource_dir", absolutePath(systemResourceDir));
        call.put("runtime_resources", resources);
        call.put("path_map", pathMap());
        writeRawRecord("calls", callId, call);
        return callId;
    }

    public static void recordNativeInvocation(String callId, File sourceFile, String parseId, Standard standard,
            List<String> compilerArgv, List<String> processArgv, Path workingDirectory,
            Map<String, String> processEnvironment, File dependencyFile,
            AstWireBenchmarkIdentity.Identity benchmarkIdentity, DataStore dataStore,
            List<String> effectiveSystemIncludes, File generatedParseRoot) {
        if (!isEnabled()) {
            return;
        }

        String eventId = UUID.randomUUID().toString();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event_id", eventId);
        event.put("event_type", "native_parse");
        event.put("captured_at_epoch_ms", System.currentTimeMillis());
        event.put("code_parser_call_id", callId);
        event.put("suite", System.getenv().getOrDefault(SUITE_ENV, "unknown"));
        event.put("source", captureFile(sourceFile));
        event.put("parse_id", parseId);
        event.put("standard", standard.getFlag());
        event.put("test_id", benchmarkIdentity == null ? null : benchmarkIdentity.testId());
        event.put("tester_invocation", benchmarkIdentity == null ? null : benchmarkIdentity.testerInvocation());
        event.put("parse_pass", benchmarkIdentity == null ? null : benchmarkIdentity.parsePass());
        event.put("resource_key", benchmarkIdentity == null ? null : benchmarkIdentity.resourceKey());
        event.put("data_store_options", dataStoreOptions(dataStore));
        event.put("effective_system_includes", new ArrayList<>(effectiveSystemIncludes));
        event.put("generated_parse_root", absolutePath(generatedParseRoot));
        event.put("argv", new ArrayList<>(compilerArgv));
        event.put("args_sha256", sha256(json(normalizeArguments(compilerArgv, sourceFile, workingDirectory))
                .getBytes(StandardCharsets.UTF_8)));
        event.put("raw_args_sha256", sha256(json(compilerArgv).getBytes(StandardCharsets.UTF_8)));
        event.put("process_argv", new ArrayList<>(processArgv));
        event.put("original_cwd", Path.of("").toAbsolutePath().normalize().toString());
        event.put("working_directory", workingDirectory.toAbsolutePath().normalize().toString());
        event.put("replay_cwd", replayPath(workingDirectory));
        event.put("effective_environment", capturedEnvironment(processEnvironment));
        event.put("environment_delta", environmentDelta(processEnvironment));
        event.put("dependency_file", dependencyFile.getAbsolutePath());
        event.put("dependencies", captureDependencies(dependencyFile, workingDirectory));
        event.put("path_map", pathMap());
        writeRawRecord("native", eventId, event);
    }

    private static Map<String, Object> codeParserOptions(CodeParser parser) {
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("use_custom_resources", parser.get(CodeParser.USE_CUSTOM_RESOURCES));
        options.put("cuda_gpu_arch", parser.get(CodeParser.CUDA_GPU_ARCH));
        options.put("cuda_path", parser.get(CodeParser.CUDA_PATH));
        options.put("parallel_parsing", parser.get(ParallelCodeParser.PARALLEL_PARSING));
        options.put("parsing_num_threads", parser.get(ParallelCodeParser.PARSING_NUM_THREADS));
        options.put("system_includes_threshold", parser.get(ParallelCodeParser.SYSTEM_INCLUDES_THRESHOLD));
        options.put("continue_on_parsing_errors", parser.get(ParallelCodeParser.CONTINUE_ON_PARSING_ERRORS));
        options.put("libc_cxx_mode", parser.get(ClangAstKeys.LIBC_CXX_MODE).name());
        options.put("dumper_folder", absolutePath(parser.get(CodeParser.DUMPER_FOLDER)));
        options.put("ast_dump_cache", parser.get(CodeParser.AST_DUMP_CACHE));
        options.put("show_exec_info", parser.get(CodeParser.SHOW_EXEC_INFO));
        options.put("show_clang_dump", parser.get(CodeParser.SHOW_CLANG_DUMP));
        options.put("show_clava_ast", parser.get(CodeParser.SHOW_CLAVA_AST));
        options.put("show_code", parser.get(CodeParser.SHOW_CODE));
        options.put("clean", parser.get(CodeParser.CLEAN));
        options.put("syntax_only", parser.get(ParallelCodeParser.SYNTAX_ONLY));
        options.put("generated_parse_root", parser.hasValue(CodeParser.GENERATED_PARSE_ROOT)
                ? absolutePath(parser.get(CodeParser.GENERATED_PARSE_ROOT)) : null);
        return options;
    }

    private static Map<String, Object> dataStoreOptions(DataStore dataStore) {
        Map<String, Object> options = new LinkedHashMap<>();
        Standard standard = dataStore.hasValue(ClavaOptions.STANDARD)
                ? dataStore.get(ClavaOptions.STANDARD) : null;
        options.put("standard", standard == null ? null : standard.getFlag());
        options.put("flags", dataStore.get(ClavaOptions.FLAGS));
        options.put("flags_list", new ArrayList<>(dataStore.get(ClavaOptions.FLAGS_LIST)));
        options.put("uses_cilk", dataStore.get(ClangAstKeys.USES_CILK));
        options.put("libc_cxx_mode", dataStore.get(ClangAstKeys.LIBC_CXX_MODE).name());
        options.put("ignore_header_includes", dataStore.get(ClangAstKeys.IGNORE_HEADER_INCLUDES).toString());
        return options;
    }

    private static List<Map<String, Object>> captureDependencies(File dependencyFile, Path workingDirectory) {
        try {
            if (!dependencyFile.isFile()) {
                throw new IOException("dependency file was not produced: " + dependencyFile);
            }

            Set<Path> dependencies = parseMakeDependencies(Files.readString(dependencyFile.toPath()));
            List<Map<String, Object>> files = new ArrayList<>();
            for (Path dependency : dependencies) {
                Path absolute = dependency.isAbsolute() ? dependency.normalize()
                        : workingDirectory.resolve(dependency).normalize();
                if (!Files.isRegularFile(absolute)) {
                    throw new IOException("compiler dependency is missing or not a file: " + absolute);
                }
                files.add(captureFile(absolute.toFile()));
            }
            return files;
        } catch (IOException e) {
            throw new RuntimeException("Could not capture Clang dependency closure", e);
        }
    }

    private static List<String> normalizeArguments(List<String> arguments, File sourceFile,
            Path workingDirectory) {
        Path absoluteSource = sourceFile.getAbsoluteFile().toPath().normalize();
        List<String> normalized = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            String argument = arguments.get(i);
            if (argument.equals("-o") && i + 1 < arguments.size()) {
                i++;
                continue;
            }
            if (argument.startsWith("-id=")) {
                normalized.add("-id=<id>");
                continue;
            }
            if (isSourceArgument(argument, absoluteSource, workingDirectory)) {
                normalized.add("<source>");
                continue;
            }
            normalized.add(argument);
        }
        return normalized;
    }

    private static boolean isSourceArgument(String argument, Path source, Path workingDirectory) {
        try {
            Path candidate = Path.of(argument);
            if (!candidate.isAbsolute()) {
                candidate = workingDirectory.resolve(candidate);
            }
            return candidate.normalize().equals(source);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static Set<Path> parseMakeDependencies(String contents) {
        String flattened = contents.replace("\\\r\n", " ").replace("\\\n", " ");
        int separator = firstUnescapedColon(flattened);
        if (separator < 0) {
            throw new IllegalArgumentException("dependency file has no target separator");
        }

        String body = flattened.substring(separator + 1);
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < body.length(); i++) {
            char current = body.charAt(i);
            if (escaped) {
                token.append(current);
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (Character.isWhitespace(current)) {
                addToken(tokens, token);
            } else {
                token.append(current);
            }
        }
        if (escaped) {
            token.append('\\');
        }
        addToken(tokens, token);

        Set<Path> paths = new LinkedHashSet<>();
        for (String value : tokens) {
            if (!value.isEmpty()) {
                paths.add(Path.of(value));
            }
        }
        return paths;
    }

    private static int firstUnescapedColon(String line) {
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == ':') {
                return i;
            }
        }
        return -1;
    }

    private static void addToken(List<String> tokens, StringBuilder token) {
        if (token.length() > 0) {
            tokens.add(token.toString());
            token.setLength(0);
        }
    }

    private static Map<String, Object> captureFile(File file) {
        Path path = file.getAbsoluteFile().toPath().normalize();
        try {
            byte[] contents = Files.readAllBytes(path);
            String digest = sha256(contents);
            Path blob = root().resolve("blobs").resolve("sha256").resolve(digest);
            Files.createDirectories(blob.getParent());
            try {
                Files.write(blob, contents, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (FileAlreadyExistsException ignored) {
                // The same content may be used by multiple parse events.
            }

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("original_path", path.toString());
            snapshot.put("replay_path", replayPath(path));
            snapshot.put("sha256", digest);
            snapshot.put("blob", Path.of("blobs", "sha256", digest).toString().replace('\\', '/'));
            snapshot.put("size_bytes", contents.length);
            return snapshot;
        } catch (IOException e) {
            throw new RuntimeException("Could not snapshot corpus input '" + path + "'", e);
        }
    }

    private static String replayPath(Path path) {
        String absolute = path.toAbsolutePath().normalize().toString();
        if (!absolute.startsWith(File.separator)) {
            throw new IllegalArgumentException("expected absolute input path: " + absolute);
        }
        return Path.of(ROOTFS).resolve(absolute.substring(1)).toString().replace('\\', '/');
    }

    private static List<Map<String, Object>> pathMap() {
        return List.of(Map.of("original_root", File.separator, "replay_root", ROOTFS));
    }

    private static Map<String, Object> capturedEnvironment(Map<String, String> environment) {
        Map<String, Object> captured = new LinkedHashMap<>();
        for (String name : CAPTURED_ENVIRONMENT) {
            String value = environment.get(name);
            if (value != null) {
                captured.put(name, value);
            }
        }
        return captured;
    }

    private static Map<String, Object> environmentDelta(Map<String, String> environment) {
        Map<String, Object> delta = new LinkedHashMap<>();
        Map<String, String> inherited = System.getenv();
        for (String name : CAPTURED_ENVIRONMENT) {
            String current = environment.get(name);
            if (!java.util.Objects.equals(current, inherited.get(name))) {
                delta.put(name, current);
            }
        }
        return delta;
    }

    private static void writeRawRecord(String directory, String eventId, Map<String, Object> record) {
        Path destination = root().resolve("raw").resolve(directory).resolve(eventId + ".json");
        try {
            Files.createDirectories(destination.getParent());
            Files.writeString(destination, json(record) + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException("Could not write parser corpus event '" + destination + "'", e);
        }
    }

    private static Path root() {
        String value = System.getenv(ROOT_ENV);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("" + ROOT_ENV + " is not set");
        }
        return Path.of(value).toAbsolutePath().normalize();
    }

    private static String absolutePath(File file) {
        return file == null ? null : file.getAbsoluteFile().toPath().normalize().toString();
    }

    private static String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String json(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return jsonString(string);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            List<String> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                entries.add(jsonString(String.valueOf(entry.getKey())) + ":" + json(entry.getValue()));
            }
            return "{" + String.join(",", entries) + "}";
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> elements = new ArrayList<>();
            for (Object element : iterable) {
                elements.add(json(element));
            }
            return "[" + String.join(",", elements) + "]";
        }
        return jsonString(value.toString());
    }

    private static String jsonString(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            switch (current) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (current < 0x20) {
                        escaped.append(String.format(Locale.ROOT, "\\u%04x", (int) current));
                    } else {
                        escaped.append(current);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }
}
