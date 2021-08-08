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

package pt.up.fe.specs.clang.transforms.legacy;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.ParenExpr;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Extracts the sub-statement from ClangLabelStmt nodes and replaces the nodes with LabelStmt nodes.
 * 
 * @author JoaoBispo
 *
 */
public class RecoverStdMacros implements SimplePreClavaRule {

    private static final Pattern STD_MACRO_PATTERN = Pattern.compile("\\(&__iob_func\\(\\)\\[(\\d)\\]\\)");

    private static final Map<String, String> STD_MACROS;
    static {
        STD_MACROS = new HashMap<>();
        STD_MACROS.put("0", "stdin");
        STD_MACROS.put("1", "stdout");
        STD_MACROS.put("2", "stderr");
    }

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        if (!(node instanceof ParenExpr)) {
            return;
        }

        ParenExpr parenExpr = (ParenExpr) node;

        // Test if std macro
        String stdMacroNumber = SpecsStrings.getRegexGroup(parenExpr.getCode(), STD_MACRO_PATTERN, 1);

        if (stdMacroNumber == null) {
            return;
        }

        // Get corresponding macro
        String macroStd = STD_MACROS.get(stdMacroNumber);
        Preconditions.checkNotNull(macroStd, "Case not defined, " + stdMacroNumber);

        // Replace expression
        queue.replace(parenExpr, node.getFactory().literalExpr(macroStd, parenExpr.getExprType()));
    }

}
