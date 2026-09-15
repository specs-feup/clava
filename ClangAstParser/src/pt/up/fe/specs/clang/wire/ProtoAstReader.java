/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.parsers.ClangAstPathResolver;
import pt.up.fe.specs.clang.parsers.ClavaNodeParser;
import pt.up.fe.specs.clang.parsers.util.PragmasLocations;
import pt.up.fe.specs.clang.version.Clang_3_8;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.ast.extra.data.OpenCLVersion;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.util.utilities.CachedItems;

/** Eager, bounded reader for the v1 protobuf Clava AST stream. */
public final class ProtoAstReader {

    private static final byte[] MAGIC = new byte[] { 'C', 'L', 'A', 'V', 'A', 'P', 'B', '1' };
    public static final String PROTOCOL_ID = "clava-ast-wire";
    public static final int PROTOCOL_MAJOR = 1;
    public static final int PROTOCOL_MINOR = 0;
    public static final String PRODUCER_VERSION = "clang-dumper-18";
    public static final int LLVM_MAJOR = 18;

    private ProtoAstReader() {
    }

    public record Metrics(long frames, long encodedBytes, long protobufDecodeNanos,
            long astConstructionNanos, long recordConstructionNanos,
            long referenceResolutionNanos, long records, long nodes, long files) {
    }

    public record Result(ClangAstData data, Metrics metrics) {
    }

    /** Returns the generated schema fingerprint used in the stream header. */
    public static String schemaHash() {
        return ProtoSchemaHash.VALUE;
    }

    /** File table and id conversion shared by node and auxiliary records. */
    static final class Files {
        private final String scope;
        private final List<String> paths = new ArrayList<>(List.of(""));
        private final Set<Long> referencedDenseIds = new HashSet<>();
        private long maxDenseId;

        Files(String scope) {
            this.scope = scope;
        }

        String id(long value) {
            if (value > 0) {
                referencedDenseIds.add(value);
                maxDenseId = Math.max(maxDenseId, value);
                return "@" + value + "_" + scope;
            }
            return switch ((int) value) {
                case -1 -> "nullptr_type";
                case -2 -> "nullptr_decl";
                case -3 -> "nullptr_expr";
                case -4 -> "nullptr_stmt";
                case -5 -> "nullptr_attr";
                case -6 -> "0_" + scope;
                default -> throw new IllegalArgumentException("Invalid dense node reference " + value);
            };
        }

        void validateDenseIds(long count, Set<Long> definedDenseIds) {
            if (count < 0 || count != maxDenseId || count != definedDenseIds.size()) {
                throw new ProtocolException("End.ids does not describe the exact dense id set: " + count
                        + " (largest=" + maxDenseId + ", defined=" + definedDenseIds.size() + ")");
            }
            if (!referencedDenseIds.equals(definedDenseIds)) {
                Set<Long> missing = new HashSet<>(referencedDenseIds);
                missing.removeAll(definedDenseIds);
                Set<Long> unreferenced = new HashSet<>(definedDenseIds);
                unreferenced.removeAll(referencedDenseIds);
                throw new ProtocolException("Dense references do not match Node definitions: missing=" + missing
                        + ", unreferenced=" + unreferenced);
            }
        }

        String path(long value) {
            if (value < 1 || value >= paths.size()) {
                throw new IllegalArgumentException("Undefined wire file id " + value);
            }
            return paths.get((int) value);
        }

        pt.up.fe.specs.clava.SourceRange range(Message range) {
            if (range == null) {
                return pt.up.fe.specs.clava.SourceRange.invalidRange();
            }
            Range value = (Range) range;
            if (!value.hasFile() || !value.hasLine() || !value.hasColumn()) {
                throw new IllegalArgumentException("Source range requires file, line and column");
            }
            var start = new pt.up.fe.specs.clava.SourceLocation(path(value.getFile()),
                    Math.toIntExact(value.getLine()), Math.toIntExact(value.getColumn()), false);
            // The native encoder always writes all end fields. A zero end file
            // is its explicit representation of a one-point range, not a file
            // table reference.
            if (!value.hasEndFile() || value.getEndFile() == 0) {
                return new pt.up.fe.specs.clava.SourceRange(start);
            }
            if (!value.hasEndLine() || !value.hasEndColumn()) {
                throw new IllegalArgumentException("Source range end requires file, line and column");
            }
            var end = new pt.up.fe.specs.clava.SourceLocation(path(value.getEndFile()),
                    Math.toIntExact(value.getEndLine()), Math.toIntExact(value.getEndColumn()), false);
            return new pt.up.fe.specs.clava.SourceRange(start, end);
        }
    }

    public static Result read(Path path, ClavaContext context, java.io.File parseRoot, String scope) throws IOException {
        try (InputStream input = java.nio.file.Files.newInputStream(path)) {
            return read(input, context, parseRoot, scope);
        }
    }

    public static Result read(InputStream input, ClavaContext context, java.io.File parseRoot, String scope) throws IOException {
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("A non-empty translation-unit scope is required");
        }
        if (context == null) {
            throw new IllegalArgumentException("ClavaContext is required");
        }

        readMagic(input);
        ClangAstData data = new ClangAstData();
        data.set(ClangAstData.CONTEXT, context);
        if (parseRoot != null) {
            data.set(ClangAstData.PARSE_ROOT, parseRoot);
        }
        initialize(data);

        var nodeParser = new ClavaNodeParser(Clang_3_8.getClassesService());
        nodeParser.init(data);
        Files files = new Files(scope);
        Map<String, String> pendingClasses = new LinkedHashMap<>();
        Map<String, String> nodeClasses = new HashMap<>();
        Set<String> classesSeen = new HashSet<>();
        Set<String> childrenSeen = new HashSet<>();
        Set<String> nodeIdsSeen = new HashSet<>();
        Set<Long> denseNodeIds = new HashSet<>();
        MetricsAccumulator metrics = new MetricsAccumulator();
        boolean[] headerSeen = { false };
        boolean[] endSeen = { false };
        long[] records = { 0 };
        long[] nodes = { 0 };
        End[] endRecord = { null };
        long[] endFrameBytes = { -1 };

        FramedProtobufReader frames = new FramedProtobufReader(input);
        try {
            frames.read(encoded -> {
                long decodeStart = System.nanoTime();
                try {
                    Envelope envelope = Envelope.parseFrom(encoded);
                    metrics.protobufDecodeNanos += System.nanoTime() - decodeStart;
                    return envelope;
                } catch (InvalidProtocolBufferException e) {
                    throw new ProtocolException("Malformed Envelope", e);
                }
            }, envelope -> {
                long constructionStart = System.nanoTime();
                records[0]++;
                if (endSeen[0]) {
                    throw new ProtocolException("Record found after End");
                }
                switch (envelope.getPayloadCase()) {
                    case HEADER -> readHeader(envelope.getHeader(), headerSeen[0]);
                    case RECORD -> {
                        if (!headerSeen[0]) {
                            throw new ProtocolException("Header must be the first Envelope");
                        }
                        readRecord(envelope.getRecord(), data, files, nodeParser, pendingClasses, nodeClasses,
                                classesSeen, childrenSeen, nodeIdsSeen, denseNodeIds, nodes);
                    }
                    case END -> {
                        if (!headerSeen[0]) {
                            throw new ProtocolException("Header must be the first Envelope");
                        }
                        readEnd(envelope.getEnd(), records[0], nodes[0], files);
                        endRecord[0] = envelope.getEnd();
                        endFrameBytes[0] = varintSize(envelope.getSerializedSize()) + envelope.getSerializedSize();
                        endSeen[0] = true;
                    }
                    case PAYLOAD_NOT_SET -> throw new ProtocolException("Envelope payload is required");
                }
                if (envelope.getPayloadCase() == Envelope.PayloadCase.HEADER) {
                    headerSeen[0] = true;
                }
                long recordConstructionNanos = System.nanoTime() - constructionStart;
                metrics.recordConstructionNanos += recordConstructionNanos;
                metrics.astConstructionNanos += recordConstructionNanos;
            });
        } catch (ProtocolException e) {
            throw new IOException("Invalid protobuf AST stream: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new IOException("Invalid protobuf AST record: " + e.getMessage(), e);
        }

        if (!headerSeen[0]) {
            throw new EOFException("Missing protobuf Header");
        }
        if (!endSeen[0]) {
            throw new EOFException("Missing protobuf End");
        }
        if (!pendingClasses.isEmpty()) {
            throw new IOException("Missing node payload for NodeClass records: " + pendingClasses.keySet());
        }
        if (endRecord[0] == null || endFrameBytes[0] < 0) {
            throw new IOException("Missing protobuf End counters");
        }
        long expectedRawBytes = MAGIC.length + frames.encodedBytes() - endFrameBytes[0];
        if (endRecord[0].getRawBytes() != expectedRawBytes) {
            throw new IOException("End.raw_bytes mismatch: " + endRecord[0].getRawBytes() + "/" + expectedRawBytes);
        }
        try {
            validateTopLevelReferences(data);
            files.validateDenseIds(endRecord[0].getIds(), denseNodeIds);
            if (!classesSeen.equals(nodeIdsSeen)) {
                Set<String> missing = new HashSet<>(nodeIdsSeen);
                missing.removeAll(classesSeen);
                Set<String> orphan = new HashSet<>(classesSeen);
                orphan.removeAll(nodeIdsSeen);
                throw new ProtocolException("NodeClass coverage does not match Nodes: missing=" + missing
                        + ", orphan=" + orphan);
            }
            if (!childrenSeen.equals(nodeIdsSeen)) {
                Set<String> missing = new HashSet<>(nodeIdsSeen);
                missing.removeAll(childrenSeen);
                Set<String> orphan = new HashSet<>(childrenSeen);
                orphan.removeAll(nodeIdsSeen);
                throw new ProtocolException("Children coverage does not match Nodes: missing=" + missing
                        + ", orphan=" + orphan);
            }
        } catch (ProtocolException e) {
            throw new IOException("Invalid protobuf AST references: " + e.getMessage(), e);
        }

        try {
            long resolveStart = System.nanoTime();
            nodeParser.close(data);
            long referenceResolutionNanos = System.nanoTime() - resolveStart;
            metrics.referenceResolutionNanos += referenceResolutionNanos;
            metrics.astConstructionNanos += referenceResolutionNanos;
        } catch (RuntimeException e) {
            throw new IOException("Could not resolve protobuf AST references", e);
        }
        metrics.frames = frames.frameIndex();
        metrics.encodedBytes = frames.encodedBytes();
        return new Result(data, metrics.toMetrics(records[0], nodes[0], files.paths.size() - 1));
    }

    private static void readMagic(InputStream input) throws IOException {
        byte[] actual = input.readNBytes(MAGIC.length);
        if (actual.length != MAGIC.length) {
            throw new EOFException("Truncated protobuf AST magic");
        }
        for (int i = 0; i < MAGIC.length; i++) {
            if (actual[i] != MAGIC[i]) {
                throw new IOException("Invalid protobuf AST magic");
            }
        }
    }

    private static void readHeader(Header header, boolean alreadySeen) {
        if (alreadySeen) {
            throw new ProtocolException("Header must occur exactly once and first");
        }
        validateHeader(header);
    }

    /**
     * Validates the typed protocol header without constructing an AST. This is
     * used by resource probing, where a dumper invocation must be checked as a
     * protobuf producer before it is selected for a real parse.
     */
    public static void validateHeader(Header header) {
        if (header == null) {
            throw new ProtocolException("Protobuf Header is required");
        }
        if (!header.hasProtocolMajor() || !header.hasProtocolMinor() || !header.hasSchemaId()
                || !header.hasProducerVersion() || !header.hasLlvmMajor() || !header.hasSchemaSha256()) {
            throw new ProtocolException("Header is missing a required field");
        }
        if (header.getProtocolMajor() != PROTOCOL_MAJOR || header.getProtocolMinor() > PROTOCOL_MINOR) {
            throw new ProtocolException("Unsupported protobuf protocol version " + header.getProtocolMajor() + "."
                    + header.getProtocolMinor());
        }
        if (!PROTOCOL_ID.equals(header.getSchemaId())) {
            throw new ProtocolException("Unexpected protobuf schema id '" + header.getSchemaId() + "'");
        }
        if (!PRODUCER_VERSION.equals(header.getProducerVersion())) {
            throw new ProtocolException("Unexpected protobuf producer '" + header.getProducerVersion() + "'");
        }
        if (header.getLlvmMajor() != LLVM_MAJOR) {
            throw new ProtocolException("Unexpected LLVM major " + header.getLlvmMajor());
        }
        if (!ProtoSchemaHash.VALUE.equals(header.getSchemaSha256().toStringUtf8())) {
            throw new ProtocolException("Protobuf schema hash does not match the generated binding");
        }
    }

    private static void readRecord(Record record, ClangAstData data, Files files, ClavaNodeParser nodeParser,
            Map<String, String> pendingClasses, Map<String, String> nodeClasses, Set<String> classesSeen,
            Set<String> childrenSeen, Set<String> nodeIdsSeen, Set<Long> denseNodeIds, long[] nodes) {
        switch (record.getRecordCase()) {
            case FILE -> readFile(record.getFile(), data, files);
            case NODE -> {
                Node node = record.getNode();
                if (!node.hasId() || node.getId() <= 0) {
                    throw new ProtocolException("Node.id must be a positive dense id");
                }
                if (!node.hasClassName() || node.getClassName().isBlank()) {
                    throw new ProtocolException("Node.class_name is required for id " + node.getId());
                }
                validateNodePayload(node);
                String id = files.id(node.getId());
                if (!nodeIdsSeen.add(id)) {
                    throw new ProtocolException("Duplicated Node " + id);
                }
                if (!denseNodeIds.add(node.getId())) {
                    throw new ProtocolException("Duplicated Node dense id " + node.getId());
                }
                checkNodeClass(id, node.getClassName(), nodeClasses);
                DataStore nodeData = ProtoNodeDataReader.read(node, data, files::id, files);
                data.get(ClangAstData.NODE_DATA).put(id, nodeData);
                nodes[0]++;
                flushClass(id, pendingClasses, nodeParser, data);
            }
            case CHILDREN -> {
                Children children = record.getChildren();
                String id = files.id(required(children.hasNode(), children.getNode(), "Children.node"));
                if (!childrenSeen.add(id)) {
                    throw new ProtocolException("Duplicated Children " + id);
                }
                List<String> ids = new ArrayList<>(children.getChildrenCount());
                for (long child : children.getChildrenList()) {
                    ids.add(files.id(child));
                }
                data.get(ClangAstData.VISITED_CHILDREN).put(id, ids);
            }
            case NODE_CLASS -> {
                NodeClass nodeClass = record.getNodeClass();
                String id = files.id(required(nodeClass.hasNode(), nodeClass.getNode(), "NodeClass.node"));
                if (!nodeClass.hasClassName() || nodeClass.getClassName().isBlank()) {
                    throw new ProtocolException("NodeClass.class_name is required for " + id);
                }
                if (classesSeen.add(id) == false) {
                    throw new ProtocolException("Duplicated NodeClass " + id);
                }
                checkNodeClass(id, nodeClass.getClassName(), nodeClasses);
                pendingClasses.put(id, nodeClass.getClassName());
                flushClass(id, pendingClasses, nodeParser, data);
            }
            case TOP_LEVEL -> readTopLevel(record.getTopLevel(), data, files);
            case INCLUDE -> readInclude(record.getInclude(), data);
            case PRAGMA -> readPragma(record.getPragma(), data);
            case TRANSLATION_UNIT_FILE -> readTranslationUnitFile(record.getTranslationUnitFile(), data);
            case COUNTER -> {
                if (!record.getCounter().hasValue()) {
                    throw new ProtocolException("Counter.value is required");
                }
            }
            case LANGUAGE -> readLanguage(record.getLanguage(), data);
            case RECORD_NOT_SET -> throw new ProtocolException("Record payload is required");
        }
    }

    private static long required(boolean present, long value, String field) {
        if (!present) {
            throw new ProtocolException(field + " is required");
        }
        return value;
    }

    private static void flushClass(String id, Map<String, String> pendingClasses, ClavaNodeParser nodeParser,
            ClangAstData data) {
        String className = pendingClasses.get(id);
        if (className != null && data.get(ClangAstData.NODE_DATA).containsKey(id)) {
            nodeParser.applyRecord(id, className, data);
            pendingClasses.remove(id);
        }
    }

    private static void checkNodeClass(String id, String className, Map<String, String> nodeClasses) {
        String previous = nodeClasses.putIfAbsent(id, className);
        if (previous != null && !previous.equals(className)) {
            throw new ProtocolException("NodeClass does not match Node for " + id + ": '" + previous + "'/'"
                    + className + "'");
        }
    }

    private static void validateNodePayload(Node node) {
        String className = node.getClassName();
        Node.NodeCase payload = node.getNodeCase();
        Node.NodeCase alias = switch (className) {
            case "CXXDestructorDecl" -> Node.NodeCase.C_X_X_METHOD_DECL_DATA;
            case "ObjCImplementationDecl", "UsingShadowDecl", "LabelDecl" -> Node.NodeCase.NAMED_DECL_DATA;
            case "ClassTemplateDecl", "FunctionTemplateDecl", "TypeAliasTemplateDecl", "VarTemplateDecl" ->
                    Node.NodeCase.TEMPLATE_DECL_DATA;
            case "EnumConstantDecl" -> Node.NodeCase.VALUE_DECL_DATA;
            case "TypeAliasDecl", "TypedefDecl" -> Node.NodeCase.TYPEDEF_NAME_DECL_DATA;
            case "VarTemplateSpecializationDecl" -> Node.NodeCase.VAR_DECL_DATA;
            case "CXXFunctionalCastExpr" -> Node.NodeCase.CAST_EXPR_DATA;
            case "CStyleCastExpr" -> Node.NodeCase.EXPLICIT_CAST_EXPR_DATA;
            case "CXXAddrspaceCastExpr", "CXXConstCastExpr", "CXXDynamicCastExpr", "CXXReinterpretCastExpr",
                    "CXXStaticCastExpr" -> Node.NodeCase.C_X_X_NAMED_CAST_EXPR_DATA;
            case "CXXOperatorCallExpr", "UserDefinedLiteral" -> Node.NodeCase.CALL_EXPR_DATA;
            case "CompoundAssignOperator" -> Node.NodeCase.BINARY_OPERATOR_DATA;
            case "FunctionNoProtoType" -> Node.NodeCase.FUNCTION_TYPE_DATA;
            case "IncompleteArrayType" -> Node.NodeCase.ARRAY_TYPE_DATA;
            case "RecordType", "EnumType" -> Node.NodeCase.TAG_TYPE_DATA;
            case "LValueReferenceType", "RValueReferenceType" -> Node.NodeCase.REFERENCE_TYPE_DATA;
            default -> null;
        };
        if (alias != null) {
            if (payload != alias) {
                throw new ProtocolException("Node payload " + payload + " does not match " + className);
            }
            return;
        }

        String expected = normalize(className + "Data");
        String actual = normalize(payload.name());
        if (expected.equals(actual)) {
            return;
        }

        boolean familyMatches = (className.endsWith("Decl") && payload.name().endsWith("_DECL_DATA"))
                || (className.endsWith("Type") && payload.name().endsWith("_TYPE_DATA"))
                || (className.endsWith("Expr") && payload.name().endsWith("_EXPR_DATA"))
                || (className.endsWith("Stmt") && payload.name().endsWith("_STMT_DATA"))
                || (className.endsWith("Attr") && (payload.name().endsWith("_ATTR_DATA")
                        || payload == Node.NodeCase.ATTRIBUTE_DATA));
        boolean genericBase = payload == Node.NodeCase.DECL_DATA || payload == Node.NodeCase.TYPE_DATA
                || payload == Node.NodeCase.EXPR_DATA || payload == Node.NodeCase.STMT_DATA
                || payload == Node.NodeCase.ATTRIBUTE_DATA;
        if (!familyMatches || !genericBase) {
            throw new ProtocolException("Node payload " + payload + " does not match " + className);
        }
    }

    private static String normalize(String value) {
        return value.replace("_", "").toLowerCase(java.util.Locale.ROOT);
    }

    private static void readFile(File file, ClangAstData data, Files files) {
        if (!file.hasId() || !file.hasPath() || file.getPath().isBlank()) {
            throw new ProtocolException("File.id and File.path are required");
        }
        if (file.getId() != files.paths.size()) {
            throw new ProtocolException("Non-contiguous wire file id " + file.getId() + ", expected " + files.paths.size());
        }
        files.paths.add(cachePath(ClangAstPathResolver.resolve(file.getPath(), data), data));
    }

    private static void readTopLevel(TopLevel topLevel, ClangAstData data, Files files) {
        if (!topLevel.hasKind() || !topLevel.hasNode()) {
            throw new ProtocolException("TopLevel.kind and TopLevel.node are required");
        }
        String id = files.id(topLevel.getNode());
        switch (topLevel.getKind()) {
            case TOPLEVELKIND_DECL -> addTopLevel(data.get(ClangAstData.TOP_LEVEL_DECL_IDS), id);
            case TOPLEVELKIND_TYPE -> addTopLevel(data.get(ClangAstData.TOP_LEVEL_TYPE_IDS), id);
            case TOPLEVELKIND_ATTR -> addTopLevel(data.get(ClangAstData.TOP_LEVEL_ATTR_IDS), id);
            case TOPLEVELKIND_UNSPECIFIED, UNRECOGNIZED -> throw new ProtocolException("Unknown top-level kind");
        }
    }

    private static void addTopLevel(Set<String> ids, String id) {
        // The historical parser stores top-level records in sets. The native
        // producer may repeat a node when it is reachable through more than
        // one declaration path, so preserve that set semantics on the wire.
        ids.add(id);
    }

    private static void readInclude(pt.up.fe.specs.clang.wire.Include include, ClangAstData data) {
        if (!include.hasSource() || !include.hasName() || !include.hasLine() || !include.hasAngled()) {
            throw new ProtocolException("Include source, name, line and angled are required");
        }
        java.io.File source = new java.io.File(ClangAstPathResolver.resolve(include.getSource(), data));
        data.get(ClangAstData.INCLUDES).add(new pt.up.fe.specs.clava.Include(source, include.getName(),
                Math.toIntExact(include.getLine()), include.getAngled()));
    }

    private static void readPragma(Pragma pragma, ClangAstData data) {
        if (!pragma.hasSource() || !pragma.hasLine() || !pragma.hasColumn()) {
            throw new ProtocolException("Pragma source, line and column are required");
        }
        java.io.File source = new java.io.File(ClangAstPathResolver.resolve(pragma.getSource(), data));
        data.get(ClangAstData.PRAGMAS_LOCATIONS).addPragmaLocation(source, Math.toIntExact(pragma.getLine()),
                Math.toIntExact(pragma.getColumn()));
    }

    private static void readTranslationUnitFile(TranslationUnitFile value, ClangAstData data) {
        if (!value.hasId() || !value.hasPath()) {
            throw new ProtocolException("TranslationUnitFile.id and path are required");
        }
        String path = ClangAstPathResolver.resolve(value.getPath(), data);
        Map<String, String> map = data.get(ClangAstData.ID_TO_FILENAME_MAP);
        if (map.containsKey(Integer.toString(value.getId()))) {
            throw new ProtocolException("Duplicated TranslationUnitFile " + value.getId());
        }
        map.put(Integer.toString(value.getId()), cachePath(path, data));
    }

    private static void readLanguage(Language value, ClangAstData data) {
        var missing = new ArrayList<String>();
        for (var field : value.getDescriptorForType().getFields()) {
            if (!value.hasField(field)) {
                missing.add(field.getName());
            }
        }
        if (!missing.isEmpty()) {
            throw new ProtocolException("Language is missing required fields: " + missing);
        }
        if (value.getFile().isBlank()) {
            throw new ProtocolException("Language.file must not be blank");
        }
        var language = new pt.up.fe.specs.clava.ast.extra.data.Language()
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.LINE_COMMENT, value.getLineComment())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.GNU_INLINE, value.getGnuInline())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.C99, value.getC99())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.C11, value.getC11())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.C_PLUS_PLUS, value.getCPlusPlus())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.C_PLUS_PLUS_11, value.getCPlusPlus11())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.C_PLUS_PLUS_14, value.getCPlusPlus14())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.C_PLUS_PLUS_17, value.getCPlusPlus17())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.C_PLUS_PLUS_20, value.getCPlusPlus20())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.C_PLUS_PLUS_23, value.getCPlusPlus23())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.C_PLUS_PLUS_26, value.getCPlusPlus26())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.HAS_DIGRAPHS, value.getHasDigraphs())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.IS_GNU, value.getIsGnu())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.HEX_FLOATS, value.getHexFloats())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.OPEN_CL, value.getOpenCl())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.OPEN_CL_VERSION, fromClangNumber(Math.toIntExact(value.getOpenClVersion())))
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.NATIVE_HALF_TYPE, value.getNativeHalfType())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.CUDA, value.getCuda())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.HAS_BOOL, value.getHasBool())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.HAS_HALF, value.getHasHalf())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.HAS_WCHAR, value.getHasWchar())
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.CHAR_WIDTH, Math.toIntExact(value.getCharWidth()))
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.FLOAT_WIDTH, Math.toIntExact(value.getFloatWidth()))
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.DOUBLE_WIDTH, Math.toIntExact(value.getDoubleWidth()))
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.LONG_DOUBLE_WIDTH, Math.toIntExact(value.getLongDoubleWidth()))
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.BOOL_WIDTH, Math.toIntExact(value.getBoolWidth()))
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.SHORT_WIDTH, Math.toIntExact(value.getShortWidth()))
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.INT_WIDTH, Math.toIntExact(value.getIntWidth()))
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.LONG_WIDTH, Math.toIntExact(value.getLongWidth()))
                .set(pt.up.fe.specs.clava.ast.extra.data.Language.LONG_LONG_WIDTH, Math.toIntExact(value.getLongLongWidth()));
        java.io.File file = new java.io.File(ClangAstPathResolver.resolve(value.getFile(), data));
        if (data.get(ClangAstData.FILE_LANGUAGE_DATA).put(file, language) != null) {
            throw new ProtocolException("Duplicated Language record for " + file);
        }
    }

    private static OpenCLVersion fromClangNumber(int value) {
        return switch (value) {
            case 0 -> OpenCLVersion.NONE;
            case 100 -> OpenCLVersion.v1_0;
            case 110 -> OpenCLVersion.v1_1;
            case 120 -> OpenCLVersion.v1_2;
            case 200 -> OpenCLVersion.v2_0;
            default -> throw new ProtocolException("Unsupported OpenCL version " + value);
        };
    }

    private static void readEnd(End end, long records, long nodes, Files files) {
        if (!end.hasRecords() || !end.hasNodes() || !end.hasRawBytes() || !end.hasFiles() || !end.hasIds()) {
            throw new ProtocolException("End is missing a required counter");
        }
        if (end.getRecords() != records || end.getNodes() != nodes || end.getFiles() != files.paths.size() - 1) {
            throw new ProtocolException("End count mismatch: records=" + end.getRecords() + "/" + records
                    + ", nodes=" + end.getNodes() + "/" + nodes + ", files=" + end.getFiles() + "/"
                    + (files.paths.size() - 1));
        }
    }

    private static void validateTopLevelReferences(ClangAstData data) {
        Map<String, DataStore> nodeData = data.get(ClangAstData.NODE_DATA);
        for (Set<String> ids : List.of(data.get(ClangAstData.TOP_LEVEL_DECL_IDS),
                data.get(ClangAstData.TOP_LEVEL_TYPE_IDS), data.get(ClangAstData.TOP_LEVEL_ATTR_IDS))) {
            for (String id : ids) {
                if (!pt.up.fe.specs.clang.parsers.ClavaNodes.isNullId(id) && !nodeData.containsKey(id)) {
                    throw new ProtocolException("Top-level reference does not name a Node: " + id);
                }
            }
        }
    }

    private static int varintSize(long value) {
        int size = 1;
        while ((value >>>= 7) != 0) {
            size++;
        }
        return size;
    }

    private static void initialize(ClangAstData data) {
        data.set(ClangAstData.NODE_DATA, new HashMap<>());
        data.set(ClangAstData.VISITED_CHILDREN, new HashMap<>());
        data.set(ClangAstData.ID_TO_FILENAME_MAP, new LinkedHashMap<>());
        data.set(ClangAstData.INCLUDES, new ArrayList<>());
        data.set(ClangAstData.TOP_LEVEL_DECL_IDS, new LinkedHashSet<>());
        data.set(ClangAstData.TOP_LEVEL_TYPE_IDS, new LinkedHashSet<>());
        data.set(ClangAstData.TOP_LEVEL_ATTR_IDS, new LinkedHashSet<>());
        data.set(ClangAstData.FILE_LANGUAGE_DATA, new LinkedHashMap<>());
        data.set(ClangAstData.PRAGMAS_LOCATIONS, new PragmasLocations());
    }

    private static String cachePath(String path, ClangAstData data) {
        CachedItems<String, String> cached = data.get(ClangAstData.CONTEXT).get(ClavaContext.CACHED_FILEPATHS);
        return cached.get(path);
    }

    private static final class MetricsAccumulator {
        long frames;
        long encodedBytes;
        long protobufDecodeNanos;
        long astConstructionNanos;
        long recordConstructionNanos;
        long referenceResolutionNanos;

        Metrics toMetrics(long records, long nodes, long files) {
            return new Metrics(frames, encodedBytes, protobufDecodeNanos, astConstructionNanos,
                    recordConstructionNanos, referenceResolutionNanos, records, nodes, files);
        }
    }

    private static final class ProtocolException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        ProtocolException(String message) {
            super(message);
        }

        ProtocolException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
