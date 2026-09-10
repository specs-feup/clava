import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import pt.up.fe.specs.clava.ast.extra.App;

/** Compares the real Clava graph produced by text, eager FlatBuffers and lazy FlatBuffers. */
final class FlatChecks {
    private record Loaded(FlatBenchmark.Mode mode, App app, FlatBenchmark.Scan scan, String allDigest,
            boolean payloadMutation, String codeDigest) {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            throw new IllegalArgumentException("usage: FlatChecks source text.dump eager.fb lazy.fb [--text-zstd]");
        }

        Path source = Path.of(args[0]);
        boolean textZstd = false;
        for (int i = 4; i < args.length; i++) {
            if (args[i].equals("--text-zstd")) {
                textZstd = true;
            }
        }

        LinkedHashMap<FlatBenchmark.Mode, Path> inputs = new LinkedHashMap<>();
        inputs.put(FlatBenchmark.Mode.TEXT, Path.of(args[1]));
        inputs.put(FlatBenchmark.Mode.EAGER, Path.of(args[2]));
        inputs.put(FlatBenchmark.Mode.LAZY, Path.of(args[3]));

        Map<FlatBenchmark.Mode, Loaded> loaded = new LinkedHashMap<>();
        for (Map.Entry<FlatBenchmark.Mode, Path> input : inputs.entrySet()) {
            FlatBenchmark.LoadedApp loadedApp = FlatBenchmark.loadAppDetailed(source, input.getKey(), input.getValue(), textZstd);
            App app = loadedApp.app();
            // This occurs before the first complete graph traversal. It checks that a copy can
            // be mutated without corrupting deferred references in the loaded graph.
            FlatBenchmark.mutationBeforeReadProbe(app);
            FlatBenchmark.Scan scan = FlatBenchmark.scan(app);
            loaded.put(input.getKey(), new Loaded(input.getKey(), app, scan, loadedApp.rawAllDigest(),
                    loadedApp.payloadMutation(), FlatBenchmark.codeDigest(app)));
        }

        Loaded expected = loaded.get(FlatBenchmark.Mode.TEXT);
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
            FlatBenchmark.Scan scan = value.scan();
            System.out.printf(Locale.ROOT, "%s,%d,%d,%d,%d,%s,%s,%s,%s%n", value.mode().label(), scan.nodes(),
                    scan.names(), scan.operators(), scan.types(), value.payloadMutation(), scan.digest(),
                    value.allDigest(), value.codeDigest());
        }
    }
}
