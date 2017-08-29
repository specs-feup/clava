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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.util.SpecsCollections;

public class DeclStmt extends Stmt {

    private static enum DeclStmtType {
        DECL_LIST,
        RECORD_DECL;
    }

    private final DeclStmtType type;
    private final boolean hasSemicolon;

    public DeclStmt(ClavaNodeInfo info, RecordDecl recordDecl, List<VarDecl> varDecls) {
        this(DeclStmtType.RECORD_DECL, true, info, SpecsCollections.concat(recordDecl, varDecls));
    }

    public DeclStmt(boolean hasSemicolon, ClavaNodeInfo info, NamedDecl decl) {
        this(DeclStmtType.DECL_LIST, false, info, Arrays.asList(decl));
    }

    public DeclStmt(ClavaNodeInfo info, List<NamedDecl> decls) {
        // Not sure if all children must be VarDecl, up until now they have been
        // Answer: No, they are not always VarDecl, they can also be a RecordDecl as the first child

        // Check if the children greater than one are always VarDecl
        this(DeclStmtType.DECL_LIST, true, info, decls);
    }

    private DeclStmt(DeclStmtType type, boolean hasSemicolon, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(info, children);

        // Check RECORD_DECL and semicolon
        if (type == DeclStmtType.RECORD_DECL) {
            Preconditions.checkArgument(hasSemicolon,
                    "DeclStmtType '" + DeclStmtType.RECORD_DECL + "' requires semicolon");
        }

        // Check number of children and semicolon
        if (!hasSemicolon) {
            Preconditions.checkArgument(children.size() == 1,
                    "No-semicolon support only when only one child is present");
        }

        this.type = type;
        this.hasSemicolon = hasSemicolon;

        // If any of the children have associated comments, "move" them up to this statement
        children.stream()
                .flatMap(child -> child.removeInlineComments().stream())
                .forEach(this::associateComment);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new DeclStmt(getInfo(), Collections.emptyList());
    }

    public List<Decl> getDecls() {
        return getChildren(Decl.class);
    }

    public DeclStmtType getType() {
        return type;
    }

    public List<VarDecl> getVarDecls() {
        Preconditions.checkArgument(type == DeclStmtType.DECL_LIST,
                "Only supported for DeclStmt of type " + DeclStmtType.DECL_LIST);

        return getChildren(VarDecl.class);
    }

    @Override
    public String getCode() {
        switch (type) {
        case DECL_LIST:
            return getCodeDeclList();
        case RECORD_DECL:
            return getCodeRecordDecl();
        default:
            throw new RuntimeException("Case not defined:" + type);
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
        if (!hasSemicolon) {
            return getChildren(NamedDecl.class).get(0).getCode();
        }

        // All elements are VarDecls
        List<NamedDecl> decls = getChildren(NamedDecl.class);

        // Write code of first type, add code of next types without variable declaration

        return decls.stream()
                .map(decl -> decl.getCode())
                .collect(Collectors.joining(";" + ln(), "", ";"));

    }

}
