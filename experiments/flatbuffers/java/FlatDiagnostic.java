import java.nio.file.*;
import java.util.*;
import pt.up.fe.specs.clang.dumper.*;
import pt.up.fe.specs.clava.*;
import org.suikasoft.jOptions.Interfaces.DataStore;

/** Field-level diagnostics for canonicalization failures; not part of timed work. */
public class FlatDiagnostic {
    public static void main(String[] args)throws Exception {
        var result=args[2].equals("text")?FlatReader.readText(Path.of(args[1]),true):FlatReader.read(Path.of(args[1]),false);
        var tu=new ClangAstParser(result.data(),false,DataStore.newInstance("config")).parseTu(Path.of(args[0]).toFile());
        var app=result.data().getFactory().app(List.of(tu));
        System.setProperty("flat.traceFile",args[3]);
        System.out.println(FlatBenchmark.allGraphDigest(app));
    }
}
