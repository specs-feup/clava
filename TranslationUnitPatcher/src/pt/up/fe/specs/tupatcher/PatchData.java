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
import java.util.HashMap;

import pt.up.fe.specs.util.SpecsIo;

public class PatchData {

    private final HashMap<String, TypeInfo> missingTypes;
    private final HashMap<String, FunctionInfo> missingFunctions;
    private final HashMap<String, String> missingVariables;

    public PatchData() {
        this.missingTypes = new HashMap<String, TypeInfo>();
        this.missingFunctions = new HashMap<String, FunctionInfo>();
        this.missingVariables = new HashMap<String, String>();
    }

    public void addType(String typeName) {
        if (typeName != null) {
            missingTypes.put(typeName, new TypeInfo());
        }
    }

    public void addVariable(String varName) {
        if (varName != null) {
            missingVariables.put(varName, "int");
        }
    }    
    public FunctionInfo getFunction(String functionName) {
        return missingFunctions.get(functionName);
    }
    public void addFunction(String functionName) {
        missingFunctions.put(functionName, new FunctionInfo(functionName));
    }

    public void removeVariable(String varName) {
        missingVariables.remove(varName);
    }
    public TypeInfo getType(String typeName) {
        return missingTypes.get(typeName);
    }

     public void setType(String typeName, TypeInfo type) {
         //missingTypes.remove(typeName);
         missingTypes.put(typeName, type);
     }

    public void copySource(String filepath) {
        // copy source file adding #include "patch.h" at the top of it
        var result = "#include \"patch.h\"\n" + SpecsIo.read(SpecsIo.existingFile(filepath));
        File destFile = new File("output/file.cpp");
        SpecsIo.write(destFile, result);
    }
    
    public String variablesPatches() {
        String result = "";
        for (String varName : missingVariables.keySet()) {
            String type = missingVariables.get(varName);
            result += type + " " + varName + ";\n";
        }
        return result;
    }
    public String typePatches() {
        String result = "";               
        for (String typeName : missingTypes.keySet()) {
            TypeInfo type = missingTypes.get(typeName);
            String kind = type.getKind();
            if (kind == "struct") {
                result += "typedef struct {\n";
                for (String field : type.getFields().keySet()) {
                result += "\t" + type.getFields().get(field).getKind() + " ";
                    result += field + ";\n";
                }
                result += "} " + typeName + ";\n";
            }
            else if (kind == "class") {

                result += "class " + typeName + "{\npublic:\n";
                for (String field : type.getFields().keySet()) {
                    result += "\t" + type.getFields().get(field).getKind() + " ";
                    result += field + ";\n";
                }
                result += functionPatches(type.getFunctions());
                
                result += "};" + "\n";
            }
            else {
                result += "typedef " + kind + " " + typeName + ";\n";                    
            }
        }
        return result;
    }
    
    public String functionPatches(HashMap<String, FunctionInfo> functions) {

        String result = "";
        for (String functionName : functions.keySet()) {
            FunctionInfo function = functions.get(functionName);
            String returnType = function.getReturnType().getName();
            int numArgs = function.getNumArgs();
            if (numArgs > 0) {
                result += "template<";
                for (int i=0; i < numArgs; i++) {
                    result += "class TemplateClass"+i;
                    if (i + 1 < numArgs){
                        result += ", ";
                    }
                }
                result += ">\n";
                result += returnType + " " + functionName + "(";
                for (int i=0; i < numArgs; i++) {
                    result += "TemplateClass" + i + " arg" + i;
                    if (i + 1 < numArgs){
                        result += ", ";
                    }
                }
                result += ") {";
                if (returnType == "int") {
                    result += "return 0;";
                }
                result += "}\n";
            }
            else {
                result += returnType + " " + functionName + "() {";
                if (returnType == "int") {
                    result += "return 0;";
                }
                result += "}\n";
            }
        }
        return result;
    }

    public String str() {
        String result = "";
        result += typePatches();
        result += variablesPatches();
        result += functionPatches(missingFunctions);
        return result;
    }
        
    public void write(String filepath) {
        File patchFile = new File("output/patch.h");
        SpecsIo.write(patchFile, str());
        copySource(filepath);

    }

}
