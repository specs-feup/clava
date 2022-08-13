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

    // private boolean isInitialized;

    public DeclarationsCache(App app) {
        // clear();
        functionPrototypes = new MultiMap<>();
        functionImplementations = new HashMap<>();
        tagPrototypes = new MultiMap<>();
        tagImplementations = new HashMap<>();

        // Build maps
        app.getDescendantsStream()
                .forEach(node -> processNode(node));

    }

    public List<FunctionDecl> getFunctionPrototypes(String signature) {
        return functionPrototypes.get(signature);
    }

    public Optional<FunctionDecl> getFunctionImplementation(String signature) {
        return Optional.ofNullable(functionImplementations.get(signature));
    }

    public List<TagDecl> getTagPrototypes(String signature) {
        return tagPrototypes.get(signature);
    }

    public Optional<TagDecl> getTagImplementation(String signature) {
        return Optional.ofNullable(tagImplementations.get(signature));
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

        // Nothing to do

    }

    private void processTag(TagDecl tagDecl) {
        var signature = tagDecl.getDeclName();
        var isCompleteDefinition = tagDecl.isCompleteDefinition();

        // Store decl in appropriate map
        if (isCompleteDefinition) {
            var previousValue = tagImplementations.put(signature, tagDecl);

            // There should be only one
            if (previousValue != null) {
                SpecsLogs.info("Found more than one implementation for record '" + signature + "':\n"
                        + "1 -> " + previousValue.getLocation() + "\n"
                        + "2 -> " + tagDecl.getLocation());
            }
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

    private void processFunction(FunctionDecl function) {

        // Check hasBody flag
        var hasBody = function.hasBody();

        // Get signature
        var signature = function.getSignature();

        // Normalize function decl
        var normalizedFunction = (FunctionDecl) ClavaNodes.normalizeDecl(function);

        // Store decl in appropriate map
        if (hasBody) {
            var previousValue = functionImplementations.put(signature, normalizedFunction);

            // There should be only one
            if (previousValue != null) {
                SpecsLogs.info("Found more than one implementation for function '" + signature + "':\n"
                        + "1 -> " + previousValue.getLocation() + "\n"
                        + "2 -> " + normalizedFunction.getLocation());
            }
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
