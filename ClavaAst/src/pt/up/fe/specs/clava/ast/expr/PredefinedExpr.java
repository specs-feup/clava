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

package pt.up.fe.specs.clava.ast.expr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

/**
 * Represents a type cast.
 * 
 * @author JoaoBispo
 *
 */
public class PredefinedExpr extends Expr {

    private final PredefinedIdType id;

    public PredefinedExpr(PredefinedIdType id, ExprData exprData, ClavaNodeInfo info,
            Expr subExpr) {
        this(id, exprData, info, Arrays.asList(subExpr));
    }

    protected PredefinedExpr(PredefinedIdType id, ExprData exprData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(exprData, info, children);

        this.id = id;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new PredefinedExpr(id, getExprData(), getInfo(), Collections.emptyList());
    }

    public PredefinedIdType getIdentifier() {
        return id;
    }

    public Expr getSubExpr() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        return getSubExpr().getCode();
    }

    public enum PredefinedIdType implements StringProvider {
        FUNC("__func__"),
        FUNCTION("__FUNCTION__"),
        L_FUNCTION("L__FUNCTION__"),
        FUND_D_NAME("__FUNCDNAME__"),
        FUNC_SIG("__FUNCSIG__"),
        PRETTY_FUNCTION("__PRETTY_FUNCTION__"),
        PRETTY_FUNCTION_NO_VIRTUAL("<not implemented for PRETTY_FUNCTION_NO_VIRTUAL>");

        private static final Lazy<EnumHelper<PredefinedIdType>> ENUM_HELPER = EnumHelper
                .newLazyHelper(PredefinedIdType.class);

        public static EnumHelper<PredefinedIdType> getEnumHelper() {
            return ENUM_HELPER.get();
        }

        private PredefinedIdType(String identTypeName) {
            this.identTypeName = identTypeName;
        }

        private final String identTypeName;

        @Override
        public String getString() {
            return identTypeName;
        }

    }

}
