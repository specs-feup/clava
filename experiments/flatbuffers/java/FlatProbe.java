import java.nio.file.*;
public class FlatProbe {
    public static void main(String[] args)throws Exception {
        boolean lazy=Boolean.parseBoolean(args[1]);
        long start=System.nanoTime();var result=FlatReader.read(Path.of(args[0]),lazy);
        System.out.println("parse_ms="+(System.nanoTime()-start)/1e6+" nodes="+result.data().getClavaNodes().getNodes().size()+" typed="+result.typed()+" decoded="+result.materialized()+" deferred="+result.deferred());
    }
}
