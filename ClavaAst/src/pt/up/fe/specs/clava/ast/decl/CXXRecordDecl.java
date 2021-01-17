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
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.data.CXXBaseSpecifier;

/**
 * Represents a C++ class.
 * 
 * @author jbispo
 *
 */
public class CXXRecordDecl extends RecordDecl {

    /// DATAKEYS BEGIN

    /**
     * A list of the base specifiers of this CXXRecordDecl.
     */
    public final static DataKey<List<CXXBaseSpecifier>> RECORD_BASES = KeyFactory
            .generic("recordBases", (List<CXXBaseSpecifier>) new ArrayList<CXXBaseSpecifier>());

    public final static DataKey<Boolean> HAS_DEFINITION = KeyFactory.bool("hasDefinition");
    public final static DataKey<Optional<CXXRecordDecl>> RECORD_DEFINITION = KeyFactory.optional("recordDefinition");
    // public final static DataKey<String> RECORD_DEFINITION_ID = KeyFactory.string("recordDefinitionId");

    /// DATAKEYS END

    public CXXRecordDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

        // System.out.println("CXXRECORD '" + get(ID) + " CHILDREN:"
        // + children.stream().map(child -> child.get(ClavaNode.ID)).collect(Collectors.joining(", ")));
        // SpecsLogs.debug("CXXRECORD '" + get(DECL_NAME) + "' CHILDREN:" + children);
        // recordBases = null;
    }

    // private final List<RecordBase> recordBases;

    /*
    public CXXRecordDecl(List<RecordBase> recordBases, RecordDeclData recordDeclData, Type type, DeclData declData,
            ClavaNodeInfo info,
            List<? extends Decl> children) {
    
        this(recordBases, recordDeclData, type, declData, info, SpecsCollections.cast(children, ClavaNode.class));
    }
    
    protected CXXRecordDecl(List<RecordBase> recordBases, RecordDeclData recordDeclData, Type type, DeclData declData,
            ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
    
        super(recordDeclData, type, declData, info, children);
    
        this.recordBases = recordBases;
    }
    
    
    @Override
    protected ClavaNode copyPrivate() {
        return new CXXRecordDecl(new ArrayList<>(recordBases), getRecordDeclData().copy(), getType(), getDeclData(),
                getInfo(), Collections.emptyList());
    }
    */

    // public List<RecordBase> getRecordBases() {
    // return recordBases;
    // }
    /*
    @Override
    public String toContentString() {
        String bases = getRecordBases().stream()
                .map(base -> base.getTypeCode())
                .collect(Collectors.joining(","));
    
        bases = bases.isEmpty() ? "" : ", bases{" + bases + "}";
    
        return super.toContentString() + bases;
    }
    */
    @Override
    public String getCode() {
        // String bases = getRecordBases().stream()
        String bases = get(RECORD_BASES).stream()
                .map(recordBase -> recordBase.getCode(this))
                .collect(Collectors.joining(", "));

        bases = bases.isEmpty() ? bases : " : " + bases;

        return super.getCode(bases);
    }

    public List<CXXMethodDecl> getMethods() {
        return getChildrenOf(CXXMethodDecl.class);
    }

    public List<CXXMethodDecl> getMethod(String methodName) {
        List<FunctionDecl> functions = getFunction(methodName);

        return functions.stream()
                .filter(CXXMethodDecl.class::isInstance)
                .map(CXXMethodDecl.class::cast)
                .collect(Collectors.toList());
    }

    /**
     * Adds a method to a class. If the given method has a definition, creates an equivalent declaration and adds it to
     * the class. In any case, the record of the method is changed to this class.
     * 
     * @param method
     */
    public void addMethod(CXXMethodDecl method) {

        // Set record of method
        method.setRecord(this);

        var methodDeclaration = method;

        // If method has a body, create a declaration based on this method
        if (method.hasBody()) {
            methodDeclaration = (CXXMethodDecl) method.deepCopy();
            methodDeclaration.getBody().get().detach();

            // If declaration is a constructor, remove initializer list, remove it
            if (methodDeclaration instanceof CXXConstructorDecl) {
                methodDeclaration.set(CXXConstructorDecl.CONSTRUCTOR_INITS, new ArrayList<>());
            }
        }

        var methodSig = methodDeclaration.getSignature();

        boolean hasDeclaration = getMethods().stream()
                .map(CXXMethodDecl::getSignature)
                .filter(signature -> signature.equals(methodSig))
                .findFirst()
                .isPresent();

        if (!hasDeclaration) {
            // Add method
            addChild(methodDeclaration);
        }

        // System.out.println("METOHD NAME: " + method.getDeclName());
        // System.out.println("HAS DEF: " + method.hasBody());
        // System.out.println("QUALIFIED PREFIX: " + method.getFullyQualifiedName());

    }

    public List<Decl> getBases() {
        return get(RECORD_BASES).stream()
                .map(baseSpec -> baseSpec.getBaseDecl(this))
                // .map(baseSpec -> baseSpec.get(CXXBaseSpecifier.TYPE).desugarAll().get(TagType.DECL))
                .collect(Collectors.toList());
    }

    /**
     *
     * @return the node representing the declaration of this Record, if it exists
     */
    public Optional<CXXRecordDecl> getDeclaration() {

        // If no body, return immediately
        if (!isCompleteDefinition()) {
            return Optional.of(this);
        }

        // Search for the declaration
        return getAppTry().flatMap(app -> app.getCxxRecordDeclaration(this));

    }

    /**
     *
     * @return the node representing the definition of this Record, if it exists
     */
    public Optional<CXXRecordDecl> getDefinition() {

        // If has body, return immediately
        if (isCompleteDefinition()) {
            return Optional.of(this);
        }

        // Search for the definition
        return getAppTry().flatMap(app -> app.getCxxRecordDefinition(this));
    }

}
