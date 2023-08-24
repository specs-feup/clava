/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava.weaver;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import pt.up.fe.specs.symja.SymjaPlusUtils;

public class MathExtraApiTools {

    public static String simplifyExpression(String expression, Map<Object, Object> constants) {

        // Convert object map to strings
        Map<String, String> stringConstants = new HashMap<>();
        if (constants != null) {
            for (Entry<Object, Object> entry : constants.entrySet()) {
                stringConstants.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }

        return SymjaPlusUtils.simplify(expression, stringConstants);
    }

    public static String convertToC(String expression) {
        return SymjaPlusUtils.convertToC(expression);
    }
}
