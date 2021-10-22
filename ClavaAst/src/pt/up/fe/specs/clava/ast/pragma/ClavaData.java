/**
 * Copyright 2021 SPeCS.
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

package pt.up.fe.specs.clava.ast.pragma;

import java.util.Arrays;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.util.stringsplitter.StringSplitter;
import pt.up.fe.specs.util.stringsplitter.StringSplitterRules;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

/**
 * Methods related with the #pragma clava data
 * 
 * @author JBispo
 *
 */
public class ClavaData {

    private static final String KEYWORD_DATA = "data";

    /**
     * 
     * 
     * @param node
     * @return if a pragma clava data exists that is associated with this node, returns it; otherwise returns null.
     */
    public static Pragma getClavaData(ClavaNode node) {

        for (var pragma : ClavaNodes.getPragmas(node)) {

            if (!pragma.getName().toLowerCase().equals("clava")) {
                continue;
            }

            // Parse content
            StringSplitter splitter = new StringSplitter(pragma.getContent());
            boolean isDataDirective = splitter.parseTry(StringSplitterRules::string)
                    .filter(string -> string.toLowerCase().equals(KEYWORD_DATA))
                    .isPresent();

            if (!isDataDirective) {
                continue;
            }

            return pragma;
        }

        return null;
    }

    /**
     * 
     * @param node
     * @return if node can accept a pragma, builds a new pragma clava data and inserts it before the node, returning the
     *         pragma. Otherwise, returns null
     */
    public static Pragma buildClavaData(ClavaNode node) {
        if (!ClavaNodes.acceptsPragmas(node)) {
            return null;
        }

        var pragma = node.getFactory().genericPragma(Arrays.asList("clava data"));

        NodeInsertUtils.insertBefore(node, pragma);

        return pragma;
    }

}
