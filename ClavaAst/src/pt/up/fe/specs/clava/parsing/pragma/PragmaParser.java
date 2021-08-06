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

package pt.up.fe.specs.clava.parsing.pragma;

import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.util.stringparser.StringParser;

@FunctionalInterface
public interface PragmaParser {

    default Pragma parse(String contents, ClavaContext context) {
        return parse(new StringParser(contents), context);
    }

    Pragma parse(StringParser contents, ClavaContext context);
}
