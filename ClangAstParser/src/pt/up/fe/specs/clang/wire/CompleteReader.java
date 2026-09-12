/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import astwire.v2.RecordPayload;
import astwire.v2.TopLevelKind;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.parsers.ClangAstPathResolver;
import pt.up.fe.specs.clang.parsers.ClangStreamParserV2;
import pt.up.fe.specs.clang.parsers.ClavaNodeParser;
import pt.up.fe.specs.clang.parsers.util.PragmasLocations;
import pt.up.fe.specs.clang.version.Clang_3_8;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.ast.extra.data.OpenCLVersion;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.util.utilities.CachedItems;

/** Reads the complete size-prefixed v2 AST wire stream into the existing Clava parser data. */
public final class CompleteReader {

    private CompleteReader() {
    }

    public record Result(ClangAstData data, SchemaRuntime.Stats stats) {
    }

    /**
     * Reads one complete v2 stream. The returned lookup tables remain available
     * until {@link #releaseLookup(ClangAstData)} is called by the production
     * multi-translation-unit pipeline.
     */
    public static Result read(Path path, ClavaContext context, File parseRoot, String scope, boolean lazy)
            throws IOException {

        ClangAstData data = ClangStreamParserV2.newInstance(context).getData();
        if(parseRoot!=null)data.set(ClangAstData.PARSE_ROOT, parseRoot);
        initializeData(data);

        var nodeParser = new ClavaNodeParser(Clang_3_8.getClassesService());
        nodeParser.init(data);

        var files = new SchemaRuntime.Files(scope);
        var importContext = new SchemaRuntime.ImportContext(data, files);
        Map<String, String> pendingClasses = new LinkedHashMap<>();
        boolean headerSeen = false;
        boolean endSeen = false;
        long records = 0;
        long nodes = 0;

        try (var mapped = new MappedRecords(path)) {
            MappedRecords.Frame frame;
            while ((frame = mapped.next()) != null) {
                var block=new astwire.v2.Block().__assign(frame.rootOffset(),frame.buffer());
                if(block.recordsVector()==null)throw new IOException("Missing block records");
                for(int recordIndex=0;recordIndex<block.recordsLength();recordIndex++) {
                if (endSeen) {
                    throw new IOException("Record found after End");
                }

                records++;
                var record = block.records(recordIndex);
                GeneratedNodes.validate(record);

                switch (record.payloadType()) {
                    case RecordPayload.Header -> {
                        if (headerSeen || records != 1) {
                            throw new IOException("Header must be the first and only header record");
                        }
                        var header = (astwire.v2.Header) record.payload(new astwire.v2.Header());
                        if (!GeneratedNodes.SCHEMA_HASH.equals(header.schemaHash())) {
                            throw new IOException("Unexpected v2 schema hash: " + header.schemaHash());
                        }
                        headerSeen = true;
                    }
                    case RecordPayload.File -> readFile(record, data, files);
                    case RecordPayload.Node -> {
                        readNode(record, data, files, importContext, lazy);
                        nodes++;
                        flushClass(recordNodeId(record, files), pendingClasses, nodeParser, data);
                    }
                    case RecordPayload.Children -> readChildren(record, data, files);
                    case RecordPayload.NodeClass -> readNodeClass(record, data, files, pendingClasses, nodeParser);
                    case RecordPayload.TopLevel -> readTopLevel(record, data, files);
                    case RecordPayload.Include -> readInclude(record, data);
                    case RecordPayload.Pragma -> readPragma(record, data);
                    case RecordPayload.TranslationUnitFile -> readTranslationUnitFile(record, data);
                    case RecordPayload.Counter -> {
                        // Counter records are retained in the stream for framing and
                        // diagnostics. The text parser has no corresponding data key.
                    }
                    case RecordPayload.Language -> readLanguage(record, data);
                    case RecordPayload.End -> {
                        readEnd(record, records, nodes, files);
                        endSeen = true;
                    }
                    default -> throw new IOException("Unknown v2 record payload type " + record.payloadType());
                }
                }
            }
        }

        if (!headerSeen) {
            throw new EOFException("Missing v2 Header record");
        }
        if (!endSeen) {
            throw new EOFException("Missing v2 End record");
        }
        if (!pendingClasses.isEmpty()) {
            throw new IOException("Missing node payload for NodeClass records: " + pendingClasses.keySet());
        }

        nodeParser.close(data);
        return new Result(data, files.stats);
    }

    /** Releases parser-only indexes after translation-unit normalization and post-processing. */
    public static void releaseLookup(ClangAstData data) {
        data.getClavaNodes().getNodes().clear();
        data.getClavaNodes().getQueuedActions().clear();
        data.get(ClangAstData.NODE_DATA).clear();
        data.get(ClangAstData.VISITED_CHILDREN).clear();
        data.get(ClangAstData.ID_TO_FILENAME_MAP).clear();
        if(data.hasValue(ClangAstData.SKIPPED_NODES_MAP))data.get(ClangAstData.SKIPPED_NODES_MAP).clear();
        data.get(ClangAstData.TOP_LEVEL_DECL_IDS).clear();
        data.get(ClangAstData.TOP_LEVEL_TYPE_IDS).clear();
        data.get(ClangAstData.TOP_LEVEL_ATTR_IDS).clear();
    }

    private static void initializeData(ClangAstData data) {
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

    private static void readFile(astwire.v2.Record record, ClangAstData data, SchemaRuntime.Files files) {
        var file = (astwire.v2.File) record.payload(new astwire.v2.File());
        long expectedId = files.paths.size();
        if (file.id() != expectedId) {
            throw new IllegalArgumentException("Non-contiguous wire file id " + file.id() + ", expected " + expectedId);
        }
        files.paths.add(cachePath(ClangAstPathResolver.resolve(file.path(), data), data));
    }

    private static void readNode(astwire.v2.Record record, ClangAstData data, SchemaRuntime.Files files,
            SchemaRuntime.ImportContext importContext, boolean lazy) {
        var node = (astwire.v2.Node) record.payload(new astwire.v2.Node());
        String id = files.id(node.id());
        if (data.get(ClangAstData.NODE_DATA).containsKey(id)) {
            throw new IllegalArgumentException("Duplicated wire node id " + id);
        }
        var payload = GeneratedNodes.payload(node);
        var descriptor = GeneratedNodes.descriptor(node.payloadType());
        if (descriptor == null) {
            throw new IllegalArgumentException("Missing descriptor for wire node payload " + node.payloadType());
        }
        DataStore nodeData = descriptor.read(payload, node.className(), id, importContext, lazy);
        data.get(ClangAstData.NODE_DATA).put(id, nodeData);
    }

    private static void readChildren(astwire.v2.Record record, ClangAstData data, SchemaRuntime.Files files) {
        var children = (astwire.v2.Children) record.payload(new astwire.v2.Children());
        String id = files.id(children.node());
        if (data.get(ClangAstData.VISITED_CHILDREN).putIfAbsent(id,
                SchemaRuntime.list(children.childrenLength(), i -> files.id(children.children(i)))) != null) {
            throw new IllegalArgumentException("Duplicated wire children record for " + id);
        }
    }

    private static void readNodeClass(astwire.v2.Record record, ClangAstData data, SchemaRuntime.Files files,
            Map<String, String> pendingClasses, ClavaNodeParser nodeParser) {
        var nodeClass = (astwire.v2.NodeClass) record.payload(new astwire.v2.NodeClass());
        String id = files.id(nodeClass.node());
        pendingClasses.putIfAbsent(id, nodeClass.className());
        flushClass(id, pendingClasses, nodeParser, data);
    }

    private static void flushClass(String id, Map<String, String> pendingClasses, ClavaNodeParser nodeParser,
            ClangAstData data) {
        String className = pendingClasses.get(id);
        if (className == null || !data.get(ClangAstData.NODE_DATA).containsKey(id)) {
            return;
        }
        nodeParser.applyRecord(id, className, data);
        pendingClasses.remove(id);
    }

    private static String recordNodeId(astwire.v2.Record record, SchemaRuntime.Files files) {
        return files.id(((astwire.v2.Node) record.payload(new astwire.v2.Node())).id());
    }

    private static void readTopLevel(astwire.v2.Record record, ClangAstData data, SchemaRuntime.Files files) {
        var topLevel = (astwire.v2.TopLevel) record.payload(new astwire.v2.TopLevel());
        String id = files.id(topLevel.node());
        switch (topLevel.kind()) {
            case TopLevelKind.Decl -> data.get(ClangAstData.TOP_LEVEL_DECL_IDS).add(id);
            case TopLevelKind.Type -> data.get(ClangAstData.TOP_LEVEL_TYPE_IDS).add(id);
            case TopLevelKind.Attr -> data.get(ClangAstData.TOP_LEVEL_ATTR_IDS).add(id);
            default -> throw new IllegalArgumentException("Unknown top-level kind " + topLevel.kind());
        }
    }

    private static void readInclude(astwire.v2.Record record, ClangAstData data) {
        var include = (astwire.v2.Include) record.payload(new astwire.v2.Include());
        File source = new File(ClangAstPathResolver.resolve(include.source(), data));
        data.get(ClangAstData.INCLUDES).add(new Include(source, include.name(), Math.toIntExact(include.line()),
                include.angled()));
    }

    private static void readPragma(astwire.v2.Record record, ClangAstData data) {
        var pragma = (astwire.v2.Pragma) record.payload(new astwire.v2.Pragma());
        File source = new File(ClangAstPathResolver.resolve(pragma.source(), data));
        data.get(ClangAstData.PRAGMAS_LOCATIONS).addPragmaLocation(source, Math.toIntExact(pragma.line()),
                Math.toIntExact(pragma.column()));
    }

    private static void readTranslationUnitFile(astwire.v2.Record record, ClangAstData data) {
        var file = (astwire.v2.TranslationUnitFile) record.payload(new astwire.v2.TranslationUnitFile());
        String path = ClangAstPathResolver.resolve(file.path(), data);
        data.get(ClangAstData.ID_TO_FILENAME_MAP).put(Integer.toString(file.id()), cachePath(path, data));
    }

    private static void readLanguage(astwire.v2.Record record, ClangAstData data) {
        var source = (astwire.v2.Language) record.payload(new astwire.v2.Language());
        var language = new Language()
                .set(Language.LINE_COMMENT, source.lineComment())
                .set(Language.GNU_INLINE, source.gnuInline())
                .set(Language.C99, source.c99())
                .set(Language.C11, source.c11())
                .set(Language.C_PLUS_PLUS, source.cPlusPlus())
                .set(Language.C_PLUS_PLUS_11, source.cPlusPlus11())
                .set(Language.C_PLUS_PLUS_14, source.cPlusPlus14())
                .set(Language.C_PLUS_PLUS_17, source.cPlusPlus17())
                .set(Language.C_PLUS_PLUS_20, source.cPlusPlus20())
                .set(Language.C_PLUS_PLUS_23, source.cPlusPlus23())
                .set(Language.C_PLUS_PLUS_26, source.cPlusPlus26())
                .set(Language.HAS_DIGRAPHS, source.hasDigraphs())
                .set(Language.IS_GNU, source.isGnu())
                .set(Language.HEX_FLOATS, source.hexFloats())
                .set(Language.OPEN_CL, source.openCl())
                .set(Language.OPEN_CL_VERSION, fromClangNumber(Math.toIntExact(source.openClVersion())))
                .set(Language.NATIVE_HALF_TYPE, source.nativeHalfType())
                .set(Language.CUDA, source.cuda())
                .set(Language.HAS_BOOL, source.hasBool())
                .set(Language.HAS_HALF, source.hasHalf())
                .set(Language.HAS_WCHAR, source.hasWchar())
                .set(Language.CHAR_WIDTH, Math.toIntExact(source.charWidth()))
                .set(Language.FLOAT_WIDTH, Math.toIntExact(source.floatWidth()))
                .set(Language.DOUBLE_WIDTH, Math.toIntExact(source.doubleWidth()))
                .set(Language.LONG_DOUBLE_WIDTH, Math.toIntExact(source.longDoubleWidth()))
                .set(Language.BOOL_WIDTH, Math.toIntExact(source.boolWidth()))
                .set(Language.SHORT_WIDTH, Math.toIntExact(source.shortWidth()))
                .set(Language.INT_WIDTH, Math.toIntExact(source.intWidth()))
                .set(Language.LONG_WIDTH, Math.toIntExact(source.longWidth()))
                .set(Language.LONG_LONG_WIDTH, Math.toIntExact(source.longLongWidth()));
        data.get(ClangAstData.FILE_LANGUAGE_DATA)
                .put(new File(ClangAstPathResolver.resolve(source.file(), data)), language);
    }

    private static OpenCLVersion fromClangNumber(int value) {
        return switch (value) {
            case 0 -> OpenCLVersion.NONE;
            case 100 -> OpenCLVersion.v1_0;
            case 110 -> OpenCLVersion.v1_1;
            case 120 -> OpenCLVersion.v1_2;
            case 200 -> OpenCLVersion.v2_0;
            default -> throw new IllegalArgumentException("Unsupported OpenCL version " + value);
        };
    }

    private static void readEnd(astwire.v2.Record record, long records, long nodes, SchemaRuntime.Files files) {
        var end = (astwire.v2.End) record.payload(new astwire.v2.End());
        if (end.records() != records - 1 || end.nodes() != nodes || end.files() != files.paths.size() - 1) {
            throw new IllegalArgumentException("v2 End count mismatch: records=" + end.records() + "/" + (records - 1)
                    + ", nodes=" + end.nodes() + "/" + nodes + ", files=" + end.files() + "/"
                    + (files.paths.size() - 1));
        }
    }

    private static String cachePath(String path, ClangAstData data) {
        CachedItems<String, String> cached = data.get(ClangAstData.CONTEXT).get(ClavaContext.CACHED_FILEPATHS);
        return cached.get(path);
    }
}
