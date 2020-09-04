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

/**
 * 
 * @author Pedro Galvao
 *
 */
public class FunctionInfo implements Definition {
    
    String name;
    /**
     * List of numbers of arguments to use in different definitions of the same function. 
     * Currently, this field is unused.
     */
    ArrayList<Integer> numArgs;
    TypeInfo returnType;
    boolean isStatic = false;
    boolean isConst = false;
    
    public FunctionInfo(String name, TypeInfo returnType) {
        this.name = name;
        numArgs = new ArrayList<Integer>();
        numArgs.add(0);
        this.returnType = returnType;
    }
    
    public void setStatic() {
        isStatic = true;
    }
    public boolean getStatic() {
        return isStatic;
    }
    public void setConst() {
        isConst = true;
    }
    public boolean getConst() {
        return isConst;
    }

    public void setReturnType(String typeName) {
        returnType.setName(typeName);
    }
    
    public TypeInfo getReturnType() {
        return returnType;
    }
        
    public ArrayList<Integer> getNumArgs() {
        return numArgs;
    }
    public void addNumArgs(int n) {
        numArgs.add(n);
    }

    @Override
    public ArrayList<Definition> getDependencies() {
        ArrayList<Definition> result = new ArrayList<Definition>();
        result.add(returnType);
        return result;
    }

    public String getName() {
        return name;
    }
    

    @Override
    public boolean equals(Definition def) {
        return this.name.equals(def.getName());
    }

    /**
     * String with template to use before function declaration.
     * <p>
     * Currently this function is not being used, since all the functions are declared with variable number of arguments.
     */
    public String template(int numArgs) {
        String result = "template<";
        for (int i=0; i < numArgs; i++) {
            result += "class TemplateClass"+i;
            if (i + 1 < numArgs){
                result += ", ";
            }
        }
        result += ">\n";
        return result;
    }
    
    /**
     * String with the function's arguments when using templates.
     * <p>
     * Currently this function is not being used, since all the functions are declared with variable number of arguments.
     */
    public String arguments(int numArgs) {
        String result = "(";
        for (int i=0; i < numArgs; i++) {
            result += "TemplateClass" + i + " arg" + i;
            if (i + 1 < numArgs){
                result += ", ";
            }
        }
        result += ")";
        return result;
    }
    
    public String str() {
        String result = "";
        //using "..."
        if (isStatic) {
            result += "static ";
        }
        result += returnType.getName() + " " + name + "(...) ";
        if (isConst) {
            result += "const ";
        }
        result += "{ ";
        if (returnType.getKind() == "struct" || returnType.getKind() == "class") {
            result += "return "+returnType.getName() + "()";
        }
        else if (TUPatcherUtils.isPrimitiveType(returnType.getKind())) {
            result += "return 0";
        }
        else {
            String returnTypeName = returnType.getKind().replace(" &","");
            if (returnTypeName.contains("struct ")) {
                result += " " + returnTypeName.replace("*", "") + " x = {};";
            }
            else {
                result += " " + returnTypeName.replace("*", "") + " x = " + returnTypeName.replace("*", "").replace("const ", "") + "();";
                
            }
            if (returnTypeName.contains("*")) {
                result += " return &x";
            }
            else {
                result += " return x";
            }
        }
        result += ";}\n";
        return result;
        
    }

}
