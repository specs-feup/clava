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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.data.ExplicitSpecifier;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.CXXCtorInitializer;
import pt.up.fe.specs.clava.ast.stmt.CXXTryStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

/**
 * Represents a C++ constructor declaration or definition.
 * 
 * <p>
 * Structure of children:<br>
 * - Parameters (ParmVarDecl)<br>
 * - Definition (Stmt)
 * 
 * @author JoaoBispo
 *
 */
public class CXXConstructorDecl extends CXXConstructorDeclBase {

    // Data declarations are generated from the wire schema in CXXConstructorDeclBase.

    public CXXConstructorDecl(DataStore data, Collection<? extends ClavaNode> children, CXXConstructorDeclBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public CXXConstructorDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public List<CXXCtorInitializer> getInitializers() {
        return getWireConstructorInits();
    }

    @Override
    public String getCode() {
        if (get(IS_IMPLICIT)) {
            return "";
        }

        // Special case: try
        Optional<Stmt> body = getFunctionDefinition();
        if (body.isPresent() && body.get() instanceof CXXTryStmt) {
            CXXTryStmt tryBody = (CXXTryStmt) body.get();
            return getDeclarationId(false) + " " + tryBody.getCode(getCodeInitList());
        }

        return getDeclarationId(false) + getCodeInitList() + getCodeBody();
    }

    @Override
    protected String getNameForCode() {
        return getRecordDecl().map(NamedDecl::getDeclName).orElseGet(super::getNameForCode);
    }

    @Override
    public String getCodeInitList() {

        // Check if super is not empty
        String superCode = super.getCodeInitList();
        if (!superCode.isEmpty()) {
            return superCode;
        }

        List<String> initList = getInitializers().stream()
                // Do not take into account default initializers
                // .filter(init -> !(init.get(CXXCtorInitializer.INIT_EXPR) instanceof CXXDefaultInitExpr))
                // Do not take into account default initializers that are not written in the source code
                .filter(init -> init.get(CXXCtorInitializer.IS_WRITTEN))
                .map(init -> init.getCode(this))
                .filter(initCode -> !initCode.isEmpty())
                .collect(Collectors.toList());

        if (initList.isEmpty()) {
            return "";
        }

        return initList.stream()
                .collect(Collectors.joining(", ", " : ", ""));
    }

}
