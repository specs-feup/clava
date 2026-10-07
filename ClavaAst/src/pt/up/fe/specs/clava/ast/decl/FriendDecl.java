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
import java.util.List;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.NullableNodeReference;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.DependentNameType;
import pt.up.fe.specs.clava.ast.type.DependentTemplateSpecializationType;
import pt.up.fe.specs.clava.ast.type.Type;

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

    /// DATAKEYS BEGIN

    public static final DataKey<CXXRecordDecl> OWNER_RECORD = KeyFactory.object("ownerRecord", CXXRecordDecl.class);

    @NullableNodeReference
    public static final DataKey<Decl> FRIEND_DECL = KeyFactory.object("friendDecl", Decl.class);

    @NullableNodeReference
    public static final DataKey<Type> FRIEND_TYPE = KeyFactory.object("friendType", Type.class);

    /// DATAKEYS END

    public FriendDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public ClavaNode getFriendNode() {
        return getStructuredFriendNode();
    }

    @Override
    public String getCode() {
        validateStructuredSupport();
        var friendNode = getFriendNode();

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

    /**
     * Validates the structured metadata and source-generation support for this friend.
     * Parsers call this after resolving all node links so unsupported constructs fail
     * while parsing, before clients can mistake the AST for a printable one.
     */
    public void validateStructuredSupport() {
        ClavaNode ownerRecord = get(OWNER_RECORD);
        if (ownerRecord == null || ownerRecord instanceof NullDecl) {
            throw unsupported("missing its structured owner record");
        }

        var friendNode = getFriendNode();
        if (isUnsupportedDependentFriend(friendNode)) {
            throw unsupported("dependent friend declarations are not yet supported by the structured printer");
        }
    }

    private boolean isUnsupportedDependentFriend(ClavaNode friendNode) {
        if (friendNode instanceof Type type) {
            return type instanceof DependentNameType
                    || type instanceof DependentTemplateSpecializationType;
        }

        FunctionDecl function = friendNode instanceof FunctionDecl direct ? direct
                : friendNode instanceof FunctionTemplateDecl template
                        && template.getTemplateDecl() instanceof FunctionDecl wrapped ? wrapped
                                : null;

        // Clang associates dependent friend member functions with their owning class
        // template. A free FunctionDecl, including a friend function template declared
        // inside a class template, has its own printable template headers instead.
        return function instanceof CXXMethodDecl
                && !function.get(FunctionDecl.TEMPLATE_PARAMETER_LIST_SIZES).isEmpty();
    }

    private ClavaNode getStructuredFriendNode() {
        Decl friendDecl = get(FRIEND_DECL);
        Type friendType = get(FRIEND_TYPE);
        boolean hasFriendDecl = !isNullReference(friendDecl);
        boolean hasFriendType = !isNullReference(friendType);
        if (hasFriendDecl == hasFriendType) {
            throw unsupported("must reference exactly one friend declaration or friend type");
        }

        // Render the current child so AST replacements and edits are reflected. The
        // separate structured reference fields describe the producer's semantic links.
        List<ClavaNode> friendChildren = getChildren().stream()
                .filter(hasFriendDecl ? Decl.class::isInstance : Type.class::isInstance)
                .filter(child -> !(child instanceof NullDecl)
                        && !(child instanceof pt.up.fe.specs.clava.utils.NullNode))
                .toList();
        if (friendChildren.size() != 1) {
            throw unsupported("must have exactly one current friend child matching its structured reference kind");
        }

        return friendChildren.get(0);
    }

    private static boolean isNullReference(ClavaNode node) {
        return node == null || node instanceof pt.up.fe.specs.clava.utils.NullNode;
    }

    private UnsupportedOperationException unsupported(String reason) {
        String location = getLocationTry().map(Object::toString).orElse("<no source location>");
        return new UnsupportedOperationException("Cannot print FriendDecl " + getId() + " at " + location + ": " + reason);
    }

}
