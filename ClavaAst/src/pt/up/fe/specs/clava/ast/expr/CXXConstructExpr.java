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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXConstructorDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.expr.enums.ConstructionKind;
import pt.up.fe.specs.clava.ast.expr.enums.ValueKind;
import pt.up.fe.specs.clava.ast.type.NullType;
import pt.up.fe.specs.util.collections.SpecsList;

/**
 * Represents a call to a C++ constructor.
 * 
 * @author JoaoBispo
 *
 */
public class CXXConstructExpr extends Expr {

    /// DATAKEYS BEGIN

    public final static DataKey<Boolean> IS_ELIDABLE = KeyFactory.bool("isElidable");

    public final static DataKey<Boolean> REQUIRES_ZERO_INITIALIZATION = KeyFactory.bool("requiresZeroInitialization");

    public final static DataKey<Boolean> IS_LIST_INITIALIZATION = KeyFactory.bool("isListInitialization");

    public final static DataKey<Boolean> IS_STD_LIST_INITIALIZATION = KeyFactory.bool("isStdInitListInitialization");

    public final static DataKey<ConstructionKind> CONSTRUCTION_KIND = KeyFactory.enumeration("constructionKind",
            ConstructionKind.class);

    public final static DataKey<Boolean> IS_TEMPORARY_OBJECT = KeyFactory.bool("isTemporaryObject");

    public final static DataKey<CXXConstructorDecl> CONSTRUCTOR_DECL = KeyFactory.object("constructorDecl",
            CXXConstructorDecl.class);

    /// DATAKEYS END

    public CXXConstructExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public boolean isElidable() {
        return get(IS_ELIDABLE);
    }

    /**
     * 
     * @return the list of arguments, including default arguments added by the parser
     */
    public List<Expr> getCompleteArgs() {
        return getChildren(Expr.class);
    }

    /**
     * 
     * @return the list of arguments that should appear in the code
     */
    public List<Expr> getArgs() {
        return getCompleteArgs().stream()
                .filter(arg -> !(arg instanceof CXXDefaultArgExpr))
                .collect(Collectors.toList());
    }

    @Override
    public String getCode() {

        // If only one non-default arg, most of the time the constructor should be omitted
        if (ommitConstructor()) {
            return getArgs().get(0).getCode();
        }

        String typeCode = getExprType() instanceof NullType ? null : getExprType().getCode(this);

        String code = getCode(typeCode);

        return code;
    }

    private boolean ommitConstructor() {
        List<Expr> args = getArgs();

        if (args.size() != 1) {
            return false;
        }

        // One case where it should not is when the parent is a CxxNewExpr
        if (getParent() instanceof CXXNewExpr) {
            return false;
        }

        return true;
    }

    public boolean isListInit() {
        return get(IS_LIST_INITIALIZATION) || get(IS_STD_LIST_INITIALIZATION);
    }

    /**
     * 
     * @param cxxRecordName
     * @return
     */
    public String getCode(String cxxRecordName) {
        if (cxxRecordName == null) {
            return "";
        }

        // Special case: constructor that receives an initializer_list
        if (isListInit()) {
            return getArgs().stream()
                    .map(arg -> arg.getCode())
                    .collect(Collectors.joining(", ", "{", "}"));
        }

        // If is elidable, check that has a single non-default argument and remove
        if (isElidable()) {

            List<Expr> args = getArgs();
            Preconditions.checkArgument(args.size() == 1);
            return args.get(0).getCode();
        }

        String argsCode = getArgsCode();

        return cxxRecordName + argsCode;
    }

    public String getArgsCode() {

        String argsCode = getArgs().stream()
                .map(arg -> arg.getCode())
                .collect(Collectors.joining(", "));

        if (argsCode.isEmpty()) {

            // Nameless temporary object created with the initializer consisting of an empty pair of parentheses
            // https://en.cppreference.com/w/cpp/language/value_initialization
            if (isTemporary()) {
                return "()";
            }

            if (get(REQUIRES_ZERO_INITIALIZATION)) {
                return "()";
            }

            // Non-static data member or a base class initialized using a member initializer with an empty pair of
            // parentheses
            return "";
        }

        return "(" + argsCode + ")";
    }

    @Override
    public ValueKind getValueKind() {
        // As default, return r-value
        return ValueKind.R_VALUE;
    }

    protected boolean isTemporary() {
        return false;
    }

    @Override
    public SpecsList<DataKey<?>> getSignatureKeys() {
        SpecsList<DataKey<?>> list = super.getSignatureKeys().andAdd(IS_ELIDABLE);

        // If temporary constructor, add ID in order to "ignore" it during normalization
        if (get(IS_TEMPORARY_OBJECT)) {
            list.add(ID);
        }

        return list;
    }

    @Override
    public Optional<Decl> getDecl() {
        return Optional.of(get(CONSTRUCTOR_DECL));
    }

}
