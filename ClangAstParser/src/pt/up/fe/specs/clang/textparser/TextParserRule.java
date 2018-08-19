/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clang.textparser;

import java.util.Iterator;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.context.ClavaContext;

public interface TextParserRule {

    /**
     * For single line rules, the iterator will not be needed. In cases where the rule can spawn multiple lines, the
     * rule must leave the iterator ready for returning the next line to be processed. This means that it can only
     * consume the lines it will process.
     * 
     * @param line
     * @param lineNumber
     * @param iterator
     * @return
     */
    Optional<ClavaNode> apply(String filepath, String line, int lineNumber, Iterator<String> iterator,
            ClavaContext context);
}
