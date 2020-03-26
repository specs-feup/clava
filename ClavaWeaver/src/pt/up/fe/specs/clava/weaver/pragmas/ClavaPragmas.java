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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;
import tdrc.utils.StringUtils;

public class ClavaPragmas {

    private static final String JOINPOINTS_PACKAGE_PREFIX = "pt.up.fe.specs.cxxweaver.abstracts.joinpoints.";

    private static final Map<String, Function<String, ClavaDirective>> CLAVA_DIRECTIVES_MAP;
    static {
        CLAVA_DIRECTIVES_MAP = new HashMap<>();
        CLAVA_DIRECTIVES_MAP.put("attribute", AttributeDirective::parse);
        // CLAVA_DIRECTIVES_MAP.put("opencl", OpenCLDirective::parse);
        // CLAVA_DIRECTIVES_MAP.put("attribute", content -> AttributeDirective::parse);
    }

    public static void processClavaPragmas(App app) {
        app.getDescendants(Pragma.class).stream()
                .filter(pragma -> pragma.getName().toLowerCase().equals("clava"))
                .forEach(ClavaPragmas::processClavaPragma);
    }

    private static void processClavaPragma(Pragma clavaPragma) {

        Optional<ClavaDirective> clavaDirective = getDirective(clavaPragma);
        if (clavaDirective.isPresent()) {
            Optional<Stmt> targetNode = clavaPragma.getTarget();
            if (!targetNode.isPresent()) {
                ClavaLog.warning(clavaPragma,
                        "No target statement found for Clava pragma '" + clavaPragma.getContent() + "'");
                return;
            }

            ACxxWeaverJoinPoint jp = CxxJoinpoints.create(targetNode.get());
            clavaDirective.ifPresent(directive -> directive.apply(jp));
            return;
        }
        // Optional<>
        // if (parser.apply(StringParsers::hasWord, "attribute")) {
        // new ClavaAttributeClause(clavaPragma).parse(parser.toString());
        // return;
        // }

        StringParser parser = new StringParser(clavaPragma.getContent());

        if (!parser.apply(StringParsers::peekStartsWith, "def ")) {
            return;
        }

        // Check if starts with name of joinpoint
        boolean hasJoinpoint = parser.apply(StringParsers::checkStringStarts, "$").isPresent();
        String joinpointName = null;
        if (hasJoinpoint) {
            joinpointName = parser.apply(StringParsers::parseWord);
        }

        Optional<Stmt> target = clavaPragma.getTarget();
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
                            "Clava pragma associated to a joinpoint $" + joinpoint.getJoinPointType()
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

    private static Optional<ClavaDirective> getDirective(Pragma clavaPragma) {
        // String pragmaName = clavaPragma.getName();

        // Preconditions.checkArgument(pragmaName.toLowerCase().equals("clava"),
        // "Expected pragma name to be 'clava', is " + pragmaName);

        // Check directive
        StringParser parser = new StringParser(clavaPragma.getContent());

        // Return if there is no directive name to parse
        if (parser.isEmpty()) {
            return Optional.empty();
        }

        String directiveName = parser.apply(StringParsers::parseWord);

        Function<String, ClavaDirective> directiveBuilder = CLAVA_DIRECTIVES_MAP.get(directiveName);
        if (directiveBuilder == null) {
            return Optional.empty();
        }

        return Optional.of(directiveBuilder.apply(parser.toString()));
        // if (!parser.apply(StringParsers::parseWordhasWord, "attribute")) {
        // return Optional.empty();
        // }

        // return parse(parser.toString());
        //
        // // TODO Auto-generated method stub
        // return null;
    }

}
