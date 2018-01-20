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

package pt.up.fe.specs.clava.weaver.pragmas;

import java.util.Optional;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;
import tdrc.utils.StringUtils;

public class ClavaPragmas {

    private static final String JOINPOINTS_PACKAGE_PREFIX = "pt.up.fe.specs.cxxweaver.abstracts.joinpoints.";

    public static void processClavaPragmas(App app) {
        app.getDescendants(Pragma.class).stream()
                .filter(pragma -> pragma.getName().toLowerCase().equals("clava"))
                .forEach(ClavaPragmas::processClavaPragma);
    }

    private static void processClavaPragma(Pragma clavaPragma) {
        StringParser parser = new StringParser(clavaPragma.getContent());

        if (parser.apply(StringParsers::hasWord, "attribute")) {
            new AttributeClauseParser(clavaPragma).parse(parser.toString());
            return;
        }

        // Check if starts with name of joinpoint
        boolean hasJoinpoint = parser.apply(StringParsers::checkStringStarts, "$").isPresent();
        String joinpointName = null;
        if (hasJoinpoint) {
            joinpointName = parser.apply(StringParsers::parseWord);
        }

        Optional<ClavaNode> target = clavaPragma.getTarget();
        if (!target.isPresent()) {
            ClavaLog.warning(clavaPragma, "Clava pragma could not be associated with a joinpoint");
            return;
        }

        // Create joinpoint
        ACxxWeaverJoinPoint joinpoint = CxxJoinpoints.create(target.get(), null);

        // Validate joinpoint, if present
        if (joinpointName != null) {
            // Make first letter uppercase
            String className = StringUtils.firstCharToUpper(joinpointName);
            // Prefix 'A'
            className = "A" + className;

            try {
                Class<?> joinpointTestClass = Class.forName(JOINPOINTS_PACKAGE_PREFIX + className);
                if (!joinpointTestClass.isInstance(joinpoint)) {
                    ClavaLog.warning(clavaPragma,
                            "Clava pragma associated to a joinpoint $" + joinpoint.getJoinpointType()
                                    + ", which is not compatible with the joinpoint $" + joinpointName
                                    + " defined in the pragma");
                }
            } catch (ClassNotFoundException e) {
                ClavaLog.warning(clavaPragma,
                        "The weaver does not recognize the joinpoit $" + joinpointName + " in Clava pragma");
            }
        }

        // Simple approach for now, enhance it as needed
        // Split remaining contents by the comma
        String[] defs = parser.getCurrentString().toString().split(",");
        for (String def : defs) {
            def = def.trim();

            // Find index of =
            int equalIndex = def.indexOf('=');

            // If index is not present, assume that we should set a bool value to true
            if (equalIndex == -1) {
                joinpoint.def(def, Boolean.TRUE);
                continue;
            }

            String attribute = def.substring(0, equalIndex).trim();
            String value = def.substring(equalIndex + 1).trim();

            joinpoint.def(attribute, value);
        }
    }

}
