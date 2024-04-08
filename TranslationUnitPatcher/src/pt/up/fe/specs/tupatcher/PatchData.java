/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.tupatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.specs.tupatcher.definition.SymbolDefinition;
import pt.up.fe.specs.tupatcher.definition.FunctionDefinition;
import pt.up.fe.specs.tupatcher.definition.TypeDefinition;
import pt.up.fe.specs.util.SpecsIo;

/**
 * 
 * @author Pedro Galvao
 *
 */
public class PatchData {

    private final Map<String, TypeDefinition> missingTypes;
    private final Map<String, FunctionDefinition> missingFunctions;
    private final Map<String, TypeDefinition> missingVariables;
    private final Map<String, TypeDefinition> missingConstVariables;

    private boolean allErrorsPatched;

    /**
     * List of all the errors fixed by the patch.
     */
    private final List<ErrorKind> errors;

    public PatchData() {
        this.missingTypes = new HashMap<>();
        this.missingFunctions = new HashMap<>();
        this.missingVariables = new HashMap<>();
        this.missingConstVariables = new HashMap<>();
        this.errors = new ArrayList<>();
        this.allErrorsPatched = false;
    }

    /**
     * 
     * @return true if the patch fixes all errors in the file
     */
    public boolean isAllErrorsPatched() {
        return allErrorsPatched;
    }

    public void setAllErrorsPatched(boolean allErrorsPatched) {
        this.allErrorsPatched = allErrorsPatched;
    }

    public void addError(ErrorKind errorKind) {
        errors.add(errorKind);
    }

    public List<ErrorKind> getErrors() {
        return errors;
    }

    public void addType(String typeName) {
        if (typeName != null) {
            if (typeName.contains("::")) {
            } else {
                missingTypes.put(typeName, new TypeDefinition(typeName));
            }
        }
    }

    public void addType(TypeDefinition type) {
        if (type.getName().contains("::")) {
            return;
        }
        missingTypes.put(type.getName(), type);
    }

    public void addVariable(String varName) {
        if (varName != null) {
            TypeDefinition type = new TypeDefinition();
            missingVariables.put(varName, type);
            missingTypes.put(type.getName(), type);
        }
    }

    public void addConstVariable(String varName) {
        if (varName != null) {
            TypeDefinition type = new TypeDefinition();
            missingConstVariables.put(varName, type);
            missingTypes.put(type.getName(), type);
        }
    }

    public TypeDefinition getVariable(String varName) {
        if (varName.contains("::")) {
            String[] classNames = varName.split("::");
            TypeDefinition type = missingTypes.get(classNames[0]);
            for (int i = 1; i < classNames.length; i++) {
                String className = classNames[i];
                System.out.println("className");
                System.out.println(className);
                type = type.getNestedType(className);
            }
            return type;
        }
        return missingVariables.get(varName);
    }

    public FunctionDefinition getFunction(String functionName) {
        return missingFunctions.get(functionName);
    }

    public void addFunction(String functionName) {
        TypeDefinition returnType = new TypeDefinition();
        addType(returnType);
        missingFunctions.put(functionName, new FunctionDefinition(functionName, returnType));
    }

    public void removeVariable(String varName) {
        missingVariables.remove(varName);
    }

    public TypeDefinition getType(String typeName) {
        typeName = typeName.replace("class ", "").replace("struct ", "");
        if (typeName.contains("::")) {
            String[] classNames = typeName.split("::");
            TypeDefinition type = missingTypes.get(classNames[0]);
            for (int i = 1; i < classNames.length; i++) {
                String className = classNames[i];
                System.out.println("className");
                System.out.println(className);
                type = type.getNestedType(className);
            }
            return type;
        } else {
            return missingTypes.get(typeName);
        }
    }

    public void setType(String typeName, TypeDefinition type) {
        // missingTypes.remove(typeName);
        missingTypes.put(typeName, type);
    }

    public Map<String, TypeDefinition> getTypes() {
        return missingTypes;
    }

    /**
     * Copy the source file to the output folder adding #include "patch.h" at the top of it
     */
    public void copySource(File originalFile, File patchedFile, String headerFilename) {
        var result = "#include \"" + headerFilename + "\"\n" + SpecsIo.read(originalFile);
        // File destFile = new File("output/file.cpp");
        SpecsIo.write(patchedFile, result);
    }

    /**
     * @return String with all the variables declarations.
     */
    public String variablesPatches() {
        StringBuilder result = new StringBuilder();
        for (String varName : missingVariables.keySet()) {
            String type = missingVariables.get(varName).getName();
            result.append(type).append(" ").append(varName).append(";\n");
        }
        int i = 0;
        for (String varName : missingConstVariables.keySet()) {
            TypeDefinition type = missingConstVariables.get(varName);
            String typeName = type.getName();
            result.append("const ").append(typeName).append(" ").append(varName);
            if (TUPatcherUtils.isPrimitiveType(type.getKind())) {
                result.append(" = ").append(i).append(";\n");
            } else {
                result.append(" = ").append(typeName).append("();\n");
            }
            i++;
        }
        return result.toString();
    }

    /**
     * @return String with the definitions of all the functions.
     */
    public String functionPatches(Map<String, FunctionDefinition> functions) {
        StringBuilder result = new StringBuilder();
        for (String functionName : functions.keySet()) {
            FunctionDefinition function = functions.get(functionName);
            result.append(function.toDefinitionString());
        }
        return result.toString();
    }

    /**
     * @return String with the content to write to the file patch.h
     */
    public String str() {
        StringBuilder result = new StringBuilder("#define NULL 0\n");
        for (TypeDefinition type : missingTypes.values()) {
            if (type.getKind().equals("class")) {
                result.append("class ").append(type.getName()).append(";\n");
            } else if (type.getKind().equals("struct")) {
                if (!type.getTypedefStruct()) {
                    result.append("struct ").append(type.getName()).append(";\n");
                }
            }
        }

        List<SymbolDefinition> defs = orderedDefinitions();
        for (SymbolDefinition def : defs) {
            result.append(def.toDefinitionString());
        }
        result.append(variablesPatches());

        return result.toString();
    }

    /**
     * Writes in the patched file and corresponding header in the output directory.
     */
    public void write(File originalFile, File patchedFile) {
        var headerFilename = TUPatcherUtils.getPatchedHeaderFilename(originalFile.getName());
        File patchHeaderFile = new File(patchedFile.getParentFile(), headerFilename);

        SpecsIo.write(patchHeaderFile, str());

        copySource(originalFile, patchedFile, headerFilename);
    }

    /**
     * Topological sorting of a graph of dependencies between types and functions.
     * <p>
     * When there is cyclic dependency between two or more types or functions, this function returns a list that is only
     * partially ordered. When it happens this problem may be solved by the fact that all structs and classes are
     * declared (but not defined) in the beginning of patch.h
     */
    public List<SymbolDefinition> orderedDefinitions() {
        List<SymbolDefinition> notSorted = new ArrayList<>();
        List<SymbolDefinition> result = new ArrayList<>();
        for (TypeDefinition type : missingTypes.values()) {
            if (!type.isNested()) {
                notSorted.add(type);
            }
        }
        notSorted.addAll(missingFunctions.values());
        while (!notSorted.isEmpty()) {
            boolean cyclic = true;
            for (SymbolDefinition def : notSorted) {
                boolean ok = true;
                for (SymbolDefinition dependency : def.getSymbolDependencies()) {
                    boolean contains = false;
                    for (SymbolDefinition def2 : notSorted) {
                        if (dependency.getName().equals(def2.getName())) {
                            contains = true;
                            break;
                        }
                    }
                    if (contains) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    notSorted.remove(def);
                    result.add(def);
                    cyclic = false;
                    break;
                }
            }
            if (cyclic) {
                for (SymbolDefinition def : notSorted) {
                    if (def instanceof TypeDefinition) {
                        if (!(((TypeDefinition) def).getKind().equals("class")
                                || ((TypeDefinition) def).getKind().equals("struct"))) {
                            result.add(def);
                        }
                    } else {
                        result.add(def);
                    }
                }
                for (SymbolDefinition def : notSorted) {
                    if (def instanceof TypeDefinition) {
                        if (((TypeDefinition) def).getKind().equals("class") || ((TypeDefinition) def).getKind().equals("struct")) {
                            result.add(def);
                        }
                    } else {
                        result.add(def);
                    }
                }
                return result;
                // throw new RuntimeException("Cyclic dependencies");
            }
        }
        return result;
    }

}
