import java.lang.management.ManagementFactory;
import java.lang.invoke.*;
import java.util.List;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinitions;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ClavaNode;
public class StorageProbe {
 static volatile Object sink;
 static MethodHandle create;
 static DataStore data(Class<?> node) throws Throwable {
  if(create!=null) return (DataStore)create.invokeExact(node);
  return DataStore.newInstance(StoreDefinitions.fromInterface(node),true);
 }
 static void loop(Class<?> type,int count) throws Throwable {
  for(int i=0;i<count;i++) {
   var data=data(type);
   sink=type==BuiltinType.class?new BuiltinType(data,List.of()):new VarDecl(data,List.of());
  }
 }
 public static void main(String[] args)throws Throwable {
  try {create=MethodHandles.publicLookup().findStatic(Class.forName("pt.up.fe.specs.clava.data.NodeDataStore"),"create",MethodType.methodType(Class.forName("pt.up.fe.specs.clava.data.NodeDataStore"),Class.class)).asType(MethodType.methodType(DataStore.class,Class.class));}
  catch(ClassNotFoundException e){}
  var bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
  for(var type:List.of(BuiltinType.class,VarDecl.class)) {
   loop(type,40000);
   for(int r=0;r<4;r++) {
    long before=bean.getThreadAllocatedBytes(Thread.currentThread().threadId()), start=System.nanoTime();
    loop(type,200000);
    long nanos=System.nanoTime()-start,bytes=bean.getThreadAllocatedBytes(Thread.currentThread().threadId())-before;
    System.out.printf("%s %d bytes/node %.1f ns/node%n",type.getSimpleName(),bytes/200000,nanos/200000.0);
   }
  }
 }
}
