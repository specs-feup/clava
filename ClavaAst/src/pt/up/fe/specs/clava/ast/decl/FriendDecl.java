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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.utils.NullNode;

/**
 * Represents the declaration of a friend entity.
 * 
 * <p>
 * Friend node can either be a Type or a Decl.
 * 
 * @author JoaoBispo
 *
 */
public class FriendDecl extends Decl {

    public FriendDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public ClavaNode getFriendNode() {
        return getChild(0);
    }

    @Override
    public String getCode() {
        var friendNode = getFriendNode();
        if (friendNode instanceof NullNode) {
            ClavaLog.warning(this, "FriendDecl not yet implemented for this case");
            return "friend";
        }

        FunctionDecl function = friendNode instanceof FunctionDecl direct ? direct
                : friendNode instanceof FunctionTemplateDecl template
                        && template.getTemplateDecl() instanceof FunctionDecl wrapped ? wrapped : null;
        if (function != null && !function.get(FunctionDecl.TEMPLATE_PARAMETER_LIST_SIZES).isEmpty()) {
            // Clang cannot resolve these dependent friend owners and reports their methods
            // in the lexical record. Preserve the declaration source instead of inventing
            // an owner from that incomplete AST. Edits inside this declaration are not printed.
            String source = getLocation().getSource().orElseThrow(() -> new IllegalStateException(
                    "Dependent friend member templates require their original declaration source"));
            if (source.contains("R\"") || source.contains("\\\n") || source.contains("\\\r")) {
                throw new UnsupportedOperationException(
                        "Raw strings and line continuations in dependent friend member declarations require a structured printer");
            }
            String prefix = friendNode instanceof FunctionTemplateDecl
                    ? function.getEnclosingTemplateHeadersCode() : "";
            String declaration = source.lines().map(String::stripLeading).collect(Collectors.joining(ln())).strip();
            return prefix + declaration + (declaration.endsWith(";") ? "" : ";");
        }

        String friendCode = friendNode.getCode();

        if (friendNode instanceof Type) {
            friendCode = friendCode + ";";
        }

        // Check if it has new lines at the beginning
        if (friendCode.startsWith(ln())) {
            friendCode = friendCode.substring(ln().length());
        }

        if (friendNode instanceof RedeclarableTemplateDecl) {
            return ((RedeclarableTemplateDecl) friendNode).getCode("friend");
        }

        return "friend " + friendCode;
    }

}
