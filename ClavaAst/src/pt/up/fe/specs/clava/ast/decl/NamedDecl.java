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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.enums.NameKind;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.utils.Typable;

/**
 * Represents a decl with a name.
 * 
 * TODO: getDeclName() should be made abstract.
 * 
 * @author JoaoBispo
 *
 */
public abstract class NamedDecl extends Decl implements Typable {

    /// DATAKEYS BEGIN

    public final static DataKey<String> QUALIFIED_NAME = KeyFactory.string("qualifiedName");

    public final static DataKey<NameKind> NAME_KIND = KeyFactory.enumeration("nameKind", NameKind.class)
            .setDefault(() -> NameKind.IDENTIFIER);

    public final static DataKey<Boolean> IS_HIDDEN = KeyFactory.bool("isHidden");

    /// DATAKEYS END

    private String declName;
    private Type type;

    /**
     * @param declName
     * @param type
     * @param declData
     * @param info
     * @param children
     */
    public NamedDecl(String declName, Type type, DeclData declData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(declData, info, children);

        // if (declName != null) {
        // declName = declName.isEmpty() ? null : declName;
        // // Preconditions.checkArgument(!declName.isEmpty(),
        // // "Empty declNames not supported, use null instead (" + getLocation() + ")");
        // }
        // this.declName = declName == null ? "" : declName;
        this.declName = declName != null && declName.isEmpty() ? null : declName;
        // this.declName = declName;
        // Types should be unique
        this.type = type == null ? ClavaNodeFactory.nullType(getInfo()) : type.copy();
        // this.type.setApp(type.getApp());
    }

    /*
    public NamedDecl(Type type, DeclData declData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
    
        super(declData, info, children);
    
        this.declName = null;
        this.type = type == null ? ClavaNodeFactory.nullType(getInfo()) : type;
        this.types = type == null ? Collections.emptyList() : Arrays.asList(type);
    }
    */
    /*
    protected String getDeclNameInternal() {
        return declName;
    }
    */
    public String getDeclName() {
        // Preconditions.checkNotNull(declName);

        // if (declName == null) {
        // return "";
        // }

        return declName;
        // if (declName == null) {
        // // throw new RuntimeException("The class '" + getClass() + "' must override getDeclName()!");
        // throw new RuntimeException("DeclName is not defined");
        // }
        //
        // return declName;
    }

    public void setDeclName(String declName) {
        this.declName = declName;
    }

    public boolean hasDeclName() {
        return declName != null;
        // return !getDeclName().isEmpty();
        // if (declName == null || declName.isEmpty()) {
        // return false;
        // }
        //
        // return true;
        // if (declName == null) {
        // throw new RuntimeException("The class '" + getClass() + "' must override hasDeclName()!");
        // }
        // return !getDeclName().isEmpty();
    }

    /**
     * The code of this decl, without the type declaration.
     * 
     * @return
     */
    public String getTypelessCode() {
        throw new RuntimeException("Not implemented for class '" + getClass().getSimpleName() + "'");
    }

    @Override
    public Type getType() {

        // if (type == null) {
        // // return ClavaNodeFactory.literalType("<no type>", getInfo());
        // return ClavaNodeFactory.nullType(getInfo());
        // }
        return type;
    }

    @Override
    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toContentString() {
        return super.toContentString() + "declName:" + declName + ", type:" + getType().getCode();
    }

}
