package pt.up.fe.specs.clang.wire;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import com.google.flatbuffers.Table;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;
import org.suikasoft.jOptions.storedefinition.StoreDefinitions;
import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clava.*;
import pt.up.fe.specs.clava.utils.ClassesService;

/** Shared mechanics for schema-generated field mappings. No field layout is defined here. */
public final class SchemaRuntime {
    private static final Map<Class<? extends ClavaNode>, StoreDefinition> STORE_DEFINITIONS =
            new ConcurrentHashMap<>();

    public static final class Stats {
        public long nodes, materialized;
    }
    public static final class Files {
        public final List<String> paths = new ArrayList<>(List.of(""));
        public final Stats stats = new Stats();
        public final String scope;
        private long largestPositiveId;
        public Files(String scope) { this.scope=scope; }
        public String id(long id) {
            if (id > 0) {
                largestPositiveId = Math.max(largestPositiveId, id);
                return "@" + id + "_" + scope;
            }
            if (id == -1) return "nullptr_type";
            if (id == -2) return "nullptr_decl";
            if (id == -3) return "nullptr_expr";
            if (id == -4) return "nullptr_stmt";
            if (id == -5) return "nullptr_attr";
            if (id == -6) return "0_" + scope;
            throw new IllegalArgumentException("Invalid wire node reference " + id);
        }
        public String ownerId(long id, String recordKind) {
            if (id <= 0) {
                throw new IllegalArgumentException(recordKind + " owner id must be positive: " + id);
            }
            return this.id(id);
        }
        public long largestPositiveId() { return largestPositiveId; }
        public SourceRange range(astwire.v2.Range r) {
            if(r==null)return SourceRange.invalidRange();
            String begin=path(r.file());
            var start=new SourceLocation(begin,Math.toIntExact(r.line()),Math.toIntExact(r.column()),false);
            if(r.endFile()==0)return new SourceRange(start);
            return new SourceRange(start,new SourceLocation(path(r.endFile()),Math.toIntExact(r.endLine()),Math.toIntExact(r.endColumn()),false));
        }
        public String path(long index) {
            if(index<1||index>=paths.size())throw new IllegalArgumentException("Undefined wire file "+index);
            return paths.get((int)index);
        }
    }
    public record ImportContext(ClangAstData data, Files files) {
        public String id(long value) {return files.id(value);}
    }
    @FunctionalInterface public interface RefReader {long read(Table table,ImportContext context);}
    @FunctionalInterface public interface RefsReader {long[] read(Table table,ImportContext context);}
    public static final class Binding {
        final DataKey<?> key;
        final BiFunction<Table,Files,Object> scalar;
        final BiFunction<Table,ImportContext,Object> compound;
        final RefReader reference;
        final RefsReader references;
        final String refKind;
        final Function<Table,astwire.v2.Range> location;
        Binding(DataKey<?> key,BiFunction<Table,Files,Object> scalar,BiFunction<Table,ImportContext,Object> compound,
                RefReader reference,RefsReader references,String refKind,Function<Table,astwire.v2.Range> location) {
            this.key=key;this.scalar=scalar;this.compound=compound;this.reference=reference;this.references=references;this.refKind=refKind;this.location=location;
        }
        boolean hasValueDecoder() {return scalar!=null||location!=null;}
        Object decode(Table table,Files files) {
            Object value=location!=null?files.range(location.apply(table)):scalar.apply(table,files);
            files.stats.materialized++;
            return value;
        }
    }
    public static Binding scalar(DataKey<?> key,BiFunction<Table,Files,Object> reader) {return new Binding(key,reader,null,null,null,null,null);}
    public static Binding compound(DataKey<?> key,BiFunction<Table,ImportContext,Object> reader) {return new Binding(key,null,reader,null,null,null,null);}
    public static Binding reference(DataKey<?> key,String kind,RefReader reader) {return new Binding(key,null,null,reader,null,kind,null);}
    public static Binding references(DataKey<?> key,RefsReader reader) {return new Binding(key,null,null,null,reader,"list",null);}
    public static Binding location(Function<Table,astwire.v2.Range> reader) {return new Binding(ClavaNode.LOCATION,null,null,null,null,null,reader);}
    public static <T> List<T> list(int size,IntFunction<T> reader) {
        var values=new ArrayList<T>(size);for(int i=0;i<size;i++)values.add(reader.apply(i));return values;
    }
    public static long[] longs(int size,IntToLongFunction reader) {
        var values=new long[size];for(int i=0;i<size;i++)values[i]=reader.applyAsLong(i);return values;
    }
    public static final class Descriptor {
        final List<Binding> bindings;
        final Binding location;
        public Descriptor(List<Binding> bindings) {
            this.bindings=bindings;var seen=Collections.newSetFromMap(new IdentityHashMap<DataKey<?>,Boolean>());
            Binding loc=null;
            for(var binding:bindings)if(binding.hasValueDecoder()) {
                if(!seen.add(binding.key))throw new IllegalArgumentException("Duplicate wire key "+binding.key);
                if(binding.location!=null)loc=binding;
            }
            this.location=loc;
        }
        @SuppressWarnings({"rawtypes","unchecked"})
        public DataStore read(Table table,String className,String id,ImportContext context) {
            validateLambdaCaptureVectors(table);
            validateFunctionTemplateLists(table);
            boolean hasLocation=location!=null&&location.location.apply(table)!=null;
            var definition=STORE_DEFINITIONS.computeIfAbsent(ClassesService.getClavaClass(className),
                    StoreDefinitions::fromInterface);
            DataStore store=DataStore.newInstance(definition,true);
            context.files.stats.nodes++;
            store.set(ClavaNode.CONTEXT,context.data.get(ClangAstData.CONTEXT));store.set(ClavaNode.ID,id);
            if(location==null){store.set(ClavaNode.IS_MACRO,false);store.set(ClavaNode.IS_IN_SYSTEM_HEADER,false);}
            for(var binding:bindings) {
                if(binding.hasValueDecoder()) {
                    if(binding.location!=null&&!hasLocation)continue;
                    store.set((DataKey)binding.key,binding.decode(table,context.files));
                } else if(binding.compound!=null)store.set((DataKey)binding.key,binding.compound.apply(table,context));
                else if(binding.references!=null) {
                    var ids=new ArrayList<String>();for(long value:binding.references.read(table,context))ids.add(context.id(value));
                    context.data.getClavaNodes().queueSetNodeList(store,(DataKey)binding.key,ids);
                } else {
                    String ref=context.id(binding.reference.read(table,context));
                    switch(binding.refKind) {
                        case "node" -> context.data.getClavaNodes().queueSetNode(store,(DataKey)binding.key,ref);
                        case "optional" -> context.data.getClavaNodes().queueSetOptionalNode(store,(DataKey)binding.key,ref);
                        case "nullable" -> context.data.getClavaNodes().queueSetNullableNode(store,(DataKey)binding.key,ref);
                        case "id" -> store.set((DataKey)binding.key,ref);
                        default -> throw new IllegalArgumentException("Unknown reference mode "+binding.refKind);
                    }
                }
            }
            if(table instanceof astwire.v2.AlignedAttrData aligned) {
                store.set(pt.up.fe.specs.clava.ast.attr.AlignedAttr.ALIGNED_ATTR_KIND,
                        aligned.isExpression()?pt.up.fe.specs.clava.ast.attr.enums.AlignedAttrKind.EXPR:pt.up.fe.specs.clava.ast.attr.enums.AlignedAttrKind.TYPE);
                if(aligned.isExpression())context.data.getClavaNodes().queueSetOptionalNode(store,
                        pt.up.fe.specs.clava.ast.attr.AlignedExprAttr.EXPR,context.id(aligned.alignment()));
                else context.data.getClavaNodes().queueSetNode(store,
                        pt.up.fe.specs.clava.ast.attr.AlignedTypeAttr.TYPE,context.id(aligned.alignment()));
            }
            if(table instanceof astwire.v2.CXXConversionDeclData) {
                context.data.getClavaNodes().queueSetAction(store,pt.up.fe.specs.clava.ast.decl.NamedDecl.DECL_NAME,
                        pt.up.fe.specs.clava.ast.decl.CXXConversionDecl::buildDeclName);
            }
            return store;
        }

        private static void validateFunctionTemplateLists(Table table) {
            astwire.v2.FunctionDeclData function;
            if (table instanceof astwire.v2.FunctionDeclData direct) {
                function = direct;
            } else if (table instanceof astwire.v2.CXXMethodDeclData method) {
                function = method.base();
            } else if (table instanceof astwire.v2.CXXConstructorDeclData constructor) {
                function = constructor.base().base();
            } else if (table instanceof astwire.v2.CXXConversionDeclData conversion) {
                function = conversion.base().base();
            } else {
                return;
            }
            long total = 0;
            for (int i = 0; i < function.templateParameterListSizesLength(); i++) {
                int size = function.templateParameterListSizes(i);
                if (size < 0) {
                    throw new IllegalArgumentException("Negative function template parameter list size");
                }
                total += size;
            }
            if (total != function.templateParametersLength()) {
                throw new IllegalArgumentException("Function template parameters do not match their list sizes");
            }
        }

        private static void validateLambdaCaptureVectors(Table table) {
            if (!(table instanceof astwire.v2.LambdaExprData lambda)) {
                return;
            }

            int captureCount = lambda.captureKindsLength();
            requireCaptureVectorLength("init_capture_names", lambda.initCaptureNamesLength(), captureCount);
            requireCaptureVectorLength("capture_init_styles", lambda.captureInitStylesLength(), captureCount);
            requireCaptureVectorLength("capture_pack_expansions", lambda.capturePackExpansionsLength(), captureCount);
            requireCaptureVectorLength("capture_is_implicit", lambda.captureIsImplicitLength(), captureCount);
        }

        private static void requireCaptureVectorLength(String field, int actualLength, int captureCount) {
            if (actualLength != captureCount) {
                throw new IllegalArgumentException("LambdaExprData capture vector length mismatch: capture_kinds="
                        + captureCount + ", " + field + "=" + actualLength);
            }
        }
    }
}
