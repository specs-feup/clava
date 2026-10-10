/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.parsers.ClangAstPathResolver;
import pt.up.fe.specs.clang.parsers.ClavaNodeParser;
import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clang.parsers.util.PragmasLocations;
import pt.up.fe.specs.clang.version.Clang_3_8;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.SourceLocation;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.ast.extra.data.OpenCLVersion;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.utils.ClassesService;
import pt.up.fe.specs.util.utilities.CachedItems;

/**
 * Reads a clang-dumper AST stream into parser data for one translation unit.
 *
 * <p>
 * The stream is a sequence of size-prefixed FlatBuffers {@code Block}s (file identifier {@code CLV2}), each holding
 * {@code Record}s. It starts with a {@code Header} carrying the schema hash and ends with {@code End}. A {@code Node}
 * record follows the records of all its children, so it becomes a Clava node as soon as it is read; references are
 * resolved after the last record. Nodes whose class Clava does not model are skipped, and only fail if a supported
 * node references them.
 */
public final class WireReader {

    private static final int BLOCK_IDENTIFIER = 0x32564c43; // "CLV2"

    /** One translation unit's reading context: parser data, file table and node-id scope. */
    public record Tu(ClangAstData data, ClavaNodes nodes, String scope, List<String> paths) {

        /** The parser id of a wire reference: positive ids are local nodes, -1 to -6 are typed null nodes. */
        public String id(long value) {
            if (value > 0) {
                return "@" + value + "_" + scope;
            }
            if (value < -6) {
                throw new IllegalArgumentException("Invalid wire node reference " + value);
            }
            return switch ((int) value) {
                case -1 -> "nullptr_type";
                case -2 -> "nullptr_decl";
                case -3 -> "nullptr_expr";
                case -4 -> "nullptr_stmt";
                case -5 -> "nullptr_attr";
                case -6 -> "0_" + scope;
                default -> throw new IllegalArgumentException("Invalid wire node reference " + value);
            };
        }

        SourceRange range(WireTable range) {
            var start = new SourceLocation(path(range.integer("file")), (int) range.integer("line"),
                    (int) range.integer("column"), false);
            if (range.integer("end_file") == 0) {
                return new SourceRange(start);
            }
            return new SourceRange(start, new SourceLocation(path(range.integer("end_file")),
                    (int) range.integer("end_line"), (int) range.integer("end_column"), false));
        }

        private String path(long file) {
            if (file < 1 || file >= paths.size()) {
                throw new IllegalArgumentException("Undefined wire file " + file);
            }
            return paths.get((int) file);
        }
    }

    private final WireSchema schema = WireSchema.get();
    private final Tu tu;
    private final ClavaNodeParser nodeParser = new ClavaNodeParser(Clang_3_8.getClassesService());
    /** Ids of emitted nodes whose class Clava does not model. */
    private final Set<String> unsupported = new HashSet<>();
    private boolean headerSeen, endSeen;

    private WireReader(ClangAstData data, String scope) {
        nodeParser.init(data);
        tu = new Tu(data, data.getClavaNodes(), scope, new ArrayList<>(List.of("")));
    }

    /**
     * Reads the AST stream in {@code dump}. {@code scope} makes node ids unique across translation units, and
     * {@code parseRoot} resolves relative paths of generated sources.
     */
    public static ClangAstData read(Path dump, ClavaContext context, File parseRoot, String scope)
            throws IOException {
        var data = new ClangAstData();
        data.set(ClangAstData.CONTEXT, context);
        if (parseRoot != null) {
            data.set(ClangAstData.PARSE_ROOT, parseRoot);
        }
        data.set(ClangAstData.NODE_DATA, new HashMap<>());
        data.set(ClangAstData.VISITED_CHILDREN, new HashMap<>());
        data.set(ClangAstData.ID_TO_FILENAME_MAP, new LinkedHashMap<>());
        data.set(ClangAstData.INCLUDES, new ArrayList<>());
        data.set(ClangAstData.TOP_LEVEL_DECL_IDS, new LinkedHashSet<>());
        data.set(ClangAstData.TOP_LEVEL_TYPE_IDS, new LinkedHashSet<>());
        data.set(ClangAstData.TOP_LEVEL_ATTR_IDS, new LinkedHashSet<>());
        data.set(ClangAstData.FILE_LANGUAGE_DATA, new LinkedHashMap<>());
        data.set(ClangAstData.PRAGMAS_LOCATIONS, new PragmasLocations());

        var reader = new WireReader(data, scope);
        try (var input = new DataInputStream(new BufferedInputStream(Files.newInputStream(dump)))) {
            reader.readBlocks(input);
        } catch (RuntimeException e) {
            throw new IOException("Invalid AST stream '" + dump + "': " + e.getMessage(), e);
        }
        if (!reader.headerSeen || !reader.endSeen) {
            throw new EOFException("Incomplete AST stream '" + dump + "': missing " + (reader.headerSeen ? "End" : "Header"));
        }
        try {
            reader.nodeParser.close(data);
        } catch (RuntimeException e) {
            throw new IOException("Could not resolve references in '" + dump + "'", e);
        }
        return data;
    }

    /** Releases parser-only indexes once the translation unit has been built and normalized. */
    public static void releaseLookup(ClangAstData data) {
        data.getClavaNodes().getNodes().clear();
        data.getClavaNodes().getQueuedActions().clear();
        data.get(ClangAstData.NODE_DATA).clear();
        data.get(ClangAstData.VISITED_CHILDREN).clear();
        data.get(ClangAstData.ID_TO_FILENAME_MAP).clear();
        data.get(ClangAstData.TOP_LEVEL_DECL_IDS).clear();
        data.get(ClangAstData.TOP_LEVEL_TYPE_IDS).clear();
        data.get(ClangAstData.TOP_LEVEL_ATTR_IDS).clear();
    }

    private void readBlocks(DataInputStream input) throws IOException {
        var blockTable = schema.table("Block");
        var records = blockTable.field("records");
        var recordTable = schema.table("Record");
        while (true) {
            int first = input.read();
            if (first < 0) {
                return;
            }
            int length = Integer.reverseBytes((first << 24) | (input.readUnsignedByte() << 16)
                    | (input.readUnsignedByte() << 8) | input.readUnsignedByte());
            if (length < 8) {
                throw new IllegalArgumentException("Invalid block length " + length);
            }
            var block = ByteBuffer.wrap(input.readNBytes(length)).order(ByteOrder.LITTLE_ENDIAN);
            if (block.limit() != length) {
                throw new EOFException("Truncated AST block");
            }
            if (block.getInt(4) != BLOCK_IDENTIFIER) {
                throw new IllegalArgumentException("Not a CLV2 AST block");
            }
            var root = new WireTable(block, block.getInt(0), blockTable);
            for (int i = 0; i < root.length(records); i++) {
                if (endSeen) {
                    throw new IllegalArgumentException("Record found after End");
                }
                read(root.tableAt(records, i).union("payload"));
            }
        }
    }

    private void read(WireTable record) {
        if (!headerSeen && !record.spec.name.equals("Header")) {
            throw new IllegalArgumentException("The stream must start with a Header record");
        }
        var data = tu.data();
        switch (record.spec.name) {
            case "Header" -> {
                if (headerSeen) {
                    throw new IllegalArgumentException("Duplicated Header record");
                }
                if (!schema.hash.equals(record.string("schema_hash"))) {
                    throw new IllegalArgumentException("AST schema " + record.string("schema_hash")
                            + " differs from Clava's " + schema.hash);
                }
                headerSeen = true;
            }
            case "File" -> {
                if (record.integer("id") != tu.paths().size()) {
                    throw new IllegalArgumentException("Non-contiguous wire file id " + record.integer("id"));
                }
                tu.paths().add(cachePath(ClangAstPathResolver.resolve(record.string("path"), data)));
            }
            case "Node" -> readNode(record);
            case "TopLevel" -> {
                String id = tu.id(record.integer("node"));
                if (unsupported.contains(id)) {
                    return;
                }
                var ids = switch ((int) record.integer("kind")) {
                    case 0 -> data.get(ClangAstData.TOP_LEVEL_DECL_IDS);
                    case 1 -> data.get(ClangAstData.TOP_LEVEL_TYPE_IDS);
                    case 2 -> data.get(ClangAstData.TOP_LEVEL_ATTR_IDS);
                    default -> throw new IllegalArgumentException("Unknown top-level kind " + record.integer("kind"));
                };
                ids.add(id);
            }
            case "Include" -> data.get(ClangAstData.INCLUDES).add(new Include(sourceFile(record.string("source")),
                    record.string("name"), (int) record.integer("line"), record.bool("angled")));
            case "Pragma" -> data.get(ClangAstData.PRAGMAS_LOCATIONS).addPragmaLocation(
                    sourceFile(record.string("source")), (int) record.integer("line"),
                    (int) record.integer("column"));
            case "TranslationUnitFile" -> data.get(ClangAstData.ID_TO_FILENAME_MAP).put(
                    Long.toString(record.integer("id")),
                    cachePath(ClangAstPathResolver.resolve(record.string("path"), data)));
            case "Language" -> data.get(ClangAstData.FILE_LANGUAGE_DATA)
                    .put(sourceFile(record.string("file")), language(record));
            case "End" -> endSeen = true;
            default -> throw new IllegalArgumentException("Unknown record " + record.spec);
        }
    }

    private void readNode(WireTable node) {
        String id = ownerId(node.integer("id"));
        String className = node.string("class_name");
        if (data().get(ClangAstData.NODE_DATA).containsKey(id) || unsupported.contains(id)) {
            throw new IllegalArgumentException("Duplicated Node record for " + id);
        }
        Class<? extends ClavaNode> nodeClass;
        try {
            nodeClass = ClassesService.getClavaClass(className);
        } catch (RuntimeException e) {
            // Clava has no class for this node; it stays unbuilt unless something requires it.
            unsupported.add(id);
            data().get(ClangAstData.TOP_LEVEL_DECL_IDS).remove(id);
            data().get(ClangAstData.TOP_LEVEL_TYPE_IDS).remove(id);
            data().get(ClangAstData.TOP_LEVEL_ATTR_IDS).remove(id);
            return;
        }
        var payload = node.union("payload");
        var bindings = NodeBindings.of(payload.spec);
        // OpenMP declarations carry the generic Decl payload.
        boolean ompDecl = className.startsWith("OMP") && bindings.payloadClass == Decl.class;
        if (!bindings.payloadClass.isAssignableFrom(nodeClass) && !ompDecl) {
            throw new IllegalArgumentException("Node " + id + " of class " + className + " has a "
                    + payload.spec + " payload");
        }
        var children = new ArrayList<String>(node.length("children"));
        for (int i = 0; i < node.length("children"); i++) {
            children.add(tu.id(node.integerAt("children", i)));
        }
        data().get(ClangAstData.VISITED_CHILDREN).put(id, children);
        data().get(ClangAstData.NODE_DATA).put(id, bindings.read(payload, nodeClass, id, tu));
        nodeParser.applyRecord(id, className, tu.data());
    }

    private String ownerId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Record owner id must be positive: " + id);
        }
        return tu.id(id);
    }

    private ClangAstData data() {
        return tu.data();
    }

    private File sourceFile(String path) {
        return new File(ClangAstPathResolver.resolve(path, tu.data()));
    }

    private String cachePath(String path) {
        CachedItems<String, String> cached = tu.data().get(ClangAstData.CONTEXT).get(ClavaContext.CACHED_FILEPATHS);
        return cached.get(path);
    }

    private static Language language(WireTable v) {
        return new Language()
                .set(Language.LINE_COMMENT, v.bool("line_comment"))
                .set(Language.GNU_INLINE, v.bool("gnu_inline"))
                .set(Language.C99, v.bool("c99"))
                .set(Language.C11, v.bool("c11"))
                .set(Language.C_PLUS_PLUS, v.bool("c_plus_plus"))
                .set(Language.C_PLUS_PLUS_11, v.bool("c_plus_plus_11"))
                .set(Language.C_PLUS_PLUS_14, v.bool("c_plus_plus_14"))
                .set(Language.C_PLUS_PLUS_17, v.bool("c_plus_plus_17"))
                .set(Language.C_PLUS_PLUS_20, v.bool("c_plus_plus_20"))
                .set(Language.C_PLUS_PLUS_23, v.bool("c_plus_plus_23"))
                .set(Language.C_PLUS_PLUS_26, v.bool("c_plus_plus_26"))
                .set(Language.HAS_DIGRAPHS, v.bool("has_digraphs"))
                .set(Language.IS_GNU, v.bool("is_gnu"))
                .set(Language.HEX_FLOATS, v.bool("hex_floats"))
                .set(Language.OPEN_CL, v.bool("open_cl"))
                .set(Language.OPEN_CL_VERSION, openClVersion((int) v.integer("open_cl_version")))
                .set(Language.NATIVE_HALF_TYPE, v.bool("native_half_type"))
                .set(Language.CUDA, v.bool("cuda"))
                .set(Language.HAS_BOOL, v.bool("has_bool"))
                .set(Language.HAS_HALF, v.bool("has_half"))
                .set(Language.HAS_WCHAR, v.bool("has_wchar"))
                .set(Language.CHAR_WIDTH, (int) v.integer("char_width"))
                .set(Language.FLOAT_WIDTH, (int) v.integer("float_width"))
                .set(Language.DOUBLE_WIDTH, (int) v.integer("double_width"))
                .set(Language.LONG_DOUBLE_WIDTH, (int) v.integer("long_double_width"))
                .set(Language.BOOL_WIDTH, (int) v.integer("bool_width"))
                .set(Language.SHORT_WIDTH, (int) v.integer("short_width"))
                .set(Language.INT_WIDTH, (int) v.integer("int_width"))
                .set(Language.LONG_WIDTH, (int) v.integer("long_width"))
                .set(Language.LONG_LONG_WIDTH, (int) v.integer("long_long_width"));
    }

    private static OpenCLVersion openClVersion(int clangNumber) {
        return switch (clangNumber) {
            case 0 -> OpenCLVersion.NONE;
            case 100 -> OpenCLVersion.v1_0;
            case 110 -> OpenCLVersion.v1_1;
            case 120 -> OpenCLVersion.v1_2;
            case 200 -> OpenCLVersion.v2_0;
            default -> throw new IllegalArgumentException("Unsupported OpenCL version " + clangNumber);
        };
    }
}
