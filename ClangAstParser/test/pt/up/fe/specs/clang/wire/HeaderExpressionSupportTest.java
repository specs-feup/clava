package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinitions;

import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.LibcMode;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.dumper.ClangAstDumper;
import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.UnresolvedUsingValueDecl;
import pt.up.fe.specs.clava.ast.decl.ValueDecl;
import pt.up.fe.specs.clava.ast.expr.CXXBoolLiteralExpr;
import pt.up.fe.specs.clava.ast.expr.CXXFoldExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.Standard;

class HeaderExpressionSupportTest {

    @TempDir
    Path tempFolder;

    @Test
    void readsFoldOperandsDirectionExpansionAndSourceLocations() {
        var context = new ClavaContext();
        var files = files("/tmp/header-expression.cpp");
        var data = astData(context);
        var clavaNodes = data.getClavaNodes();
        var pattern = boolLiteral(true);
        var lhs = boolLiteral(false);
        var rhs = boolLiteral(true);
        var init = boolLiteral(false);
        var callee = boolLiteral(true);
        clavaNodes.getNodes().put(files.id(2), lhs);
        clavaNodes.getNodes().put(files.id(3), rhs);
        clavaNodes.getNodes().put(files.id(4), init);
        clavaNodes.getNodes().put(files.id(5), pattern);
        clavaNodes.getNodes().put(files.id(6), callee);

        var foldData = CXXFoldExprData.newBuilder()
                .setBase(exprData(10))
                .setOp(BinaryOperatorKind.BINARYOPERATORKIND_ADD)
                .setLhs(2)
                .setRhs(3)
                .setInit(4)
                .setPattern(5)
                .setCallee(6)
                .setNumExpansions(2)
                .setIsLeftFold(true)
                .setIsRightFold(false)
                .setLParenLoc(sourceLocation(1, 10, 4, false))
                .setEllipsisLoc(sourceLocation(1, 10, 11, true))
                .setRParenLoc(sourceLocation(1, 10, 18, false));
        var wire = Node.newBuilder()
                .setId(1)
                .setClassName("CXXFoldExpr")
                .setCXXFoldExprData(foldData)
                .build();

        var store = ProtoNodeDataReader.read(wire, data, files::id, files);
        var fold = new CXXFoldExpr(store, List.of());
        clavaNodes.getNodes().put(files.id(1), fold);
        clavaNodes.runQueuedActions();

        assertSame(lhs, fold.get(CXXFoldExpr.LHS).orElseThrow());
        assertSame(rhs, fold.get(CXXFoldExpr.RHS).orElseThrow());
        assertSame(init, fold.get(CXXFoldExpr.INIT).orElseThrow());
        assertSame(pattern, fold.get(CXXFoldExpr.PATTERN));
        assertSame(callee, fold.get(CXXFoldExpr.CALLEE).orElseThrow());
        assertEquals(pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind.Add, fold.get(CXXFoldExpr.OP));
        assertEquals(Optional.of(2), fold.get(CXXFoldExpr.NUM_EXPANSIONS));
        assertTrue(fold.get(CXXFoldExpr.IS_LEFT_FOLD));
        assertFalse(fold.get(CXXFoldExpr.IS_RIGHT_FOLD));
        assertEquals(10, fold.get(CXXFoldExpr.ELLIPSIS_LOC).orElseThrow().getLine());
        assertEquals(11, fold.get(CXXFoldExpr.ELLIPSIS_LOC).orElseThrow().getColumn());
        assertTrue(fold.get(CXXFoldExpr.ELLIPSIS_LOC).orElseThrow().isMacro());
        assertEquals("(false + ... + true)", fold.getCode());
    }

    @Test
    void nativeFoldRoundTripPreservesAllFormsAndGeneratedCode() throws Exception {
        File tool = nativeTool();
        assumeTrue(tool.isFile(), "set CLANG_DUMPER_TOOL to the native clang-dumper build/tool");

        String sourceText = """
                template <class... Ts>
                auto unary_left(Ts... values) { return (values - ...); }
                template <class... Ts>
                auto unary_right(Ts... values) { return (... - values); }
                template <class... Ts>
                auto binary_left(Ts... values) { return (5 - ... - values); }
                template <class... Ts>
                auto binary_right(Ts... values) { return (values - ... - 5); }
                """;
        Path source = Files.writeString(tempFolder.resolve("fold-roundtrip.cpp"), sourceText);
        String canonicalSource = source.toFile().getCanonicalPath();
        var dumper = new ClangAstDumper(false, tool, List.of(), null, new ParallelCodeParser());
        var data = dumper.parse(source.toFile(), "42", Standard.CXX20, config());
        var folds = data.get(ClangAstData.CLAVA_NODES).getNodes().values().stream()
                .filter(CXXFoldExpr.class::isInstance)
                .map(CXXFoldExpr.class::cast)
                .filter(fold -> fold.getLocation().getFilepath().equals(canonicalSource))
                .sorted(Comparator.comparingInt(fold -> fold.getLocation().getStartLine()))
                .toList();

        List<String> expectedCode = List.of(
                "(values - ...)",
                "(... - values)",
                "(5 - ... - values)",
                "(values - ... - 5)");
        assertEquals(expectedCode.size(), folds.size(), "native source should produce exactly four fold nodes");
        List<String> sourceLines = sourceText.lines().toList();
        for (int index = 0; index < expectedCode.size(); index++) {
            CXXFoldExpr fold = folds.get(index);
            String expected = expectedCode.get(index);
            boolean isRightFold = index == 0 || index == 3;
            boolean hasInit = index >= 2;

            assertEquals(expected, fold.getCode());
            assertEquals(pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind.Sub,
                    fold.get(CXXFoldExpr.OP));
            assertEquals(isRightFold, fold.get(CXXFoldExpr.IS_RIGHT_FOLD));
            assertEquals(!isRightFold, fold.get(CXXFoldExpr.IS_LEFT_FOLD));
            assertEquals("values", fold.get(CXXFoldExpr.PATTERN).getCode());

            Expr patternOperand = fold.get(isRightFold ? CXXFoldExpr.LHS : CXXFoldExpr.RHS).orElseThrow();
            assertSame(fold.get(CXXFoldExpr.PATTERN), patternOperand);
            Optional<Expr> init = fold.get(CXXFoldExpr.INIT);
            assertEquals(hasInit, init.isPresent());
            if (hasInit) {
                assertEquals("5", init.orElseThrow().getCode());
                Expr initOperand = fold.get(isRightFold ? CXXFoldExpr.RHS : CXXFoldExpr.LHS).orElseThrow();
                assertSame(init.orElseThrow(), initOperand);
            } else {
                assertTrue(fold.get(isRightFold ? CXXFoldExpr.RHS : CXXFoldExpr.LHS).isEmpty());
            }

            int lineNumber = fold.getLocation().getStartLine();
            String line = sourceLines.get(lineNumber - 1);
            int returnStart = line.indexOf("return") + "return".length();
            String originalExpression = line.substring(returnStart, line.indexOf(';', returnStart)).trim();
            assertEquals(normalizeCode(originalExpression), normalizeCode(fold.getCode()));

            var lParen = fold.get(CXXFoldExpr.L_PAREN_LOC).orElseThrow();
            var ellipsis = fold.get(CXXFoldExpr.ELLIPSIS_LOC).orElseThrow();
            var rParen = fold.get(CXXFoldExpr.R_PAREN_LOC).orElseThrow();
            int lParenColumn = line.indexOf('(', returnStart) + 1;
            int ellipsisColumn = line.indexOf("...", returnStart) + 1;
            int rParenColumn = line.indexOf(')', returnStart) + 1;
            assertEquals(lineNumber, lParen.getLine());
            assertEquals(lParenColumn, lParen.getColumn());
            assertEquals(lineNumber, ellipsis.getLine());
            assertEquals(ellipsisColumn, ellipsis.getColumn());
            assertEquals(lineNumber, rParen.getLine());
            assertEquals(rParenColumn, rParen.getColumn());

            Path generated = tempFolder.resolve("generated-fold-" + index + ".cpp");
            Files.writeString(generated, "template <class... Ts>\nauto generated(Ts... values) { return "
                    + fold.getCode() + "; }\n");
            assertNull(dumper.validateSyntax(generated.toFile(), "44",
                    Standard.CXX20, config()), "generated fold should compile: " + fold.getCode());
        }
    }

    @Test
    void readsUnresolvedUsingValueDeclTypeQualifierAndLocations() {
        var context = new ClavaContext();
        var files = files("/tmp/header-expression.hpp");
        var data = astData(context);
        var clavaNodes = data.getClavaNodes();
        var type = builtinInt();
        clavaNodes.getNodes().put(files.id(2), type);

        var usingData = unresolvedUsingData()
                .setQualifierSourceRange(range(12, 9, 12, 24))
                .setUsingLocation(sourceLocation(1, 12, 3, false))
                .setNameLocation(sourceLocation(1, 12, 25, true));
        var wire = Node.newBuilder()
                .setId(1)
                .setClassName("UnresolvedUsingValueDecl")
                .setUnresolvedUsingValueDeclData(usingData)
                .build();

        var store = ProtoNodeDataReader.read(wire, data, files::id, files);
        var usingDecl = new UnresolvedUsingValueDecl(store, List.of());
        clavaNodes.getNodes().put(files.id(1), usingDecl);
        clavaNodes.runQueuedActions();

        assertSame(type, usingDecl.get(ValueDecl.TYPE));
        assertEquals("ValueBase<T>::", usingDecl.get(UnresolvedUsingValueDecl.QUALIFIER));
        assertFalse(usingDecl.get(UnresolvedUsingValueDecl.IS_PACK_EXPANSION));
        assertEquals(9, usingDecl.get(UnresolvedUsingValueDecl.QUALIFIER_SOURCE_RANGE)
                .orElseThrow().getStart().getColumn());
        assertEquals(24, usingDecl.get(UnresolvedUsingValueDecl.QUALIFIER_SOURCE_RANGE)
                .orElseThrow().getEnd().getColumn());
        pt.up.fe.specs.clava.SourceLocation usingLocation =
                usingDecl.get(UnresolvedUsingValueDecl.USING_LOCATION).orElseThrow();
        assertEquals(3, usingLocation.getColumn());
        assertEquals("using ValueBase<T>::value;", usingDecl.getCode());
    }

    @Test
    void rejectsMissingRequiredTemplateUsingTypeAndFoldPatternReferences() {
        assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.validate(TemplateType.newBuilder().build(), "TemplateArgument"));

        var missingType = unresolvedUsingData().setBase(
                unresolvedUsingData().getBase().toBuilder().clearType()).build();
        IllegalArgumentException usingError = assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.validate(missingType, "UnresolvedUsingValueDecl"));
        assertTrue(usingError.getMessage().contains("ValueDeclData.type"), usingError.getMessage());

        var missingPattern = CXXFoldExprData.newBuilder()
                .setBase(exprData(10))
                .setOp(BinaryOperatorKind.BINARYOPERATORKIND_ADD)
                .setIsLeftFold(true)
                .setIsRightFold(false)
                .build();
        IllegalArgumentException foldError = assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.validate(missingPattern, "CXXFoldExpr"));
        assertTrue(foldError.getMessage().contains("CXXFoldExprData.pattern"), foldError.getMessage());
    }

    private static UnresolvedUsingValueDeclData.Builder unresolvedUsingData() {
        return UnresolvedUsingValueDeclData.newBuilder()
                .setBase(ValueDeclData.newBuilder()
                        .setBase(NamedDeclData.newBuilder()
                                .setBase(DeclData.newBuilder()
                                        .setBase(NodeData.newBuilder().setSource(sourceInfo(12)))
                                        .setIsImplicit(false)
                                        .setIsUsed(false)
                                        .setIsReferenced(false)
                                        .setIsInvalidDecl(false)
                                        .setIsModulePrivate(false))
                                .setQualifiedPrefix("")
                                .setDeclName("value")
                                .setNameKind(NameKind.NAMEKIND_IDENTIFIER)
                                .setIsCxxClassMember(true)
                                .setIsCxxInstanceMember(false)
                                .setLinkage(Linkage.LINKAGE_EXTERNAL)
                                .setVisibility(Visibility.VISIBILITY_DEFAULT))
                        .setType(2)
                        .setIsWeak(false))
                .setQualifier("ValueBase<T>::")
                .setIsPackExpansion(false);
    }

    private static ExprData exprData(int line) {
        return ExprData.newBuilder()
                .setBase(StmtData.newBuilder().setBase(NodeData.newBuilder().setSource(sourceInfo(line))))
                .setType(2)
                .setValueKind(ValueKind.VALUEKIND_R_VALUE)
                .setObjectKind(ObjectKind.OBJECTKIND_ORDINARY)
                .setIsDefaultArgument(false)
                .build();
    }

    private static SourceInfo sourceInfo(int line) {
        return SourceInfo.newBuilder()
                .setExpansion(range(line, 1, line, 30))
                .setIsMacro(false)
                .setSystemHeader(false)
                .build();
    }

    private static Range range(int line, int column, int endLine, int endColumn) {
        return Range.newBuilder().setFile(1).setLine(line).setColumn(column)
                .setEndFile(1).setEndLine(endLine).setEndColumn(endColumn).build();
    }

    private static SourceLocation sourceLocation(int file, int line, int column, boolean macro) {
        return SourceLocation.newBuilder().setFile(file).setLine(line).setColumn(column)
                .setIsMacro(macro).build();
    }

    private static ProtoAstReader.Files files(String path) {
        var files = new ProtoAstReader.Files("header-expression-test");
        files.addPath(path);
        return files;
    }

    private static ClangAstData astData(ClavaContext context) {
        var data = new ClangAstData();
        data.set(ClangAstData.CONTEXT, context);
        data.set(ClangAstData.CLAVA_NODES, new ClavaNodes(context.get(ClavaContext.FACTORY)));
        return data;
    }

    private static String normalizeCode(String code) {
        return code.replaceAll("\\s+", "");
    }

    private static DataStore config() {
        var config = ClangAstKeys.toDataStore(List.of());
        config.set(ClangAstKeys.USES_CILK, false);
        config.set(ClangAstKeys.LIBC_CXX_MODE, LibcMode.SYSTEM);
        config.add(ClavaNode.CONTEXT, new ClavaContext());
        return config;
    }

    private static File nativeTool() {
        String configured = System.getenv("CLANG_DUMPER_TOOL");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toFile();
        }

        Path workingDirectory = Path.of(System.getProperty("user.dir"));
        var candidates = new ArrayList<Path>();
        candidates.add(workingDirectory.resolve("../../clang-dumper/build/tool"));
        candidates.add(workingDirectory.resolve("../clang-dumper/build/tool"));
        candidates.add(workingDirectory.resolve("clang-dumper/build/tool"));
        return candidates.stream()
                .map(Path::toAbsolutePath)
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(candidates.get(0))
                .toFile();
    }

    private static CXXBoolLiteralExpr boolLiteral(boolean value) {
        var store = DataStore.newInstance(StoreDefinitions.fromInterface(CXXBoolLiteralExpr.class), true);
        store.set(CXXBoolLiteralExpr.VALUE, value);
        return new CXXBoolLiteralExpr(store, List.of());
    }

    private static BuiltinType builtinInt() {
        var store = DataStore.newInstance(StoreDefinitions.fromInterface(BuiltinType.class), true);
        store.set(BuiltinType.KIND, BuiltinKind.Int);
        store.set(BuiltinType.KIND_LITERAL, "int");
        return new BuiltinType(store, List.of());
    }
}
