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

import pt.up.fe.specs.tupatcher.parser.TUErrorData;
import pt.up.fe.specs.tupatcher.parser.TUErrorsData;

public class ErrorPatcher {

    // final public static int UNKNOWN_TYPE = 3822;

    private final PatchData patchData;

    public ErrorPatcher(PatchData patchData) {
        this.patchData = patchData;
    }

    public void patch(TUErrorsData errorsData) {
        for (TUErrorData data : errorsData.get(TUErrorsData.ERRORS)) {
            int errorNumber = (int) data.getValue("errorNumber");

            var error = ErrorKind.getKind(errorNumber);
            switch (error) {
            case UNKNOWN_TYPE:
                // unknown type identifier
                String typeName = ((HashMap<String, String>) (data.getValue("map"))).get("identifier_name");
                patchData.addType(typeName);
                break;
            }
            // if (errorNumber == ErrorKind.UNKNOWN_TYPE.getId()) {
            // // unknown type identifier
            // String typeName = ((HashMap<String, String>) (data.getValue("map"))).get("identifier_name");
            // PatchData.addType(typeName);
            //
            // }
        }
        // else if (...) other errors

        return;
    }

}
