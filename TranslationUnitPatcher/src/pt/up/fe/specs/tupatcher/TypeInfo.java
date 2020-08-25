/**
 *  Copyright 2020 SPeCS.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package pt.up.fe.specs.tupatcher;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * 
 * @author Pedro Galvao
 *
 */
public class TypeInfo implements Definition {

    String kind;//int, char, struct, union, class, etc
    String name;
    HashMap<String, TypeInfo> fields = new HashMap<String, TypeInfo>();
    HashMap<String, FunctionInfo> functions = new HashMap<String, FunctionInfo>();
    ArrayList<Integer> constructors = new ArrayList<>();
    boolean useTypedefStruct = true;
    HashMap<String, Boolean> isStatic = new HashMap<String, Boolean>();
    
    static int counter = 0;
    
    public TypeInfo() {
        kind = "int";
        this.name = "TYPE_PATCH_"+counter;
        counter++;
    }
    public void setStatic(String field) {
        isStatic.put(field, true);
    }
    public boolean getStatic(String field) {
        return isStatic.get(field);
    }
    public boolean getTypedefStruct() {
        return useTypedefStruct;
    }
    
    public TypeInfo(String name) {
        kind = "int";
        this.name = name;
    }

    public void addField(String name, PatchData patchData) {
        if (!(kind=="struct" || kind=="union" || kind=="class")) kind = "struct";
        TypeInfo type = new TypeInfo("TYPE_PATCH_"+counter);
        fields.put(name, type);
        isStatic.put(name, false);
        patchData.addType(type);                
        counter++;
    }
    public void addField(String name, TypeInfo type, PatchData patchData) {
        if (!(kind=="struct" || kind=="union" || kind=="class")) kind = "struct";
        fields.put(name, type);
        isStatic.put(name, false);
        patchData.addType(type);
    }
    
    public HashMap<String, TypeInfo> getFields() {
        return fields;
    }    
    
    public String getKind(){
        return kind;
    }
    
    public void setAsStruct() {
        kind = "struct";        
    }    
    public void setAsStructWithoutTipedef() {
        kind = "struct";
        useTypedefStruct = false;
    }
    public void setAsClass() {
        kind = "class";        
    }
    
    
    public void setAs(String kind) {
        this.kind = kind;        
    }
    
    public void incNumFields(PatchData patchData) {
        addField("field"+counter, new TypeInfo("TYPE_PATCH_"+counter), patchData);
        counter++;
    }
    
    public void setName(String name){
        this.name = name;
    }
        
    public String getName(){
        return name;
    }
    
    public void addFunction(String name, PatchData patchData) {
        if (! (kind == "class")) kind = "class";
        TypeInfo returnType = new TypeInfo();
        functions.put(name, new FunctionInfo(name, returnType));
        isStatic.put(name, false);
        patchData.addType(returnType);
    }
    public HashMap<String, FunctionInfo> getFunctions(){
        return functions;
    }
    
    public void addConstructor(int numArgs) {
        if (! (kind == "class")) kind = "class";
        constructors.add(numArgs);        
    }
    public void setMemberAsPointer(String member, PatchData patchData) {
       fields.remove(member, patchData);
       addField(member+" *",  patchData);
    }
    
    
    public ArrayList<Definition> getDependencies(){
        ArrayList<Definition> result = new ArrayList<Definition>();
        String kind2 = kind.split(" ")[0];
        if (!(TUPatcherUtils.getPrimitiveTypes().contains(kind2))) {
            result.add(new TypeInfo(kind2));
        }
        for (Definition def : fields.values()) {
            result.add(def);
        }
        for (FunctionInfo def : functions.values()) {
            result.add(def.getReturnType());
            result.add(def);
        }
        
        return result;
    }
    
    @Override
    public boolean equals(Definition def) {
        return this.name.equals(def.getName());
    }
    
    public String str() {
        String result = "";
        if (kind == "struct") {
            if (useTypedefStruct) {
                result += "typedef struct {\n";
            }
            else {
                result += "struct " + name + " {\n";
            }
            for (String field : fields.keySet()) {
                result += "\t" + fields.get(field).getName() + " ";
                result += field + ";\n";
            }
            result += "} ";
            if (useTypedefStruct) {
                result += name;
            }
            result += ";\n";
        }
        else if (kind == "class") {

            result += "class " + name + "{\npublic:\n";
            for (String field :fields.keySet()) {
                result += "\t";
                if (isStatic.get(field)) {
                    result += "static ";
                }
                result += fields.get(field).getName() + " ";
                result += field + ";\n";
            }
            for (FunctionInfo function: functions.values()) {
                result += "\t";
                if (isStatic.get(function.getName())) {
                    result += "static ";
                }
                result += function.str();
            }
            //constructor using "..."
            result += "\t" + name + "(...) {}\n";
            /*using templates
            for (int numArgs : constructors) {
                FunctionInfo function = new FunctionInfo(name);
                function.setReturnType("");
                result += function.template(numArgs);
                result += name;
                result += function.arguments(numArgs);
                result += "{}\n";
            }*/
            result += "};" + "\n";
        }
        else {
            result += "typedef " + kind + " " + name + ";\n";                    
        }
        return result;
    }
    
}
