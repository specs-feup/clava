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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.omp.clang.GenericClangOMP;

/**
 * Maps Clang AST dump classnames to ClavaNode classes.
 *
 * <p>
 * Delegates the generic lookup machinery to jOptions' ClassesService, keeping only the Clava-specific rules: the
 * classname suffix mapping to the pt.up.fe.specs.clava.ast packages, and the two fallbacks for unknown attributes and
 * OMP nodes.
 */
public class ClassesService extends org.suikasoft.jOptions.treenode.ClassesService<ClavaNode> {

    private static final String CLAVA_AST_PACKAGE = "pt.up.fe.specs.clava.ast";
    private static final DataStore EMPTY_DATA_STORE = DataStore.newInstance("Empty DataStore");

    private static final ClassesService STATIC_INSTANCE = new ClassesService();

    private final CustomClassnameMapper customClassMap;
    private final Map<String, Class<? extends ClavaNode>> fallbackClassMap = new HashMap<>();
    private final Set<String> warnedClasses = ConcurrentHashMap.newKeySet();

    public ClassesService(CustomClassnameMapper customClassMap) {
        // The suffix mapping below always produces a full name, so no package scan is needed
        super(ClavaNode.class, Collections.emptyList());
        this.customClassMap = customClassMap;
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

    public Class<? extends ClavaNode> getClass(String classname, DataStore data) {

        // Try custom map
        Class<? extends ClavaNode> clavaNodeClass = customClassMap.getClass(classname, data);
        if (clavaNodeClass != null) {
            return clavaNodeClass;
        }

        return getClass(classname);
    }

    @Override
    public Class<? extends ClavaNode> getClass(String classname) {

        // Try cached fallbacks
        var fallbackClass = fallbackClassMap.get(classname);
        if (fallbackClass != null) {
            return fallbackClass;
        }

        try {
            return super.getClass(classname);
        } catch (RuntimeException e) {
            var newFallbackClass = discoverFallbackClass(classname);
            if (newFallbackClass == null) {
                throw e;
            }

            fallbackClassMap.put(classname, newFallbackClass);
            return newFallbackClass;
        }
    }

    /**
     * Clava-specific fallbacks, applied when no class is found for the given classname.
     */
    private Class<? extends ClavaNode> discoverFallbackClass(String classname) {
        if (classname.endsWith("Attr")) {
            if (!warnedClasses.contains(classname)) {
                warnedClasses.add(classname);

                ClavaLog.info("No parser defined for attribute '" + classname
                        + "', using generic attribute parser with no arguments");
            }

            return Attribute.class;
        }

        if (classname.startsWith("OMP")) {
            return GenericClangOMP.class;
        }

        return null;
    }

    @Override
    protected String customSimpleNameToFullName(String nodeClassname) {
        return simpleNameToFullName(nodeClassname);
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
        return getNodeBuilder(clavaNodeClass);
    }

}
