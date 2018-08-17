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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.stmt.enums.DeclStmtType;

public class DeclStmt extends Stmt {

    /// DATAKEYS BEGIN

    public final static DataKey<DeclStmtType> DECL_STMT_TYPE = KeyFactory
            .enumeration("declStmtType", DeclStmtType.class);

    public final static DataKey<Boolean> HAS_SEMICOLON = KeyFactory.bool("hasSemicolon")
            .setDefault(() -> true);

    /// DATAKEYS END

    // private final DeclStmtType type;
    // private final boolean hasSemicolon;

    public DeclStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
        /*
        // Filter out nodes that are not Decl
        super(data, children.stream()
                .filter(child -> child instanceof Decl)
                .collect(Collectors.toList()));
        
        // Filter out nodes that are not Decl
        List<ClavaNode> declChildren = getChildren();
        // List<ClavaNode> declChildren = children.stream()
        // .filter(child -> child instanceof Decl)
        // .collect(Collectors.toList());
        */
        /*
        List<ClavaNode> nonDeclChildren = children.stream()
                .filter(child -> !(child instanceof Decl))
                .collect(Collectors.toList());
        
        System.out.println("NON DECL CHILDREN:" + ClavaNode.toTree(nonDeclChildren));
        */
        // Determine Decl type
        boolean isRecordDecl = children.stream()
                .findFirst()
                .filter(child -> child instanceof RecordDecl)
                .isPresent();

        // System.out.println("DECLSTMT CHILDREN:" + children);
        // System.out.println("IS RECORD DECL:" + isRecordDecl);
        // TODO: If RecordDecl, verify if remaining children are VarDecl nodes?

        DeclStmtType declStmtType = isRecordDecl ? DeclStmtType.RECORD_DECL : DeclStmtType.DECL_LIST;

        set(DECL_STMT_TYPE, declStmtType);

        // Check if children after the first are VarDecl
        if (isRecordDecl) {
            // put(HAS_SEMICOLON, true);

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

        // No-semicolon support only when only one child is present (and when is not RecordDecl type)
        // if (!get(HAS_SEMICOLON) && children.size() > 1) {
        // put(HAS_SEMICOLON, true);
        // }

        // Check number of children and semicolon
        // if (!get(HAS_SEMICOLON)) {
        // Preconditions.checkArgument(children.size() == 1,
        // "No-semicolon support only when only one child is present, has '" + children.size() + "':\n"
        // + children);
        // }

        // If any of the children have associated comments, "move" them up to this statement
        children.stream()
                .flatMap(child -> child.removeInlineComments().stream())
                .forEach(this::associateComment);

    }
    /*
    public DeclStmt(ClavaNodeInfo info, RecordDecl recordDecl, List<VarDecl> varDecls) {
        this(new LegacyToDataStore().setNodeInfo(info).getData(), SpecsCollections.concat(recordDecl, varDecls));
        // this(DeclStmtType.RECORD_DECL, true, info, SpecsCollections.concat(recordDecl, varDecls));
    }
    */

    /*
    public DeclStmt(boolean hasSemicolon, ClavaNodeInfo info, NamedDecl decl) {
        this(new LegacyToDataStore()
                .setNodeInfo(info)
                .set(HAS_SEMICOLON, hasSemicolon)
                .getData(),
                Arrays.asList(decl));
    
        // this(DeclStmtType.DECL_LIST, hasSemicolon, info, Arrays.asList(decl));
    }
    */
    /*
    public DeclStmt(ClavaNodeInfo info, List<NamedDecl> decls) {
        // Not sure if all children must be VarDecl, up until now they have been
        // Answer: No, they are not always VarDecl, they can also be a RecordDecl as the first child
    
        // Check if the children greater than one are always VarDecl
        this(new LegacyToDataStore().setNodeInfo(info).set(HAS_SEMICOLON, true).getData(), decls);
        // this(DeclStmtType.DECL_LIST, true, info, decls);
    }
    */
    /*
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
    */
    /*
    @Override
    protected ClavaNode copyPrivate() {
        return new DeclStmt(getInfo(), Collections.emptyList());
    }
    */

    public List<Decl> getDecls() {
        return getChildren(Decl.class);
    }

    /*
    public DeclStmtType getType() {
        return type;
    }
    */

    public List<VarDecl> getVarDecls() {
        Preconditions.checkArgument(get(DECL_STMT_TYPE) == DeclStmtType.DECL_LIST,
                "Only supported for DeclStmt of type " + DeclStmtType.DECL_LIST);

        return getChildren(VarDecl.class);
    }

    @Override
    public String getCode() {
        switch (get(DECL_STMT_TYPE)) {
        case DECL_LIST:
            String code = getCodeDeclList();
            // System.out.println("FINAL CODE:\n" + code);
            return code;
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
            // System.out.println("NO SEMI");
            // return getChildren(NamedDecl.class).get(0).getCode();
            return getChildren(Decl.class).get(0).getCode();
        }

        // All elements are Decls
        // List<NamedDecl> decls = getChildren(NamedDecl.class);
        List<Decl> decls = getChildren(Decl.class);

        // Write code of first type, add code of next types without variable declaration
        // System.out.println("DECLS:");
        // for (Decl decl : decls) {
        // System.out.println("asdasd");
        // System.out.println("DECL CLASS:" + decl.getClass());
        // System.out.println(decl.getCode());
        // }

        // StringBuilder code = new StringBuilder();
        String code = decls.get(0).getCode();

        // String firstDecl = decls.get(0).getCode();
        // code.append(firstDecl);
        // if(!firstDecl.endsWith(";")) {
        // code.append(";");
        // }
        for (int i = 1; i < decls.size(); i++) {
            if (!code.trim().endsWith(";")) {
                code += ";";
            }

            code += ln() + decls.get(i).getCode();
        }

        // String code = decls.stream()
        // .map(decl -> decl.getCode())
        // // .filter(code -> !code.isEmpty())
        // // .collect(Collectors.joining(";" + ln(), "", ";"));
        // .collect(Collectors.joining(";" + ln()));

        // System.out.println("CODE TRIM:" + code.trim());
        if (!code.trim().endsWith(";")) {
            // System.out.println("ADDING ;");
            code += ";";
        }
        // System.out.println("FINAL CODE:" + code);
        return code;
    }

    public DeclStmt setHasSemicolon(boolean hasSemicolon) {
        set(HAS_SEMICOLON, hasSemicolon);
        return this;
    }

}
