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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.language.TagKind;

/**
 * Represents a declaration of a struct, union, class or enum.
 * 
 * @author JoaoBispo
 *
 */
public abstract class TagDecl extends TypeDecl {

    /// DATAKEYS BEGIN

    /**
     * The kind of this tag (e.g., struct, union, class, enum...).
     */
    public final static DataKey<TagKind> TAG_KIND = KeyFactory.enumeration("tagKind", TagKind.class);

    /**
     * True if this decl has its body fully specified.
     */
    public final static DataKey<Boolean> IS_COMPLETE_DEFINITION = KeyFactory.bool("isCompleteDefinition");

    /**
     * Variables declared by this TagDecl.
     * 
     * <p>
     * E.g., struct { int i; } a_var;
     * 
     * <p>
     * In the end this option was dropped in favor of adding the VarDecls in the tree, so that they can still be found
     * through a query.
     */
    // public final static DataKey<List<DeclaratorDecl>> DECLS = KeyFactory.list("decls", DeclaratorDecl.class)
    // .setDefault(() -> new ArrayList<>());

    /// DATAKEYS END

    public TagDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public TagKind getTagKind() {
        return get(TAG_KIND);
    }

    public Boolean isCompleteDefinition() {
        return get(IS_COMPLETE_DEFINITION);
    }

    /**
     * 
     * @return if this TagDecl declares any variables, returns those variables
     */
    public List<DeclaratorDecl> getDeclaredVariables() {
        return getChildrenOf(DeclaratorDecl.class).stream()
                .filter(child -> child.get(DeclaratorDecl.IS_TAG_DECLARATION))
                .collect(Collectors.toList());
    }

    /**
     * 
     * @return if this TagDecl declares any variables, returns a string with the variable names separated by commas
     *         (e.g. a, b). Otherwise, returns an empty string
     */
    protected String getDeclsString() {

        var declsString = getDeclaredVariables().stream()
                .map(decl -> decl.getDeclName())
                .collect(Collectors.joining(", ", " ", ""));

        // If only whitespace, return empty string
        if (declsString.trim().isBlank()) {
            return "";
        }

        return declsString;
    }

    /**
     * 
     * @param child
     * @return true if the given node contributes with code for the TagDecl
     */
    protected boolean hasTagDeclCode(ClavaNode child) {
        if (child instanceof DeclaratorDecl && child.get(DeclaratorDecl.IS_TAG_DECLARATION)) {
            return false;
        }

        return true;
    }

}
