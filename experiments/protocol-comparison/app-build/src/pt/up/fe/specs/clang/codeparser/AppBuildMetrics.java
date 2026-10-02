package pt.up.fe.specs.clang.codeparser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.suikasoft.jOptions.Datakey.DataKey;
import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

/** Emits per-call measurements for the temporary parser shadow used by the App-build matrix. */
public final class AppBuildMetrics {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final AtomicLong CALL_ORDINAL = new AtomicLong();
    private static final Object WRITE_LOCK = new Object();
    private static final List<ContextStamp> CONTEXTS = new ArrayList<>();
    private static long nextContextId = 1L;

    private static String metricsPath;
    private static String suite;
    private static String stage;
    private static String mode;
    private static String repeat;
    private static String stageRoot;
    private static String overlayJar;
    private static String overlaySha256;
    private static String helperClassOrigin;
    private static String parserClassOrigin;
    private static List<String> jvmInputArguments = Collections.emptyList();
    private static long jvmMaxMemoryBytes;
    private static boolean initialized;

    private AppBuildMetrics() {
    }

    /** Called from the shadow parser's class initializer, outside every measured parse call. */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        metricsPath = environment("APP_BUILD_METRICS_PATH");
        suite = environment("APP_BUILD_SUITE");
        stage = environment("APP_BUILD_STAGE");
        mode = environment("APP_BUILD_MODE");
        repeat = environment("APP_BUILD_REPEAT");
        stageRoot = environment("APP_BUILD_STAGE_ROOT");
        overlayJar = environment("APP_BUILD_OVERLAY_JAR");
        helperClassOrigin = classOrigin(AppBuildMetrics.class);
        parserClassOrigin = classOrigin(ParallelCodeParser.class);
        overlaySha256 = sha256IfFile(overlayJar);
        jvmInputArguments = new ArrayList<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
        jvmMaxMemoryBytes = Runtime.getRuntime().maxMemory();
        initialized = true;
    }

    public static long nextCallOrdinal() {
        return CALL_ORDINAL.incrementAndGet();
    }

    public static void recordSuccess(
            ParallelCodeParser parser,
            List<File> inputSources,
            List<String> compilerOptions,
            ClavaContext context,
            long callOrdinal,
            long startNanos,
            long stopNanos,
            boolean originalShowExecInfo,
            boolean appReturnedNull) {
        double elapsedMillis = nanosToMillis(Math.max(0L, stopNanos - startNanos));
        emit(parser, inputSources, compilerOptions, context, callOrdinal,
                elapsedMillis, !appReturnedNull, appReturnedNull,
                null, false, originalShowExecInfo, null);
    }

    public static void recordSyntaxOnly(
            ParallelCodeParser parser,
            List<File> inputSources,
            List<String> compilerOptions,
            ClavaContext context,
            long callOrdinal,
            boolean originalShowExecInfo) {
        emit(parser, inputSources, compilerOptions, context, callOrdinal,
                null, true, true, "syntax_only", true, originalShowExecInfo, null);
    }

    public static void recordFailure(
            ParallelCodeParser parser,
            List<File> inputSources,
            List<String> compilerOptions,
            ClavaContext context,
            long callOrdinal,
            long startNanos,
            long stopNanos,
            boolean originalShowExecInfo,
            boolean syntaxOnly,
            Throwable failure) {
        String reason = "exception:" + failure.getClass().getName();
        String failureMessage = String.valueOf(failure.getMessage());
        emit(parser, inputSources, compilerOptions, context, callOrdinal,
                syntaxOnly ? null : nanosToMillis(Math.max(0L, stopNanos - startNanos)),
                false, null, syntaxOnly ? "syntax_only" : reason,
                syntaxOnly, originalShowExecInfo, reason + ":" + failureMessage);
    }

    @SuppressWarnings("unchecked")
    public static RuntimeException rethrow(Throwable failure) {
        AppBuildMetrics.<RuntimeException>throwUnchecked(failure);
        throw new AssertionError("unreachable");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwUnchecked(Throwable failure) throws T {
        throw (T) failure;
    }

    private static void emit(
            ParallelCodeParser parser,
            List<File> inputSources,
            List<String> compilerOptions,
            ClavaContext context,
            long callOrdinal,
            Double elapsedMillis,
            boolean valid,
            Boolean appReturnedNull,
            String excludedReason,
            boolean syntaxOnly,
            boolean originalShowExecInfo,
            String failure) {
        if (metricsPath == null || metricsPath.isBlank()) {
            return;
        }

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("schema_version", 1);
        row.put("suite", suite);
        row.put("stage", stage);
        row.put("mode", mode);
        row.put("repeat", repeat);
        row.put("call_ordinal", callOrdinal);
        row.put("elapsed_ms", elapsedMillis);
        row.put("valid", valid);
        row.put("app_returned_null", appReturnedNull);
        row.put("excluded_reason", excludedReason);
        row.put("syntax_only", syntaxOnly);
        if (failure != null) {
            row.put("failure_reason", failure);
        }

        ContextStamp contextStamp = contextStamp(context, callOrdinal);
        row.put("context_id", contextStamp.id);
        row.put("context_first_ordinal", contextStamp.firstOrdinal);
        row.put("input_sources", inputPaths(inputSources));
        List<Map<String, Object>> sourceMetadata = resolvedSources(inputSources);
        row.put("resolved_sources", sourceMetadata);
        row.put("compiler_options", compilerOptions == null
                ? Collections.emptyList() : new ArrayList<>(compilerOptions));
        Map<String, Object> config = parserConfig(parser, originalShowExecInfo);
        row.put("parser_config", config);
        List<String> configErrors = metadataErrors(sourceMetadata, config);
        row.put("metadata_complete", configErrors.isEmpty());
        if (!configErrors.isEmpty()) {
            row.put("valid", false);
            if (excludedReason == null) {
                row.put("excluded_reason", "metadata_error");
            }
            row.put("metadata_errors", configErrors);
        }
        row.put("parser_class_origin", parserClassOrigin);
        row.put("helper_class_origin", helperClassOrigin);
        row.put("overlay_jar", overlayJar);
        row.put("overlay_sha256", overlaySha256);
        row.put("stage_root", stageRoot);
        row.put("working_directory", Path.of("").toAbsolutePath().normalize().toString());
        row.put("jvm_input_arguments", jvmInputArguments);
        row.put("jvm_max_memory_bytes", jvmMaxMemoryBytes);

        try {
            Path destination = Path.of(metricsPath).toAbsolutePath().normalize();
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            byte[] line = (GSON.toJson(row) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
            synchronized (WRITE_LOCK) {
                Files.write(destination, line, StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not append App-build metrics to " + metricsPath, exception);
        }
    }

    private static List<String> inputPaths(List<File> inputSources) {
        if (inputSources == null) {
            return Collections.emptyList();
        }
        List<String> paths = new ArrayList<>(inputSources.size());
        for (File input : inputSources) {
            paths.add(input == null ? null : input.getPath());
        }
        return paths;
    }

    private static List<Map<String, Object>> resolvedSources(List<File> inputSources) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (inputSources == null) {
            return result;
        }

        try {
            Map<String, File> sourceMap = SpecsIo.getFileMap(inputSources, SourceType.getPermittedExtensions());
            List<String> parserPaths = new ArrayList<>(sourceMap.keySet());
            Collections.sort(parserPaths);
            for (String parserPath : parserPaths) {
                File parsedFile = new File(parserPath);
                Map<String, Object> source = new LinkedHashMap<>();
                source.put("path", parserPath);
                source.put("absolute_path", parsedFile.getAbsoluteFile().toPath().normalize().toString());
                try {
                    source.put("sha256", sha256(parsedFile.toPath()));
                } catch (IOException exception) {
                    source.put("sha256", null);
                    source.put("sha256_error", exception.toString());
                }
                result.add(source);
            }
        } catch (RuntimeException exception) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("resolution_error", exception.toString());
            result.add(error);
        }
        return result;
    }

    private static List<String> metadataErrors(
            List<Map<String, Object>> sourceMetadata, Map<String, Object> parserConfig) {
        List<String> errors = new ArrayList<>();
        for (Map<String, Object> source : sourceMetadata) {
            for (String key : new String[]{"resolution_error", "sha256_error"}) {
                Object error = source.get(key);
                if (error != null) {
                    errors.add("resolved_sources." + key + ":" + error);
                }
            }
            if (source.containsKey("sha256") && source.get("sha256") == null) {
                errors.add("resolved_sources.sha256:null:" + source.get("path"));
            }
        }
        for (Map.Entry<String, Object> entry : parserConfig.entrySet()) {
            if (entry.getKey().endsWith("_error")) {
                errors.add("parser_config." + entry.getKey() + ":" + entry.getValue());
            }
        }
        return errors;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Map<String, Object> parserConfig(ParallelCodeParser parser, boolean originalShowExecInfo) {
        Map<String, Object> result = new TreeMap<>();
        for (Class<?> type : new Class<?>[]{CodeParser.class, ParallelCodeParser.class}) {
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !DataKey.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    DataKey key = (DataKey) field.get(null);
                    if (!parser.hasValue(key) && "GENERATED_PARSE_ROOT".equals(field.getName())) {
                        result.put(field.getName(), null);
                        continue;
                    }
                    result.put(field.getName(), printable(parser.get(key)));
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    result.put(field.getName() + "_error", exception.toString());
                }
            }
        }
        try {
            result.put("LIBC_CXX_MODE", printable(parser.get(ClangAstKeys.LIBC_CXX_MODE)));
        } catch (RuntimeException exception) {
            result.put("LIBC_CXX_MODE_error", exception.toString());
        }
        result.put("SHOW_EXEC_INFO", originalShowExecInfo);
        return result;
    }

    private static Object printable(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof File) {
            return ((File) value).getPath();
        }
        if (value instanceof Path) {
            return value.toString();
        }
        if (value instanceof Iterable<?>) {
            List<Object> result = new ArrayList<>();
            for (Object item : (Iterable<?>) value) {
                result.add(printable(item));
            }
            return result;
        }
        if (value instanceof Map<?, ?>) {
            Map<String, Object> result = new TreeMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                result.put(String.valueOf(entry.getKey()), printable(entry.getValue()));
            }
            return result;
        }
        return String.valueOf(value);
    }

    private static ContextStamp contextStamp(ClavaContext context, long ordinal) {
        if (context == null) {
            return new ContextStamp(null, "context-null-" + ordinal, ordinal);
        }
        synchronized (CONTEXTS) {
            ContextStamp existing = null;
            Iterator<ContextStamp> iterator = CONTEXTS.iterator();
            while (iterator.hasNext()) {
                ContextStamp candidate = iterator.next();
                ClavaContext candidateContext = candidate.context.get();
                if (candidateContext == null) {
                    iterator.remove();
                } else if (candidateContext == context) {
                    existing = candidate;
                }
            }
            if (existing != null) {
                existing.firstOrdinal = Math.min(existing.firstOrdinal, ordinal);
                return existing;
            }
            ContextStamp created = new ContextStamp(
                    new WeakReference<>(context), "context-" + nextContextId++, ordinal);
            CONTEXTS.add(created);
            return created;
        }
    }

    private static String classOrigin(Class<?> type) {
        try {
            URL location = type.getProtectionDomain().getCodeSource().getLocation();
            return location == null ? null : location.toExternalForm();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String environment(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value;
    }

    private static String sha256IfFile(String path) {
        if (path == null) {
            return null;
        }
        try {
            return sha256(Path.of(path));
        } catch (IOException | RuntimeException exception) {
            return null;
        }
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest.digest()) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static final class ContextStamp {
        private final WeakReference<ClavaContext> context;
        private final String id;
        private long firstOrdinal;

        private ContextStamp(WeakReference<ClavaContext> context, String id, long firstOrdinal) {
            this.context = context;
            this.id = id;
            this.firstOrdinal = firstOrdinal;
        }
    }
}
