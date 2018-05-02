/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clava.context;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.DummyDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.enums.BuiltinKind;
import pt.up.fe.specs.clava.ast.expr.legacy.FloatingLiteralLegacy.FloatKind;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.type.BuiltinType;

public class ClavaFactory {

    private static final String TYPE_ID_PREFIX = "type_";
    private static final String EXPR_ID_PREFIX = "expr_";
    private static final String DECL_ID_PREFIX = "decl_";
    private static final String EXTRA_ID_PREFIX = "extra_";
    // private static final String DECL_ID_PREFIX = "decl_";

    private final ClavaContext context;
    private final DataStore baseData;

    public ClavaFactory(ClavaContext context) {
        this(context, null);
    }

    public ClavaFactory(ClavaContext context, DataStore baseData) {
        this.context = context;
        this.baseData = baseData;
    }

    private DataStore newDataStore(String idPrefix) {
        DataStore data = DataStore.newInstance("ClavaFactory Node");

        // Add base node, if present
        if (baseData != null) {
            data.addAll(baseData);
        }

        // Set context
        data.set(ClavaNode.CONTEXT, context);
        // Set id
        data.set(ClavaNode.ID, context.get(ClavaContext.ID_GENERATOR).next(idPrefix));

        return data;
    }

    private DataStore newTypeDataStore() {
        return newDataStore(TYPE_ID_PREFIX);
    }

    private DataStore newExprDataStore() {
        return newDataStore(EXPR_ID_PREFIX);
    }

    private DataStore newExtraDataStore() {
        return newDataStore(EXTRA_ID_PREFIX);
    }

    private DataStore newDeclDataStore() {
        return newDataStore(DECL_ID_PREFIX);
    }

    /// EXTRA

    public App app(List<TranslationUnit> tUnits) {
        DataStore data = newExtraDataStore();
        return new App(data, tUnits);
    }

    /// TYPES

    public BuiltinType builtinType(BuiltinKind kind) {
        DataStore data = newTypeDataStore().put(BuiltinType.KIND, kind);
        return new BuiltinType(data, Collections.emptyList());
    }

    /// EXPRS

    public IntegerLiteral integerLiteral(int integer) {
        DataStore data = newExprDataStore()
                .put(IntegerLiteral.VALUE, BigInteger.valueOf(integer))
                .put(Expr.TYPE, builtinType(BuiltinKind.INT));

        return new IntegerLiteral(data, Collections.emptyList());
    }

    public FloatingLiteral floatingLiteral(FloatKind floatKind, double value) {
        DataStore data = newExprDataStore()
                .put(FloatingLiteral.VALUE, value)
                .put(Expr.TYPE, builtinType(floatKind.getBuiltinKind()));

        return new FloatingLiteral(data, Collections.emptyList());
    }

    /// DECLS

    public DummyDecl dummyDecl(String dummyContent) {
        DataStore data = newDeclDataStore()
                .put(DummyDecl.DUMMY_CONTENT, dummyContent);

        return new DummyDecl(data, Collections.emptyList());
    }

}
