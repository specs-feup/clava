/**
 * Copyright 2018 SPeCS.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;

public class AttributeDirective implements ClavaDirective {

    private final List<String> select;
    private final Map<String, String> setters;

    public AttributeDirective(List<String> select, Map<String, String> setters) {
        this.select = select;
        this.setters = setters;
    }

    // public static Optional<AttributeDirective> parse(Pragma clavaPragma) {
    // String pragmaName = clavaPragma.getName();
    //
    // Preconditions.checkArgument(pragmaName.toLowerCase().equals("clava"),
    // "Expected pragma name to be 'clava', is " + pragmaName);
    //
    // // Check if is a directive 'attribute'
    // StringParser parser = new StringParser(clavaPragma.getContent());
    // if (!parser.apply(StringParsers::hasWord, "attribute")) {
    // return Optional.empty();
    // }
    //
    // return parse(parser.toString());
    // }

    public static AttributeDirective parse(String directiveContent) {

        List<String> clauses = ArgumentsParser.newPragmaText().parse(directiveContent);

        List<String> select = new ArrayList<>();
        Map<String, String> setters = new HashMap<>();

        for (String clause : clauses) {
            int openParIndex = clause.indexOf('(');

            // If no open parenthesis, assume clause is the key, and value is 'true'
            if (openParIndex == -1) {
                setters.put(clause, "true");
                continue;
            }

            // Split the key name from the value
            Preconditions.checkArgument(clause.endsWith(")"), "Expected clause '" + clause + "' to end with ')'");
            String key = clause.substring(0, openParIndex);
            String value = clause.substring(openParIndex + 1, clause.length() - 1);

            // Special key 'select'
            if (key.equals("select")) {
                // Check if there was already a select
                if (!select.isEmpty()) {
                    ClavaLog.info("Overwriting previous 'select' clause with content '" + select + "'");
                }

                select = Arrays.asList(value.split("\\."));
                continue;
            }

            // Store key-value pair
            setters.put(key, value);
        }

        // System.out.println("STRING:" + directiveContent);
        // System.out.println("PARSED STRING:\n" + clauses.stream().collect(Collectors.joining("\n")));
        /*
        // Apply parsing rules
        while (!parser.isEmpty()) {
        
        }
        */
        // System.out.println("SELECT:" + select);
        // System.out.println("SETTERS:" + setters);
        return new AttributeDirective(select, setters);
    }

    @Override
    public void apply(AJoinPoint jp) {

        // Apply select
        if (!select.isEmpty()) {
            List<AJoinPoint> selectedJps = selectJps(Arrays.asList(jp), select);
            for (AJoinPoint selectedJp : selectedJps) {
                new AttributeDirective(Collections.emptyList(), setters).apply(selectedJp);
            }
            return;
        }

        // Apply each setter on joinpoint using def
        for (Entry<String, String> entry : setters.entrySet()) {
            jp.defImpl(entry.getKey(), entry.getValue());
        }

    }

    private static List<AJoinPoint> selectJps(List<AJoinPoint> jps, List<String> select) {

        List<AJoinPoint> currentJps = jps;

        for (String selectName : select) {
            // Create select jps
            List<AJoinPoint> selectedJps = new ArrayList<>();

            // For each of current join point, apply select
            for (AJoinPoint jp : currentJps) {
                selectedJps.addAll(SpecsCollections.cast(jp.select(selectName), AJoinPoint.class));
            }

            // Make select jps be current jps
            currentJps = selectedJps;
        }
        // System.out.println("CURRENT JPS:" + currentJps);
        return currentJps;

        // Preconditions.checkArgument(select.isEmpty(), "Select must not be empty");
        //
        // if (select.size() == 1) {
        // String selectJp = select.get(0);
        // List<AJoinPoint> selectedJps = new ArrayList<>();
        // for (AJoinPoint jp : jps) {
        // selectedJps.addAll(SpecsCollections.cast(jp.select(selectJp), AJoinPoint.class));
        // }
        //
        // return selectedJps;
        // }
        //
        // // TODO Auto-generated method stub
        // return null;
    }
}
