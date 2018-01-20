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

import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;

public class AttributeClauseParser {

    private final Pragma clavaPragma;

    public AttributeClauseParser(Pragma clavaPragma) {
        this.clavaPragma = clavaPragma;
    }

    public void parse(String contents) {

        List<String> parsedContents = ArgumentsParser.newPragmaText().parse(contents);
        System.out.println("STRING:" + contents);
        System.out.println("PARSED STRING:\n" + parsedContents.stream().collect(Collectors.joining("\n")));
        /*
        // Apply parsing rules
        while (!parser.isEmpty()) {
        
        }
        */

    }

}
