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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.pragmas;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class ClavaPragmas {

    private static final Map<String, Function<String, ClavaDirective>> CLAVA_DIRECTIVES_MAP;
    static {
        CLAVA_DIRECTIVES_MAP = new HashMap<>();
    }

    public static void processClavaPragmas(App app) {
        app.getDescendants(Pragma.class).stream()
                .filter(pragma -> pragma.getName().toLowerCase().equals("clava"))
                .forEach(ClavaPragmas::processClavaPragma);
    }

    private static void processClavaPragma(Pragma clavaPragma) {

        Optional<ClavaDirective> clavaDirective = getDirective(clavaPragma);
        if (clavaDirective.isPresent()) {
            var targetNode = clavaPragma.getTarget();
            if (!targetNode.isPresent()) {
                ClavaLog.warning(clavaPragma,
                        "No target found for Clava pragma '" + clavaPragma.getContent() + "'");
                return;
            }

            ACxxWeaverJoinPoint jp = CxxJoinpoints.create(targetNode.get());
            clavaDirective.ifPresent(directive -> directive.apply(jp));
            return;
        }
    }

    private static Optional<ClavaDirective> getDirective(Pragma clavaPragma) {
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
    }

}
