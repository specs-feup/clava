/*
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.suikasoft.jOptions.Datakey.DataKey;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clang.transforms.TreeTransformer;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Small experiment-only entry point for the production Clava parser.
 *
 * <p>The heap mode prints one {@code PROTOBUF_HEAP} JSON object for each
 * retained-heap checkpoint. GNU time must wrap this process when peak RSS is
 * needed. The identity mode writes two generated source trees for the
 * parse/generate/reparse wrapper.</p>
 */
public final class ProtobufMemoryIdentityProbe {

    private ProtobufMemoryIdentityProbe() {
    }

    public static void main(String[] arguments) {
        try {
            Options options = Options.parse(arguments);
            SpecsSystem.programStandardInit();

            if (options.identity) {
                runIdentity(options);
                return;
            }

            runHeap(options);
            // Keep this checkpoint outside runHeap so its stack no longer holds
            // the App, generated file list, query results, or copied subtree.
            emitHeap("detached_retained", Map.of());
        } catch (Throwable exception) {
            exception.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void runHeap(Options options) throws IOException {
        Files.createDirectories(options.work);
        Path cache = options.work.resolve("dumper-cache");
        Path parseRoot = options.work.resolve("parse-root");

        App app = parse(options.source, parseRoot, cache, options.standard);
        long nodes = app.getDescendantsAndSelfStream().count();
        emitHeap("graph_ready", mapOf(
                "nodes", nodes,
                "translation_units", app.getTranslationUnits().size()));

        QueryStats queryStats = query(app);
        emitHeap("post_query", mapOf(
                "nodes", queryStats.nodes,
                "functions", queryStats.functions,
                "calls", queryStats.calls,
                "field_nodes", queryStats.fieldNodes));

        QueryStats copyStats = exerciseCopyMutationAndTransform(app);
        Path generated = options.work.resolve("generated");
        List<File> generatedFiles = app.write(generated.toFile());
        long generatedBytes = generatedFiles.stream().mapToLong(ProtobufMemoryIdentityProbe::fileSize).sum();
        emitHeap("post_codegen", mapOf(
                "nodes", queryStats.nodes,
                "generated_files", generatedFiles.size(),
                "generated_bytes", generatedBytes,
                "copy_isolated", copyStats.copyIsolated ? 1 : 0,
                "transform_visited", copyStats.transformVisited));
    }

    private static void runIdentity(Options options) throws IOException {
        Files.createDirectories(options.work);
        Files.createDirectories(options.first);
        Files.createDirectories(options.second);

        App first = parse(options.source, options.work.resolve("first-parse-root"),
                options.work.resolve("first-dumper-cache"), options.standard);
        QueryStats queryStats = query(first);
        QueryStats copyStats = exerciseCopyMutationAndTransform(first);
        List<File> firstFiles = first.write(options.first.toFile());
        if (firstFiles.isEmpty()) {
            throw new IOException("The first parse generated no source files");
        }

        File reparsedSource = firstFiles.get(0);
        App second = parse(reparsedSource.toPath(), options.work.resolve("second-parse-root"),
                options.work.resolve("second-dumper-cache"), options.standard);
        List<File> secondFiles = second.write(options.second.toFile());

        Map<String, Object> result = new HashMap<>();
        result.put("mode", "identity");
        result.put("source", options.source.toAbsolutePath().normalize().toString());
        result.put("first_files", firstFiles.size());
        result.put("second_files", secondFiles.size());
        result.put("first_nodes", queryStats.nodes);
        result.put("first_functions", queryStats.functions);
        result.put("first_calls", queryStats.calls);
        result.put("copy_isolated", copyStats.copyIsolated);
        result.put("transform_visited", copyStats.transformVisited);
        System.out.println("PROTOBUF_IDENTITY " + json(result));
    }

    private static App parse(Path source, Path parseRoot, Path dumperFolder, String standard) throws IOException {
        if (!Files.isRegularFile(source)) {
            throw new IOException("Source file does not exist: " + source);
        }

        Files.createDirectories(parseRoot);
        Files.createDirectories(dumperFolder);

        CodeParser parser = CodeParser.newInstance();
        // These options are stable in the current distribution. Reflection
        // keeps the helper runnable with a separate older text baseline when
        // that baseline predates an experiment-only option.
        setOption(parser, CodeParser.class, "DUMPER_FOLDER", dumperFolder.toFile());
        setOption(parser, CodeParser.class, "SHOW_EXEC_INFO", false);
        setOption(parser, CodeParser.class, "AST_DUMP_CACHE", false);
        setOption(parser, CodeParser.class, "GENERATED_PARSE_ROOT", parseRoot.toFile());
        setOption(parser, ParallelCodeParser.class, "PARALLEL_PARSING", false);
        return parser.parse(List.of(source.toFile()), List.of("-std=" + standard));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void setOption(CodeParser parser, Class<?> owner, String fieldName, Object value) {
        try {
            Field field = owner.getField(fieldName);
            parser.set((DataKey) field.get(null), value);
        } catch (NoSuchFieldException exception) {
            // Older baseline distributions do not have every current parser
            // option. Their normal defaults are sufficient for this probe.
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not set parser option " + fieldName, exception);
        }
    }

    private static QueryStats query(App app) {
        List<ClavaNode> nodes = app.getDescendantsAndSelfStream().toList();
        long functions = nodes.stream().filter(FunctionDecl.class::isInstance).count();
        long calls = nodes.stream().filter(CallExpr.class::isInstance).count();
        long fieldNodes = app.getDescendantsAndFields().size();
        return new QueryStats(nodes.size(), functions, calls, fieldNodes, false, 0);
    }

    private static QueryStats exerciseCopyMutationAndTransform(App app) {
        Optional<ClavaNode> candidate = app.getDescendantsAndSelfStream()
                .filter(node -> node instanceof FunctionDecl)
                .findFirst();
        if (candidate.isEmpty()) {
            return new QueryStats(0, 0, 0, 0, true, 0);
        }

        ClavaNode original = candidate.get();
        String originalId = original.getId();
        ClavaNode copy = original.copy(false, true);
        AtomicBoolean visited = new AtomicBoolean();
        new TreeTransformer(List.of(new SimplePreClavaRule() {
            @Override
            public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
                if (visited.compareAndSet(false, true)) {
                    node.setId(node.getId() + "_probe");
                }
            }
        })).transform(copy);

        boolean copyIsolated = originalId.equals(original.getId()) && !originalId.equals(copy.getId());
        return new QueryStats(0, 0, 0, 0, copyIsolated, visited.get() ? 1 : 0);
    }

    private static long fileSize(File file) {
        return file.isFile() ? file.length() : 0L;
    }

    private static void emitHeap(String phase, Map<String, Object> fields) {
        Map<String, Object> values = new HashMap<>();
        values.put("phase", phase);
        values.put("used_bytes", usedHeapAfterExplicitGc());
        values.put("gc_requested", true);
        values.putAll(fields);
        System.out.println("PROTOBUF_HEAP " + json(values));
        System.out.flush();
    }

    private static long usedHeapAfterExplicitGc() {
        for (int index = 0; index < 3; index++) {
            System.gc();
            System.runFinalization();
        }
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new HashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put((String) values[index], values[index + 1]);
        }
        return result;
    }

    private static String json(Map<String, Object> values) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                builder.append(value);
            } else {
                builder.append('"').append(String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\""))
                        .append('"');
            }
        }
        return builder.append('}').toString();
    }

    private record QueryStats(long nodes, long functions, long calls, long fieldNodes,
            boolean copyIsolated, int transformVisited) {
    }

    private static final class Options {
        private final Path source;
        private final Path work;
        private final Path first;
        private final Path second;
        private final String standard;
        private final boolean identity;

        private Options(Path source, Path work, Path first, Path second, String standard, boolean identity) {
            this.source = source;
            this.work = work;
            this.first = first;
            this.second = second;
            this.standard = standard;
            this.identity = identity;
        }

        private static Options parse(String[] arguments) {
            Map<String, List<String>> values = new HashMap<>();
            for (int index = 0; index < arguments.length; index++) {
                String argument = arguments[index];
                if (!argument.startsWith("--")) {
                    throw new IllegalArgumentException("Unexpected argument: " + argument);
                }
                String name = argument.substring(2);
                if (name.equals("identity")) {
                    values.computeIfAbsent(name, ignored -> new ArrayList<>()).add("true");
                    continue;
                }
                if (index + 1 >= arguments.length) {
                    throw new IllegalArgumentException("Missing value for --" + name);
                }
                values.computeIfAbsent(name, ignored -> new ArrayList<>()).add(arguments[++index]);
            }

            Path source = path(values, "source", true);
            Path work = path(values, "work", false);
            boolean identity = values.containsKey("identity");
            Path first = path(values, "first", identity);
            Path second = path(values, "second", identity);
            if (work == null) {
                work = identity ? first.resolveSibling(first.getFileName() + ".work") : source.resolveSibling("probe-work");
            }
            String standard = single(values, "standard", inferStandard(source));
            return new Options(source, work, first, second, standard, identity);
        }

        private static Path path(Map<String, List<String>> values, String name, boolean required) {
            String value = single(values, name, null);
            if (value == null) {
                if (required) {
                    throw new IllegalArgumentException("Missing --" + name);
                }
                return null;
            }
            return Path.of(value).toAbsolutePath().normalize();
        }

        private static String single(Map<String, List<String>> values, String name, String fallback) {
            List<String> found = values.get(name);
            if (found == null || found.isEmpty()) {
                return fallback;
            }
            if (found.size() != 1) {
                throw new IllegalArgumentException("Option may only be supplied once: --" + name);
            }
            return found.get(0);
        }

        private static String inferStandard(Path source) {
            String name = source.getFileName().toString().toLowerCase(Locale.ROOT);
            return name.endsWith(".c") ? "c11" : "c++17";
        }
    }
}
