import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.lang.reflect.Array;

import java.math.BigInteger;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.DataStore.DataClass;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.dumper.ClangAstParser;
import pt.up.fe.specs.clang.transforms.TreeTransformer;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.UnaryOperator;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.parsing.snippet.TextParser;
import pt.up.fe.specs.clava.utils.Typable;

/**
 * Measures FlatReader import, real Clava TU/App construction, traversal and code generation.
 *
 * <p>The input syntax is intentionally usable by the native driver:</p>
 *
 * <pre>
 * FlatBenchmark fixture source.c text,eager,lazy text.dump eager.fb lazy.fb
 * FlatBenchmark --fixture=fixture --source=source.c --modes=text,eager,lazy \
 *     text.dump eager.fb lazy.fb
 * </pre>
 *
 * <p>Modes are randomized within each round. Three warmups and twelve measured rounds are
 * used by default; both are configurable with {@code --warmup=N} and {@code --measurements=N}.</p>
 */
final class FlatBenchmark {
    private static final int DEFAULT_WARMUP = 3;
    private static final int DEFAULT_MEASUREMENTS = 12;
    private static final long SHUFFLE_SEED = 720L;

    enum Mode {
        TEXT,
        EAGER,
        LAZY;

        static Mode parse(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
            case "text" -> TEXT;
            case "eager", "flat-eager" -> EAGER;
            case "lazy", "flat-lazy" -> LAZY;
            default -> throw new IllegalArgumentException("Unknown FlatReader mode: " + value);
            };
        }

        String label() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    record Input(String fixture, Path source, LinkedHashMap<Mode, Path> dumps, boolean textZstd,
            int warmup, int measurements) {
    }

    record Scan(String digest, long nodes, long names, long operators, long types) {
    }

    record LoadedApp(App app, boolean payloadMutation, String rawAllDigest) {
    }

    record Counters(long typed, long materialized, long deferred) {
    }

    record Measurement(long parseNanos, long tuNanos, long queryNanos, long codegenNanos,
            Scan scan, Counters afterRead, Counters afterTu, Counters afterQuery, Counters afterCodegen,
            String codeDigest, int codeBytes) {
        long totalNanos() {
            return parseNanos + tuNanos + queryNanos + codegenNanos;
        }

        long typed() {
            return afterCodegen.typed();
        }

        long materialized() {
            return afterCodegen.materialized();
        }

        long deferred() {
            return afterCodegen.deferred();
        }
    }

    private static final class Hasher {
        private final MessageDigest digest;

        Hasher() {
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                throw new AssertionError(e);
            }
        }

        void add(Object value) {
            String text = String.valueOf(value);
            digest.update(text.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
        }

        String finish() {
            byte[] bytes = digest.digest();
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                result.append(String.format(Locale.ROOT, "%02x", value & 0xff));
            }
            return result.toString();
        }
    }

    public static void main(String[] args) throws Exception {
        Input input = parseArgs(args);
        printMetadata(input);
        System.out.println("fixture,source,mode,trial,parse_ms,tu_ms,query_ms,codegen_ms,total_ms,nodes,names,operators,types,typed,materialized,deferred,code_bytes,code_sha256,query_sha256,materialized_read,materialized_tu,materialized_query,materialized_codegen");

        List<Map.Entry<Mode, Path>> modes = new ArrayList<>(input.dumps().entrySet());
        for (int round = -input.warmup(); round < input.measurements(); round++) {
            Collections.shuffle(modes, new Random(SHUFFLE_SEED + round));
            for (Map.Entry<Mode, Path> mode : modes) {
                Measurement measurement = measure(input.source(), mode.getKey(), mode.getValue(), input.textZstd());
                if (round < 0) {
                    continue;
                }

                System.out.printf(Locale.ROOT,
                        "%s,%s,%s,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d,%d,%d,%d,%d,%d,%d,%s,%s,%d,%d,%d,%d%n",
                        csv(input.fixture()), csv(input.source().toString()), mode.getKey().label(), round,
                        millis(measurement.parseNanos()), millis(measurement.tuNanos()), millis(measurement.queryNanos()),
                        millis(measurement.codegenNanos()), millis(measurement.totalNanos()), measurement.scan().nodes(),
                        measurement.scan().names(), measurement.scan().operators(), measurement.scan().types(),
                        measurement.typed(), measurement.materialized(), measurement.deferred(), measurement.codeBytes(),
                        measurement.codeDigest(), measurement.scan().digest(),
                        measurement.afterRead().materialized(), measurement.afterTu().materialized(),
                        measurement.afterQuery().materialized(), measurement.afterCodegen().materialized());
            }
        }
    }

    /** Loads a dump and performs the same construction phase used by the benchmark. */
    static App loadApp(Path source, Mode mode, Path dump, boolean textZstd) throws Exception {
        return loadAppDetailed(source, mode, dump, textZstd).app();
    }

    static LoadedApp loadAppDetailed(Path source, Mode mode, Path dump, boolean textZstd) throws Exception {
        FlatReader.Result result = read(mode, dump, textZstd);
        ClangAstData data = result.data();
        try {
            boolean payloadMutation = payloadMutationBeforeReadProbe(data);
            App app = buildRawApp(data, source);
            String rawAllDigest = allGraphDigest(app);
            postprocess(app);
            return new LoadedApp(app, payloadMutation, rawAllDigest);
        } finally {
            // The TU/App retain node payloads, while the reader's construction indexes are no
            // longer needed. This is deliberately before traversal so lazy lookup retention is
            // visible in the benchmark.
            result.releaseLookup();
        }
    }

    static FlatReader.Result read(Mode mode, Path dump, boolean textZstd) throws Exception {
        return switch (mode) {
        case TEXT -> FlatReader.readText(dump, textZstd);
        case EAGER -> FlatReader.read(dump, false);
        case LAZY -> FlatReader.read(dump, true);
        };
    }

    static App buildApp(ClangAstData data, Path source) {
        App app = buildRawApp(data, source);
        postprocess(app);
        return app;
    }

    private static App buildRawApp(ClangAstData data, Path source) {
        TranslationUnit tu = new ClangAstParser(data, false, DataStore.newInstance("config"))
                .parseTu(source.toFile());
        App app = data.getFactory().app(List.of(tu));
        app.getContext().pushApp(app);
        return app;
    }

    private static void postprocess(App app) {
        // parseTu creates the TU; the production parser then applies these structural passes
        // before Query/getCode consumers see the App.
        new TreeTransformer(ClangAstParser.getPostParsingRules()).transform(app);
        new TextParser(app.getContext()).addElements(app);
        new TreeTransformer(ClangAstParser.getTextParsingRules()).transform(app);
    }

    /**
     * Finds a materialized IntegerLiteral, copies it before the original payload is read, and
     * mutates only the copy. The first original VALUE access then proves that the copy did not
     * alias the deferred payload store. Fixtures without integer literals report false.
     */
    static boolean payloadMutationBeforeReadProbe(ClangAstData data) {
        for (ClavaNode node : data.getClavaNodes().getNodes().values()) {
            if (!(node instanceof IntegerLiteral literal)) {
                continue;
            }

            ClavaNode copy = literal.copy(false, false);
            BigInteger marker = BigInteger.valueOf(987654321L);
            copy.set(IntegerLiteral.VALUE, marker);
            if (!marker.equals(copy.get(IntegerLiteral.VALUE))) throw new AssertionError("Copy discarded edited value");
            BigInteger original = literal.get(IntegerLiteral.VALUE);
            if (marker.equals(original)) {
                throw new AssertionError("IntegerLiteral copy aliases original lazy payload");
            }
            return true;
        }
        return false;
    }

    /**
     * Traverses children and all node-valued fields, then reads representative scalar fields.
     * The graph digest uses encounter-order ordinals in place of source IDs, so it compares
     * graph shape and references without depending on raw ID spelling.
     */
    static Scan scan(App app) {
        List<ClavaNode> nodes = new ArrayList<>();
        nodes.add(app);
        nodes.addAll(app.getDescendantsAndFields());

        IdentityHashMap<ClavaNode, Integer> ordinals = new IdentityHashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            ordinals.put(nodes.get(i), i);
        }

        Hasher hash = new Hasher();
        hash.add("nodes");
        hash.add(nodes.size());
        long names = 0;
        long operators = 0;
        long types = 0;

        for (int i = 0; i < nodes.size(); i++) {
            ClavaNode node = nodes.get(i);
            hash.add(i);
            hash.add(node.getClass().getName());

            if (node instanceof NamedDecl named) {
                names++;
                hash.add("name");
                hash.add(named.getTry(NamedDecl.DECL_NAME).orElse(""));
            }

            if (node instanceof BinaryOperator binary) {
                operators++;
                hash.add("binary-op");
                hash.add(binary.getOp());
            } else if (node instanceof UnaryOperator unary) {
                operators++;
                hash.add("unary-op");
                hash.add(unary.getOp());
            }

            if (node instanceof Typable typable) {
                Type type = typable.getType();
                if (type != null) {
                    types++;
                    hash.add("type");
                    hash.add(type.getTry(Type.TYPE_AS_STRING).orElse(""));
                }
            }

            hash.add("children");
            for (ClavaNode child : node.getChildren()) {
                hash.add(ordinals.getOrDefault(child, -1));
            }

            hash.add("fields");
            for (ClavaNode field : node.getNodeFields()) {
                hash.add(ordinals.getOrDefault(field, -1));
            }
        }

        return new Scan(hash.finish(), nodes.size(), names, operators, types);
    }

    /**
     * Hashes every stored node field after replacing node references with encounter-order
     * ordinals. This is intentionally separate from the timed query digest, whose scalar reads
     * are limited to names, operators and types.
     */
    static String allGraphDigest(App app) {
        List<ClavaNode> nodes = new ArrayList<>();
        nodes.add(app);
        nodes.addAll(app.getDescendantsAndFields());
        IdentityHashMap<ClavaNode, Integer> ordinals = new IdentityHashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            ordinals.put(nodes.get(i), i);
        }

        // Include semantic references nested in compound payloads, which the normal tree walk
        // may not enumerate. No reference is allowed to collapse to an unknown ordinal.
        for (int i = 0; i < nodes.size(); i++) {
            var node = nodes.get(i);
            var keys = new ArrayList<>(node.getDataKeysWithValues());
            keys.sort((a,b) -> a.getName().compareTo(b.getName()));
            for (var key : keys) {
                if (key != ClavaNode.CONTEXT && key != ClavaNode.ORIGIN)
                    collectReferences(node.get(key), nodes, ordinals, new IdentityHashMap<>());
            }
        }

        StringBuilder trace = new StringBuilder();
        Hasher hash = new Hasher();
        for (int i = 0; i < nodes.size(); i++) {
            ClavaNode node = nodes.get(i);
            hash.add(i);
            hash.add(node.getClass().getName());
            List<DataKey<?>> keys = new ArrayList<>(node.getDataKeysWithValues());
            keys.sort((left, right) -> left.getName().compareTo(right.getName()));
            for (DataKey<?> key : keys) {
                // Context and origin are runtime wiring, not parsed graph content. Hashing their
                // object identity would make an otherwise equal graph differ per load.
                if (key == ClavaNode.CONTEXT || key == ClavaNode.ORIGIN || key == ClavaNode.PREVIOUS_ID) {
                    continue;
                }
                if (key == pt.up.fe.specs.clava.ast.decl.CXXMethodDecl.RECORD_ID) {
                    var target = node.get(pt.up.fe.specs.clava.ast.decl.CXXMethodDecl.RECORD);
                    if (!node.get(key).equals(target.getId())) throw new AssertionError("Record ID disagrees with resolved reference");
                    hash.add(key.getName());hash.add(ordinals.get(target));
                    continue;
                }
                if (key == ClavaNode.ID) {
                    hash.add(key.getName());
                    hash.add("normalized-" + i);
                    continue;
                }
                hash.add(key.getName());
                appendValue(hash, node.get(key), ordinals);
                if (System.getProperty("flat.traceFile") != null) {
                    Hasher item = new Hasher();
                    appendValue(item, node.get(key), ordinals);
                    trace.append(i).append(':').append(node.getClass().getSimpleName()).append('.').append(key.getName()).append('=').append(item.finish()).append('\n');
                }
            }
        }
        if (System.getProperty("flat.traceFile") != null) {
            try { Files.writeString(Path.of(System.getProperty("flat.traceFile")),trace); }
            catch (IOException e) { throw new java.io.UncheckedIOException(e); }
        }
        return hash.finish();
    }

    private static void collectReferences(Object value, List<ClavaNode> nodes,
            IdentityHashMap<ClavaNode,Integer> ordinals, IdentityHashMap<Object,Boolean> seen) {
        if (value == null || seen.put(value,true) != null) return;
        if (value instanceof ClavaNode node) {
            if (!ordinals.containsKey(node)) { ordinals.put(node,nodes.size());nodes.add(node); }
        } else if (value instanceof DataClass<?> data) {
            var keys = new ArrayList<>(data.getDataKeysWithValues());
            keys.sort((a,b) -> a.getName().compareTo(b.getName()));
            for (var key : keys) if (key != ClavaNode.CONTEXT)
                collectReferences(data.get(key),nodes,ordinals,seen);
        } else if (value instanceof Optional<?> optional) {
            optional.ifPresent(v -> collectReferences(v,nodes,ordinals,seen));
        } else if (value instanceof Iterable<?> iterable) {
            for (Object v : iterable) collectReferences(v,nodes,ordinals,seen);
        } else if (value instanceof Map<?,?> map) {
            for (var entry : map.entrySet()) {
                collectReferences(entry.getKey(),nodes,ordinals,seen);
                collectReferences(entry.getValue(),nodes,ordinals,seen);
            }
        } else if (value.getClass().isArray()) {
            for (int i=0;i<Array.getLength(value);i++) collectReferences(Array.get(value,i),nodes,ordinals,seen);
        }
    }

    @SuppressWarnings("unchecked")
    private static void appendValue(Hasher hash, Object value, IdentityHashMap<ClavaNode, Integer> ordinals) {
        if (value == null) {
            hash.add("null");
        } else if (value instanceof ClavaNode node) {
            hash.add("node");
            hash.add(ordinals.getOrDefault(node, -1));
        } else if (value instanceof DataClass<?> data) {
            hash.add(value.getClass().getName());
            var keys = new ArrayList<>(data.getDataKeysWithValues());
            keys.sort((a,b) -> a.getName().compareTo(b.getName()));
            for (var key : keys) {
                if (key == ClavaNode.CONTEXT) continue;
                hash.add(key.getName());
                appendValue(hash, data.get(key), ordinals);
            }
        } else if (value instanceof Optional<?> optional) {
            hash.add("optional");
            optional.ifPresentOrElse(item -> appendValue(hash, item, ordinals), () -> hash.add("empty"));
        } else if (value instanceof Iterable<?> iterable) {
            hash.add("iterable");
            for (Object item : iterable) {
                appendValue(hash, item, ordinals);
            }
            hash.add("end-iterable");
        } else if (value instanceof Map<?, ?> map) {
            hash.add("map");
            List<String> entries = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                entries.add(canonicalValue(entry.getKey(), ordinals) + "="
                        + canonicalValue(entry.getValue(), ordinals));
            }
            Collections.sort(entries);
            entries.forEach(hash::add);
            hash.add("end-map");
        } else if (value.getClass().isArray()) {
            hash.add("array");
            for (int i = 0; i < Array.getLength(value); i++) {
                appendValue(hash, Array.get(value, i), ordinals);
            }
            hash.add("end-array");
        } else {
            hash.add(value.getClass().getName());
            hash.add(value);
        }
    }

    private static String canonicalValue(Object value, IdentityHashMap<ClavaNode, Integer> ordinals) {
        if (value == null) {
            return "null";
        }
        if (value instanceof ClavaNode node) {
            return "node#" + ordinals.getOrDefault(node, -1);
        }
        if (value instanceof Optional<?> optional) {
            return optional.map(item -> "some(" + canonicalValue(item, ordinals) + ")")
                    .orElse("empty");
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> values = new ArrayList<>();
            for (Object item : iterable) {
                values.add(canonicalValue(item, ordinals));
            }
            return values.toString();
        }
        if (value instanceof Map<?, ?> map) {
            List<String> values = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                values.add(canonicalValue(entry.getKey(), ordinals) + "="
                        + canonicalValue(entry.getValue(), ordinals));
            }
            Collections.sort(values);
            return values.toString();
        }
        if (value.getClass().isArray()) {
            List<String> values = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) {
                values.add(canonicalValue(Array.get(value, i), ordinals));
            }
            return values.toString();
        }
        return value.getClass().getName() + ":" + String.valueOf(value);
    }

    /** Computes the digest only after the terminal Clava getCode call. */
    static String codeDigest(App app) {
        return digest(app.getCode());
    }

    /**
     * Copies and mutates a node before the first full traversal. The original graph must remain
     * usable afterwards; this catches readers that accidentally expose mutable lookup state.
     */
    static void mutationBeforeReadProbe(App app) {
        ClavaNode candidate = app.getChildren().isEmpty() ? app : app.getChildren().get(0);
        String originalId = candidate.getId();
        ClavaNode copy = candidate.copy(false, false);
        copy.set(ClavaNode.PREVIOUS_ID, "flat-check-mutation");
        if (Objects.equals(originalId, copy.getId())) {
            throw new AssertionError("copy did not receive a distinct normalized id");
        }
        if (!"flat-check-mutation".equals(copy.get(ClavaNode.PREVIOUS_ID))) {
            throw new AssertionError("mutation did not survive copy-before-read");
        }
    }

    static Measurement measure(Path source, Mode mode, Path dump, boolean textZstd) throws Exception {
        long start = System.nanoTime();
        long parseStart = start;
        FlatReader.Result result = read(mode, dump, textZstd);
        long parseEnd = System.nanoTime();
        Counters afterRead = counters(result);

        App app;
        long tuStart = System.nanoTime();
        try {
            app = buildApp(result.data(), source);
        } finally {
            result.releaseLookup();
        }
        long tuEnd = System.nanoTime();
        Counters afterTu = counters(result);

        long queryStart = tuEnd;
        Scan scan = scan(app);
        long queryEnd = System.nanoTime();
        Counters afterQuery = counters(result);

        long codegenStart = queryEnd;
        String code = app.getCode();
        String codeDigest = digest(code);
        int codeBytes = code.getBytes(StandardCharsets.UTF_8).length;
        long codegenEnd = System.nanoTime();
        Counters afterCodegen = counters(result);

        return new Measurement(parseEnd - parseStart, tuEnd - tuStart, queryEnd - queryStart,
                codegenEnd - codegenStart, scan, afterRead, afterTu, afterQuery, afterCodegen,
                codeDigest, codeBytes);
    }

    private static Counters counters(FlatReader.Result result) {
        return new Counters(result.typed(), result.materialized(), result.deferred());
    }

    private static Input parseArgs(String[] args) {
        if (args.length == 0 || contains(args, "--help")) {
            throw new IllegalArgumentException("usage: FlatBenchmark [--fixture=name] [--source=file] "
                    + "[--modes=text,eager,lazy] [--warmup=3] [--measurements=12] "
                    + "fixture source modes files... [--text-zstd[=true|false]]");
        }

        Map<String, String> options = new HashMap<>();
        List<String> positional = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                positional.add(arg);
                continue;
            }
            String option = arg.substring(2);
            int equals = option.indexOf('=');
            if (equals >= 0) {
                options.put(option.substring(0, equals), option.substring(equals + 1));
            } else if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                options.put(option, args[++i]);
            } else {
                options.put(option, "true");
            }
        }

        int position = 0;
        String fixture = options.get("fixture");
        if (fixture == null) {
            fixture = require(positional, position++, "fixture");
        }
        String sourceName = options.get("source");
        if (sourceName == null) {
            sourceName = require(positional, position++, "source");
        }
        String modesName = options.get("modes");
        if (modesName == null) {
            modesName = require(positional, position++, "mode list");
        }

        List<Mode> modes = new ArrayList<>();
        for (String mode : modesName.split(",")) {
            if (!mode.isBlank()) {
                modes.add(Mode.parse(mode.trim()));
            }
        }
        if (modes.isEmpty()) {
            throw new IllegalArgumentException("mode list is empty");
        }

        List<String> files = new ArrayList<>();
        String filesOption = options.get("files");
        if (filesOption != null) {
            Collections.addAll(files, filesOption.split(","));
        } else {
            files.addAll(positional.subList(position, positional.size()));
        }

        LinkedHashMap<Mode, Path> dumps = new LinkedHashMap<>();
        if (files.size() != modes.size()) {
            // Also accept mode=path pairs, which are convenient when one mode is omitted.
            dumps.clear();
            for (String file : files) {
                int equals = file.indexOf('=');
                if (equals < 1) {
                    throw new IllegalArgumentException("expected one dump path per mode; got " + file);
                }
                dumps.put(Mode.parse(file.substring(0, equals)), Path.of(file.substring(equals + 1)));
            }
            if (!dumps.keySet().containsAll(modes) || dumps.size() != modes.size()) {
                throw new IllegalArgumentException("dump paths do not match modes " + modesName);
            }
        } else {
            for (int i = 0; i < modes.size(); i++) {
                dumps.put(modes.get(i), Path.of(files.get(i)));
            }
        }

        int warmup = integerOption(options, "warmup", DEFAULT_WARMUP);
        int measurements = integerOption(options, "measurements", DEFAULT_MEASUREMENTS);
        if (warmup < 0 || measurements <= 0) {
            throw new IllegalArgumentException("warmup must be >= 0 and measurements must be > 0");
        }

        boolean textZstd = Boolean.parseBoolean(options.getOrDefault("text-zstd", "false"));
        return new Input(fixture, Path.of(sourceName), dumps, textZstd, warmup, measurements);
    }

    private static void printMetadata(Input input) {
        String modes = String.join(";", input.dumps().keySet().stream().map(Mode::label).toList());
        System.out.printf(Locale.ROOT, "# fixture=%s,source=%s,modes=%s,warmup=%d,measurements=%d,text_zstd=%s%n",
                csv(input.fixture()), csv(input.source().toString()), modes, input.warmup(), input.measurements(),
                input.textZstd());
        System.out.println("# query=App.getDescendantsAndFields plus names/operators/types; ids=encounter-order normalized");
    }

    private static int integerOption(Map<String, String> options, String key, int defaultValue) {
        String value = options.get(key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    private static String require(List<String> values, int index, String name) {
        if (index >= values.size()) {
            throw new IllegalArgumentException("missing " + name);
        }
        return values.get(index);
    }

    private static boolean contains(String[] values, String expected) {
        for (String value : values) {
            if (value.equals(expected)) {
                return true;
            }
        }
        return false;
    }

    private static double millis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static String csv(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static String digest(String value) {
        Hasher hash = new Hasher();
        hash.add(value);
        return hash.finish();
    }
}
