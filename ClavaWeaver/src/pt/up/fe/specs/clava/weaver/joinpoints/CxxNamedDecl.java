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

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.AccessSpecDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.language.AccessSpecifier;
import pt.up.fe.specs.clava.language.TagKind;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ANamedDecl;

public class CxxNamedDecl extends ANamedDecl {

    private final NamedDecl namedDecl;

    public CxxNamedDecl(NamedDecl namedDecl) {
        super(new CxxDecl(namedDecl));

        this.namedDecl = namedDecl;
    }

    @Override
    public ClavaNode getNode() {
        return namedDecl;
    }

    @Override
    public String getNameImpl() {
        return namedDecl.hasDeclName() ? namedDecl.getDeclName() : null;
    }

    @Override
    public Boolean getIsPublicImpl() {
        // Search for the first AccessSpecDecl that appears before this node
        int declIndex = namedDecl.indexOfSelf();
        List<ClavaNode> siblings = namedDecl.getParent().getChildren();

        for (int i = declIndex - 1; i >= 0; i--) {
            if (siblings.get(i) instanceof AccessSpecDecl) {
                return ((AccessSpecDecl) siblings.get(i)).getAccessSpecifier() == AccessSpecifier.PUBLIC;
            }
        }

        boolean isInsideClass = namedDecl.getAncestorTry(RecordDecl.class)
                .map(recordDecl -> recordDecl.get(RecordDecl.TAG_KIND) == TagKind.CLASS)
                .orElse(false);

        // By default, return true, unless is inside a class
        return isInsideClass ? false : true;
    }

    @Override
    public void setNameImpl(String name) {
        defNameImpl(name);
    }

    @Override
    public void defNameImpl(String value) {
        namedDecl.set(NamedDecl.DECL_NAME, value);
    }

    @Override
    public String getQualifiedPrefixImpl() {
        return namedDecl.get(NamedDecl.QUALIFIED_PREFIX);
    }

    @Override
    public String getQualifiedNameImpl() {
        return namedDecl.getCurrentQualifiedName();
    }

    @Override
    public void defQualifiedPrefixImpl(String value) {
        namedDecl.set(NamedDecl.QUALIFIED_PREFIX, value);
    }

    @Override
    public void defQualifiedNameImpl(String value) {
        namedDecl.setQualifiedName(value);
    }

    @Override
    public void setQualifiedPrefixImpl(String qualifiedPrefix) {
        defQualifiedPrefixImpl(qualifiedPrefix);
    }

    @Override
    public void setQualifiedNameImpl(String name) {
        defQualifiedNameImpl(name);
    }

}
