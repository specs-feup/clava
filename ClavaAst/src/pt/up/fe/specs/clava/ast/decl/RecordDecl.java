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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.RecordDeclData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.TagKind;
import pt.up.fe.specs.util.SpecsCollections;

/**
 * Common class of struct, union and class.
 * 
 * @author JoaoBispo
 *
 */
public class RecordDecl extends TagDecl {

    private final RecordDeclData recordDeclData;

    public RecordDecl(RecordDeclData recordDeclData, Type type, DeclData declData, ClavaNodeInfo info,
            List<? extends Decl> decls) {

        this(recordDeclData, type, declData, info, SpecsCollections.cast(decls, ClavaNode.class));
    }

    protected RecordDecl(RecordDeclData recordDeclData, Type type, DeclData declData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(type, declData, info, children);

        this.recordDeclData = recordDeclData;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new RecordDecl(recordDeclData.copy(), getType(),
                getDeclData(), getInfo(), Collections.emptyList());
    }

    public RecordDeclData getRecordDeclData() {
        return recordDeclData;
    }

    @Override
    public String getDeclName() {
        return recordDeclData.getRecordName();
    }

    @Override
    public boolean hasDeclName() {
        return true;
    }

    @Override
    public TagKind getTagKind() {
        return recordDeclData.getRecordKind();
    }

    /*
    public void addVarDecl(VarDecl var) {
    if (getTagKind() != TagKind.STRUCT && getTagKind() != TagKind.UNION) {
        LoggingUtils
    	    .msgWarn("Method not supported for " + getTagKind() + " only supported for structs and unions");
        return;
    }
    
    // Add child
    addChild(var);
    
    // Increment counter
    numVarDecl++;
    }
    */

    public List<FieldDecl> getFields() {
        return getChildrenOf(FieldDecl.class);
    }

    public List<Decl> getRecordFields() {
        return getChildrenOf(Decl.class);
        /*
        	int endIndex = numChildren() - numVarDecl;
        
        	return CollectionUtils.cast(Decl.class, getChildren().subList(0, endIndex));
        	*/
    }

    /**
     * Unions and structs can declare variables.
     * 
     * @return
     */
    // public List<VarDecl> getVarDecls() {
    //
    // if (numVarDecl == 0) {
    // return Collections.emptyList();
    // }
    //
    // int startIndex = numChildren() - numVarDecl;
    //
    // return CollectionUtils.cast(VarDecl.class, CollectionUtils.subList(getChildren(), startIndex));
    // }

    @Override
    public String getCode() {
        return getCode("");
    }

    protected String getCode(String bases) {
        StringBuilder code = new StringBuilder();
        code.append(ln()).append(getTagKind().getCode());
        if (hasDeclName()) {
            code.append(" ").append(getDeclName());
        }

        // Append bases
        code.append(bases);

        // Add attributes
        recordDeclData.getAttributes().forEach(attr -> code.append(" ").append(attr.getCode()));

        if (recordDeclData.isCompleteDefinition()) {
            code.append(getDefinitionCode());
        }

        code.append(";" + ln());

        return code.toString();
    }

    protected String getDefinitionCode() {
        StringBuilder code = new StringBuilder();

        code.append(" {" + ln());

        // String membersCode = getRecordFields().stream()
        // .map(decl -> decl.getCode())
        String membersCode = getChildrenStream()
                .map(child -> child.getCode())
                .collect(Collectors.joining(ln()));

        code.append(indentCode(membersCode));
        code.append("}");

        return code.toString();
    }

}
