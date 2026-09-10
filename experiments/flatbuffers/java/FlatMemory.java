import java.nio.file.*;
import java.lang.management.*;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/** Separate JVM probe; explicit GC is outside performance timing. */
public class FlatMemory {
    static void snapshot(String phase,FlatReader.Result result,Object keep) {
        System.gc();
        System.out.println(phase+",heap="+ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()+",decoded="+result.materialized()+",deferred="+result.deferred());
        Reference.reachabilityFence(keep);
    }
    public static void main(String[] args)throws Exception {
        var source=Path.of(args[0]);var dump=Path.of(args[1]);var mode=FlatBenchmark.Mode.parse(args[2]);
        var result=FlatBenchmark.read(mode,dump,mode==FlatBenchmark.Mode.TEXT);
        snapshot("graph",result,result);
        var app=FlatBenchmark.buildApp(result.data(),source);
        result.releaseLookup();
        snapshot("ready",result,app);
        FlatBenchmark.scan(app);
        snapshot("query",result,app);
        String code=app.getCode();System.out.println("code_chars="+code.length());code=null;
        snapshot("codegen",result,app);
        var tu=app.getChild(0);var weak=new WeakReference<>(tu);app.removeChild(0);tu=null;
        snapshot("detached",result,app);
        System.out.println("tu_collected="+(weak.get()==null));
        Reference.reachabilityFence(app);
    }
}
