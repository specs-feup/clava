import java.io.*;
import java.nio.file.*;
import com.github.luben.zstd.ZstdInputStream;
import pt.up.fe.specs.clang.wire.*;
import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.parsers.ClangStreamParserV2;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.util.utilities.LineStream;

final class CompleteInput {
 record Result(ClangAstData data,SchemaRuntime.Stats stats) {
  long typed(){return stats.nodes;} long materialized(){return stats.materialized;} long deferred(){return stats.deferred;}
  void releaseLookup(){CompleteReader.releaseLookup(data);}
 }
 static Result read(Path dump,boolean lazy)throws Exception {
  var result=CompleteReader.read(dump,new ClavaContext(),null,"1",lazy);return new Result(result.data(),result.stats());
 }
 static Result readText(Path dump,boolean zstd)throws Exception {
  var parser=ClangStreamParserV2.newInstance(new ClavaContext());
  InputStream in=Files.newInputStream(dump);if(zstd)in=new ZstdInputStream(in);
  try(parser;var lines=LineStream.newInstance(in,null)) {
   while(lines.hasNextLine()) {
    String marker=lines.nextLine();
    if(parser.getIds().contains(marker))parser.parse(marker,lines);
    else if(!parser.getLineIgnore().test(marker))throw new IOException("Unparsed line "+marker);
   }
   if(parser.hasExceptions())throw new IOException("Text parser failed");
  }
  return new Result(parser.getData(),new SchemaRuntime.Stats());
 }
}
