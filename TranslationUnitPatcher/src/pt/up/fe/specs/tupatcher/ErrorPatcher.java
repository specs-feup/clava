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
import java.util.Map;
import java.util.function.BiConsumer;

import pt.up.fe.specs.tupatcher.parser.TUErrorData;
import pt.up.fe.specs.tupatcher.parser.TUErrorsData;
import pt.up.fe.specs.util.SpecsLogs;

/**
 * 
 * @author Pedro Galvao
 *
 */
public class ErrorPatcher {

    private final static Map<ErrorKind, BiConsumer<TUErrorData, PatchData>> ERROR_PATCHERS;
    static {
        ERROR_PATCHERS = new HashMap<>();
        
        ERROR_PATCHERS.put(ErrorKind.UNKNOWN_TYPE, ErrorPatcher::unknownType);
        ERROR_PATCHERS.put(ErrorKind.UNDECLARED_IDENTIFIER, ErrorPatcher::undeclaredIdentifier);
        ERROR_PATCHERS.put(ErrorKind.UNDECLARED_IDENTIFIER_DID_YOU_MEAN, ErrorPatcher::undeclaredIdentifier);
        ERROR_PATCHERS.put(ErrorKind.UNKNOWN_TYPE_DID_YOU_MEAN, ErrorPatcher::unknownType);
        ERROR_PATCHERS.put(ErrorKind.NOT_STRUCT_OR_UNION, ErrorPatcher::notStructOrUnion);
        ERROR_PATCHERS.put(ErrorKind.CANT_BE_REFERENCED_WITH_STRUCT, ErrorPatcher::incompleteTypeStruct);
        ERROR_PATCHERS.put(ErrorKind.NOT_A_FUNCTION_OR_FUNCTION_POINTER, ErrorPatcher::notAFunctionOrFunctionPointer);
        ERROR_PATCHERS.put(ErrorKind.NO_MATCHING_FUNCTION, ErrorPatcher::noMatchingFunction);
        ERROR_PATCHERS.put(ErrorKind.NO_MEMBER, ErrorPatcher::noMember);
        ERROR_PATCHERS.put(ErrorKind.NO_MEMBER_DID_YOU_MEAN, ErrorPatcher::noMember);
        ERROR_PATCHERS.put(ErrorKind.CANT_INITIALIZE_WITH_TYPE, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.COMPARISON_BETWEEN, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.REDEFINITION_WITH_DIFFERENT_TYPE, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.COMPARISON_OF_DISTINCT_POINTER_TYPES, ErrorPatcher::invalidOperandsToBinaryExpression);
        ERROR_PATCHERS.put(ErrorKind.INCOMPLETE_TYPE_STRUCT, ErrorPatcher::incompleteTypeStruct);
        ERROR_PATCHERS.put(ErrorKind.VARIABLE_INCOMPLETE_TYPE_STRUCT, ErrorPatcher::incompleteTypeStruct);
        ERROR_PATCHERS.put(ErrorKind.EXCESS_ELEMENTS_IN_INITIALIZER, ErrorPatcher::excessElementsInInitializer);
        ERROR_PATCHERS.put(ErrorKind.NO_MATCHING_CONSTRUCTOR, ErrorPatcher::noMatchingConstructor);
        ERROR_PATCHERS.put(ErrorKind.INCOMPATIBLE_TYPE, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.INCOMPATIBLE_OPERAND_TYPES, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.REFERENCE_TO_TYPE_COULD_NOT_BIND_TO_RVALUE, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.NO_VIABLE_CONVERSION, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.TYPE_IS_NOT_POINTER, ErrorPatcher::typeIsNotPointer);
        ERROR_PATCHERS.put(ErrorKind.INDIRECTION_REQUIRES_POINTER_OPERAND, ErrorPatcher::typeIsNotPointer);
        ERROR_PATCHERS.put(ErrorKind.CXX_REQUIRES_TYPE_SPECIFIER, ErrorPatcher::cxxRequiresTypeSpecifier);
        ERROR_PATCHERS.put(ErrorKind.NOT_CLASS_NAMESPACE_OR_ENUMERATION, ErrorPatcher::notClassNamespaceOrEnumeration);
        ERROR_PATCHERS.put(ErrorKind.INVALID_USE_OF_NON_STATIC, ErrorPatcher::invalidUseOfNonStatic);
        ERROR_PATCHERS.put(ErrorKind.NON_STATIC_MEMBER_FUNCTION, ErrorPatcher::nonStaticMemberFunction);
        ERROR_PATCHERS.put(ErrorKind.NON_CONST_CANT_BIND_TO_TEMPORARY, ErrorPatcher::nonConstCantBindToTemporary);
        ERROR_PATCHERS.put(ErrorKind.INVALID_OPERANDS_TO_BINARY_EXPRESSION, ErrorPatcher::invalidOperandsToBinaryExpression);
        ERROR_PATCHERS.put(ErrorKind.NO_VIABLE_OVERLOADED, ErrorPatcher::noViableOverloaded);
        ERROR_PATCHERS.put(ErrorKind.IS_NOT_ARRAY_POINTER_OR_VECTOR, ErrorPatcher::isNotArrayPointerOrVector);
        ERROR_PATCHERS.put(ErrorKind.NO_TYPE_NAMED_IN, ErrorPatcher::noTypeNamedIn);
        ERROR_PATCHERS.put(ErrorKind.FUNCTION_IS_NOT_MARKED_CONST, ErrorPatcher::functionIsNotMarkedConst);
        ERROR_PATCHERS.put(ErrorKind.CASE_VALUE_IS_NOT_CONST, ErrorPatcher::caseValueIsNotConst);
        
        
        
    }

    private final PatchData patchData;

    public ErrorPatcher(PatchData patchData) {
        this.patchData = patchData;
    }

    /**
     * 
     * @param errorsData
     * @return true if patchData was updated, false otherwise
     */
    public boolean patch(TUErrorsData errorsData) {
        var errors = errorsData.get(TUErrorsData.ERRORS);
        if (errors.isEmpty()) {
            SpecsLogs.info("No errors found!");
            return false;
        }

        // Try to correct only the first error
        var data = errors.get(0);

        // for (TUErrorData data : errorsData.get(TUErrorsData.ERRORS)) {
        int errorNumber = (int) data.getValue("errorNumber");

        var error = ErrorKind.getKind(errorNumber);
        
        patchData.addError(error);        

        var errorPatcher = ERROR_PATCHERS.get(error);
        if (errorPatcher == null) {
            if (error == null) {
                throw new RuntimeException("Error kind not supported yet: " + errorNumber);
            }
            else {
                throw new RuntimeException("Error kind not supported yet: " + error);                
            }
        }

        
        errorPatcher.accept(data, patchData);
        return true;

    }

    public static void unknownType(TUErrorData data, PatchData patchData) {
        String typeName = data.get(TUErrorData.MAP).get("identifier_name");
        patchData.addType(typeName);
        
    }
    
    public static void notStructOrUnion(TUErrorData data, PatchData patchData) {
        String qualType = data.get(TUErrorData.MAP).get("qualtype");
        qualType = qualType.replace(" (void)","");
        patchData.getType(qualType).setAsStruct();
        
    }
    
    public static void undeclaredIdentifier(TUErrorData data, PatchData patchData) {

        String location = data.get(TUErrorData.MAP).get("location");
        String message = data.get(TUErrorData.MAP).get("message");
        String name = TUPatcherUtils.extractFromSingleQuotes(message).get(0);
        if (TUPatcherUtils.isFunctionCall(location)) {
            patchData.addFunction(name);
        }
        else if (TUPatcherUtils.isVariable(location)) {
            patchData.addVariable(name);
        }
        else if (TUPatcherUtils.getTypeFromDeclaration(location).equals(name)) {
            patchData.addType(name);            
        }
        else if (TUPatcherUtils.isCast(location)) {
            patchData.addType(name);
        }
        else {
            patchData.addVariable(name);
        }
    }
    
    public static void noMember(TUErrorData data, PatchData patchData) {
        
        String message = data.get(TUErrorData.MAP).get("message");
        
        String field = TUPatcherUtils.getTypesFromMessage(message).get(0);

        String structOrClass = TUPatcherUtils.getTypesFromMessage(message).get(1);

        //String source = data.get(TUErrorData.MAP).get("source");
        /*if (source != null) {
            if (source.charAt(source.indexOf(field)+field.length())=='(') {
                patchData.getType(structOrClass).addFunction(field);
            }
            else {
                patchData.getType(structOrClass).addField(field);
            }
        }
        else {*/
            String location = data.get(TUErrorData.MAP).get("location");
            if (TUPatcherUtils.isFunctionCall(location)) {
                patchData.getType(structOrClass).addFunction(field, patchData);
                return;
            }
            else {
                patchData.getType(structOrClass).addField(field, patchData);
                return;
            }
        //}

    }
    
    public static void notAFunctionOrFunctionPointer(TUErrorData data, PatchData patchData) {

        String source = data.get(TUErrorData.MAP).get("source");
        String functionName = source.substring(0, source.indexOf('('));
        if (!functionName.contains(".")) {
            patchData.removeVariable(functionName);
            patchData.addFunction(functionName);
        }
        else {
            throw new RuntimeException("Can't solve this error. Don't know in which class the function is defined");
        }
        
    }
    
    public static void noMatchingFunction(TUErrorData data, PatchData patchData) {
        
        String call = data.get(TUErrorData.MAP).get("source");
                
        String functionName = call.substring(0, call.indexOf('('));
        FunctionInfo function = patchData.getFunction(functionName);
        if (function == null) {
            patchData.addFunction(functionName);
            function = patchData.getFunction(functionName);
        }        
        
        int numArgs = TUPatcherUtils.getTypesFromMessage(call).get(0).split(",").length;
        function.addNumArgs(numArgs);
    }
    
    /**
     * Increase number of fields in a struct to match the number of elements in the initializer.
     */
    public static void excessElementsInInitializer(TUErrorData data, PatchData patchData) {

        //int sint = Integer.parseInt(data.get(TUErrorData.MAP).get("sint"));
        String location = data.get(TUErrorData.MAP).get("location");
        String typeName = TUPatcherUtils.getTypeFromStructDeclaration(location);
        TypeInfo type = patchData.getType(typeName);
        if (type != null) {
            type.incNumFields(patchData);
        }
        else {
            typeName = TUPatcherUtils.getTypeFromDeclaration(location);
            type = patchData.getType(typeName);
            if (type != null) {
                type.incNumFields(patchData);
            }
            
        }
        
    }
    
    public static void noMatchingConstructor(TUErrorData data, PatchData patchData) {

//        String call = data.get(TUErrorData.MAP).get("source");      
        //int numArgs = TUPatcherUtils.extractFromParenthesis(call).get(0).split(",").length;
        String typeName = data.get(TUErrorData.MAP).get("qualtype");
        TypeInfo type = patchData.getType(typeName);
        type.setAsClass();
/*
        String message = data.get(TUErrorData.MAP).get("message");
        String typeName = message.substring(message.indexOf('\'')+1, message.indexOf('\'',message.indexOf('\'')+1));
       
        type.addConstructor(numArgs);*/
    }

    /**
     * This function is used to solve many different errors. 
     * <p>
     * All these errors are caused by incompatibility between two types, meaning that one of them must be modified. 
     */
    public static void incompatibleType(TUErrorData data, PatchData patchData) {
        
        String message = data.get(TUErrorData.MAP).get("message");
        String fromTypeName = data.get(TUErrorData.MAP).get("qualtype");
        ArrayList<String> types = TUPatcherUtils.getTypesFromMessage(message);
        ArrayList<String> akas = TUPatcherUtils.getAkaFromMessage(message);
        String aka1 = akas.get(0);
        String aka2 = akas.get(1);
        String toTypeName = types.get(0);
        if (message.indexOf("from") < message.indexOf("to")) {
            fromTypeName = types.get(0).replace(" &","");
            toTypeName = types.get(1).replace(" &","");
            aka1 = akas.get(1);
            aka2 = akas.get(0);
        }
        fromTypeName = TUPatcherUtils.removeBracketsFromType(fromTypeName);
        toTypeName = TUPatcherUtils.removeBracketsFromType(toTypeName);
        TypeInfo fromType;
        TypeInfo toType;
        fromType = patchData.getType(TUPatcherUtils.getTypeName(fromTypeName));
        toType = patchData.getType(TUPatcherUtils.getTypeName(toTypeName));
        
        if (toType != null && fromType != null) {
            if (toTypeName.contains("*") && !fromTypeName.contains("*")) {
                fromType.setAs(toTypeName);
            }
            else if (!toTypeName.contains("*") && fromTypeName.contains("*")) {
                toType.setAs(fromTypeName);                
            }
            else if (toType.getKind().equals("int")) {
                if (aka2.equals("")) {
                    toType.setAs(fromTypeName);
                }
                else {
                    toType.setAs(aka2);
                }
            }
            else if ( fromType.getKind().equals("int")){
                if (aka1.equals("")) {
                    fromType.setAs(toTypeName);                    
                }
                else {
                    fromType.setAs(aka1);
                }
            }
            else {
                throw new RuntimeException("No solution to this error. Don't know which type should be changed");                
            }
        }
        else if (toType != null) {
            if (fromTypeName.contains("*") && toTypeName.contains("*")) {
                fromTypeName = fromTypeName.replace("*", "");
            }
            if (aka2.equals("")) {
                toType.setAs(fromTypeName);                    
            }
            else {
                toType.setAs(aka2);
            }
        }
        else {
            if (fromTypeName.contains("*") && toTypeName.contains("*")) {
                toTypeName = toTypeName.replace("*", "");
            }
            if (aka1.equals("")) {
                fromType.setAs(toTypeName);                 
            }
            else {
                fromType.setAs(aka1);
            }
        }
    }

    public static void incompleteTypeStruct(TUErrorData data, PatchData patchData) {
        String typeName = data.get(TUErrorData.MAP).get("qualtype");
        if (typeName == null) {
            String message = data.get(TUErrorData.MAP).get("message");
            typeName = TUPatcherUtils.extractFromSingleQuotes(message).get(0);
        }
        typeName = typeName.replace("struct ", "");
        if (typeName.contains("class ")) {
            throw new RuntimeException("Unable to solve incomplete type error for "+typeName);
        }
        TypeInfo type = new TypeInfo(typeName);
        type.setAsStructWithoutTipedef();
        patchData.addType(type);
    }
    public static void typeIsNotPointer(TUErrorData data, PatchData patchData) {
        String typeName = data.get(TUErrorData.MAP).get("qualtype");
        TypeInfo type = new TypeInfo();
        type.setAsStruct();
        patchData.addType(typeName);
        patchData.getType(typeName).setAs(type.getName()+" *");
    }

    public static void cxxRequiresTypeSpecifier(TUErrorData data, PatchData patchData) {
        //sometimes this error is caused by a constructor being called with undeclared variables.
        //the problem is solved declaring these variables
        String location = data.get(TUErrorData.MAP).get("location");
        String typeName = TUPatcherUtils.getTypeFromDeclaration(location);
        ArrayList<String> args = TUPatcherUtils.getArguments(location);
        TypeInfo type = patchData.getType(typeName);
        for (String arg : args) {
            if (patchData.getType(arg)==null && patchData.getVariable(arg)==null && patchData.getFunction(arg)==null) {
                patchData.addVariable(arg);
            }
        }
        if (type!= null) {
            type.setAsClass();
        }
    
    }

    /**
     *  Transform the type into a class. 
     */
    public static void notClassNamespaceOrEnumeration(TUErrorData data, PatchData patchData) {
        String typeName = data.get(TUErrorData.MAP).get("identifier_name");
        if (typeName == null) {
            typeName = data.get(TUErrorData.MAP).get("qualtype");
        }
        TypeInfo type = patchData.getType(typeName);
        if (type == null) {
            patchData.addType(typeName);
            patchData.getType(typeName).setAsClass();
        }
        else {
            type.setAsClass();
        }
    }

    /**
     *  Make a variable static. 
     */
    public static void invalidUseOfNonStatic(TUErrorData data, PatchData patchData) {
        String message = data.get(TUErrorData.MAP).get("message");
        String source = data.get(TUErrorData.MAP).get("source");
        String identifier = TUPatcherUtils.extractFromSingleQuotes(message).get(0);
        String className = source.split("::")[0];
        TypeInfo type = patchData.getType(className);
        type.setStatic(identifier);
    }

    /**
     *  Make a function static. 
     */
    public static void nonStaticMemberFunction(TUErrorData data, PatchData patchData) {
        String location = data.get(TUErrorData.MAP).get("location");
        String functionName = TUPatcherUtils.getTokenFromLocation(location);
        String className = TUPatcherUtils.getTokenBeforeLocation(location);
        TypeInfo type = patchData.getType(className);
        type.setStatic(functionName);
    }

    /**
     *  . 
     */
    public static void nonConstCantBindToTemporary(TUErrorData data, PatchData patchData) {
        String typeName = data.get(TUErrorData.MAP).get("qualtype");
        String message = data.get(TUErrorData.MAP).get("message");
        String typeName2 = TUPatcherUtils.extractFromSingleQuotes(message).get(0);
        patchData.getType(typeName).setAs(typeName2+" &");
        patchData.getType(typeName2).setAsClass();
        
    }

    /**
     *  Define a binary operator for a class. 
     */
    public static void invalidOperandsToBinaryExpression(TUErrorData data, PatchData patchData) {
        String source = data.get(TUErrorData.MAP).get("source");
        String operator = TUPatcherUtils.extractOperator(source);
        String typeName = data.get(TUErrorData.MAP).get("qualtype");
        if (typeName.contains("class ")) {
            TypeInfo type = patchData.getType(TUPatcherUtils.getTypeName(typeName));
            type.addOperator(operator);            
        }
        else {
            TypeInfo type = patchData.getType(TUPatcherUtils.getTypeName(typeName));
            if (type != null) {
                type.setAsClass();
                type.addOperator(operator);
            }
            else {
                typeName = TUPatcherUtils.getTypesFromMessage(data.get(TUErrorData.MAP).get("message")).get(0);
                type = patchData.getType(TUPatcherUtils.getTypeName(typeName));
                if (type != null) {
                    type.setAsClass();
                    type.addOperator(operator);
                }
                else {
                    typeName = TUPatcherUtils.getTypesFromMessage(data.get(TUErrorData.MAP).get("message")).get(1);
                    type = patchData.getType(TUPatcherUtils.getTypeName(typeName));
                    type.setAsClass();
                    type.addOperator(operator);
                }
            }
        }
    }

    /**
     *  Define an operator for a class. 
     *  <p>
     *  The error message does not indicate which class needs the operator, so it is added to all of them.
     */
    public static void noViableOverloaded(TUErrorData data, PatchData patchData) {
        String operator = data.get(TUErrorData.MAP).get("string");
        for (TypeInfo type : patchData.getTypes().values()) {
            if (type.getKind().equals("class")) {
                type.addOperator(operator);
            }
        }
    }
    
    public static void isNotArrayPointerOrVector(TUErrorData data, PatchData patchData) {
        String location = data.get(TUErrorData.MAP).get("location");
        String variableName = TUPatcherUtils.getTokenBeforeLocation(location);
        variableName = TUPatcherUtils.removeBracketsFromType(variableName).replace("*","");
        TypeInfo type = patchData.getVariable(variableName);
        if (type != null) {
            TypeInfo type2 = new TypeInfo();
            patchData.addType(type2);
            type.setAs(type2.getName()+" *");
        }
        else {
            TypeInfo fieldType = null;
            for (TypeInfo type2 : patchData.getTypes().values()) {
                for (String fieldName : type2.getFields().keySet()) {
                    if (fieldName.equals(variableName)) {
                        fieldType = type2.getFields().get(fieldName);
                        break;
                    }
                }
            }
            if (fieldType != null) {
                TypeInfo type3 = new TypeInfo();
                patchData.addType(type3);
                fieldType.setAs(type3.getName()+" *");
            }
            else {
                throw new RuntimeException("Variable is not pointer array or vector. Cannot identify type of the variable");
            }
        }
    }

    /**
     * Define a nested type in a class.
     */
    public static void noTypeNamedIn(TUErrorData data, PatchData patchData) {
        String typeName = data.get(TUErrorData.MAP).get("identifier_name");
        String message = data.get(TUErrorData.MAP).get("message");
        String className = TUPatcherUtils.getTypesFromMessage(message).get(1);
        TypeInfo classInfo = patchData.getType(className);
        classInfo.addNestedType(typeName, patchData);
    }

    /**
     *  Set the return type of a function as const. 
     */
    public static void functionIsNotMarkedConst(TUErrorData data, PatchData patchData) {
        String message = data.get(TUErrorData.MAP).get("message");
        String functionName = TUPatcherUtils.extractFromSingleQuotes(message).get(1);
        FunctionInfo function = patchData.getFunction(functionName);
        if (function == null) {
            for (TypeInfo type : patchData.getTypes().values()) {
                function = type.getFunctions().get(functionName);
                if (function!=null) {
                    break;
                }
            }
        }
        TypeInfo returnType = function.getReturnType();
        returnType.setAs("const " + returnType.getKind().replace("const ", ""));
        function.setConst();
    }

    /**
     *  Set a variable as const.
     *  <p>
     *  Still not working for variables in nested types.
     */
    public static void caseValueIsNotConst(TUErrorData data, PatchData patchData) {
        String completeName = data.get(TUErrorData.MAP).get("source");
        if (!completeName.contains("::")) {
            String varName = completeName.replace(":", "");
            patchData.removeVariable(varName);
            patchData.addConstVariable(varName);
        }
        else {
            //variable in a nested type
            throw new RuntimeException("There is no patch for const variables in nested types.");
        }
        
    }
}
