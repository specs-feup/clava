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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import pt.up.fe.specs.tupatcher.parser.TUErrorData;
import pt.up.fe.specs.tupatcher.parser.TUErrorsData;
import pt.up.fe.specs.util.SpecsLogs;

public class ErrorPatcher {

    private final static Map<ErrorKind, BiConsumer<TUErrorData, PatchData>> ERROR_PATCHERS;
    static {
        ERROR_PATCHERS = new HashMap<>();

        ERROR_PATCHERS.put(ErrorKind.UNKNOWN_TYPE, ErrorPatcher::unknownType);
        ERROR_PATCHERS.put(ErrorKind.UNDECLARED_IDENTIFIER, ErrorPatcher::undeclaredIdentifier);
        ERROR_PATCHERS.put(ErrorKind.UNKNOWN_TYPE_DID_YOU_MEAN, ErrorPatcher::unknownType);
        ERROR_PATCHERS.put(ErrorKind.NOT_STRUCT_OR_UNION, ErrorPatcher::notStructOrUnion);
        ERROR_PATCHERS.put(ErrorKind.NOT_A_FUNCTION_OR_FUNCTION_POINTER, ErrorPatcher::notAFunctionOrFunctionPointer);
        ERROR_PATCHERS.put(ErrorKind.NO_MATCHING_FUNCTION, ErrorPatcher::noMatchingFunction);
        ERROR_PATCHERS.put(ErrorKind.NO_MEMBER, ErrorPatcher::noMember);
        
        
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

    public static void unknownTypeDidYouMean(TUErrorData data, PatchData patchData) {
        String typeName = data.get(TUErrorData.MAP).get("identifier_name");
        String suggestion = data.get(TUErrorData.MAP).get("string");
        suggestion = suggestion.substring(1, suggestion.length()-1);
        TypeInfo typeInfo = new TypeInfo();
        typeInfo.setAs(suggestion);
        patchData.setType(typeName, typeInfo);
        
    }
    
    public static void notStructOrUnion(TUErrorData data, PatchData patchData) {
        String qualType = data.get(TUErrorData.MAP).get("qualtype");
        patchData.getType(qualType).setAsStruct();
        
    }
    public static void undeclaredIdentifier(TUErrorData data, PatchData patchData) {
        
        String message = data.get(TUErrorData.MAP).get("message");
        
        //find identifier between single quotes
        String name = message.substring(message.indexOf('\'')+1, message.indexOf('\'',message.indexOf('\'')+1));
        
        patchData.addVariable(name);

    }
    public static void noMember(TUErrorData data, PatchData patchData) {
        
        String message = data.get(TUErrorData.MAP).get("message");
        
        //find identifiers between single quotes
        int index1 = message.indexOf('\'');
        int index2 = message.indexOf('\'', index1 + 1);
        int index3 = message.indexOf('\'', index2 + 1);
        int index4 = message.indexOf('\'', index3 + 1);
        
        String field = message.substring(index1+1, index2);

        String struct_or_class = message.substring(index3+1, index4);
        
        patchData.getType(struct_or_class).addField(field, new TypeInfo());

    }
    
    public static void notAFunctionOrFunctionPointer(TUErrorData data, PatchData patchData) {

        String function_name = data.get(TUErrorData.MAP).get("source");
        patchData.removeVariable(function_name);
        patchData.addFunction(function_name);
        
    }
    
    public static void noMatchingFunction(TUErrorData data, PatchData patchData) {
        
        String call = data.get(TUErrorData.MAP).get("source");
        
        //find identifiers between parenthesis
        int index1 = call.indexOf('(');
        int index2 = call.indexOf(')');
        
        String functionName = call.substring(0, index1);
        FunctionInfo function = patchData.getFunction(functionName);
        if (function == null) {
            patchData.addFunction(functionName);
            function = patchData.getFunction(functionName);
        }      
        
        
        int numArgs = call.substring(index1+1, index2).split(",").length;
        for (int i = 0; i < numArgs; i++) {
            function.addArgument(new TypeInfo("undefined"));
        }
        
    }
    
}
