/** Diagnostic reproduction of concurrent parser-default insertion losing explicit options. */
import java.util.*;
import java.util.concurrent.*;
import pt.up.fe.specs.clang.codeparser.*;
public class ParserConfigProbe {
 public static void main(String[] args) throws Exception {
  var pool=Executors.newFixedThreadPool(8);
  int losses=0;
  for(int r=0;r<2000;r++) {
   var parser=CodeParser.newInstance();
   parser.set(CodeParser.SHOW_CODE,true);
   var keys=parser.getStoreDefinitionTry().orElseThrow().getKeys();
   if(args.length>0) for(var key:keys)if(key != CodeParser.GENERATED_PARSE_ROOT && key.hasDefaultValue()&&!parser.hasValue(key))parser.get(key);
   var start=new CountDownLatch(1);var tasks=new ArrayList<Future<?>>();
   for(int w=0;w<8;w++)tasks.add(pool.submit(()->{try{start.await();for(var key:keys)if(key.hasDefaultValue())parser.get(key);}catch(InterruptedException e){throw new RuntimeException(e);}}));
   start.countDown();for(var t:tasks)t.get();
   if(!parser.get(CodeParser.SHOW_CODE))losses++;
  }
  pool.shutdown();System.out.println("Lost SHOW_CODE settings: "+losses+" / 2000");
 }
}
