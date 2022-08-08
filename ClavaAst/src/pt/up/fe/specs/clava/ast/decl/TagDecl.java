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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.extra.TagDeclVars;
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
        return getTagDeclVarsTry()
                .map(tagDeclVars -> tagDeclVars.getChildren(DeclaratorDecl.class))
                .orElseGet(() -> Collections.emptyList());
    }

    /**
     * 
     * @return the node that contains the variables declared by this TagDecl, as children. If no node exists, one will
     *         be created and added to the TagDecl children
     */
    public TagDeclVars getTagDeclVars() {
        var maybeTagDeclVars = getTagDeclVarsTry();

        // If exists, just return it
        if (maybeTagDeclVars.isPresent()) {
            return maybeTagDeclVars.get();
        }

        // Create node and add it
        var tagDeclVars = getFactory().tagDeclVars(Collections.emptyList());

        // Since the node was newly created and has no parent, the node was inserted, instead of a copy
        addChild(tagDeclVars);

        return tagDeclVars;

    }

    /**
     * 
     * @return the node that contains the variables declared by this TagDecl, as children
     */
    public Optional<TagDeclVars> getTagDeclVarsTry() {
        var tagDeclVarsList = getChildrenOf(TagDeclVars.class);

        if (tagDeclVarsList.size() > 1) {
            throw new RuntimeException(
                    "Inconsistent AST: found more than one instance of TagDeclVars (" + tagDeclVarsList.size() + ")");
        }

        if (tagDeclVarsList.size() == 1) {
            return Optional.of(tagDeclVarsList.get(0));
        }

        return Optional.empty();
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
        if (child instanceof TagDeclVars) {
            return false;
        }

        return true;
    }

    /**
     * 
     * @return children of TagDecl which contribute with code for the declaration
     */
    protected List<ClavaNode> getChildrenWithCode() {
        return getChildrenStream()
                .filter(this::hasTagDeclCode)
                .collect(Collectors.toList());
    }

}
