/**
 * Copyright 2016 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFile;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinpoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AProgram;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;

public class CxxProgram<Self extends CxxProgram<Self>> extends AProgram<Self> {

    private final String name;

    public CxxProgram(App app, CxxWeaver weaver) {
        super(app, weaver);
        this.name = weaver.getProgramName();
    }

    @Override
    public App getNodeImpl() {
        return (App) super.getNodeImpl();
    }

    @Override
    public String getNameImpl() {
        return name;
    }

    @Override
    public boolean rebuildImpl() {
        SpecsLogs.msgInfo("Rebuilding tree...");
        return getWeaverEngine().rebuildAst(true);
    }

    @Override
    public void rebuildFuzzyImpl() {
        SpecsLogs.msgInfo("Fuzzy rebuilding tree...");
        getWeaverEngine().rebuildAstFuzzy();
    }

    @Override
    public AJoinpoint<?> addFileImpl(AFile<?> file) {
        TranslationUnit tu = (TranslationUnit) file.getNodeImpl();
        TranslationUnit trueTu = this.getNodeImpl().addFile(tu);

        if (tu == trueTu) {
            return file;
        }

        return new CxxFile<>(trueTu, getWeaverEngine());
    }

    @Override
    public String[] getIncludeFoldersImpl() {
        Set<String> includeFolders = getWeaverEngine().getIncludeFolders();

        return includeFolders.toArray(new String[0]);
    }

    @Override
    public String getStandardImpl() {
        return getWeaverEngine().getConfig().get(ClavaOptions.STANDARD).getString();
    }

    @Override
    public String getStdFlagImpl() {
        return getWeaverEngine().getStdFlag();
    }

    @Override
    public String[] getDefaultFlagsImpl() {
        return CxxWeaver.getDefaultFlags().toArray(new String[0]);
    }

    @Override
    public String[] getUserFlagsImpl() {
        return getWeaverEngine().getUserFlags().toArray(new String[0]);
    }

    @Override
    public String getBaseFolderImpl() {
        List<File> sources = getWeaverEngine().getSources();
        if (sources.isEmpty()) {
            SpecsLogs.warn("Expected at least program to have one source folder, found none");
            return null;
        }
        File path = sources.get(0);
        File baseFolder = path.isFile() ? path.getParentFile() : path;

        return baseFolder.getAbsolutePath();
    }

    public DataStore getAppData() {
        return this.getNodeImpl().getAppData();
    }

    @Override
    public String getCodeImpl() {
        return this.getNodeImpl().getCode();
    }

    @Override
    public void pushImpl() {
        getWeaverEngine().pushAst();
    }

    @Override
    public void popImpl() {
        getWeaverEngine().popAst();
    }

    @Override
    public String getWeavingFolderImpl() {
        return getWeaverEngine().getWeavingFolder().getAbsolutePath();
    }

    @Override
    public boolean getIsCxxImpl() {
        return getWeaverEngine().getConfig().get(ClavaOptions.STANDARD).isCxx();
    }

    @Override
    public String[] getExtraSourcesImpl() {
        return this.getNodeImpl().getExternalDependencies().getExtraSources().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    public String[] getExtraIncludesImpl() {
        return this.getNodeImpl().getExternalDependencies().getExtraIncludes().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    public String[] getExtraProjectsImpl() {
        return this.getNodeImpl().getExternalDependencies().getProjects().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    public String[] getExtraLibsImpl() {

        return this.getNodeImpl().getExternalDependencies().getLibs()
                .toArray(new String[0]);
    }

    @Override
    public void addExtraIncludeImpl(String path) {
        this.getNodeImpl().getExternalDependencies().addInclude(new File(path));
    }

    @Override
    public void addExtraIncludeFromGitImpl(String gitRepository, String path) {
        this.getNodeImpl().getExternalDependencies().addIncludeFromGit(gitRepository, path);
    }

    @Override
    public void addExtraSourceImpl(String path) {
        this.getNodeImpl().getExternalDependencies().addSource(new File(path));

    }

    @Override
    public void addExtraSourceFromGitImpl(String gitRepository, String path) {
        this.getNodeImpl().getExternalDependencies().addSourceFromGit(gitRepository, path);
    }

    @Override
    public void addExtraLibImpl(String lib) {
        this.getNodeImpl().getExternalDependencies().addLib(lib);
    }

    @Override
    public void addProjectFromGitImpl(String gitRepo, String[] libs, String path) {
        this.getNodeImpl().getExternalDependencies().addProjectFromGit(gitRepo, Arrays.asList(libs), path);
    }

    @Override
    public AJoinpoint<?> addFileFromPathImpl(Object filepath) {
        File file = getFile(filepath);

        if (!file.isFile()) {
            ClavaLog.info("Could not add file, given path was not found: '" + filepath + "'");
            return null;
        }

        // Load file to a literal declaration
        Decl code = getFactory().literalDecl(SpecsIo.read(file));

        // Create file join point
        TranslationUnit newTu = getFactory().translationUnit(file, Arrays.asList(code));

        return addFileImpl(new CxxFile<>(newTu, getWeaverEngine()));
    }

    private File getFile(Object filepath) {
        if (filepath instanceof File) {
            return (File) filepath;
        }

        return new File(filepath.toString());
    }

    @Override
    public AFunction<?> getMainImpl() {
        for (TranslationUnit tunit : this.getNodeImpl().getTranslationUnits()) {
            for (ClavaNode child : tunit.getChildren()) {
                if (!(child instanceof FunctionDecl)) {
                    continue;
                }

                FunctionDecl function = (FunctionDecl) child;
                if (!function.getDeclName().toLowerCase().equals("main")) {
                    continue;
                }

                // Calling isDefinition() can be expensive, specially if there are many functions,
                // testing name first is faster
                if (!function.isDefinition()) {
                    continue;
                }

                return CxxJoinpoints.create(function, getWeaverEngine(), AFunction.class);
            }
        }

        return null;
    }

    @Override
    public void atexitImpl(AFunction<?> function) {
        AFunction<?> mainFunction = getMainImpl();

        if (mainFunction == null) {
            ClavaLog.info("atexit: main() function not found, could not register function");
            return;
        }

        // Create call
        Expr atexitCall = getFactory().literalExpr("atexit(" + function.getNameImpl() + ")",
                getFactory().builtinType("void"));

        // Insert call at the beginning of the main function
        mainFunction.getBodyImpl().insertBeginImpl(CxxJoinpoints.create(atexitCall, getWeaverEngine()));

        // Add include for atexit
        AFile<?> file = (AFile<?>) mainFunction.getGetAncestorImpl("file");
        Objects.requireNonNull(file, () -> "Expected main function to be inside a file: " + mainFunction.getNodeImpl());
        file.addIncludeImpl("stdlib.h", true);

        // Add include for function
        file.addIncludeJpImpl(function);
    }

    @Override
    public AFile<?>[] getFilesImpl() {
        return this.getNodeImpl().getTranslationUnits().stream()
                .map(tunit -> CxxJoinpoints.create(tunit,
                        getWeaverEngine(), AFile.class))
                .collect(Collectors.toList()).toArray(size -> new AFile[size]);
    }
}
