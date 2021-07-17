/**
 * Copyright 2016 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.comment.Comment;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.decl.TypedefDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.LiteralExpr;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.lara.LaraMarkerPragma;
import pt.up.fe.specs.clava.ast.lara.LaraTagPragma;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.TagKind;
import pt.up.fe.specs.clava.weaver.CxxActions;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AClass;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AComment;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFile;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AInclude;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMarker;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMethod;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.APragma;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ARecord;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStruct;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATag;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATypedefDecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;

public class CxxFile extends AFile {

    private final TranslationUnit tunit;

    public CxxFile(TranslationUnit tunit) {
        this.tunit = tunit;
    }

    @Override
    public String getNameImpl() {
        return tunit.getFilename();
    }

    @Override
    public List<? extends AFunction> selectFunction() {
        return getFunctions().stream()
                .map(function -> CxxJoinpoints.create(function, AFunction.class))
                .collect(Collectors.toList());

        /*
        // TODO: This can be optimzed if there are no FunctionDecl inside FunctionDecl?
        return tunit.getDescendantsStream()
        	// FunctionDecl represents C function, C++ methods, constructors and destructors
        	.filter(node -> node instanceof FunctionDecl)
        	.map(function -> new CxxFunction((FunctionDecl) function, this))
        	.collect(Collectors.toList());
        */
    }

    @Override
    public List<? extends AMethod> selectMethod() {
        return getMethods().stream()
                .map(function -> CxxJoinpoints.create(function, AMethod.class))
                .collect(Collectors.toList());
    }

    @Override
    public TranslationUnit getNode() {
        return tunit;
    }

    public TranslationUnit getTu() {
        return tunit;
    }

    @Override
    public Boolean getHasMainImpl() {
        return getFunctions().stream()
                .filter(function -> function.getDeclName().equals("main"))
                .findFirst().isPresent();
    }

    private List<FunctionDecl> getFunctions() {
        return tunit.getDescendantsStream()
                // FunctionDecl represents C function, C++ methods, constructors and destructors
                .filter(node -> node instanceof FunctionDecl)
                .map(function -> (FunctionDecl) function)
                .collect(Collectors.toList());
    }

    private List<CXXMethodDecl> getMethods() {
        return tunit.getDescendantsStream()
                // FunctionDecl represents C function, C++ methods, constructors and destructors
                .filter(node -> node instanceof CXXMethodDecl)
                .map(function -> (CXXMethodDecl) function)
                .collect(Collectors.toList());
    }

    @Override
    public void addIncludeImpl(String name, boolean isAngled) {
        tunit.addInclude(name, isAngled);
        // tunit.getIncludes().addInclude(ClavaNodeFactory.include(name, isAngled));
    }

    @Override
    public void addIncludeImpl(String name) {
        addIncludeImpl(name, false);
    }

    @Override
    public void addCIncludeImpl(String name, boolean isAngled) {
        tunit.addCInclude(name, isAngled);
    }

    @Override
    public void addCIncludeImpl(String name) {
        addCIncludeImpl(name, false);
    }

    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        var tentativeNode = CxxWeaver.getSnippetParser().parseStmt(code);
        ClavaNode nodeToInsert = tentativeNode instanceof WrapperStmt ? tentativeNode.getChild(0)
                : CxxWeaver.getFactory().literalDecl(code);
        // Decl literalDecl = CxxWeaver.getFactory().literalDecl(code);
        // return CxxActions.insertAsChild(position, getNode(), literalDecl, getWeaverEngine());
        return CxxActions.insertAsChild(position, getNode(), nodeToInsert, getWeaverEngine());
    }

    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        // Check node is a decl
        if (!(node.getNode() instanceof Decl)) {
            SpecsLogs.msgInfo(
                    "Can only insert Decl nodes in a file, tried to insert a '" + node.getJoinPointType() + "'");
            return null;
        }

        CxxActions.insertAsChild("after", getNode(), node.getNode(), getWeaverEngine());

        return node;
    }

    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        // Check node is a decl
        if (node.getNode() instanceof Decl) {
            SpecsLogs.msgInfo(
                    "Can only insert Decl nodes in a file, tried to insert a '" + node.getJoinPointType() + "'");
            return null;
        }

        CxxActions.insertAsChild("before", getNode(), node.getNode(), getWeaverEngine());

        return node;
    }

    @Override
    public List<? extends APragma> selectPragma() {
        return CxxSelects.select(APragma.class, tunit.getChildren(), true, Pragma.class);

    }

    @Override
    public List<? extends AMarker> selectMarker() {
        return CxxSelects.select(AMarker.class, tunit.getChildren(), true, LaraMarkerPragma.class);
    }

    @Override
    public List<? extends ATag> selectTag() {
        return CxxSelects.select(ATag.class, tunit.getChildren(), true, LaraTagPragma.class);
    }

    @Override
    public List<? extends ARecord> selectRecord() {
        return CxxSelects.select(ARecord.class, tunit.getChildren(), true, RecordDecl.class);
    }

    @Override
    public List<? extends AStruct> selectStruct() {
        return CxxSelects.select(AStruct.class, tunit.getChildren(), true,
                node -> node instanceof RecordDecl && ((RecordDecl) node).getTagKind() == TagKind.STRUCT);
    }

    @Override
    public List<? extends AClass> selectClass() {
        return CxxSelects.select(AClass.class, tunit.getChildren(), true,
                node -> node instanceof CXXRecordDecl && ((CXXRecordDecl) node).getTagKind() == TagKind.CLASS);
    }

    /*
    @Override
    public String getFilenameImpl() {
        return tunit.getFilename();
    }
    */

    @Override
    public String getPathImpl() {
        return tunit.getFolderpath().orElse(null);
        /*
        String filename = tunit.getFilename();
        String path = tunit.getFilepath();
        if (path.endsWith(filename)) {
            return path.substring(0, path.length() - filename.length());
        }
        
        return path;
        */
    }

    @Override
    public void addIncludeJpImpl(AJoinPoint jp) {
        // If jp is a function, include declaration if available
        if (jp.instanceOf("function")) {
            AFunction functionJp = (AFunction) jp;
            AJoinPoint decl = functionJp.getDeclarationJpImpl();
            jp = decl != null ? decl : jp;
        }

        // Get first joinpoint that is a CxxFile
        CxxFile includeFile = CxxJoinpoints.getAncestorandSelf(jp, CxxFile.class).get();

        // If file is the same as the current file, ignore
        if (includeFile.tunit.getLocation().equals(tunit.getLocation())) {
            ClavaLog.debug("addIncludeJp: ignoring include '" + includeFile.getNode().getRelativeFilepath()
                    + "', since it is in the same file");
            return;
        }

        if (!includeFile.tunit.isHeaderFile()) {
            ClavaLog.info("addIncludeJp: not adding file '" + includeFile.getNode().getRelativeFilepath()
                    + "' as an include, since it is not a header file");
            return;
        }

        // String includePath = CxxWeaver.getRelativeFilepath(includeFile.getNode());
        String includePath = includeFile.getNode().getRelativeFilepath();

        tunit.addInclude(includePath, false);
        // tunit.addInclude(includeFile.getNode(), getWeaverEngine().getBaseSourceFolder());

        // Get relative path to include the file in this file
        // String relativePath = SpecsIo.getRelativePath(includeFile.getTu().getFile(), tunit.getFile());
        // addIncludeImpl(relativePath);

    }

    @Override
    public String getFilepathImpl() {
        return tunit.getFile().getPath();
    }

    @Override
    public String getRelativeFolderpathImpl() {
        return tunit.getRelativeFolderpath().orElse(null);
        // return CxxWeaver.getRelativeFolderpath(tunit);
    }

    @Override
    public void defRelativeFolderpathImpl(String value) {
        // tunit.setRelativeFolderpath(value);
        tunit.setRelativePath(value);
    }

    @Override
    public void setRelativeFolderpathImpl(String path) {
        defRelativeFolderpathImpl(path);
    }

    @Override
    public String getRelativeFilepathImpl() {
        return tunit.getRelativeFilepath();
        // return CxxWeaver.getRelativeFilepath(tunit);
    }

    @Override
    public Boolean getIsCxxImpl() {
        return tunit.isCXXUnit();
    }

    @Override
    public List<? extends AVardecl> selectVardecl() {
        return tunit.getDescendantsStream()
                .filter(node -> node instanceof VarDecl)
                .map(varDecl -> CxxJoinpoints.create((VarDecl) varDecl, AVardecl.class))
                .collect(Collectors.toList());
    }

    @Override
    public AVardecl addGlobalImpl(String name, AJoinPoint type, String initValue) {

        // Check if joinpoint is a CxxType
        if (!(type instanceof AType)) {
            SpecsLogs.msgInfo("addGlobal: the provided join point (" + type.getJoinPointType() + ") is not a type");
            return null;
        }

        Type typeNode = (Type) type.getNode();
        LiteralExpr literalExpr = CxxWeaver.getFactory().literalExpr(initValue, typeNode);
        // LiteralExpr literalExpr = ClavaNodeFactory.literalExpr(initValue,
        // ClavaNodeFactory.nullType(ClavaNodeInfo.undefinedInfo()));

        VarDecl global = tunit.getApp().getGlobalManager().addGlobal(tunit, name, typeNode, literalExpr);

        return CxxJoinpoints.create(global, AVardecl.class);
    }

    @Override
    public List<? extends AStatement> selectStmt() {
        return CxxSelects.select(AStatement.class, tunit.getChildren(), true, CxxSelects::stmtFilter);

    }

    @Override
    public List<? extends AStatement> selectChildStmt() {
        return tunit.getChildren().stream()
                .map(stmt -> (AStatement) CxxJoinpoints.create(stmt))
                .collect(Collectors.toList());
    }

    @Override
    public void insertBeginImpl(AJoinPoint node) {
        if (!tunit.hasChildren()) {
            tunit.addChild(node.getNode());
            return;
        }

        tunit.addChild(0, node.getNode());
    }

    @Override
    public void insertBeginImpl(String code) {
        insertBeginImpl(AstFactory.declLiteral(code));
    }

    @Override
    public void insertEndImpl(AJoinPoint node) {
        super.insertEndImpl(node);
    }

    @Override
    public void insertEndImpl(String code) {
        insertEndImpl(AstFactory.declLiteral(code));
    }

    @Override
    public List<? extends AComment> selectComment() {
        return CxxSelects.select(AComment.class, tunit.getChildren(), true, Comment.class::isInstance);
    }

    @Override
    public AJoinPoint addFunctionImpl(String name) {
        CxxFunction function = AstFactory.functionVoid(name);

        // Add function to the tree
        tunit.addChild(function.getNode());

        return function;
    }

    @Override
    public Boolean getIsHeaderImpl() {
        return tunit.isHeaderFile();
    }

    @Override
    public String writeImpl(String destinationFoldername) {
        File destinationFolder = SpecsIo.mkdir(destinationFoldername);
        if (destinationFolder == null) {
            ClavaLog.info("$file.exec write: Could not obtain destination folder '" + destinationFoldername + "'");
            return null;
        }

        // File baseSourceFolder = getWeaverEngine().getBaseSourceFolder();
        // tunit.write(destinationFolder, baseSourceFolder);

        File writtenFile = tunit.write(destinationFolder);
        getWeaverEngine().getWeaverData().addManualWrittenFile(writtenFile);

        return writtenFile.getAbsolutePath();
    }

    @Override
    public Boolean getIsOpenCLImpl() {
        return tunit.isOpenCLFile();
    }

    @Override
    public List<? extends AInclude> selectInclude() {
        return CxxSelects.select(AInclude.class, tunit.getChildren(), false, IncludeDecl.class);
    }

    @Override
    public String getBaseSourcePathImpl() {
        SpecsLogs.msgWarn(
                "Attribute $file.baseSourcePath is deprecated, please use attribute $file.relativeFolderpath, which returns the same.");
        return tunit.getRelativeFolderpath().orElse(null);
    }

    @Override
    public List<? extends ADecl> selectDecl() {
        return CxxSelects.select(ADecl.class, tunit.getChildren(), true, Decl.class);
    }

    @Override
    public List<? extends ATypedefDecl> selectTypedefDecl() {
        return tunit.getDescendantsStream()
                .filter(node -> node instanceof TypedefDecl)
                .map(varDecl -> CxxJoinpoints.create((TypedefDecl) varDecl, ATypedefDecl.class))
                .collect(Collectors.toList());
    }

    @Override
    public String getDestinationFilepathImpl() {
        return destinationFilepathImpl(getWeaverEngine().getWeavingFolder().getAbsolutePath());
    }

    @Override
    public String destinationFilepathImpl(String destinationFolderpath) {
        return tunit.getDestinationFile(new File(destinationFolderpath)).getAbsolutePath();
    }

    @Override
    public AFile rebuildImpl() {
        TranslationUnit rebuiltTunit = getWeaverEngine().rebuildFile(tunit);

        AFile rebuiltFile = CxxJoinpoints.create(rebuiltTunit, AFile.class);
        replaceWith(rebuiltFile);
        return rebuiltFile;
    }

    @Override
    public AJoinPoint rebuildTryImpl() {
        try {
            return rebuildImpl();
        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e);
            return new CxxClavaException(e);
        }
    }

    @Override
    public Object getFileImpl() {
        return tunit.getFile();
    }

    @Override
    public String getSourceFoldernameImpl() {
        return tunit.get(TranslationUnit.SOURCE_FOLDERNAME).orElse(null);
    }

    @Override
    public Boolean getHasParsingErrorsImpl() {
        return tunit.get(TranslationUnit.HAS_PARSING_ERRORS);
    }

    @Override
    public String getErrorOutputImpl() {
        return tunit.get(TranslationUnit.ERROR_OUTPUT);
    }
}
