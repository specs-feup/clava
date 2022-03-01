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

package pt.up.fe.specs.clava.ast.stmt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.stmt.enums.DeclStmtType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.util.SpecsStrings;

/**
 * A statement that represents a declaration.
 * 
 * @author JBispo
 *
 */
public class DeclStmt extends Stmt {

    /// DATAKEYS BEGIN

    public final static DataKey<DeclStmtType> DECL_STMT_TYPE = KeyFactory
            .enumeration("declStmtType", DeclStmtType.class);

    public final static DataKey<Boolean> HAS_SEMICOLON = KeyFactory.bool("hasSemicolon")
            .setDefault(() -> true);

    /**
     * If true, forces single line declaration of initializations
     */
    public final static DataKey<Boolean> FORCE_SINGLE_LINE = KeyFactory.bool("forceSingleLine")
            .setDefault(() -> false);

    /// DATAKEYS END

    public DeclStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

        // Determine Decl type
        boolean isRecordDecl = children.stream()
                .findFirst()
                .filter(child -> child instanceof RecordDecl)
                .isPresent();

        // TODO: If RecordDecl, verify if remaining children are VarDecl nodes?

        DeclStmtType declStmtType = isRecordDecl ? DeclStmtType.RECORD_DECL : DeclStmtType.DECL_LIST;

        set(DECL_STMT_TYPE, declStmtType);

        // Check if children after the first are VarDecl
        if (isRecordDecl) {

            boolean childrenNotVarDecl = children.stream()
                    .skip(1)
                    .filter(child -> !(child instanceof VarDecl))
                    .findFirst()
                    .isPresent();

            if (childrenNotVarDecl) {
                throw new RuntimeException(
                        "Expected all children after first to be VarDecl, but found this:\n" + children);
            }
        }

        // If any of the children have associated comments, "move" them up to this statement
        children.stream()
                .flatMap(child -> child.removeInlineComments().stream())
                .forEach(this::associateComment);

    }

    public List<Decl> getDecls() {
        return getChildren(Decl.class);
    }

    public List<VarDecl> getVarDecls() {
        Preconditions.checkArgument(get(DECL_STMT_TYPE) == DeclStmtType.DECL_LIST,
                "Only supported for DeclStmt of type " + DeclStmtType.DECL_LIST);

        return getChildren(VarDecl.class);
    }

    @Override
    public String getCode() {
        switch (get(DECL_STMT_TYPE)) {
        case DECL_LIST:
            return getCodeDeclList();
        case RECORD_DECL:
            return getCodeRecordDecl();
        default:
            throw new RuntimeException("Case not defined:" + get(DECL_STMT_TYPE));
        }
    }

    public String getCodeRecordDecl() {

        // First element is a RecordDecl
        RecordDecl record = getChild(RecordDecl.class, 0);

        // Remaining elements are VarDecls
        List<VarDecl> varDecls = getChildren(VarDecl.class, 1);

        // Write code of the Record as a statement
        StringBuilder code = new StringBuilder();
        code.append(record.getCode());

        // Write each VarDecl as a statement
        for (VarDecl varDecl : varDecls) {
            code.append(varDecl.getCode()).append(";" + ln());
        }

        return code.toString();

    }

    public String getCodeDeclList() {

        // If no semicolon, can only have one decl
        if (!get(HAS_SEMICOLON)) {
            return getChildren(Decl.class).get(0).getCode();
        }

        // All elements are Decls with types
        List<Decl> decls = getChildren(Decl.class);

        boolean singleLineDecl = isSingleLineDecl(decls);

        String code = decls.get(0).getCode();

        // Write code of first type, add code of next types without variable declaration
        if (singleLineDecl) {
            for (int i = 1; i < decls.size(); i++) {
                NamedDecl decl = (NamedDecl) decls.get(i);
                code += ", ";
                code += getPointerPrefix(decl);
                code += decl.getTypelessCode();
            }
        }
        // Otherwise, write a single statement per declaration
        else {
            for (int i = 1; i < decls.size(); i++) {
                if (!code.trim().endsWith(";")) {
                    code += ";";
                }

                code += ln() + decls.get(i).getCode();
            }
        }

        if (!code.trim().endsWith(";")) {
            code += ";";
        }

        return code;
    }

    private String getPointerPrefix(Decl decl) {
        if (!(decl instanceof Typable)) {
            return "";
        }

        Type declType = ((Typable) decl).getType();
        int pointerArity = Types.getPointerArity(declType);

        String pointerPrefix = SpecsStrings.buildLine("*", pointerArity);
        return pointerPrefix;

    }

    private boolean isSingleLineDecl(List<Decl> decls) {

        if (get(FORCE_SINGLE_LINE)) {
            return true;
        }

        // If not all decls have a type, return false
        List<Typable> typables = decls.stream()
                .filter(decl -> decl instanceof Typable)
                .map(Typable.class::cast)
                .collect(Collectors.toList());

        if (typables.size() != decls.size()) {
            return false;
        }

        Type firstType = typables.get(0).getType();

        // If array, return false
        if (firstType.isArray()) {
            return false;
        }

        boolean isMultiLine = typables.stream()
                .filter(typable -> !typable.getType().equals(firstType))
                .findFirst()
                .isPresent();

        return !isMultiLine;
    }

    public DeclStmt setHasSemicolon(boolean hasSemicolon) {
        set(HAS_SEMICOLON, hasSemicolon);
        return this;
    }

}
