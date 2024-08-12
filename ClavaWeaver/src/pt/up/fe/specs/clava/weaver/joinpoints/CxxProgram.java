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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

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
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AProgram;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CxxProgram extends AProgram {

    private final String name;
    private final App app;
    private final CxxWeaver weaver;
    // private final File baseFolder;

    // private final List<String> parserOptions;

    // public CxxProgram(File baseFolder, App app, List<String> parserOptions) {
    // this.baseFolder = baseFolder;
    // this.app = app;
    // this.parserOptions = parserOptions;
    // }

    public CxxProgram(String name, App app, CxxWeaver weaver) {
        this.name = name;
        this.app = app;
        this.weaver = weaver;
    }

    @Override
    public List<? extends AFile> selectFile() {
        return app.getTranslationUnits().stream()
                .map(tunit -> CxxJoinpoints.create(tunit, AFile.class))
                .collect(Collectors.toList());
    }

    @Override
    public App getNode() {
        return app;
    }

    @Override
    public String getNameImpl() {
        return name;
    }

    public CxxWeaver getWeaver() {
        return weaver;
    }

    @Override
    public boolean rebuildImpl() {
        SpecsLogs.msgInfo("Rebuilding tree...");
        return weaver.rebuildAst(true);
    }

    @Override
    public void rebuildFuzzyImpl() {
        SpecsLogs.msgInfo("Fuzzy rebuilding tree...");
        weaver.rebuildAstFuzzy();
    }

    @Override
    public AJoinPoint addFileImpl(AFile file) {
        TranslationUnit tu = (TranslationUnit) file.getNode();
        TranslationUnit trueTu = app.addFile(tu);

        if (tu == trueTu) {
            return file;
        }

        return new CxxFile(trueTu);
    }

    @Override
    public String[] getIncludeFoldersArrayImpl() {
        Set<String> includeFolders = weaver.getIncludeFolders();

        return includeFolders.toArray(new String[0]);
    }

    @Override
    public String getStandardImpl() {
        return weaver.getConfig().get(ClavaOptions.STANDARD).getString();
    }

    @Override
    public String getStdFlagImpl() {
        return weaver.getStdFlag();
    }

    @Override
    public String[] getDefaultFlagsArrayImpl() {
        return CxxWeaver.getDefaultFlags().toArray(new String[0]);
    }

    @Override
    public String[] getUserFlagsArrayImpl() {
        return weaver.getUserFlags().toArray(new String[0]);
    }

    // @Override
    // public void messageToUserImpl(String message) {
    // weaver.addMessageToUser(message);
    // }

    @Override
    public String getBaseFolderImpl() {
        // ClavaLog.deprecated("attribute baseFolder should not be used, instead use file.sourcePath");
        List<File> sources = getWeaver().getSources();
        if (sources.isEmpty()) {
            SpecsLogs.warn("Expected at least program to have one source folder, found none");
            return null;
        }
        File path = sources.get(0);
        File baseFolder = path.isFile() ? path.getParentFile() : path;

        return baseFolder.getAbsolutePath();
    }

    public DataStore getAppData() {
        return app.getAppData();
    }

    @Override
    public String getCodeImpl() {
        return app.getCode();
    }

    @Override
    public void pushImpl() {
        weaver.pushAst();
    }

    @Override
    public void popImpl() {
        weaver.popAst();
    }

    @Override
    public String getWeavingFolderImpl() {
        return weaver.getWeavingFolder().getAbsolutePath();
    }

    @Override
    public Boolean getIsCxxImpl() {
        return weaver.getConfig().get(ClavaOptions.STANDARD).isCxx();
    }

    @Override
    public String[] getExtraSourcesArrayImpl() {
        return app.getExternalDependencies().getExtraSources().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    public String[] getExtraIncludesArrayImpl() {
        return app.getExternalDependencies().getExtraIncludes().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    public String[] getExtraProjectsArrayImpl() {
        return app.getExternalDependencies().getProjects().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    public String[] getExtraLibsArrayImpl() {

        return app.getExternalDependencies().getLibs()
                .toArray(new String[0]);
    }

    @Override
    public void addExtraIncludeImpl(String path) {
        app.getExternalDependencies().addInclude(new File(path));
    }

    @Override
    public void addExtraIncludeFromGitImpl(String gitRepository, String path) {
        app.getExternalDependencies().addIncludeFromGit(gitRepository, path);
    }

    @Override
    public void addExtraSourceImpl(String path) {
        app.getExternalDependencies().addSource(new File(path));

    }

    @Override
    public void addExtraSourceFromGitImpl(String gitRepository, String path) {
        app.getExternalDependencies().addSourceFromGit(gitRepository, path);
    }

    @Override
    public void addExtraLibImpl(String lib) {
        app.getExternalDependencies().addLib(lib);
    }

    @Override
    public void addProjectFromGitImpl(String gitRepo, String[] libs, String path) {
        app.getExternalDependencies().addProjectFromGit(gitRepo, Arrays.asList(libs), path);
    }

    @Override
    public AJoinPoint addFileFromPathImpl(Object filepath) {
        File file = getFile(filepath);

        if (!file.isFile()) {
            ClavaLog.info("Could not add file, given path was not found: '" + filepath + "'");
            return null;
        }

        // Load file to a literal declaration
        Decl code = getFactory().literalDecl(SpecsIo.read(file));

        // Create file join point
        TranslationUnit newTu = getFactory().translationUnit(file, Arrays.asList(code));

        return addFileImpl(new CxxFile(newTu));
    }

    private File getFile(Object filepath) {
        if (filepath instanceof File) {
            return (File) filepath;
        }

        return new File(filepath.toString());
    }

    @Override
    public AFunction getMainImpl() {
        for (TranslationUnit tunit : app.getTranslationUnits()) {
            for (ClavaNode child : tunit.getChildren()) {
                // ClavaLog.debug("getMain: checking if child is FunctionDecl");
                if (!(child instanceof FunctionDecl)) {
                    continue;
                }

                FunctionDecl function = (FunctionDecl) child;
                // ClavaLog.debug("getMain: checking if function is main");
                if (!function.getDeclName().toLowerCase().equals("main")) {
                    continue;
                }

                // ClavaLog.debug("getMain: checking if function '" + function.getDeclName() + "' is definition");

                // Calling isDefinition() can be expensive, specially if there are many functions,
                // testing name first is faster
                if (!function.isDefinition()) {
                    continue;
                }

                return (AFunction) CxxJoinpoints.create(function);
            }
        }

        return null;
        /*
        // Find main function
        return (AFunction) app.getDescendantsStream()
                // get functions
                .filter(FunctionDecl.class::isInstance)
                .map(FunctionDecl.class::cast)
                // only definitions
                .filter(FunctionDecl::isDefinition)
                // the main function
                .filter(fdecl -> fdecl.getDeclName().toLowerCase().equals("main"))
                .map(CxxJoinpoints::create)
                .findFirst()
                .orElse(null);
                */
    }

    @Override
    public void atexitImpl(AFunction function) {
        // ClavaLog.debug("Getting main function");
        AFunction mainFunction = getMainImpl();

        if (mainFunction == null) {
            ClavaLog.info("atexit: main() function not found, could not register function");
            return;
        }

        // Create call
        Expr atexitCall = getFactory().literalExpr("atexit(" + function.getNameImpl() + ")",
                getFactory().builtinType("void"));

        // Insert call at the beginning of the main function
        // ClavaLog.debug("Inserting atexit call at beginning of main");
        mainFunction.getBodyImpl().insertBegin(CxxJoinpoints.create(atexitCall));

        // Add include for atexit
        // ClavaLog.debug("Getting file ancestor");
        AFile file = (AFile) mainFunction.getAncestorImpl("file");
        SpecsCheck.checkNotNull(file, () -> "Expected main function to be inside a file: " + mainFunction.getNode());
        // ClavaLog.debug("Adding stdlib.h include");
        file.addInclude("stdlib.h", true);

        // Add include for function
        // ClavaLog.debug("Adding function include");
        file.addIncludeJpImpl(function);

        // ClavaLog.debug("Finsished");
    }

    @Override
    public AFile[] getFilesArrayImpl() {
        return selectFile().toArray(size -> new AFile[size]);
    }
}
