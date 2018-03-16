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

package pt.up.fe.specs.clang.transforms;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.data.TemplateKind;
import pt.up.fe.specs.clava.transform.SimplePostClavaRule;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Removes FunctionDecls that are template specializations.
 * 
 * @author JoaoBispo
 *
 */
public class DeleteTemplateSpecializations implements SimplePostClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        if (!(node instanceof FunctionDecl)) {
            return;
        }

        FunctionDecl functionDecl = (FunctionDecl) node;

        if (!functionDecl.getFunctionDeclData().getTemplateKind().isSpecialization()) {
            return;
        }

        // Only tested function specialization, check other cases
        if (functionDecl.getFunctionDeclData().getTemplateKind() != TemplateKind.FUNCTION_TEMPLATE_SPECIALIZATION) {
            SpecsLogs.msgWarn(
                    "Removing template specialization of kind '" + functionDecl.getFunctionDeclData().getTemplateKind()
                            + "', this has not yet been tested. Please contact the developers.");
        }

        queue.delete(functionDecl);

    }

}
