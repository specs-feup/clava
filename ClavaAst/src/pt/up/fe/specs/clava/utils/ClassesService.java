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

package pt.up.fe.specs.clava.utils;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.omp.clang.GenericClangOMP;
import pt.up.fe.specs.util.SpecsLogs;

public class ClassesService {

    private static final String CLAVA_AST_PACKAGE = "pt.up.fe.specs.clava.ast";
    private static final DataStore EMPTY_DATA_STORE = DataStore.newInstance("Empty DataStore");

    private static final ClassesService STATIC_INSTANCE = new ClassesService();

    private final CustomClassnameMapper customClassMap;
    private final Map<String, Class<? extends ClavaNode>> autoClassMap;

    public ClassesService(CustomClassnameMapper customClassMap) {
        this.customClassMap = customClassMap;
        this.autoClassMap = new HashMap<>();
    }

    public ClassesService() {
        this(new CustomClassnameMapper());
    }

    public CustomClassnameMapper getCustomClassMap() {
        return customClassMap;
    }

    public static Class<? extends ClavaNode> getClavaClass(String classname) {
        return STATIC_INSTANCE.getClass(classname, EMPTY_DATA_STORE);
    }

    // public void addCustomMapping(String className, Class<? extends ClavaNode> clavaNodeClass) {
    // autoClassMap.put(className, clavaNodeClass);
    // }

    // public Class<? extends ClavaNode> getClass(String classname) {
    // // TODO: Maybe DataStore argument is no longer needed? It is required only for custom mappings
    // return getClass(classname, EMPTY_DATA_STORE);
    // }

    public Class<? extends ClavaNode> getClass(String classname, DataStore data) {

        // Try custom map
        Class<? extends ClavaNode> clavaNodeClass = customClassMap.getClass(classname, data);
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
            // Before throwing exception, try some cases
            if (clangClassname.endsWith("Attr")) {
                return Attribute.class;
            }

            if (clangClassname.startsWith("OMP")) {
                return GenericClangOMP.class;
            }

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

        if (nodeClassname.endsWith("Attr")) {
            return CLAVA_AST_PACKAGE + ".attr." + nodeClassname;
        }

        if (nodeClassname.startsWith("OMP")) {
            return CLAVA_AST_PACKAGE + ".omp.clang." + nodeClassname;
        }

        // By default, if none of the above, try expression
        return CLAVA_AST_PACKAGE + ".expr." + nodeClassname;
    }

    public BiFunction<DataStore, List<? extends ClavaNode>, ClavaNode> getClavaNodeBuilder(
            Class<? extends ClavaNode> clavaNodeClass) {

        // Create builder
        try {

            Constructor<? extends ClavaNode> constructor = clavaNodeClass.getConstructor(DataStore.class,
                    Collection.class);

            return (data, children) -> {
                try {
                    return constructor.newInstance(data, children);
                } catch (Exception e) {
                    throw new RuntimeException("Could not call constructor for ClavaNode", e);
                }
            };

        } catch (Exception e) {
            SpecsLogs.msgLib("Could not create constructor for ClavaNode:" + e.getMessage());
            return null;
        }

    }

}
