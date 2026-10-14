import java.io.File;
import pt.up.fe.specs.clang.ClangResources;
import pt.up.fe.specs.clang.LibcMode;
import pt.up.fe.specs.clang.codeparser.CodeParser;
public class ProtobufResourceProbe {
 public static void main(String[] args) {
  var options = CodeParser.newInstance();
  options.set(CodeParser.DUMPER_FOLDER, new File(args[0]));
  var resources = new ClangResources(options);
  resources.getClangFiles(LibcMode.BUILTIN_AND_LIBC);
  int count = Integer.parseInt(args[1]);
  long start = System.nanoTime();
  for (int i = 0; i < count; i++) resources.getClangFiles(LibcMode.BUILTIN_AND_LIBC);
  System.out.printf("calls=%d elapsed_ms=%.3f ms_per_call=%.3f%n", count, (System.nanoTime()-start)/1e6, (System.nanoTime()-start)/1e6/count);
 }
}
