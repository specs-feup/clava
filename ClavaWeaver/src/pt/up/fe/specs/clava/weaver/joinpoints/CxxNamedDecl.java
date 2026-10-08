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

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.AccessSpecDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.language.AccessSpecifier;
import pt.up.fe.specs.clava.language.TagKind;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ANamedDecl;

public class CxxNamedDecl<Self extends CxxNamedDecl<Self>> extends ANamedDecl<Self> {

    public CxxNamedDecl(NamedDecl namedDecl, CxxWeaver weaver) {
        super(namedDecl, weaver);
    }

    @Override
    public NamedDecl getNodeImpl() {
        return (NamedDecl) super.getNodeImpl();
    }

    @Override
    public String getNameImpl() {
        return this.getNodeImpl().hasDeclName() ? this.getNodeImpl().getDeclName() : null;
    }

    @Override
    public boolean getIsPublicImpl() {
        // Search for the first AccessSpecDecl that appears before this node
        int declIndex = this.getNodeImpl().indexOfSelf();
        List<ClavaNode> siblings = this.getNodeImpl().getParent().getChildren();

        for (int i = declIndex - 1; i >= 0; i--) {
            if (siblings.get(i) instanceof AccessSpecDecl) {
                return ((AccessSpecDecl) siblings.get(i)).getAccessSpecifier() == AccessSpecifier.PUBLIC;
            }
        }

        boolean isInsideClass = this.getNodeImpl().getAncestorTry(RecordDecl.class)
                .map(recordDecl -> recordDecl.get(RecordDecl.TAG_KIND) == TagKind.CLASS)
                .orElse(false);

        // By default, return true, unless is inside a class
        return isInsideClass ? false : true;
    }

    @Override
    public void setNameImpl(String name) {
        this.getNodeImpl().set(NamedDecl.DECL_NAME, name);
    }

    @Override
    public String getQualifiedPrefixImpl() {
        return this.getNodeImpl().get(NamedDecl.QUALIFIED_PREFIX);
    }

    @Override
    public String getQualifiedNameImpl() {
        return this.getNodeImpl().getFullyQualifiedName();
    }

    @Override
    public void setQualifiedPrefixImpl(String qualifiedPrefix) {
        this.getNodeImpl().set(NamedDecl.QUALIFIED_PREFIX, qualifiedPrefix);
    }

    @Override
    public void setQualifiedNameImpl(String name) {
        this.getNodeImpl().setQualifiedName(name);
    }

}
