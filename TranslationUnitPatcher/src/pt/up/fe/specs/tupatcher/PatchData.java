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

import pt.up.fe.specs.util.SpecsIo;

/**
 * 
 * @author Pedro Galvao
 *
 */
public class PatchData {

    private final HashMap<String, TypeInfo> missingTypes;
    private final HashMap<String, FunctionInfo> missingFunctions;
    private final HashMap<String, TypeInfo> missingVariables;
    private final HashMap<String, TypeInfo> missingConstVariables;

    private boolean allErrorsPatched;

    /**
     * List of all the errors fixed by the patch.
     */
    private final ArrayList<ErrorKind> errors;

    public PatchData() {
        this.missingTypes = new HashMap<String, TypeInfo>();
        this.missingFunctions = new HashMap<String, FunctionInfo>();
        this.missingVariables = new HashMap<String, TypeInfo>();
        this.missingConstVariables = new HashMap<String, TypeInfo>();
        this.errors = new ArrayList<ErrorKind>();
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

    public ArrayList<ErrorKind> getErrors() {
        return errors;
    }

    public void addType(String typeName) {
        if (typeName != null) {
            if (typeName.contains("::")) {
                return;
            } else {
                missingTypes.put(typeName, new TypeInfo(typeName));
            }
        }
    }

    public void addType(TypeInfo type) {
        if (type.getName().contains("::")) {
            return;
        }
        missingTypes.put(type.getName(), type);
    }

    public void addVariable(String varName) {
        if (varName != null) {
            TypeInfo type = new TypeInfo();
            missingVariables.put(varName, type);
            missingTypes.put(type.getName(), type);
        }
    }

    public void addConstVariable(String varName) {
        if (varName != null) {
            TypeInfo type = new TypeInfo();
            missingConstVariables.put(varName, type);
            missingTypes.put(type.getName(), type);
        }
    }

    public TypeInfo getVariable(String varName) {
        if (varName.contains("::")) {
            String[] classNames = varName.split("::");
            TypeInfo type = missingTypes.get(classNames[0]);
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

    public FunctionInfo getFunction(String functionName) {
        return missingFunctions.get(functionName);
    }

    public void addFunction(String functionName) {
        TypeInfo returnType = new TypeInfo();
        addType(returnType);
        missingFunctions.put(functionName, new FunctionInfo(functionName, returnType));
    }

    public void removeVariable(String varName) {
        missingVariables.remove(varName);
    }

    public TypeInfo getType(String typeName) {
        typeName = typeName.replace("class ", "").replace("struct ", "");
        if (typeName.contains("::")) {
            String[] classNames = typeName.split("::");
            TypeInfo type = missingTypes.get(classNames[0]);
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

    public void setType(String typeName, TypeInfo type) {
        // missingTypes.remove(typeName);
        missingTypes.put(typeName, type);
    }

    public HashMap<String, TypeInfo> getTypes() {
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
            TypeInfo type = missingConstVariables.get(varName);
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
    public String functionPatches(HashMap<String, FunctionInfo> functions) {
        StringBuilder result = new StringBuilder();
        for (String functionName : functions.keySet()) {
            FunctionInfo function = functions.get(functionName);
            result.append(function.str());
        }
        return result.toString();
    }

    /**
     * @return String with the content to write to the file patch.h
     */
    public String str() {
        StringBuilder result = new StringBuilder("#define NULL 0\n");
        for (TypeInfo type : missingTypes.values()) {
            if (type.getKind().equals("class")) {
                result.append("class ").append(type.getName()).append(";\n");
            } else if (type.getKind().equals("struct")) {
                if (!type.getTypedefStruct()) {
                    result.append("struct ").append(type.getName()).append(";\n");
                }
            }
        }

        ArrayList<Definition> defs = orderedDefinitions();
        for (Definition def : defs) {
            result.append(def.str());
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
    public ArrayList<Definition> orderedDefinitions() {
        ArrayList<Definition> notSorted = new ArrayList<Definition>();
        ArrayList<Definition> result = new ArrayList<Definition>();
        for (TypeInfo type : missingTypes.values()) {
            if (!type.isNested()) {
                notSorted.add(type);
            }
        }
        for (FunctionInfo function : missingFunctions.values()) {
            notSorted.add(function);
        }
        while (!notSorted.isEmpty()) {
            boolean cyclic = true;
            for (Definition def : notSorted) {
                boolean ok = true;
                for (Definition dependency : def.getDependencies()) {
                    boolean contains = false;
                    for (Definition def2 : notSorted) {
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
                for (Definition def : notSorted) {
                    if (def instanceof TypeInfo) {
                        if (!(((TypeInfo) def).getKind().equals("class")
                                || ((TypeInfo) def).getKind().equals("struct"))) {
                            result.add(def);
                        }
                    } else {
                        result.add(def);
                    }
                }
                for (Definition def : notSorted) {
                    if (def instanceof TypeInfo) {
                        if (((TypeInfo) def).getKind().equals("class") || ((TypeInfo) def).getKind().equals("struct")) {
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
