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
import java.util.Collections;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.util.parsing.StringCodec;

public class BuiltinType extends Type {

    /// DATAKEYS BEGIN

    // public final static DataKey<Integer> KIND_ORDINAL = KeyFactory.integer("kindOrdinal", -1);
    /**
     * The kind of the built-in.
     */
    public final static DataKey<BuiltinKind> KIND = KeyFactory.enumeration("builtinKind", BuiltinKind.class)
            .setDecoder(StringCodec.newInstance(kind -> kind.getCode(), BuiltinKind::newInstance));

    /**
     * Optional, the literal code for this built-in type.
     */
    public final static DataKey<String> KIND_LITERAL = KeyFactory.string("kindLiteral");

    /// DATAKEYS END

    public BuiltinType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /**
     * @param data
     * @param info
     * @param children
     */
    // @Deprecated
    // protected BuiltinType(TypeData data, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    // this(new LegacyToDataStore().setType(data).setNodeInfo(info).getData(), children);
    //
    // // put(KIND, BuiltinKind.getHelper().fromValue(data.getBareType()));
    // // Type.put() creates a copy
    // getData().put(KIND_LITERAL, data.getBareType());
    // }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {

        // First, try kind code, then literal
        String type = get(KIND).getCodeTry(sourceNode).orElse(get(KIND_LITERAL));

        // Give priority to kind literal
        // String type = getData().hasValue(KIND_LITERAL) ? get(KIND_LITERAL) : get(KIND).getCode(getContext());

        // boolean isCxx = getApp().getAppData().get(ClavaOptions.STANDARD).isCxx();
        // boolean isCxx = getData().getStandard().isCxx();
        // String type = getKind().getCode();

        String varName = name == null ? "" : " " + name;
        return type + varName;
    }

    public BuiltinType setKindLiteral(String literalKind) {
        getData().put(KIND_LITERAL, literalKind);
        return this;
    }

    @Override
    protected List<DataKey<Type>> getUnderlyingTypeKeys() {
        return Collections.emptyList();
    }
    // @Override
    // protected Type setUnderlyingTypeProtected(Type oldType, Type newType) {
    // // No underlying type to set
    // return this;
    // }

    // public BuiltinKind getKind() {
    // public BuiltinKindV2 getKind() {
    // return get(KIND);
    // }

    /**
     * TODO: Remove this method, move to IntegerLiteral (only use), used BuiltinKind
     */
    /*
    @Override
    public String getConstantCode(String constant) {
        boolean isUnsigned = getKind().isUnsigned();
    
        if (isUnsigned) {
            // if (getBareType().startsWith("unsigned")) {
            return constant + "u";
        }
    
        return constant;
    
    }
    */

    // public boolean isVoid() {
    // return getKind() == BuiltinKind.VOID;
    // }

    @Override
    public int getBitwidth(ClavaNode node) {

        BuiltinKind builtinKind = get(BuiltinType.KIND);

        // get the language information
        var tUnit = getAncestorTry(TranslationUnit.class).orElse(null);
        if (tUnit == null) {
            ClavaLog.info("BuiltinType.getBitwidth: Given node is not part of a TranslationUnit");
            return -1;
        }

        Language lang = tUnit.get(TranslationUnit.LANGUAGE);

        return builtinKind.getBitwidth(lang);
    }
}
