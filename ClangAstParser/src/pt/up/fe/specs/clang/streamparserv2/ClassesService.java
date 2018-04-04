/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clang.streamparserv2;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.util.SpecsLogs;

public class ClassesService {

    private static final String CLAVA_AST_PACKAGE = "pt.up.fe.specs.clava.ast";

    private final Map<String, Class<? extends ClavaNode>> customClassMap;
    private final Map<String, Class<? extends ClavaNode>> autoClassMap;

    private final Map<Class<? extends ClavaNode>, BiFunction<ClavaData, List<ClavaNode>, ClavaNode>> builders;

    public ClassesService(Map<String, Class<? extends ClavaNode>> customClassMap) {
        this.customClassMap = customClassMap;
        this.autoClassMap = new HashMap<>();
        builders = new HashMap<>();
    }

    public BiFunction<ClavaData, List<ClavaNode>, ClavaNode> getBuilder(Class<? extends ClavaNode> clavaNodeClass,
            Class<? extends ClavaData> clavaDataClass) {
        // Check if builder is ready
        BiFunction<ClavaData, List<ClavaNode>, ClavaNode> builder = builders.get(clavaNodeClass);
        if (builder != null) {
            return builder;
        }

        // Create and store builder
        try {

            Constructor<? extends ClavaNode> constructor = clavaNodeClass.getConstructor(clavaDataClass, List.class);
            builder = (node, children) -> {
                try {
                    return constructor.newInstance(node, children);
                } catch (Exception e) {
                    throw new RuntimeException("Could not call constructor for ClavaNode", e);
                }
            };

            builders.put(clavaNodeClass, builder);

            return builder;
        } catch (Exception e) {
            SpecsLogs.debug("Could not create constructor for ClavaNode:" + e.getMessage());
            return null;
        }

    }

    public Class<? extends ClavaNode> getClass(String classname) {

        // Try custom map
        Class<? extends ClavaNode> clavaNodeClass = customClassMap.get(classname);
        if (clavaNodeClass != null) {
            return clavaNodeClass;
        }

        // Try cached nodes
        clavaNodeClass = autoClassMap.get(classname);
        if (clavaNodeClass != null) {
            return clavaNodeClass;
        }

        // Try discovering the node
        clavaNodeClass = discoverClass(classname);
        autoClassMap.put(classname, clavaNodeClass);
        return clavaNodeClass;
        // if (clavaNodeClass != null) {
        // autoClassMap.put(classname, clavaNodeClass);
        // return clavaNodeClass;
        // }
        //
        // throw new RuntimeException("Could not determine class for the name '" + classname + "'");
    }

    // private Optional<Class<? extends ClavaNode>> discoverClass(String clangClassname) {
    private Class<? extends ClavaNode> discoverClass(String clangClassname) {

        String fullClassname = simpleNameToFullName(clangClassname);

        try {
            // Get class
            Class<?> aClass = Class.forName(fullClassname);

            // Check if class is a subtype of ClavaNode
            if (!ClavaNode.class.isAssignableFrom(aClass)) {
                throw new RuntimeException("Classname '" + clangClassname + "' was converted to a (" + fullClassname
                        + ") that is not a ClavaNode");
            }

            // Cast class object
            // return Optional.of(aClass.asSubclass(ClavaNode.class));
            return aClass.asSubclass(ClavaNode.class);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not map classname '" + clangClassname + "' to a ClavaNode class");
            // return Optional.empty();
        }
    }

    private static String simpleNameToFullName(String nodeClassname) {
        if (nodeClassname.endsWith("Decl")) {
            return CLAVA_AST_PACKAGE + ".decl." + nodeClassname;
        }

        if (nodeClassname.endsWith("Stmt")) {
            return CLAVA_AST_PACKAGE + ".stmt." + nodeClassname;
        }

        if (nodeClassname.endsWith("Type")) {
            return CLAVA_AST_PACKAGE + ".type." + nodeClassname;
        }

        // By default, if none of the above, try expression
        return CLAVA_AST_PACKAGE + ".expr." + nodeClassname;
    }

}
