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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.lara.interpreter.weaver.interf.enums.InsertPosition;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.LiteralExpr;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxActions;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFile;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AInclude;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinpoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;

public class CxxFile<Self extends CxxFile<Self>> extends AFile<Self> {

    public CxxFile(TranslationUnit tunit, CxxWeaver weaver) {
        super(tunit, weaver);
    }

    @Override
    public TranslationUnit getNodeImpl() {
        return (TranslationUnit) super.getNodeImpl();
    }

    @Override
    public String getNameImpl() {
        return this.getNodeImpl().getFilename();
    }

    @Override
    public void setNameImpl(String filename) {
        var previousFile = this.getNodeImpl().get(TranslationUnit.SOURCE_FILE);
        var baseFolder = previousFile != null ? previousFile.getParentFile() : null;
        var newFile = new File(baseFolder, filename);
        this.getNodeImpl().set(TranslationUnit.SOURCE_FILE, newFile);
    }

    public TranslationUnit getTu() {
        return this.getNodeImpl();
    }

    @Override
    public boolean getHasMainImpl() {
        return getFunctions().stream()
                .filter(function -> function.getDeclName().equals("main"))
                .findFirst().isPresent();
    }

    private List<FunctionDecl> getFunctions() {
        return this.getNodeImpl().getDescendantsStream()
                // FunctionDecl represents C function, C++ methods, constructors and destructors
                .filter(node -> node instanceof FunctionDecl)
                .map(function -> (FunctionDecl) function)
                .collect(Collectors.toList());
    }

    @Override
    public void addIncludeImpl(String name, boolean isAngled) {
        this.getNodeImpl().addInclude(name, isAngled);
    }

    @Override
    public void addCIncludeImpl(String name, boolean isAngled) {
        this.getNodeImpl().addCInclude(name, isAngled);
    }

    @Override
    public AJoinpoint<?>[] insertImpl(InsertPosition position, String code) {
        var tentativeNode = getWeaverEngine().getSnippetParser().parseStmt(code);
        ClavaNode nodeToInsert = tentativeNode instanceof WrapperStmt ? tentativeNode.getChild(0)
                : getWeaverEngine().getFactory().literalDecl(code);

        return CxxActions.insertAsChild(position.getDisplay(), this.getNodeImpl(), nodeToInsert, getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> insertAfterImpl(AJoinpoint<?> node) {

        // Check node is a decl
        if (!(node.getNodeImpl() instanceof Decl)) {
            SpecsLogs.msgInfo(
                    "Can only insert Decl nodes in a file, tried to insert a '" + node.joinPointType() + "'");
            return null;
        }

        CxxActions.insertAsChild("after", this.getNodeImpl(), node.getNodeImpl(), getWeaverEngine());

        return node;
    }

    @Override
    public AJoinpoint<?> insertBeforeImpl(AJoinpoint<?> node) {

        // Check node is a decl
        if (node.getNodeImpl() instanceof Decl) {
            SpecsLogs.msgInfo(
                    "Can only insert Decl nodes in a file, tried to insert a '" + node.joinPointType() + "'");
            return null;
        }

        CxxActions.insertAsChild("before", this.getNodeImpl(), node.getNodeImpl(), getWeaverEngine());

        return node;
    }

    @Override
    public String getPathImpl() {
        return this.getNodeImpl().getFolderpath().orElse(null);
    }

    @Override
    public void addIncludeJpImpl(AJoinpoint<?> jp) {

        // If jp is a function, include declaration if available
        if (jp.instanceOf("function")) {
            AFunction<?> functionJp = (AFunction<?>) jp;
            AJoinpoint<?> decl = functionJp.getDeclarationJpImpl();
            jp = decl != null ? decl : jp;
        }

        // Get first joinpoint that is a CxxFile
        CxxFile<?> includeFile = CxxJoinpoints.getAncestorandSelf(jp, CxxFile.class).get();

        // If file is the same as the current file, ignore
        if (includeFile.getNodeImpl().getLocation().equals(this.getNodeImpl().getLocation())) {
            ClavaLog.debug("addIncludeJp: ignoring include '" + includeFile.getNodeImpl().getRelativeFilepath()
                    + "', since it is in the same file");
            return;
        }

        if (!includeFile.getNodeImpl().isHeaderFile()) {
            ClavaLog.info("addIncludeJp: not adding file '" + includeFile.getNodeImpl().getRelativeFilepath()
                    + "' as an include, since it is not a header file");
            return;
        }

        String includePath = includeFile.getNodeImpl().getRelativeFilepath();

        this.getNodeImpl().addInclude(includePath, false);
    }

    @Override
    public String getFilepathImpl() {
        return this.getNodeImpl().getFile().getPath();
    }

    @Override
    public String getRelativeFolderpathImpl() {
        return this.getNodeImpl().getRelativeFolderpath().orElse(null);
    }

    @Override
    public void setRelativeFolderpathImpl(String path) {
        this.getNodeImpl().setRelativePath(path);
    }

    @Override
    public String getRelativeFilepathImpl() {
        return this.getNodeImpl().getRelativeFilepath();
    }

    @Override
    public boolean getIsCxxImpl() {
        return this.getNodeImpl().isCXXUnit();
    }

    @Override
    public AVardecl<?> addGlobalImpl(String name, AJoinpoint<?> type, String initValue) {

        // Check if joinpoint is a CxxType
        if (!(type instanceof AType)) {
            SpecsLogs.msgInfo("addGlobal: the provided join point (" + type.joinPointType() + ") is not a type");
            return null;
        }

        Type typeNode = (Type) type.getNodeImpl();
        LiteralExpr literalExpr = getWeaverEngine().getFactory().literalExpr(initValue, typeNode);

        VarDecl global = this.getNodeImpl().getApp().getGlobalManager().addGlobal(this.getNodeImpl(), name, typeNode, literalExpr);

        return CxxJoinpoints.create(global, getWeaverEngine(), AVardecl.class);
    }

    @Override
    public void insertBeginImpl(AJoinpoint<?> node) {
        if (!this.getNodeImpl().hasChildren()) {
            this.getNodeImpl().addChild(node.getNodeImpl());
            return;
        }

        this.getNodeImpl().addChild(0, node.getNodeImpl());
    }

    @Override
    public void insertBeginImpl(String code) {
        insertBeginImpl(AstFactory.declLiteral(getWeaverEngine(),code));
    }

    @Override
    public void insertEndImpl(AJoinpoint<?> node) {
        if (!this.getNodeImpl().hasChildren()) {
            this.getNodeImpl().addChild(node.getNodeImpl());
            return;
        }

        this.getNodeImpl().addChild(node.getNodeImpl());
    }

    @Override
    public void insertEndImpl(String code) {
        insertEndImpl(AstFactory.declLiteral(getWeaverEngine(), code));
    }

    @Override
    public AJoinpoint<?> addFunctionImpl(String name) {
        CxxFunction<?> function = AstFactory.functionVoid(getWeaverEngine(), name);

        // Add function to the tree
        this.getNodeImpl().addChild(function.getNodeImpl());

        return function;
    }

    @Override
    public boolean getIsHeaderImpl() {
        return this.getNodeImpl().isHeaderFile();
    }

    @Override
    public String writeImpl(String destinationFoldername) {
        File destinationFolder = SpecsIo.mkdir(destinationFoldername);
        if (destinationFolder == null) {
            ClavaLog.info("$file.exec write: Could not obtain destination folder '" + destinationFoldername + "'");
            return null;
        }

        File writtenFile = this.getNodeImpl().write(destinationFolder);
        getWeaverEngine().getWeaverData().addManualWrittenFile(writtenFile);

        return writtenFile.getAbsolutePath();
    }

    @Override
    public boolean getIsOpenCLImpl() {
        return this.getNodeImpl().isOpenCLFile();
    }

    @Override
    public AInclude<?>[] getIncludesImpl() {
        return CxxSelects.select(getWeaverEngine(), AInclude.class, this.getNodeImpl().getChildren(), false, IncludeDecl.class);
    }

    @Override
    public String getBaseSourcePathImpl() {
        SpecsLogs.warn(
                "Attribute $file.baseSourcePath is deprecated, please use attribute $file.relativeFolderpath, which returns the same.");
        return this.getNodeImpl().getRelativeFolderpath().orElse(null);
    }

    @Override
    public String getGetDestinationFilepathImpl(String destinationFolderpath) {
        File file;

        if (destinationFolderpath == "" ) {
            file = getWeaverEngine().getWeavingFolder();
        } else {
            file = new File(destinationFolderpath);
        }

        return this.getNodeImpl().getDestinationFile(file).getAbsolutePath();
    }

    @Override
    public AFile<?> rebuildImpl() {
        TranslationUnit rebuiltTunit = getWeaverEngine().rebuildFile(this.getNodeImpl());

        AFile<?> rebuiltFile = CxxJoinpoints.create(rebuiltTunit, getWeaverEngine(), AFile.class);
        replaceWith(rebuiltFile);
        return rebuiltFile;
    }

    @Override
    public Object getFileImpl() {
        return this.getNodeImpl().getFile();
    }

    @Override
    public String getSourceFoldernameImpl() {
        return this.getNodeImpl().get(TranslationUnit.SOURCE_FOLDERNAME).orElse(null);
    }

    @Override
    public boolean getHasParsingErrorsImpl() {
        return this.getNodeImpl().get(TranslationUnit.HAS_PARSING_ERRORS);
    }

    @Override
    public String getErrorOutputImpl() {
        return this.getNodeImpl().get(TranslationUnit.ERROR_OUTPUT);
    }

}
