import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import astwire.flat.*;

/** Reconstructs legacy bytes independently of Clava materialization for fidelity checks. */
public class FlatWireCheck {
    static final String[] CLASSES={"","ImplicitCastExpr","IntegerLiteral","BinaryOperator","CompoundAssignOperator","UnaryOperator","CastExpr","CXXFunctionalCastExpr","DeclRefExpr","Expr","Stmt"};
    static final String[] DATA={"","Expr","IntegerLiteral","BinaryOperator","BinaryOperator","UnaryOperator","CastExpr","CastExpr","DeclRefExpr","Expr","Stmt"};
    static String id(long id,String nil){return id==0?nil:"@"+id;}
    static void range(StringBuilder s,Range r,List<String> files) {
        if(r==null){s.append("<invalid>\n");return;}
        s.append(files.get((int)r.file())).append('\n').append(r.line()).append('\n').append(r.column()).append('\n');
        if(r.endFile()==0)s.append("<end>\n");
        else s.append(files.get((int)r.endFile())).append('\n').append(r.endLine()).append('\n').append(r.endColumn()).append('\n');
    }
    static String node(Node n,List<String> files) {
        int k=n.kind();var s=new StringBuilder().append('<').append(DATA[k]).append("Data>\n@").append(n.id()).append('\n').append(CLASSES[k]).append('\n');
        range(s,n.expansion(),files);s.append(n.isMacro()?"1\n":"0\n");if(n.isMacro())range(s,n.spelling(),files);s.append(n.systemHeader()?"1\n":"0\n");
        if(k!=10)s.append(id(n.typeId(),"nullptr_type")).append('\n').append(n.valueKind()).append('\n').append(n.objectKind()).append('\n').append(n.defaultArgument()?"1\n":"0\n");
        switch(k) {
            case 2 -> {var p=(IntegerLiteral)n.payload(new IntegerLiteral());s.append("%CLAVA_SOURCE_BEGIN%\n").append(FlatReader.utf8(p.literalSourceAsByteBuffer())).append("\n%CLAVA_SOURCE_END%\n").append(FlatReader.utf8(p.integerDecimalAsByteBuffer())).append('\n');}
            case 3 -> s.append(FlatReader.utf8(((BinaryOperator)n.payload(new BinaryOperator())).opcodeAsByteBuffer())).append('\n');
            case 4 -> s.append(FlatReader.utf8(((CompoundAssign)n.payload(new CompoundAssign())).opcodeAsByteBuffer())).append('\n');
            case 5 -> {var p=(UnaryOperator)n.payload(new UnaryOperator());s.append(FlatReader.utf8(p.opcodeAsByteBuffer())).append('\n').append(p.postfix()?"POSTFIX\n":"PREFIX\n");}
            case 6 -> s.append(FlatReader.utf8(((CastExpr)n.payload(new CastExpr())).opcodeAsByteBuffer())).append('\n');
            case 7 -> s.append(FlatReader.utf8(((FunctionalCast)n.payload(new FunctionalCast())).opcodeAsByteBuffer())).append('\n');
            case 8 -> s.append("\n0\n").append(id(((DeclRef)n.payload(new DeclRef())).declId(),"nullptr_decl")).append('\n');
        }
        return s.toString();
    }
    public static void main(String[] args)throws Exception {
        var files=new ArrayList<String>();files.add("");long nodes=0;
        try(var frames=new MappedRecords(Path.of(args[0]));var out=Files.newOutputStream(Path.of(args[1]))) {
            MappedRecords.Frame f;
            while((f=frames.next())!=null) {
                var r=new astwire.flat.Record().__assign(f.rootOffset(),f.buffer());
                if(r.bodyType()==RecordBody.File)files.add(FlatReader.utf8(((astwire.flat.File)r.body(new astwire.flat.File())).pathAsByteBuffer()));
                else if(r.bodyType()==RecordBody.Raw) {var b=((Raw)r.body(new Raw())).bytesAsByteBuffer();byte[] bytes=new byte[b.remaining()];b.get(bytes);out.write(bytes);}
                else if(r.bodyType()==RecordBody.Node){out.write(node((Node)r.body(new Node()),files).getBytes(StandardCharsets.UTF_8));nodes++;}
            }
        }
        System.out.println("reconstructed_typed="+nodes);
    }
}
