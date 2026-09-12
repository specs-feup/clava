import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import pt.up.fe.specs.clava.ast.extra.App;

/** Compares the real Clava graph produced by text, eager FlatBuffers and lazy FlatBuffers. */
final class CompleteChecks {
    private record Loaded(CompleteBenchmark.Mode mode, App app, CompleteBenchmark.Scan scan, String allDigest,
            boolean payloadMutation, String codeDigest) {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            throw new IllegalArgumentException("usage: CompleteChecks source text.dump eager.fb lazy.fb [--text-zstd]");
        }

        Path source = Path.of(args[0]);
        boolean textZstd = false;
        for (int i = 4; i < args.length; i++) {
            if (args[i].equals("--text-zstd")) {
                textZstd = true;
            }
        }

        LinkedHashMap<CompleteBenchmark.Mode, Path> inputs = new LinkedHashMap<>();
        inputs.put(CompleteBenchmark.Mode.TEXT, Path.of(args[1]));
        inputs.put(CompleteBenchmark.Mode.EAGER, Path.of(args[2]));
        inputs.put(CompleteBenchmark.Mode.LAZY, Path.of(args[3]));

        Map<CompleteBenchmark.Mode, Loaded> loaded = new LinkedHashMap<>();
        for (Map.Entry<CompleteBenchmark.Mode, Path> input : inputs.entrySet()) {
            if(System.getProperty("complete.tracePrefix")!=null)System.setProperty("flat.traceFile",System.getProperty("complete.tracePrefix")+"."+input.getKey().label());
            CompleteBenchmark.LoadedApp loadedApp = CompleteBenchmark.loadAppDetailed(source, input.getKey(), input.getValue(), textZstd);
            App app = loadedApp.app();
            // This occurs before the first complete graph traversal. It checks that a copy can
            // be mutated without corrupting deferred references in the loaded graph.
            CompleteBenchmark.mutationBeforeReadProbe(app);
            CompleteBenchmark.Scan scan = CompleteBenchmark.scan(app);
            loaded.put(input.getKey(), new Loaded(input.getKey(), app, scan, loadedApp.rawAllDigest(),
                    loadedApp.payloadMutation(), CompleteBenchmark.codeDigest(app)));
        }

        Loaded expected = loaded.get(CompleteBenchmark.Mode.TEXT);
        for (Loaded actual : loaded.values()) {
            if (!Objects.equals(expected.scan().digest(), actual.scan().digest())) {
                throw new AssertionError("normalized graph mismatch: text vs " + actual.mode().label()
                        + " (" + expected.scan().digest() + " != " + actual.scan().digest() + ")");
            }
            if (!Objects.equals(expected.allDigest(), actual.allDigest())) {
                throw new AssertionError("all-field graph mismatch: text vs " + actual.mode().label()
                        + " (" + expected.allDigest() + " != " + actual.allDigest() + ")");
            }
            if (!Objects.equals(expected.codeDigest(), actual.codeDigest())) {
                throw new AssertionError("getCode digest mismatch: text vs " + actual.mode().label());
            }
        }

        System.out.println("graph_equal=true,all_fields_equal=true,code_equal=true,mutation_before_read_copy=true");
        System.out.println("mode,nodes,names,operators,types,payload_mutation,query_sha256,all_graph_sha256,code_sha256");
        for (Loaded value : loaded.values()) {
            CompleteBenchmark.Scan scan = value.scan();
            System.out.printf(Locale.ROOT, "%s,%d,%d,%d,%d,%s,%s,%s,%s%n", value.mode().label(), scan.nodes(),
                    scan.names(), scan.operators(), scan.types(), value.payloadMutation(), scan.digest(),
                    value.allDigest(), value.codeDigest());
        }
    }
}
