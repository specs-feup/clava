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
import java.util.List;

/**
 * 
 * @author Pedro Galvao
 *
 */
public class FunctionInfo implements Definition {

    final String name;
    /**
     * List of numbers of arguments to use in different definitions of the same function. Currently, this field is
     * unused.
     */
    final List<Integer> numArgs;
    final TypeInfo returnType;
    boolean isStatic = false;
    boolean isConst = false;

    public FunctionInfo(String name, TypeInfo returnType) {
        this.name = name;
        this.numArgs = new ArrayList<>();
        this.numArgs.add(0);
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

    public List<Integer> getNumArgs() {
        return numArgs;
    }

    public void addNumArgs(int n) {
        numArgs.add(n);
    }

    @Override
    public List<Definition> getDependencies() {
        List<Definition> result = new ArrayList<>();
        result.add(returnType);
        return result;
    }

    @Override
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
     * Currently this function is not being used, since all the functions are declared with variable number of
     * arguments.
     */
    public String template(int numArgs) {
        StringBuilder result = new StringBuilder("template<");
        for (int i = 0; i < numArgs; i++) {
            result.append("class TemplateClass").append(i);
            if (i + 1 < numArgs) {
                result.append(", ");
            }
        }
        result.append(">\n");
        return result.toString();
    }

    /**
     * String with the function's arguments when using templates.
     * <p>
     * Currently this function is not being used, since all the functions are declared with variable number of
     * arguments.
     */
    public String arguments(int numArgs) {
        StringBuilder result = new StringBuilder("(");
        for (int i = 0; i < numArgs; i++) {
            result.append("TemplateClass").append(i).append(" arg").append(i);
            if (i + 1 < numArgs) {
                result.append(", ");
            }
        }
        result.append(")");
        return result.toString();
    }

    @Override
    public String str() {
        String result = "";
        // using "..."
        if (isStatic) {
            result += "static ";
        }
        result += returnType.getName() + " " + name + "(...) ";
        if (isConst) {
            result += "const ";
        }
        result += "{ ";

        result += getFunctionBody();

        result += ";}\n";

        return result;

    }

    private String getFunctionBody() {

        if (returnType.getKind().equals("struct") || returnType.getKind().equals("class")) {
            return "return " + returnType.getName() + "()";
        }

        if (TUPatcherUtils.isPrimitiveType(returnType.getKind())) {
            return "return 0";
        }

        var result = new StringBuilder();

        String returnTypeName = returnType.getKind().replace(" &", "");

        // If return type is a pointer, just return 0
        if (returnTypeName.contains("*")) {
            return "return 0";
        }

        /*
        if (returnTypeName.contains("struct ")) {
            result.append(" " + returnTypeName.replace("*", "") + " x = {};");
        } else {
            result.append(" " + returnTypeName.replace("*", "") + " x = "
                    + returnTypeName.replace("*", "").replace("const ", "") + "();");
        
        }
        */

        if (returnTypeName.contains("struct ")) {
            result.append(" ").append(returnTypeName).append(" x = {};");
        } else {
            result.append(" ").append(returnTypeName).append(" x = ").append(returnTypeName.replace("const ", "")).append("();");

        }

        /*
        if (returnTypeName.contains("*")) {
            result.append(" return &x");
        } else {
            result.append(" return x");
        }
        */

        result.append(" return x");

        return result.toString();
    }

}
