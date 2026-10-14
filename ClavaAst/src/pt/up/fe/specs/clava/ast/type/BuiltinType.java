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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.util.parsing.StringCodec;

public class BuiltinType extends BuiltinTypeBase {

    // Data declarations are generated from the wire schema in BuiltinTypeBase.

    public BuiltinType(DataStore data, Collection<? extends ClavaNode> children, BuiltinTypeBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public BuiltinType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {

        // First, try kind code, then literal
        String type = getWireKind().getCodeTry(sourceNode).orElse(getWireKindLiteral());

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

    @Override
    public int getBitwidth(ClavaNode node) {

        BuiltinKind builtinKind = get(BuiltinType.KIND);

        // Get the language information
        var tUnit = node.getAncestorTry(TranslationUnit.class).orElse(null);
        if (tUnit == null) {
            ClavaLog.info("BuiltinType.getBitwidth: Given node is not part of a TranslationUnit");
            return -1;
        }

        Language lang = tUnit.get(TranslationUnit.LANGUAGE);

        return builtinKind.getBitwidth(lang);
    }
}
