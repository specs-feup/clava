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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.TagDecl;
import pt.up.fe.specs.clava.language.TagKind;

public abstract class TagType extends Type {

    /// DATAKEYS BEGIN

    // TODO: Change to TagDecl
    public final static DataKey<Decl> DECL = KeyFactory.object("decl", Decl.class);

    /// DATAKEYS END

    // private final DeclRef declInfo;
    // private final TagKind tagKind;

    // HACK
    // private final boolean tempNode;

    public TagType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /*
    public TagType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    
        // TODO: TEMPORARY, ADD DATA
        this.declInfo = new DeclRef("place_holder_1", "place_holder_2", "place_holder_3");
        this.tagKind = TagKind.NO_KIND;
    
        tempNode = true;
    }
    */
    /*
    public TagType(DeclRef declInfo, TagKind tagKind, TypeData typeData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(typeData, info, children);
    
        this.declInfo = declInfo;
        this.tagKind = tagKind;
    
        tempNode = false;
    }
    */

    // public DeclRef getDeclInfo() {
    // return declInfo;
    // }

    public TagDecl getDecl() {
        return (TagDecl) get(DECL);
    }

    public TagKind getTagKind() {
        return getDecl().get(TagDecl.TAG_KIND);
        // return tagKind;
    }

    // @Override
    // public String toContentString() {
    // return "tagKind: " + getTagKind();
    // }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {
        // if (tempNode) {
        // if (name == null) {
        // return get(TYPE_AS_STRING);
        // }
        // return get(TYPE_AS_STRING) + " " + get(TYPE_AS_STRING);
        // }

        // System.out.println("TAGTYPE:" + getTagKind());
        // System.out.println("DECL INFO:" + getDeclInfo());
        // System.out.println("TAG KIND:" + getTagKind());
        // System.out.println("TYPE DATA:" + getTypeData());
        // String baseType = getDeclInfo().getDeclType();

        String baseType = getDecl().get(TagDecl.TYPE_FOR_DECL).getCode();
        if (baseType.isEmpty()) {
            baseType = getBareType();
        }

        String enumType = getTagKind().getCode() + " " + baseType;
        if (name == null) {
            return enumType;
        }

        return enumType + " " + name;
    }

}
