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
    private ArrayList<ErrorKind> errors; 

    public PatchData() {
        this.missingTypes = new HashMap<String, TypeInfo>();
        this.missingFunctions = new HashMap<String, FunctionInfo>();
        this.missingVariables = new HashMap<String, TypeInfo>();
        this.errors = new ArrayList<ErrorKind>(); 
    }
    
    public void addError(ErrorKind errorKind) {
        errors.add(errorKind);
    }
    
    public ArrayList<ErrorKind> getErrors() {
        return errors;
    }

    public void addType(String typeName) {
        if (typeName != null) {
            missingTypes.put(typeName, new TypeInfo(typeName));
        }
    }
    public void addType(TypeInfo type) {
        missingTypes.put(type.getName(), type);
    }

    public void addVariable(String varName) {
        if (varName != null) {
            TypeInfo type = new TypeInfo();
            missingVariables.put(varName, type);
            missingTypes.put(type.getName(), type);
        }
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
            String type = missingVariables.get(varName).getName();
            result += type + " " + varName + ";\n";
        }
        return result;
    }
    

    
    public String functionPatches(HashMap<String, FunctionInfo> functions) {
        String result = "";
        for (String functionName : functions.keySet()) {
            FunctionInfo function = functions.get(functionName);
            result += function.str();
        }
        return result;
    }

    public String str() {
        String result = "";
   /*     result += typePatches();
        result += variablesPatches();
        result += functionPatches(missingFunctions);*/
        
        ArrayList<Definition> defs = orderedDefinitions();
        for (Definition def : defs) {
            result += def.str();
        }
        result += variablesPatches();
        
        return result;
    }
        
    public void write(String filepath) {
        File patchFile = new File("output/patch.h");
        SpecsIo.write(patchFile, str());
        copySource(filepath);

    }
    
    public ArrayList<Definition> orderedDefinitions(){
        ArrayList<Definition> notSorted = new ArrayList<Definition>();
        ArrayList<Definition> result = new ArrayList<Definition>();
        
        for (TypeInfo type : missingTypes.values()) {
            if (type.getKind().equals("struct") && type.getKind().equals("class")) {
                result.add(type);
            }
            else {
                notSorted.add(type);                
            }
        }        
        for (FunctionInfo function : missingFunctions.values()) {
            notSorted.add(function);
        }
        while (! notSorted.isEmpty()) {
            for (Definition def : notSorted) {
                boolean ok = true;
                for (Definition dependency : def.getDependencies()) {
                    boolean contains = false;
                    for (Definition def2: notSorted) {
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
                    break;
                }
            }
        }
        return result;
    }

}
