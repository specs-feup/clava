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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.NamespaceDecl;
import pt.up.fe.specs.clava.language.Standard;
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

    // This limits the number of App objects to one per thread
    // This is being used because o Qualifier needs to generate different code for restrict
    // depending on the current compilation standard
    private static final ThreadLocal<Standard> CURRENT_STANDARD = new ThreadLocal<>();

    public static Standard getCurrentStandard() {
        Standard standard = CURRENT_STANDARD.get();
        if (standard == null) {
            SpecsLogs.msgLib("App.getCurrentStandard: App object has not been initialized");
        }

        return standard;
    }

    private static final Set<String> EXTENSIONS_IMPLEMENTATION = new HashSet<>(Arrays.asList("c", "cpp"));
    private static final Set<String> EXTENSIONS_HEADERS = new HashSet<>(Arrays.asList("h", "hpp"));
    private static final Set<String> PERMITTED_EXTENSIONS = new HashSet<>(
            SpecsCollections.concat(EXTENSIONS_IMPLEMENTATION, EXTENSIONS_HEADERS));

    public static Set<String> getExtensionsHeaders() {
        return EXTENSIONS_HEADERS;
    }

    public static Set<String> getExtensionsImplementation() {
        return EXTENSIONS_IMPLEMENTATION;
    }

    public static Set<String> getPermittedExtensions() {
        return PERMITTED_EXTENSIONS;
    }

    private List<File> sources;

    private GlobalManager globalManager;
    private Map<String, ClavaNode> nodesCache;
    // Can be used to store information that should be accessible through the application
    private DataStore appData;

    public App(Collection<TranslationUnit> children) {
        super(ClavaNodeInfo.undefinedInfo(), children);

        sources = Collections.emptyList();
        globalManager = new GlobalManager();
        nodesCache = new HashMap<>();
        appData = DataStore.newInstance("Clava App Data");
        // System.out.println("SETTING STANDARD:" + appData.get(ClavaOptions.STANDARD));
        CURRENT_STANDARD.set(appData.get(ClavaOptions.STANDARD));
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new App(Collections.emptyList());
    }

    public DataStore getAppData() {
        return appData;
    }

    /**
     * getLocation() is not implemented for AppNode
     */
    @Override
    public SourceRange getLocation() {
        throw new RuntimeException("Not implemented for AppNode");
    }

    @Override
    public String getCode() {
        return getCode(null);
    }

    public String getCode(File baseFolder) {
        StringBuilder code = new StringBuilder();

        for (TranslationUnit tu : getTranslationUnits()) {
            String basepath = baseFolder != null ? SpecsIo.getRelativePath(new File(tu.getFilepath()), baseFolder)
                    : tu.getFilepath();

            code.append("/**** File '" + basepath + tu.getFilename() + "' ****/" + ln() + ln());
            code.append(tu.getCode());
            code.append(ln() + "/**** End File ****/" + ln() + ln());
        }

        return code.toString();
    }

    public void setSources(List<File> sources) {
        this.sources = sources;
    }

    public List<TranslationUnit> getTranslationUnits() {
        return getChildren(TranslationUnit.class);
    }

    public void writeFromTopFile(File topFile, File destinationFolder) {
        // Check if topFile exists in the translation units
        String canonicalPath = SpecsIo.getCanonicalPath(topFile);
        if (!getTranslationUnits().stream()
                .filter(tu -> SpecsIo.getCanonicalPath(tu.getFile()).equals(canonicalPath))
                .findFirst()
                .isPresent()) {

            SpecsLogs.msgInfo(
                    "Could not find top file '" + topFile + "' in the translation units, returning without writing");
            return;
        }

        write(SpecsIo.getCanonicalFile(topFile).getParentFile(), destinationFolder);

    }

    public void write(File baseInputFolder, File destinationFolder) {
        for (Entry<File, String> entry : getCode(baseInputFolder, destinationFolder).entrySet()) {
            SpecsIo.write(entry.getKey(), entry.getValue());
        }
    }

    // private static List<File> getAllSourcefiles(List<File> sources) {
    // return getAllSourcefiles(sources, false);
    // }

    private static List<File> getAllSourcefiles(List<File> sources, boolean includeHeaders) {

        if (includeHeaders) {
            return SpecsIo.getFiles(sources, PERMITTED_EXTENSIONS).stream()
                    .map(filename -> new File(filename))
                    .collect(Collectors.toList());
        }

        List<File> allFiles = new ArrayList<>();

        List<File> implementationFiles = SpecsIo.getFiles(sources, EXTENSIONS_IMPLEMENTATION).stream()
                .map(filename -> new File(filename))
                .collect(Collectors.toList());

        allFiles.addAll(implementationFiles);

        for (File sourceFolder : sources) {
            allFiles.addAll(SpecsIo.getFilesRecursive(sourceFolder, EXTENSIONS_HEADERS));
        }

        return allFiles;
    }

    public Map<File, String> getCode(File baseInputFolder, File destinationFolder) {

        Map<File, String> files = new HashMap<>();

        // Generate code for each translation unit considering the given destination
        // and using a path relative to the topFile

        for (TranslationUnit tUnit : getTranslationUnits()) {
            String relativePath = getRelativePath(new File(tUnit.getFilepath()), baseInputFolder);

            // Build destination path
            File actualDestinationFolder = SpecsIo.mkdir(new File(destinationFolder, relativePath));
            File destinationFile = new File(actualDestinationFolder, tUnit.getFilename());

            files.put(destinationFile, tUnit.getCode());
            // IoUtils.write(destinationFile, tUnit.getCode());
        }

        List<File> localSources = sources.isEmpty() ? Arrays.asList(baseInputFolder) : sources;
        List<File> allFiles = getAllSourcefiles(localSources, true);

        // System.out.println("All files:" + allFiles);

        Set<String> relativeWeaved = files.keySet().stream()
                .map(file -> SpecsIo.getRelativePath(file, destinationFolder))
                .collect(Collectors.toSet());
        // System.out.println("GENERATED FILES:" + relativeWeaved);

        for (File file : allFiles) {
            String relativeSource = getRelativePath(file, baseInputFolder);
            // String relativeSource = IoUtils.getRelativePath(file, baseInputFolder);

            if (relativeWeaved.contains(relativeSource)) {
                continue;
            }

            SpecsLogs.msgInfo("Creating empty source for file '" + relativeSource + "'");

            // Create translation unit for the missing file
            String relativePath = SpecsIo.getRelativePath(file.getParentFile(), baseInputFolder);

            // Avoid writing outside of the destination folder, if relative path has '../', remove them
            while (relativePath.startsWith("../")) {
                relativePath = relativePath.substring("../".length());
            }

            // Build destination path
            File actualDestinationFolder = SpecsIo.mkdir(new File(destinationFolder, relativePath));
            File destinationFile = new File(actualDestinationFolder, file.getName());

            TranslationUnit tUnit = ClavaNodeFactory.translationUnit(file.getName(), file.getParent(),
                    Collections.emptyList());

            files.put(destinationFile, tUnit.getCode());
        }

        return files;
    }

    public String getRelativePath(File baseInputFolder, TranslationUnit tUnit) {
        return getRelativePath(tUnit.getFile(), baseInputFolder);
    }

    public String getRelativeFolderPath(File baseInputFolder, TranslationUnit tUnit) {
        return getRelativePath(tUnit.getFile().getParentFile(), baseInputFolder);
    }

    private String getRelativePath(File baseFile, File baseInputFolder) {
        String relativePath = SpecsIo.getRelativePath(baseFile, baseInputFolder);

        // Avoid writing outside of the destination folder, if relative path has '../', remove them
        while (relativePath.startsWith("../")) {
            relativePath = relativePath.substring("../".length());
        }

        return relativePath;
    }

    public Optional<TranslationUnit> getFile(String filename) {
        return getTranslationUnits().stream()
                .filter(tu -> tu.getFilename().equals(filename))
                .findFirst();
    }

    public ClavaNode getNode(String id) {
        return getNodeTry(id)
                .orElseThrow(() -> new RuntimeException("Could not find node with id '" + id + "'"));
    }

    public Optional<ClavaNode> getNodeTry(String id) {
        // Check if node was already asked
        ClavaNode cachedNode = nodesCache.get(id);
        if (cachedNode != null) {
            return Optional.of(cachedNode);
        }

        Optional<ClavaNode> askedNode = getDescendantsAndSelfStream()
                .filter(node -> node.getId().isPresent())
                .filter(node -> node.getId().get().getExtendedId().equals(id))
                .findFirst();

        askedNode.ifPresent(node -> nodesCache.put(id, node));

        return askedNode;
    }

    /**
     * @deprecated use the version that has a namespace as argument
     * @param declName
     * @return
     */
    @Deprecated
    public CXXRecordDecl getCXXRecordDecl(String declName) {
        return getDescendantsStream().filter(child -> child instanceof CXXRecordDecl)
                .map(child -> (CXXRecordDecl) child)
                .filter(recordDecl -> recordDecl.getDeclName().equals(declName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find CXXRecordDecl with name '" + declName + "'"));

    }

    public CXXRecordDecl getCXXRecordDecl(String namespace, String declName) {
        // Iterate over translation units, NamespaceDecl and CXXRecordDecl without namespace are directly under TUs
        Stream<ClavaNode> cxxRecordCandidates = getTranslationUnits().stream()
                .flatMap(tu -> tu.getChildrenStream());

        // If there is a namespace, filter the stream for the corresponding NamespaceDecl first
        if (namespace != null) {
            cxxRecordCandidates = cxxRecordCandidates
                    .filter(child -> child instanceof NamespaceDecl)
                    .map(namespaceDecl -> (NamespaceDecl) namespaceDecl)
                    .filter(namespaceDecl -> namespaceDecl.getDeclName().equals(namespace))
                    .flatMap(namespaceDecl -> namespaceDecl.getChildrenStream());
        }

        // Find CXXRecordDecl
        return cxxRecordCandidates.filter(child -> child instanceof CXXRecordDecl)
                .map(child -> (CXXRecordDecl) child)
                .filter(recordDecl -> recordDecl.getDeclName().equals(declName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Could not find CXXRecordDecl with name '" + (namespace == null ? "" : namespace + "::")
                                + declName + "'"));

    }

    public GlobalManager getGlobalManager() {
        return globalManager;
    }

    public void addFile(TranslationUnit tu) {
        addChild(tu);
    }
}
