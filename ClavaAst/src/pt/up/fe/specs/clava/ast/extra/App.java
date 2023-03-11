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

package pt.up.fe.specs.clava.ast.extra;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.extra.data.IdNormalizer;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.transform.call.CallInliner;
import pt.up.fe.specs.clava.utils.DeclarationsCache;
import pt.up.fe.specs.clava.utils.ExternalDependencies;
import pt.up.fe.specs.clava.utils.GlobalManager;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;

/**
 * Represents an application composed by a set of TranslationUnits.
 *
 * @author JoaoBispo
 *
 */
public class App extends ClavaNode {

    /// DATAKEYS BEGIN

    /**
     * External dependencies of this application.
     */
    public final static DataKey<ExternalDependencies> EXTERNAL_DEPENDENCIES = KeyFactory
            .object("externalDependencies", ExternalDependencies.class)
            .setDefault(() -> new ExternalDependencies())
            .setCopyFunction(dependencies -> dependencies.copy());

    /**
     * Can be used to store arbitrary information that should be accessible through the application
     *
     */
    public final static DataKey<DataStore> APP_DATA = KeyFactory.object("appData", DataStore.class)
            .setDefault(() -> DataStore.newInstance("Clava App Arbitrary Data"))
            .setCopyFunction(dataStore -> DataStore.newInstance(dataStore.getName(), dataStore));

    /**
     * Contains the list of source files that were ignored during parsing (e.g. due to parsing errors)
     */
    public final static DataKey<List<File>> IGNORED_FILES = KeyFactory.list("ignoredFiles", File.class);

    /// DATAKEYS END

    private Map<File, File> sourceFiles;

    private GlobalManager globalManager;

    private DeclarationsCache declarationsCache;

    private final IdNormalizer idNormalizer;
    private final CallInliner callInliner;

    /**
     *
     * @param dataStore
     * @param children
     */
    public App(DataStore dataStore, Collection<? extends ClavaNode> children) {
        super(dataStore, children);

        sourceFiles = new HashMap<>();
        globalManager = new GlobalManager();

        declarationsCache = null;

        idNormalizer = new IdNormalizer();
        callInliner = new CallInliner(idNormalizer);
    }

    private DeclarationsCache getDeclarationsCache() {

        // If null, initialize it
        if (declarationsCache == null) {
            declarationsCache = new DeclarationsCache(this);
        }

        return declarationsCache;
    }

    /**
     * Clears cached data.
     */
    public void clearCache() {
        declarationsCache = null;
    }

    public DataStore getAppData() {
        return get(APP_DATA);
    }

    public ExternalDependencies getExternalDependencies() {
        return get(EXTERNAL_DEPENDENCIES);
    }

    public void setIdsAlias(Map<String, String> idsAlias) {
        idNormalizer.addAlias(idsAlias);
    }

    /**
     * Custom location that represents the program.
     */
    @Override
    public SourceRange getLocation() {
        return SourceRange.newCustomLocation("<Program>");
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        for (TranslationUnit tu : getTranslationUnits()) {
            code.append("/**** File '" + SpecsIo.normalizePath(tu.getRelativeFilepath()) + "' ****/"
                    + ln() + ln());
            code.append(tu.getCode());
            code.append(ln() + "/**** End File ****/" + ln() + ln());
        }

        return code.toString();
    }

    public List<TranslationUnit> getTranslationUnits() {
        return getChildren(TranslationUnit.class);
    }

    /**
     * Writes the source-code of the current application to the given destination folder.
     * 
     * @param destinationFolder
     * @return
     */
    public List<File> write(File destinationFolder) {
        List<File> writtenFiles = new ArrayList<>();

        for (Entry<File, String> entry : getCode(destinationFolder).entrySet()) {
            SpecsIo.write(entry.getKey(), entry.getValue());
            writtenFiles.add(entry.getKey());
        }

        return writtenFiles;
    }

    private boolean enableModifiedFilesFilter(List<File> programFiles, Set<String> modifiedFiles) {
        // If set of files to generate is null, return false
        if (modifiedFiles == null) {
            return false;
        }

        // Check if all files have different names
        Set<String> filenames = new HashSet<>();
        for (File file : programFiles) {
            boolean newElement = filenames.add(file.getName());
            if (!newElement) {
                SpecsLogs.msgInfo("Cannot use generation of modified files only, found two files with the same name: '"
                        + file.getName() + "'");
                return false;
            }
        }

        return true;
    }

    public Map<File, String> getCode(File destinationFolder) {
        return getCode(destinationFolder, null);
    }

    /**
     *
     * @param baseInputFolder
     * @param destinationFolder
     * @param modifiedFiles
     *            a set of filenames which is a white-list for files that should be generated from the AST. If null,
     *            generates all files from the AST
     * @return
     */
    public Map<File, String> getCode(File destinationFolder, Set<String> modifiedFiles) {
        Map<File, String> files = new HashMap<>();

        boolean enableModifiedFilesFilter = enableModifiedFilesFilter(getFiles(), modifiedFiles);

        // Generate code for each translation unit considering the given destination
        // and using a path relative to the source path

        for (TranslationUnit tUnit : getTranslationUnits()) {
            File destinationFile = tUnit.getDestinationFile(destinationFolder);
            String code = getTuCode(tUnit, enableModifiedFilesFilter, modifiedFiles);

            files.put(destinationFile, code);
        }

        return files;
    }

    private String getTuCode(TranslationUnit tUnit, boolean enableModifiedFilesFilter, Set<String> modifiedFiles) {
        // If modified files filter is not enable, generate code from the translation unit
        if (!enableModifiedFilesFilter) {
            return tUnit.getCode();
        }

        // If file was modified, generate code from the translation uni
        if (modifiedFiles.contains(tUnit.getFilename())) {
            return tUnit.getCode();
        }

        File originalFile = tUnit.getFile();

        // If the original file does not exist, generate code
        if (!originalFile.isFile()) {
            return tUnit.getCode();
        }

        // Otherwise, return the original file
        String relativeSource = tUnit.getRelativeFilepath();
        SpecsLogs.msgInfo("Using original source for file '" + relativeSource + "'");
        return SpecsIo.read(originalFile);
    }

    /**
     *
     * @return list of current files in the program
     */
    public List<File> getFiles() {
        return getTranslationUnits().stream()
                .map(TranslationUnit::getFile)
                .collect(Collectors.toList());
    }

    public Optional<TranslationUnit> getFile(String filename) {
        return getTranslationUnits().stream()
                .filter(tu -> tu.getFilename().equals(filename))
                .findFirst();
    }

    public List<FunctionDecl> getFunctionPrototypes(FunctionDecl function) {
        return getDeclarationsCache().getFunctionPrototypes(function);
    }

    public Optional<FunctionDecl> getFunctionImplementation(FunctionDecl function) {
        return getDeclarationsCache().getFunctionImplementation(function);
    }

    public List<CXXRecordDecl> getCxxRecordDeclarations(CXXRecordDecl record) {
        return SpecsCollections.castUnchecked(getDeclarationsCache().getTagPrototypes(record), CXXRecordDecl.class);

    }

    public Optional<CXXRecordDecl> getCxxRecordDefinition(CXXRecordDecl record) {
        return getDeclarationsCache().getTagImplementation(record).map(CXXRecordDecl.class::cast);
    }

    public Optional<VarDecl> getGlobalVarDefinition(VarDecl varDecl) {
        return getDeclarationsCache().getGlobalVarDefinition(varDecl);
    }

    public GlobalManager getGlobalManager() {
        return globalManager;
    }

    public TranslationUnit addFile(TranslationUnit tu) {
        return (TranslationUnit) addChild(tu);
    }

    public boolean inline(CallExpr callExpr) {
        return callInliner.inline(callExpr);
    }

    /**
     * Looks for a node based on the filepath and the id.
     *
     * @param filepath
     * @param astId
     * @return
     */
    public Optional<ClavaNode> find(String filepath, String astId) {

        File originalFilepath = new File(filepath);
        TranslationUnit tu = getTranslationUnits().stream()
                .filter(node -> node.getFile().equals(originalFilepath))
                .findFirst()
                .orElse(null);

        if (tu == null) {
            return Optional.empty();
        }

        return tu.getDescendantsAndSelfStream()
                // Filter nodes that do not have an id equal to the given id
                .filter(node -> node.getExtendedId().map(astId::equals).orElse(false))
                .findFirst();
    }

    public Optional<ClavaNode> find(ClavaNode node) {

        // Get translation unit
        TranslationUnit tu = node.getAncestorTry(TranslationUnit.class).orElse(null);
        if (tu == null) {
            return Optional.empty();
        }

        String id = node.getExtendedId().orElse(null);
        if (id == null) {
            return Optional.empty();
        }

        return find(tu.getFile().getPath(), id);
    }

    /**
     * Helper method that accepts a map that has String instead of File as keys.
     *
     * @param allStringFiles
     */
    public void setSourcesFromStrings(Map<String, File> allStringFiles) {
        Map<File, File> allFiles = new HashMap<>();

        for (Entry<String, File> entry : allStringFiles.entrySet()) {
            allFiles.put(new File(entry.getKey()), entry.getValue());
        }

        setSources(allFiles);
    }

    /**
     * Sets the sources of each Translation Unit that will be used to calculate the relative paths.
     * 
     * <p>
     * Receives a map of App files to the corresponding base source folder
     *
     * @param allFiles
     */
    public void setSources(Map<File, File> allFiles) {

        // Set sourceFiles
        sourceFiles = allFiles;

        for (TranslationUnit tu : getTranslationUnits()) {
            // Find the corresponding source
            File sourcePath = sourceFiles.get(tu.getFile());

            if (sourcePath == null) {
                tu.setRelativePath(null);
                continue;
            }

            // Calculate the relative path between the sourcePath and the file folder
            String relativePath = SpecsIo.getRelativePath(tu.getFile().getParentFile(), sourcePath);

            tu.setRelativePath(relativePath);
        }
    }

    /**
     * Sets the source folder names of each Translation Unit, which will be used to determine the folder to which the
     * source will be written.
     * 
     * <p>
     * Receives a map of App files to the corresponding source folder name, if any
     *
     * @param allFiles
     */
    public void setSourceFoldernames(Map<File, String> sourceFoldernames) {

        for (TranslationUnit tu : getTranslationUnits()) {
            // Find the corresponding source
            String sourceFoldername = sourceFoldernames.get(tu.getFile());

            // No problem if sourceFoldername is null, will be converted to Optional.empty()
            tu.setOptional(TranslationUnit.SOURCE_FOLDERNAME, sourceFoldername);
        }
    }

    public void addConfig(DataStore config) {
        get(APP_DATA).addAll(config);
    }

    public Standard getStandard() {
        // TODO: Should standard be in Context instead?
        return get(APP_DATA).get(ClavaOptions.STANDARD);
    }

    public TranslationUnit getTranslationUnit(SourceRange location) {
        for (TranslationUnit unit : getTranslationUnits()) {
            if (unit.getLocation().toString().equals(location.toString())) {
                return unit;
            }
        }

        throw new RuntimeException("Could not find");
    }

    /**
     * Creates a copy of this App, pushes it to the stack of Apps in ClavaContext, returns the new copied App.
     *
     * @return the new App in the top of the stack.
     */
    public App pushAst() {
        // Create a copy of the App
        App newApp = (App) copy(true);

        // Push new App in ClavaContext
        get(App.CONTEXT).pushApp(newApp);

        return newApp;
    }

    /**
     * 
     * @return true, if any of the TranslationUnits has parsing errors
     */
    public boolean hasParsingErrors() {
        return getTranslationUnits().stream()
                .filter(tu -> tu.get(TranslationUnit.HAS_PARSING_ERRORS))
                .findFirst()
                .isPresent();
    }

}
