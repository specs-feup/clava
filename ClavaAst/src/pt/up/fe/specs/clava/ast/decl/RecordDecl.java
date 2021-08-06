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
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.attr.Attribute;

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
        return getCode("");
    }

    protected String getCode(String bases) {
        boolean addNewlines = addNewLines();

        StringBuilder code = new StringBuilder();

        if (addNewlines) {
            code.append(ln());
        }

        code.append(getTagKind().getCode());

        // Add attributes
        // recordDeclData.getAttributes().forEach(attr -> code.append(" ").append(attr.getCode()));

        // String preAttributesCode = recordDeclData.getAttributes().stream()
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

        // Append bases
        code.append(bases);

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

        code.append(";");

        if (addNewlines) {
            code.append(ln());
        }

        // System.out.println("CXXRECORD CODE:\n" + code);
        // System.out.println("HAS DECL NAME: " + hasDeclName());
        // System.out.println("DECL NAME: " + getDeclName());

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

        // String membersCode = getRecordFields().stream()
        // .map(decl -> decl.getCode())
        String membersCode = getChildrenStream()
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

    @Override
    public String getDeclName() {
        // If anonymous, create name
        if (get(IS_ANONYMOUS)) {
            return "anonymous_" + get(ID);
        }

        return super.getDeclName();
    }

    public void addField(FieldDecl field) {
        addChild(field);
    }
}
