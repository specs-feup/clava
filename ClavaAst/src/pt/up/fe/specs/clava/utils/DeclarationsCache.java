/**
 * Copyright 2022 SPeCS.
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

package pt.up.fe.specs.clava.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.TagDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.collections.MultiMap;

/**
 * Stores mappings between decl names and the corresponding declarations and definitions.
 * 
 * <p>
 * Currently supports caching of FunctionDecl and TagDecl.
 * 
 * @author Joao Bispo
 *
 */
public class DeclarationsCache {

    private final MultiMap<String, FunctionDecl> functionPrototypes;
    private final Map<String, FunctionDecl> functionImplementations;
    private final MultiMap<String, TagDecl> tagPrototypes;
    private final Map<String, TagDecl> tagImplementations;
    private final Map<String, VarDecl> globalVarDefinitions;

    // private boolean isInitialized;

    public DeclarationsCache(App app) {
        // clear();
        functionPrototypes = new MultiMap<>();
        functionImplementations = new HashMap<>();
        tagPrototypes = new MultiMap<>();
        tagImplementations = new HashMap<>();
        globalVarDefinitions = new HashMap<>();

        // Build maps
        app.getDescendantsStream()
                .forEach(node -> processNode(node));

    }

    public List<FunctionDecl> getFunctionPrototypes(FunctionDecl function) {
        return getFunctionPrototypes(getFunctionKey(function));
    }

    private List<FunctionDecl> getFunctionPrototypes(String signature) {
        return functionPrototypes.get(signature);
    }

    public Optional<FunctionDecl> getFunctionImplementation(FunctionDecl function) {
        return getFunctionImplementation(getFunctionKey(function));
    }

    private Optional<FunctionDecl> getFunctionImplementation(String signature) {
        return Optional.ofNullable(functionImplementations.get(signature));
    }

    public List<TagDecl> getTagPrototypes(TagDecl tagDecl) {
        return getTagPrototypes(getTagKey(tagDecl));
    }

    private List<TagDecl> getTagPrototypes(String signature) {
        return tagPrototypes.get(signature);
    }

    public Optional<TagDecl> getTagImplementation(TagDecl tagDecl) {
        return getTagImplementation(getTagKey(tagDecl));
    }

    private Optional<TagDecl> getTagImplementation(String signature) {
        return Optional.ofNullable(tagImplementations.get(signature));
    }

    public Optional<VarDecl> getGlobalVarDefinition(VarDecl varDecl) {
        return Optional.ofNullable(globalVarDefinitions.get(getVarDeclKey(varDecl)));
    }

    // private void initialize(App app) {
    //
    //
    // // isInitialized = true;
    // }

    private void processNode(ClavaNode node) {

        // Check if function
        if (node instanceof FunctionDecl) {
            processFunction((FunctionDecl) node);
            return;
        }

        // Check if tag
        if (node instanceof TagDecl) {
            processTag((TagDecl) node);
            return;
        }

        if (node instanceof VarDecl) {
            processVarDecl((VarDecl) node);
            return;
        }

        // Nothing to do

    }

    private void processVarDecl(VarDecl varDecl) {
        // Only globals
        if (!varDecl.get(VarDecl.HAS_GLOBAL_STORAGE)) {
            return;
        }

        var key = getVarDeclKey(varDecl);

        // If not none, is not the definition
        if (varDecl.get(VarDecl.STORAGE_CLASS) != StorageClass.None) {
            return;
        }

        addImplementation(key, varDecl, globalVarDefinitions);
    }

    private String getVarDeclKey(VarDecl varDecl) {
        return varDecl.get(VarDecl.DECL_NAME);
    }

    private void processTag(TagDecl tagDecl) {
        var signature = getTagKey(tagDecl);
        var isCompleteDefinition = tagDecl.isCompleteDefinition();

        // Store decl in appropriate map
        if (isCompleteDefinition) {
            addImplementation(signature, tagDecl, tagImplementations);
        } else {
            tagPrototypes.put(signature, tagDecl);
        }

        // return getDescendantsStream().filter(child -> child instanceof CXXRecordDecl)
        // .map(child -> (CXXRecordDecl) child)
        // // Only if it has the same record name
        // .filter(recordDecl -> recordDecl.getDeclName().equals(record.getDeclName()))
        // // Only if it is a complete definition
        // .filter(recordDecl -> recordDecl.isCompleteDefinition())
        // // There should be only one definition
        // .findFirst();

    }

    private <T extends ClavaNode> void addImplementation(String key, T node, Map<String, T> cache) {
        var previousValue = cache.put(key, node);

        // There should be only one
        if (previousValue != null) {
            SpecsLogs.info("Found more than one implementation for node " + node.getClass().getSimpleName() + " '" + key
                    + "', returning the first occurence:\n"
                    + "1 -> " + previousValue.getLocation() + "\n"
                    + "2 -> " + node.getLocation());

            // Setting to previous value, this way we avoid asking if the map contains the value
            // and the more common path does not have that overhead
            cache.put(key, previousValue);
        }
    }

    public String getFunctionKey(FunctionDecl function) {
        return function.getSignature();
    }

    public String getTagKey(TagDecl tagDecl) {
        return tagDecl.getDeclName();
    }

    private void processFunction(FunctionDecl function) {

        // Check hasBody flag
        var hasBody = function.hasBody();

        // Get signature
        var signature = getFunctionKey(function);

        // Normalize function decl
        var normalizedFunction = (FunctionDecl) ClavaNodes.normalizeDecl(function);

        // Store decl in appropriate map
        if (hasBody) {
            addImplementation(signature, normalizedFunction, functionImplementations);

            // var previousValue = functionImplementations.put(signature, normalizedFunction);
            //
            // // There should be only one
            // if (previousValue != null) {
            // SpecsLogs.info("Found more than one implementation for function '" + signature
            // + "', returning the first occurence:\n"
            // + "1 -> " + previousValue.getLocation() + "\n"
            // + "2 -> " + normalizedFunction.getLocation());
            //
            // // Setting to previous value, this way we avoid asking if the map contains the value
            // // and the more common path does not have that overhead
            // functionImplementations.put(signature, normalizedFunction);
            // }
        } else {
            functionPrototypes.put(signature, normalizedFunction);
        }
    }

    // private void initMaps() {
    // functionPrototypes = new MultiMap<>();
    // functionImplementations = new HashMap<>();
    // tagPrototypes = new MultiMap<>();
    // tagImplementations = new HashMap<>();
    // }
    //
    // public void clear() {
    // this.functionPrototypes = null;
    // this.functionImplementations = null;
    // this.tagPrototypes = null;
    // this.tagImplementations = null;
    //
    // this.isInitialized = false;
    // }
}
