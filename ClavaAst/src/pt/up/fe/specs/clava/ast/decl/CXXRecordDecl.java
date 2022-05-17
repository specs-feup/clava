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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.data.CXXBaseSpecifier;
import pt.up.fe.specs.util.SpecsLogs;

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

    /// DATAKEYS END

    public CXXRecordDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        // String bases = getBasesCode();

        return super.getCode(get(RECORD_BASES));
    }

    // private String getBasesCode() {
    // // String bases = getRecordBases().stream()
    // String bases = get(RECORD_BASES).stream()
    // .map(recordBase -> recordBase.getCode(this))
    // .collect(Collectors.joining(", "));
    //
    // bases = bases.isEmpty() ? bases : " : " + bases;
    // return bases;
    // }

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

    public List<CXXRecordDecl> getBases() {
        return get(RECORD_BASES).stream()
                .map(baseSpec -> baseSpec.getBaseDecl(this))
                .filter(decl -> {
                    if (!(decl instanceof CXXRecordDecl)) {
                        SpecsLogs.info("Found base that is not a CXXRecordDecl, check how to handle this case (is a "
                                + decl.getClass() + ")");
                        return false;
                    }
                    return true;
                })
                .map(CXXRecordDecl.class::cast)
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

    /**
     * All bases, including the ones from other the bases, if any.
     * 
     * @return
     */
    public List<CXXRecordDecl> getAllBases() {
        Set<CXXRecordDecl> currentBases = new LinkedHashSet<>();

        getAllBases(this, currentBases);

        return new ArrayList<>(currentBases);
    }

    private static void getAllBases(CXXRecordDecl aClass, Set<CXXRecordDecl> currentBases) {
        var bases = aClass.getBases();

        // Add all bases from this class
        currentBases.addAll(bases);

        // Add all methods from the bases
        for (var base : bases) {
            getAllBases(base, currentBases);
        }
    }

    /**
     * All methods, including the ones from the bases, if any.
     * 
     * @return
     */
    public List<CXXMethodDecl> getAllMethods(boolean includeOverridenMethods) {
        // Uses signature to identify methods
        // Map<String, CXXMethodDecl> allMethods = new LinkedHashMap<>();

        Set<CXXMethodDecl> allMethods = new LinkedHashSet<>();

        // Add own methods
        addMethods(getMethods(), allMethods);
        // allMethods.addAll(getMethods());

        var allBases = getAllBases();

        for (var base : allBases) {
            addMethods(base.getMethods(), allMethods);
            // allMethods.addAll(base.getMethods());
        }

        // Exclude overridden methods
        if (!includeOverridenMethods) {
            Set<CXXMethodDecl> overridenMethods = new HashSet<>();

            // Collect overridden methods
            for (var method : allMethods) {
                overridenMethods.addAll(method.get(CXXMethodDecl.OVERRIDDEN_METHODS));
            }

            // Exclude overriden methods
            allMethods.removeAll(overridenMethods);
        }

        // return new ArrayList<>(allMethods.values());
        return new ArrayList<>(allMethods);
    }

    private void addMethods(List<CXXMethodDecl> methods, Set<CXXMethodDecl> allMethods) {
        for (var method : methods) {

            if (allMethods.contains(method)) {
                SpecsLogs.debug(
                        () -> "CXXRecordDecl.addMethods: skipping method with signature '" + method.getSignature()
                                + "', already present");
                continue;
            }

            allMethods.add(method);
        }

    }

    /*
    private void addMethods(List<CXXMethodDecl> methods, Map<String, CXXMethodDecl> allMethods) {
        for (var method : methods) {
            var signature = method.getSignature();
    
            if (allMethods.containsKey(signature)) {
                SpecsLogs.debug(() -> "CXXRecordDecl.addMethods: skipping method, signature '" + signature
                        + "' already present");
                continue;
            }
    
            allMethods.put(signature, method);
        }
    
    }
    */
    /**
     * 
     * 
     * @return true, if contains at least a pure function.
     */
    public boolean isAbstract() {
        return getAllMethods(false).stream()
                .filter(method -> method.get(FunctionDecl.IS_PURE))
                .findFirst()
                .isPresent();
    }

    /**
     * 
     * 
     * @return true, if all functions are pure (not counting destructors).
     */
    public boolean isInterface() {
        for (var method : getAllMethods(false)) {
            // Check if destructor
            if (method instanceof CXXDestructorDecl) {
                // System.out.println("FOUND DESTRUCTOR: " + method.getSignature());
                continue;
            }

            // Check if pure
            if (method.get(FunctionDecl.IS_PURE)) {
                // System.out.println("FOUND PURE: " + method.getSignature());
                continue;
            }

            // System.out.println("NEITHER DESTRUCTOR OR PURE: " + method.getSignature());
            return false;
        }

        return true;

        // // If at least one non-pure, return false
        // boolean hasNonPure = getAllMethods(false).stream()
        // .filter(method -> !method.get(FunctionDecl.IS_PURE))
        // .findFirst()
        // .isPresent();
        //
        // return !hasNonPure;
    }

    // private static void getAllMethods(CXXRecordDecl aClass, Set<CXXMethodDecl> currentMethods) {
    // // Add all methods from this class
    // currentMethods.addAll(aClass.getMethods());
    //
    // // Add all methods from the bases
    // for (var base : aClass.getBases()) {
    // var cxxRecord = base;
    // getAllMethods(cxxRecord, currentMethods);
    // }
    // }

}
