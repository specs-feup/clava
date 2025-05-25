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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.decl.data.CXXBaseSpecifier;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;

/**
 * Common class of struct, union and class.
 * 
 * @author JoaoBispo
 *
 */
public class RecordDecl extends TagDecl {

    /// DATAKEYS BEGIN

    /**
     * True if this is an anonymous struct or union.
     * 
     * <p>
     * To be an anonymous struct or union, it must have been declared without a name and there must be no objects of
     * this type declared.
     * 
     */
    public final static DataKey<Boolean> IS_ANONYMOUS = KeyFactory.bool("isAnonymous");

    /// DATAKEYS END

    public RecordDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public List<FieldDecl> getFields() {
        return getChildrenOf(FieldDecl.class);
    }

    public List<Decl> getRecordFields() {
        return getChildrenOf(Decl.class);
    }

    @Override
    public String getCode() {
        return getCode(Collections.emptyList());
    }

    protected String getCode(List<CXXBaseSpecifier> bases) {
        return getCode(bases, Collections.emptyList());
    }

    protected String getCode(List<CXXBaseSpecifier> bases, List<TemplateArgument> templateArgs) {
        boolean addNewlines = addNewLines();

        StringBuilder code = new StringBuilder();

        if (addNewlines) {
            code.append(ln());
        }

        code.append(getTagDeclVarsQualifiersCode());

        code.append(getTagKind().getCode());

        // Add attributes
        // var preAttributes = get(ATTRIBUTES).stream()
        // .filter(attr -> !attr.isPostAttr())
        // .collect(Collectors.toList());
        //
        // String preAttributesCode = getAttributesCode(preAttributes, false, false);

        String preAttributesCode = get(ATTRIBUTES).stream()
                .filter(attr -> !attr.isPostAttr())
                .map(Attribute::getCode)
                .collect(Collectors.joining(" "));

        // Append pre-attributes
        if (!preAttributesCode.isEmpty()) {
            code.append(" ").append(preAttributesCode);
        }

        if (hasDeclName()) {
            code.append(" ").append(getDeclName());
        }

        if (!templateArgs.isEmpty()) {
            var templateArgsCode = TemplateArgument.getCode(templateArgs, this);
            code.append(templateArgsCode);
        }

        // Append bases
        if (!bases.isEmpty()) {
            String basesCode = bases.stream()
                    .map(recordBase -> recordBase.getCode(this))
                    .collect(Collectors.joining(", "));

            basesCode = basesCode.isEmpty() ? basesCode : " : " + basesCode;
            code.append(basesCode);
        }

        // if (recordDeclData.isCompleteDefinition()) {
        if (get(IS_COMPLETE_DEFINITION)) {
            code.append(getDefinitionCode());
        }

        // String postAttributesCode = recordDeclData.getAttributes().stream()
        String postAttributesCode = get(ATTRIBUTES).stream()
                .filter(attr -> attr.isPostAttr())
                .map(Attribute::getCode)
                .collect(Collectors.joining(" "));

        // Append post-attributes
        if (!postAttributesCode.isEmpty()) {
            code.append(" ").append(postAttributesCode);
        }

        // TODO: confirm this is the correct place
        code.append(getDeclsString());

        code.append(";");

        if (addNewlines) {
            code.append(ln());
        }

        return code.toString();
    }

    private boolean addNewLines() {

        // If inside a Template declaration, do not add new lines
        var parent = getParent();

        if (parent instanceof TemplateDecl) {
            return false;
        }

        return true;
    }

    protected String getDefinitionCode() {
        StringBuilder code = new StringBuilder();

        code.append(" {" + ln());

        String membersCode = getChildrenWithCode().stream()
                .map(child -> child.getCode())
                .collect(Collectors.joining(ln()));

        code.append(indentCode(membersCode));
        code.append("}");

        return code.toString();
    }

    public List<FunctionDecl> getFunction(String functionName) {
        List<FunctionDecl> functions = new ArrayList<>();

        for (Decl recordField : getRecordFields()) {
            if (!(recordField instanceof FunctionDecl)) {
                continue;
            }

            FunctionDecl functionDecl = (FunctionDecl) recordField;
            if (!functionDecl.hasDeclName()) {
                continue;
            }

            String declName = functionDecl.getDeclName();

            if (!declName.equals(functionName)) {
                continue;
            }

            functions.add(functionDecl);
        }

        return functions;
    }

    public List<FunctionDecl> getFunctions() {
        List<FunctionDecl> functions = new ArrayList<>();

        for (Decl recordField : getRecordFields()) {
            if (!(recordField instanceof FunctionDecl)) {
                continue;
            }

            FunctionDecl functionDecl = (FunctionDecl) recordField;

            functions.add(functionDecl);
        }

        return functions;
    }

    public void addField(FieldDecl field) {
        addChild(field);
    }
}
