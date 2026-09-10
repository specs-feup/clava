import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.math.BigInteger;
import java.util.function.Function;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.DataStore.MemoizedDataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinitions;
import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.parsers.*;
import pt.up.fe.specs.clava.*;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.ast.expr.*;
import pt.up.fe.specs.clava.ast.expr.enums.*;
import pt.up.fe.specs.clava.language.CastKind;
import pt.up.fe.specs.clava.utils.ClassesService;
import pt.up.fe.specs.util.utilities.LineStream;
import com.github.luben.zstd.ZstdInputStream;
import astwire.flat.RecordBody;
import astwire.flat.NodePayload;

/** Hybrid experiment: generated mapped accessors for selected nodes, legacy parsing for the rest. */
public final class FlatReader {
    static {
        ClangAstData.NODE_DATA.setValueClass(Map.class);
        ClangAstData.VISITED_CHILDREN.setValueClass(Map.class);
    }
    private static final String MARKER = "<AST_FLAT_NODE>";
    private static final byte[] MARKER_BYTES = (MARKER + "\n").getBytes(StandardCharsets.UTF_8);
    private static final String[] CLASSES = {"", "ImplicitCastExpr", "IntegerLiteral", "BinaryOperator",
        "CompoundAssignOperator", "UnaryOperator", "CastExpr", "CXXFunctionalCastExpr", "DeclRefExpr", "Expr", "Stmt"};
    private static final Map<Integer,List<DataKey<?>>> KEY_SETS = new java.util.concurrent.ConcurrentHashMap<>();
    static final class Stats { long typed, deferred, materialized; }
    public static final class Result {
        private final ClangAstData data;
        private final Stats stats;
        Result(ClangAstData data, Stats stats) { this.data=data; this.stats=stats; }
        public ClangAstData data() { return data; }
        public long typed() { return stats.typed; }
        public long materialized() { return stats.materialized; }
        public long deferred() { return stats.deferred; }
        /** Call after TU construction has consumed the temporary parser indexes. */
        public void releaseLookup() {
            data.getClavaNodes().getNodes().clear();
            data.getClavaNodes().getQueuedActions().clear();
            data.get(ClangAstData.NODE_DATA).clear();
            data.get(ClangAstData.VISITED_CHILDREN).clear();
            if(data.hasValue(ClangAstData.ID_TO_FILENAME_MAP))data.get(ClangAstData.ID_TO_FILENAME_MAP).clear();
            if(data.hasValue(ClangAstData.SKIPPED_NODES_MAP))data.get(ClangAstData.SKIPPED_NODES_MAP).clear();
            data.get(ClangAstData.TOP_LEVEL_DECL_IDS).clear();
            data.get(ClangAstData.TOP_LEVEL_TYPE_IDS).clear();
            data.get(ClangAstData.TOP_LEVEL_ATTR_IDS).clear();
        }
    }
    static String utf8(ByteBuffer bytes) {
        if (bytes == null) throw new IllegalArgumentException("Missing required bytes");
        return StandardCharsets.UTF_8.decode(bytes).toString();
    }
    static SourceRange range(astwire.flat.Range r, List<String> files) {
        var begin=new SourceLocation(files.get(Math.toIntExact(r.file())),Math.toIntExact(r.line()),Math.toIntExact(r.column()),false);
        if(r.endFile()==0) return new SourceRange(begin);
        return new SourceRange(begin,new SourceLocation(files.get(Math.toIntExact(r.endFile())),Math.toIntExact(r.endLine()),Math.toIntExact(r.endColumn()),false));
    }
    /** Contains no parser state or node index. All reference properties are installed eagerly. */
    static final class Payload implements Function<DataKey<?>,Object> {
        final astwire.flat.Node node;
        final List<String> files;
        final Stats stats;
        Payload(astwire.flat.Node node,List<String> files,Stats stats) { this.node=node;this.files=files;this.stats=stats; }
        public Object apply(DataKey<?> key) {
            Object value;
            if(key==ClavaNode.LOCATION) value=range(node.expansion(),files);
            else if(key==ClavaNode.IS_MACRO) value=node.isMacro();
            else if(key==ClavaNode.IS_IN_SYSTEM_HEADER) value=node.systemHeader();
            else if(key==Expr.VALUE_KIND) value=ValueKind.getEnumHelper().fromValue(Math.toIntExact(node.valueKind()));
            else if(key==Expr.OBJECT_KIND) value=ObjectKind.getEnumHelper().fromValue(Math.toIntExact(node.objectKind()));
            else if(key==Expr.IS_DEFAULT_ARGUMENT) value=node.defaultArgument();
            else if(key==Literal.SOURCE_LITERAL) value=utf8(((astwire.flat.IntegerLiteral)node.payload(new astwire.flat.IntegerLiteral())).literalSourceAsByteBuffer());
            else if(key==IntegerLiteral.VALUE) value=new BigInteger(utf8(((astwire.flat.IntegerLiteral)node.payload(new astwire.flat.IntegerLiteral())).integerDecimalAsByteBuffer()));
            else if(key==BinaryOperator.OP) {
                ByteBuffer bytes=node.kind()==3
                    ? ((astwire.flat.BinaryOperator)node.payload(new astwire.flat.BinaryOperator())).opcodeAsByteBuffer()
                    : ((astwire.flat.CompoundAssign)node.payload(new astwire.flat.CompoundAssign())).opcodeAsByteBuffer();
                value=BinaryOperatorKind.valueOf(utf8(bytes));
            } else if(key==UnaryOperator.OP) value=UnaryOperatorKind.valueOf(utf8(((astwire.flat.UnaryOperator)node.payload(new astwire.flat.UnaryOperator())).opcodeAsByteBuffer()));
            else if(key==UnaryOperator.POSITION) value=((astwire.flat.UnaryOperator)node.payload(new astwire.flat.UnaryOperator())).postfix()?UnaryOperatorPosition.POSTFIX:UnaryOperatorPosition.PREFIX;
            else if(key==CastExpr.CAST_KIND) {
                ByteBuffer bytes=node.kind()==6
                    ? ((astwire.flat.CastExpr)node.payload(new astwire.flat.CastExpr())).opcodeAsByteBuffer()
                    : ((astwire.flat.FunctionalCast)node.payload(new astwire.flat.FunctionalCast())).opcodeAsByteBuffer();
                value=CastKind.getHelper().fromName(utf8(bytes));
            } else throw new IllegalArgumentException("Unknown deferred key "+key.getName());
            stats.materialized++;
            return value;
        }
    }
    static final class Bridge extends InputStream {
        final MappedRecords records;
        final ClangAstData data;
        final ArrayDeque<astwire.flat.Node> pending=new ArrayDeque<>();
        final ArrayList<String> files=new ArrayList<>(List.of(""));
        final ArrayList<String> ids=new ArrayList<>(List.of(""));
        ByteBuffer current=ByteBuffer.allocate(0);
        long count,typed,rawBytes;
        long declaredIds;
        boolean started,ended;
        Bridge(Path path,ClangAstData data)throws IOException { records=new MappedRecords(path);this.data=data; }
        String id(long n) {
            if(n<=0||n>10_000_000)throw new IllegalArgumentException("Invalid pilot ID "+n);
            int i=(int)n;
            while(ids.size()<=i)ids.add(null);
            String s=ids.get(i);if(s==null){s="@"+i;ids.set(i,s);}return s;
        }
        boolean advance()throws IOException {
            while(!current.hasRemaining()) {
                var frame=records.next();
                if(frame==null) { if(!ended)throw new EOFException("Missing end record");return false; }
                if(ended)throw new IOException("Record after end");
                count++;
                var r=new astwire.flat.Record().__assign(frame.rootOffset(),frame.buffer());
                if(!started&&r.bodyType()!=RecordBody.Header)throw new IOException("Missing header");
                switch(r.bodyType()) {
                    case RecordBody.Header -> {
                        var h=(astwire.flat.Header)r.body(new astwire.flat.Header());
                        if(started||count!=1||!"ast-wire-flat-v1".equals(h.schemaId()))throw new IOException("Wrong schema header: "+h.schemaId());
                        started=true;
                    }
                    case RecordBody.File -> {
                        var f=(astwire.flat.File)r.body(new astwire.flat.File());
                        if(f.id()!=files.size())throw new IOException("Noncontiguous file ID");
                        String path=utf8(f.pathAsByteBuffer());
                        files.add(data.get(ClangAstData.CONTEXT).get(ClavaContext.CACHED_FILEPATHS).get(ClangAstPathResolver.resolve(path,data)));
                    }
                    case RecordBody.Raw -> {
                        var raw=(astwire.flat.Raw)r.body(new astwire.flat.Raw());
                        if(raw.bytesLength()>65536)throw new IOException("Oversized raw chunk");
                        rawBytes+=raw.bytesLength();current=raw.bytesAsByteBuffer();
                    }
                    case RecordBody.Node -> {
                        var node=(astwire.flat.Node)r.body(new astwire.flat.Node());
                        validate(node);
                        typed++;pending.add(node);current=ByteBuffer.wrap(MARKER_BYTES);
                    }
                    case RecordBody.End -> {
                        var end=(astwire.flat.End)r.body(new astwire.flat.End());
                        if(end.records()!=count||end.nodes()!=typed||end.rawBytes()!=rawBytes||end.files()!=files.size()-1)throw new IOException("End count mismatch");
                        declaredIds=end.ids();ended=true;
                    }
                    default -> throw new IOException("Unknown record body");
                }
            }
            return true;
        }
        void validate(astwire.flat.Node n)throws IOException {
            int k=n.kind();
            int[] payloads={0,NodePayload.BaseExpr,NodePayload.IntegerLiteral,NodePayload.BinaryOperator,NodePayload.CompoundAssign,NodePayload.UnaryOperator,NodePayload.CastExpr,NodePayload.FunctionalCast,NodePayload.DeclRef,NodePayload.BaseExpr,NodePayload.BaseStmt};
            if(k<1||k>10||n.payloadType()!=payloads[k])throw new IOException("Node kind/payload mismatch");
            for(var loc:new astwire.flat.Range[]{n.expansion(),n.spelling()})if(loc!=null) {
                if(loc.file()<1||loc.file()>=files.size()||loc.endFile()>=files.size())throw new IOException("Undefined source file");
            }
        }
        public int read()throws IOException{return advance()?current.get()&255:-1;}
        public int read(byte[] out,int off,int len)throws IOException {
            if(len==0)return 0;if(!advance())return -1;int done=0;
            do{int n=Math.min(len-done,current.remaining());current.get(out,off+done,n);done+=n;}while(done<len&&advance());return done;
        }
        public void close()throws IOException {records.close();}
    }
    public static Result read(Path path,boolean lazy)throws Exception {return parse(path,lazy,true,false);}
    public static Result readText(Path path,boolean zstd)throws Exception {return parse(path,false,false,zstd);}
    private static Result parse(Path path,boolean lazy,boolean flat,boolean zstd)throws Exception {
        System.setProperty("dense.maps",Boolean.toString(flat||Boolean.getBoolean("flat.denseText")));
        var parser=ClangStreamParserV2.newInstance(new ClavaContext());
        if(flat||Boolean.getBoolean("flat.denseText")) {
            parser.getData().set(ClangAstData.NODE_DATA,new DenseIdMap<>());
            parser.getData().set(ClangAstData.VISITED_CHILDREN,new DenseIdMap<>());
        }
        Stats stats=new Stats();
        Bridge bridge=flat?new Bridge(path,parser.getData()):null;
        InputStream in=flat?bridge:Files.newInputStream(path);
        if(zstd)in=new ZstdInputStream(in);
        try(parser;var lines=LineStream.newInstance(in,null)) {
            while(lines.hasNextLine()) {
                String marker=lines.nextLine();
                if(flat&&marker.equals(MARKER)) {
                    var node=bridge.pending.poll();if(node==null)throw new IOException("Missing pending node");
                    importNode(node,bridge,parser.getData(),lazy,stats);
                } else if(parser.getIds().contains(marker))parser.parse(marker,lines);
                else if(!parser.getLineIgnore().test(marker))throw new IOException("Unparsed line "+marker);
            }
            if(parser.hasExceptions()||(flat&&(!bridge.pending.isEmpty()||bridge.ids.size()-1>bridge.declaredIds)))throw new IOException("Parser failed");
        }
        return new Result(parser.getData(),stats);
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    static void importNode(astwire.flat.Node n,Bridge b,ClangAstData all,boolean lazy,Stats stats) {
        int kind=n.kind();
        var definition=StoreDefinitions.fromInterface(ClassesService.getClavaClass(CLASSES[kind]));
        var keys=KEY_SETS.computeIfAbsent(kind*2+(n.expansion()!=null?1:0), code -> {
            int selected=code/2;
            var list=new ArrayList<DataKey<?>>();
            if(code%2!=0)list.add(ClavaNode.LOCATION);
            list.add(ClavaNode.IS_MACRO);list.add(ClavaNode.IS_IN_SYSTEM_HEADER);
            if(selected!=10){list.add(Expr.VALUE_KIND);list.add(Expr.OBJECT_KIND);list.add(Expr.IS_DEFAULT_ARGUMENT);}
            switch(selected){
                case 2 -> {list.add(Literal.SOURCE_LITERAL);list.add(IntegerLiteral.VALUE);}
                case 3,4 -> list.add(BinaryOperator.OP);
                case 5 -> {list.add(UnaryOperator.OP);list.add(UnaryOperator.POSITION);}
                case 6,7 -> list.add(CastExpr.CAST_KIND);
            }
            return List.copyOf(list);
        });
        var payload=new Payload(n,b.files,stats);
        DataStore store=lazy?new MemoizedDataStore(definition,keys,payload):DataStore.newInstance(definition,true);
        stats.typed++;stats.deferred+=keys.size();
        if(!lazy)for(var key:keys)store.set((DataKey)key,payload.apply(key));
        store.set(ClavaNode.CONTEXT,all.get(ClangAstData.CONTEXT));store.set(ClavaNode.ID,b.id(n.id()));
        if(kind!=10)all.getClavaNodes().queueSetOptionalNode(store,Expr.TYPE,n.typeId()==0?"nullptr_type":b.id(n.typeId()));
        if(kind==8) {
            store.set(DeclRefExpr.QUALIFIER,"");store.set(DeclRefExpr.TEMPLATE_ARGUMENTS,new ArrayList<>());
            long decl=((astwire.flat.DeclRef)n.payload(new astwire.flat.DeclRef())).declId();
            all.getClavaNodes().queueSetNode(store,DeclRefExpr.DECL,decl==0?"nullptr_decl":b.id(decl));
        }
        if(all.get(ClangAstData.NODE_DATA).put(b.id(n.id()),store)!=null)throw new IllegalArgumentException("Duplicate node ID");
    }
}
