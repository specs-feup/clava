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

public class FunctionInfo implements Definition {
    
    String name;
    ArrayList<Integer> numArgs;
    TypeInfo returnType;
    
    public FunctionInfo(String name, TypeInfo returnType) {
        this.name = name;
        numArgs = new ArrayList<Integer>();
        numArgs.add(0);
        this.returnType = returnType;
        
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
        result += returnType.getName() + " " + name + "(...) { return ";
        if (returnType.getKind() == "struct" || returnType.getKind() == "class") {
            result += returnType.getName() + "()";
        }
        else {
            result += "0";
        }
        result += ";}\n";
        /*using templates
         for (Integer numArgs : this.numArgs) {   
            if (numArgs > 0) {
                result += template(numArgs);
                result += returnType.getName() + " " + name;
                result += arguments(numArgs);
                result += " {";
                if (returnType.getKind() == "int") {
                    result += "return 0;";
                }
                result += "}\n";
            }
            else {
                result += returnType.getName() + " " + name + "() {";
                if (returnType.getKind() == "int") {
                    result += "return 0;";
                }
                result += "}\n";
            }
        }*/
        return result;
        
    }

}
