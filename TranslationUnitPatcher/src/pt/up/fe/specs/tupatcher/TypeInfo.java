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

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * @author Pedro Galvao
 *
 */
public class TypeInfo implements Definition {

    String kind;// int, char, struct, union, class, etc
    String name;
    HashMap<String, TypeInfo> fields = new HashMap<String, TypeInfo>();
    HashMap<String, TypeInfo> nestedTypes = new HashMap<String, TypeInfo>();
    HashMap<String, FunctionInfo> functions = new HashMap<String, FunctionInfo>();
    ArrayList<Integer> constructors = new ArrayList<>();
    boolean useTypedefStruct = true;
    HashMap<String, Boolean> isStatic = new HashMap<String, Boolean>();
    ArrayList<String> operators = new ArrayList<>();
    boolean nested = false;

    static int counter = 0;

    public TypeInfo() {
        kind = "int";
        this.name = "TYPE_PATCH_" + counter;
        counter++;
    }

    public void addOperator(String operator) {
        if (!operators.contains(operator)) {
            operators.add(operator);
        }
    }

    public void setNested() {
        nested = true;
    }

    public boolean isNested() {
        return nested;
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
        if (!(kind.equals("struct") || kind.equals("union") || kind.equals("class")))
            kind = "struct";
        TypeInfo type = new TypeInfo("TYPE_PATCH_" + counter);
        fields.put(name, type);
        isStatic.put(name, false);
        patchData.addType(type);
        counter++;
    }

    public void addNestedType(String name, PatchData patchData) {
        if (!(kind.equals("struct") || kind.equals("union") || kind.equals("class")))
            kind = "class";
        TypeInfo type = new TypeInfo(name);
        nestedTypes.put(name, type);
        isStatic.put(name, false);
        patchData.addType(type);
    }

    public TypeInfo getNestedType(String name) {
        return nestedTypes.get(name);
    }

    public void addField(String name, TypeInfo type, PatchData patchData) {
        if (!(kind.equals("struct") || kind.equals("union") || kind.equals("class")))
            kind = "struct";
        fields.put(name, type);
        isStatic.put(name, false);
        patchData.addType(type);
    }

    public HashMap<String, TypeInfo> getFields() {
        return fields;
    }

    public String getKind() {
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
        addField("field" + counter, new TypeInfo("TYPE_PATCH_" + counter), patchData);
        counter++;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void addFunction(String name, PatchData patchData) {
        if (!(kind.equals("class")))
            kind = "class";
        TypeInfo returnType = new TypeInfo();
        functions.put(name, new FunctionInfo(name, returnType));
        isStatic.put(name, false);
        patchData.addType(returnType);
    }

    public HashMap<String, FunctionInfo> getFunctions() {
        return functions;
    }

    public void addConstructor(int numArgs) {
        if (!(kind.equals("class")))
            kind = "class";
        constructors.add(numArgs);
    }

    public void setMemberAsPointer(String member, PatchData patchData) {
        fields.remove(member, patchData);
        addField(member + " *", patchData);
    }

    /**
     * List of all the types and functions that must be defined/declared before this one.
     * <p>
     * It includes the types of all the fields, return types of the functions and dependencies of nested types. When the
     * type is not a struct or class it has only one or zero dependencies.
     * 
     * @return List of the types and functions.
     */
    @Override
    public ArrayList<Definition> getDependencies() {
        ArrayList<Definition> result = new ArrayList<Definition>();
        String kind2 = TUPatcherUtils.getTypeName(kind);
        if (!TUPatcherUtils.isPrimitiveType(kind2)) {
            result.add(new TypeInfo(kind2));
        }
        for (Definition def : fields.values()) {
            result.add(def);
        }
        for (FunctionInfo def : functions.values()) {
            result.add(def.getReturnType());
            result.add(def);
        }
        for (TypeInfo type : nestedTypes.values()) {
            for (Definition def : type.getDependencies()) {
                result.add(def);
            }
        }

        return result;
    }

    @Override
    public boolean equals(Definition def) {
        return this.name.equals(def.getName());
    }

    @Override
    public String str() {
        StringBuilder result = new StringBuilder();
        if (kind.equals("struct")) {
            if (useTypedefStruct) {
                result.append("typedef struct {\n");
            } else {
                result.append("struct ").append(name).append(" {\n");
            }
            for (String field : fields.keySet()) {
                result.append("\t").append(fields.get(field).getName()).append(" ");
                result.append(field).append(";\n");
            }
            result.append("} ");
            if (useTypedefStruct) {
                result.append(name);
            }
            result.append(";\n");
        } else if (kind.equals("class")) {

            result.append("class ").append(name).append("{\npublic:\n");
            for (String field : fields.keySet()) {
                result.append("\t");
                if (isStatic.get(field)) {
                    result.append("static ");
                }
                result.append(fields.get(field).getName()).append(" ");
                result.append(field).append(";\n");
            }
            for (FunctionInfo function : functions.values()) {
                result.append("\t");
                if (isStatic.get(function.getName())) {
                    result.append("static ");
                }
                result.append(function.str());
            }
            for (String operator : operators) {
                result.append("\ttemplate <class T>\n");
                result.append("\tfriend bool operator").append(operator).append("(const ").append(name).append("& t1, T& t2) { return 0;}\n");
            }
            for (TypeInfo type : nestedTypes.values()) {
                result.append(type.str());
            }
            // constructor
            result.append("\t").append(name).append("(...) {}\n");
            result.append("};" + "\n");
        } else {
            result.append("typedef ").append(kind).append(" ").append(name).append(";\n");
        }
        return result.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "TypeInfo [kind=" + kind + ", name=" + name + ", fields=" + fields + ", nestedTypes=" + nestedTypes
                + ", functions=" + functions + ", constructors=" + constructors + ", useTypedefStruct="
                + useTypedefStruct + ", isStatic=" + isStatic + ", operators=" + operators + ", nested=" + nested + "]";
    }

}
