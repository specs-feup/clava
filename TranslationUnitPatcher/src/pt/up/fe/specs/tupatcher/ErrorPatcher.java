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
        ERROR_PATCHERS.put(ErrorKind.TOO_MANY_ARGUMENTS, ErrorPatcher::tooManyArguments);
        ERROR_PATCHERS.put(ErrorKind.NO_MEMBER, ErrorPatcher::noMember);
        ERROR_PATCHERS.put(ErrorKind.NO_MEMBER_DID_YOU_MEAN, ErrorPatcher::noMember);
        ERROR_PATCHERS.put(ErrorKind.CANT_INITIALIZE_WITH_TYPE, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.COMPARISON_BETWEEN, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.REDEFINITION_WITH_DIFFERENT_TYPE, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.COMPARISON_OF_DISTINCT_POINTER_TYPES, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.INCOMPLETE_TYPE_STRUCT, ErrorPatcher::incompleteTypeStruct);
        ERROR_PATCHERS.put(ErrorKind.VARIABLE_INCOMPLETE_TYPE_STRUCT, ErrorPatcher::incompleteTypeStruct);
        ERROR_PATCHERS.put(ErrorKind.EXCESS_ELEMENTS_IN_INITIALIZER, ErrorPatcher::excessElementsInInitializer);
        ERROR_PATCHERS.put(ErrorKind.NO_MATCHING_CONSTRUCTOR, ErrorPatcher::noMatchingConstructor);
        ERROR_PATCHERS.put(ErrorKind.INCOMPATIBLE_TYPE, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.NO_VIABLE_CONVERSION, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.TYPE_IS_NOT_POINTER, ErrorPatcher::typeIsNotPointer);
        ERROR_PATCHERS.put(ErrorKind.CXX_REQUIRES_TYPE_SPECIFIER, ErrorPatcher::cxxRequiresTypeSpecifier);
        ERROR_PATCHERS.put(ErrorKind.NOT_CLASS_NAMESPACE_OR_ENUMERATION, ErrorPatcher::notClassNamespaceOrEnumeration);
        ERROR_PATCHERS.put(ErrorKind.INVALID_USE_OF_NON_STATIC, ErrorPatcher::invalidUseOfNonStatic);
        ERROR_PATCHERS.put(ErrorKind.NON_STATIC_MEMBER_FUNCTION, ErrorPatcher::nonStaticMemberFunction);
        ERROR_PATCHERS.put(ErrorKind.NON_CONST_CANT_BIND_TO_TEMPORARY, ErrorPatcher::nonConstCantBindToTemporary);
        
        
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
        patchData.getType(qualType).setAsStruct();
        
    }
    
    public static void undeclaredIdentifier(TUErrorData data, PatchData patchData) {

        String location = data.get(TUErrorData.MAP).get("location");
        String message = data.get(TUErrorData.MAP).get("message");
        String name = TUPatcherUtils.extractFromSingleQuotes(message).get(0);
        if (TUPatcherUtils.isFunctionCall(location)) {
            patchData.addFunction(name);            
        }
        else if (TUPatcherUtils.getTypeFromDeclaration(location).equals(name)) {
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
    
    public static void tooManyArguments(TUErrorData data, PatchData patchData) {
        //int numArgs = Integer.parseInt(data.get(TUErrorData.MAP).get("uint"));
    //    function.addNumArgs(numArgs);
    }
    
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
    /*public static void cantInitializeMemberWithType(TUErrorData data, PatchData patchData) {

        String qualType = data.get(TUErrorData.MAP).get("qualtype");
        String message = data.get(TUErrorData.MAP).get("message");

        ArrayList<String> types = TUPatcherUtils.getTypesFromMessage(message);
        TypeInfo toType = patchData.getType(types.get(0));
        TypeInfo fromType = patchData.getType(types.get(1));
        
        
        if (qualType.contains("[")) {
            String fix = qualType.substring(0, qualType.indexOf('['));
            fix += "*";
            qualType = fix;
        }
        
        patchData.getType(typeName).setAs(qualType);
        
    }*/
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
        /*System.out.println("from q: "+fromTypeName);
        System.out.println("to q: "+toTypeName);
        System.out.println("from: "+TUPatcherUtils.getTypeName(fromTypeName));
        System.out.println("to: "+TUPatcherUtils.getTypeName(toTypeName));*/
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
                /*System.out.println("to int");
                System.out.println("aka2: "+aka2);*/
                if (aka2.equals("")) {
                    toType.setAs(fromTypeName);
                }
                else {
                    toType.setAs(aka2);
                }
            }
            else if ( fromType.getKind().equals("int")){
                /*System.out.println("from int");
                System.out.println("aka1: "+aka1);*/
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
            /*System.out.println("to type != null");
            System.out.println("aka2: "+aka2);*/
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
           /* System.out.println("totype == null");
            System.out.println("aka1: "+aka1);
            System.out.println("totypeName: "+toTypeName);
            System.out.println("fromtypeName: "+fromTypeName);*/
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
                System.out.println("arg: "+arg);
            if (patchData.getType(arg)==null && patchData.getVariable(arg)==null && patchData.getFunction(arg)==null) {
                patchData.addVariable(arg);
            }
        }
        if (type!= null) {
            type.setAsClass();
        }
    
    }

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
    
    
    public static void invalidUseOfNonStatic(TUErrorData data, PatchData patchData) {
        String message = data.get(TUErrorData.MAP).get("message");
        String source = data.get(TUErrorData.MAP).get("source");
        String identifier = TUPatcherUtils.extractFromSingleQuotes(message).get(0);
        String className = source.split("::")[0];
        TypeInfo type = patchData.getType(className);
        type.setStatic(identifier);
    }
    public static void nonStaticMemberFunction(TUErrorData data, PatchData patchData) {
        String location = data.get(TUErrorData.MAP).get("location");
        String functionName = TUPatcherUtils.getTokenFromLocation(location);
        String className = TUPatcherUtils.getTokenBeforeLocation(location);
        TypeInfo type = patchData.getType(className);
        type.setStatic(functionName);
    }
    public static void nonConstCantBindToTemporary(TUErrorData data, PatchData patchData) {
        String typeName = data.get(TUErrorData.MAP).get("qualtype");
        String message = data.get(TUErrorData.MAP).get("message");
        String typeName2 = TUPatcherUtils.extractFromSingleQuotes(message).get(0);
        patchData.getType(typeName).setAs(typeName2+" &");
        patchData.getType(typeName2).setAsClass();
        
    }
}
