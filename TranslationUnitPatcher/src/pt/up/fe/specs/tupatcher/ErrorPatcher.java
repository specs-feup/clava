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
        ERROR_PATCHERS.put(ErrorKind.NOT_A_FUNCTION_OR_FUNCTION_POINTER, ErrorPatcher::notAFunctionOrFunctionPointer);
        ERROR_PATCHERS.put(ErrorKind.NO_MATCHING_FUNCTION, ErrorPatcher::noMatchingFunction);
        ERROR_PATCHERS.put(ErrorKind.TOO_MANY_ARGUMENTS, ErrorPatcher::tooManyArguments);
        ERROR_PATCHERS.put(ErrorKind.NO_MEMBER, ErrorPatcher::noMember);
        ERROR_PATCHERS.put(ErrorKind.NO_MEMBER_DID_YOU_MEAN, ErrorPatcher::noMember);
        ERROR_PATCHERS.put(ErrorKind.CANT_INITIALIZE_WITH_TYPE, ErrorPatcher::incompatibleType);
        ERROR_PATCHERS.put(ErrorKind.INCOMPLETE_TYPE_STRUCT, ErrorPatcher::incompleteTypeStruct);
        ERROR_PATCHERS.put(ErrorKind.VARIABLE_INCOMPLETE_TYPE_STRUCT, ErrorPatcher::incompleteTypeStruct);
        ERROR_PATCHERS.put(ErrorKind.EXCESS_ELEMENTS_IN_INITIALIZER, ErrorPatcher::excessElementsInInitializer);
        ERROR_PATCHERS.put(ErrorKind.NO_MATCHING_CONSTRUCTOR, ErrorPatcher::noMatchingConstructor);
        ERROR_PATCHERS.put(ErrorKind.INCOMPATIBLE_TYPE, ErrorPatcher::incompatibleType);
        
        
        
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
            throw new RuntimeException("Error kind not supported yet: " + error);
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
                System.out.println(structOrClass);
                patchData.getType(structOrClass).addField(field, patchData);
                return;
            }
        //}

    }
    
    public static void notAFunctionOrFunctionPointer(TUErrorData data, PatchData patchData) {

        String source = data.get(TUErrorData.MAP).get("source");
        String functionName = source.substring(0, source.indexOf('('));
        if (!functionName.contains(".")) {
            System.out.println("function: "+functionName);
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
        String typeName = TUPatcherUtils.getTypeFromDeclaration(location);
        patchData.getType(typeName).incNumFields(patchData);
        
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

        String call = data.get(TUErrorData.MAP).get("source");      
        int numArgs = TUPatcherUtils.extractFromParenthesis(call).get(0).split(",").length;

        String message = data.get(TUErrorData.MAP).get("message");
        String typeName = message.substring(message.indexOf('\'')+1, message.indexOf('\'',message.indexOf('\'')+1));
       
        TypeInfo type = patchData.getType(typeName);
        type.addConstructor(numArgs);
    }
    public static void incompatibleType(TUErrorData data, PatchData patchData) {
        
        String message = data.get(TUErrorData.MAP).get("message");
        String fromTypeName = data.get(TUErrorData.MAP).get("qualtype");
        ArrayList<String> types = TUPatcherUtils.getTypesFromMessage(message);
        String toTypeName = types.get(0);
        fromTypeName = TUPatcherUtils.removeBracketsFromType(fromTypeName);
        toTypeName = TUPatcherUtils.removeBracketsFromType(toTypeName);
        TypeInfo toType = patchData.getType(toTypeName);
        TypeInfo fromType = patchData.getType(fromTypeName);
        
        if (toType != null && fromType != null) {
            if (toType.getKind().equals("int")) {
                toType.setAs(fromType.getKind());            
            }
            else if ( fromType.getKind().equals("int")){
                fromType.setAs(toType.getKind());  
            }
            else {
                System.out.println("No solution to this error!!!!!");
                System.out.println(fromType.getKind());
                System.out.println(toType.getKind());
                
            }
        }
        else if (toType != null) {
            toType.setAs(fromTypeName);  
        }
        else {
            fromType.setAs(toTypeName);
        }
    }

    public static void incompleteTypeStruct(TUErrorData data, PatchData patchData) {
        String typeName = data.get(TUErrorData.MAP).get("qualtype");
        typeName = typeName.replace("struct ", "");
        TypeInfo type = new TypeInfo(typeName);
        type.setAsStructWithoutTipedef();
        patchData.addType(type);
    }

}
