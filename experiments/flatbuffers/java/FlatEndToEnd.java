import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Measures native/cache materialization and the complete Java workload in the same trial. */
public class FlatEndToEnd {
    public static void main(String[] args)throws Exception {
        String tool=args[0];Path source=Path.of(args[1]),root=Path.of(args[2]);
        int trials=args.length>3?Integer.parseInt(args[3]):12;
        Files.createDirectories(root);
        Files.writeString(root.resolve("text.key"),"original-text-zstd-v1\n");
        Files.writeString(root.resolve("flat.key"),"flat-uncompressed-v1\n");
        var modes=List.of("text-cold","eager-cold","lazy-cold","text-warm","eager-warm","lazy-warm","packed-cold","packed-warm");
        System.out.println("mode,trial,total_ms,native_ms,parse_ms,tu_ms,query_ms,codegen_ms,bytes,typed,materialized,deferred,native_rss_kib,native_user_s,native_sys_s,code_sha256");
        for(int trial=-3;trial<trials;trial++) {
            var order=new ArrayList<>(modes);Collections.shuffle(order,new Random(721+trial));
            for(String mode:order) {
                boolean text=mode.startsWith("text"),cold=mode.endsWith("cold"),packed=mode.startsWith("packed");
                Path out=root.resolve("dump-"+mode+"-"+trial),dep=root.resolve("dump.d");
                var command=new ArrayList<>(List.of("/usr/bin/time","-f","%M,%U,%S","-o",root.resolve("native.time").toString(),"ccache",tool,"-c",source.toString(),"-id=1","-system-header-threshold=1","-o",out.toString(),"-MD","-MF",dep.toString()));
                if(text)command.add("-ast-dump-compression=zstd");
                command.addAll(List.of("--",source.toString().endsWith(".c")?"-std=c11":"-std=c++17","-Wno-unknown-pragmas"));
                var pb=new ProcessBuilder(command).redirectError(root.resolve("diagnostics").toFile());
                var env=pb.environment();env.keySet().removeIf(k->k.startsWith("CCACHE_")||k.startsWith("AST_WIRE_"));
                if(!text)env.put("AST_WIRE_FLAT","1");
                env.put("CCACHE_DIR",root.resolve(cold?mode+"-"+trial:text?"text-cache":packed?"packed-cache":"flat-cache").toString());
                env.put("CCACHE_COMPILERTYPE","clang");env.put("CCACHE_DEPEND","true");env.put("CCACHE_NOHASHDIR","true");if(packed){env.put("CCACHE_COMPRESS","true");env.put("CCACHE_COMPRESSLEVEL","1");}else env.put("CCACHE_NOCOMPRESS","true");
                env.put("CCACHE_EXTRAFILES",root.resolve(text?"text.key":"flat.key").toString());
                long start=System.nanoTime();
                var process=pb.start();process.getInputStream().transferTo(OutputStream.nullOutputStream());
                if(process.waitFor()!=0)throw new IOException(Files.readString(root.resolve("diagnostics")));
                long nativeEnd=System.nanoTime();
                var javaMode=text?FlatBenchmark.Mode.TEXT:(mode.startsWith("lazy")||packed)?FlatBenchmark.Mode.LAZY:FlatBenchmark.Mode.EAGER;
                var measured=FlatBenchmark.measure(source,javaMode,out,text);
                long end=System.nanoTime();
                if(trial>=0)System.out.printf(Locale.ROOT,"%s,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%d,%d,%d,%s,%s%n",mode,trial,(end-start)/1e6,(nativeEnd-start)/1e6,measured.parseNanos()/1e6,measured.tuNanos()/1e6,measured.queryNanos()/1e6,measured.codegenNanos()/1e6,Files.size(out),measured.typed(),measured.materialized(),measured.deferred(),Files.readString(root.resolve("native.time")).trim(),measured.codeDigest());
                // This logical execution has finished; its nodes and immutable backing file are no longer used.
                Files.delete(out);
            }
        }
    }
}
