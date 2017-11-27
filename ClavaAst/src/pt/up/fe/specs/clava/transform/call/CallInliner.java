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

package pt.up.fe.specs.clava.transform.call;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;

public class CallInliner {

    private final CallExpr call;

    public CallInliner(CallExpr call) {
        this.call = call;
    }

    private void failureMsg(String message) {
        ClavaLog.info(
                "Could not inline call " + call.getCalleeName() + "@line " + call.getLocation().getStartLine() + ": "
                        + message);
    }

    public boolean inline() {
        System.out.println("TRYING TO INLINE");

        // Get definition
        FunctionDecl functionDecl = call.getDefinition().orElse(null);
        if (functionDecl == null) {
            failureMsg("could not find source code of function declaration");
            return false;
        }

        if (!functionDecl.hasBody()) {
            failureMsg("function declaration does not have a body");
            return false;
        }

        // Check if nodes are valid
        CallAnalysis.checkNodes(functionDecl.getBody().get());

        return true;
    }

}
