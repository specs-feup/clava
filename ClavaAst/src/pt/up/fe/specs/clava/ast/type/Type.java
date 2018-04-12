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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.data2.TypeDataV2;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

/**
 * The base class of the type hierarchy.
 *
 * <p>
 * Type instances are immutable, setters should return a copy of the node with the modified attribute.
 *
 * @author JoaoBispo
 *
 */
public abstract class Type extends ClavaNode {

    private TypeData data;
    // private Supplier<App> app;
    // private App app;

    public Type(TypeData data, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);

        this.data = data;
        // app = () -> null;
        // app = null;
    }

    public Type(TypeDataV2 dataV2, Collection<? extends ClavaNode> children) {
        super(dataV2, children);
    }

    @Override
    public TypeDataV2 getData() {
        return (TypeDataV2) super.getData();
    }

    public TypeData getTypeData() {
        return data;
    }

    // public void setApp(App app) {
    // // setApp(() -> app);
    // this.app = app;
    // }

    // public void setApp(Supplier<App> app) {
    // this.app = app;
    // }

    @Override
    public Type copy() {
        Type copy = (Type) super.copy();

        // Set app
        // copy.app = app;

        return copy;
    }

    /**
     * Helper method which passes null as the name.
     */
    @Override
    public String getCode() {
        return getCode(null);
    }

    /**
     * Type instances getCode() method receive a String (e.g., with the name of a variable), in case this is code to
     * declare something.
     *
     * <p>
     * Accepts null in case there is no name to use.
     */
    public String getCode(String name) {
        // throw new NotImplementedException(getClass());
        if (name == null) {
            return getBareType();
        }

        return getBareType() + " " + name;
        // String nameString = name == null ? "" : name;
        // return getType() + " " + nameString;
    }

    public void setStringType(String stringType) {
        data = new TypeData(stringType, data);
    }

    public String getBareType() {
        return data.getBareType();
    }

    @Override
    public String toContentString() {
        if (getData() != null) {
            return super.toContentString();
        }
        return super.toContentString() + getCode();
    }

    /**
     * By default returns false.
     *
     * @return true if type is considered anonymous (e.g., anonymous struct)
     */
    // public boolean isAnonymous() {
    // return getCode().contains("(anonymous ");
    // // return false;
    // }

    /**
     * Code for a literal constant (e.g., 1u when unsigned)
     *
     * @param constant
     * @return
     */
    public String getConstantCode(String constant) {
        return constant;
    }

    /**
     * By default, if has sugar, returns the desugared implementation of template args. Otherwise, returns an empty
     * list.
     *
     * @return
     */
    public List<String> getTemplateArgumentStrings() {
        if (!hasSugar()) {
            return Collections.emptyList();
        }

        return desugar().getTemplateArgumentStrings();
    }

    public List<Type> getTemplateArgumentTypes() {
        if (!hasSugar()) {
            return Collections.emptyList();
        }

        return desugar().getTemplateArgumentTypes();
    }

    /**
     * Sets all argument types of the template.
     * 
     * @param newTemplateArgTypes
     */
    public void setTemplateArgumentTypes(List<Type> newTemplateArgTypes) {
        // If no sugar, do nothing
        if (!hasSugar()) {
            return;
        }

        desugar().setTemplateArgumentTypes(newTemplateArgTypes);

        Types.updateSugaredType(this);
    }

    /**
     * Sets a single argument type of the template. Cannot use an index larger than the number of original arguments.
     * 
     * @param index
     * @param newTemplateArgType
     */
    public void setTemplateArgumentType(int index, Type newTemplateArgType) {
        // If no sugar, do nothing
        if (!hasSugar()) {
            return;
        }

        desugar().setTemplateArgumentType(index, newTemplateArgType);

        Types.updateSugaredType(this);
    }

    /**
     *
     *
     * @return true if there are template arguments, false otherise
     */
    public boolean hasTemplateArgs() {
        return !getTemplateArgumentStrings().isEmpty();
    }

    public boolean hasTemplateArgTypes() {
        return !getTemplateArgumentTypes().isEmpty();
    }

    /**
     * 
     * @return true if this type updated its template argument types, false otherwise
     */
    public boolean hasUpdatedTemplateArgTypes() {
        // If no sugar, return false
        if (!hasSugar()) {
            // System.out.println("NO SUGAR:" + this.getClass().getSimpleName());
            return false;
        }
        // System.out.println("DESUGARING:" + this.getClass().getSimpleName());
        return desugar().hasUpdatedTemplateArgTypes();
    }

    public boolean hasSugar() {
        return getTypeData().hasSugar();
    }

    /**
     * Desugars a type until it finds a Type of the given class.
     * 
     * <p>
     * TODO: Should return self if given type if the same type as this?
     * 
     * @param typeClass
     * @return
     */
    public <T extends Type> Optional<T> desugarToTry(Class<T> typeClass) {
        // If no sugar, return
        if (!hasSugar()) {
            return Optional.empty();
        }

        Type desugared = desugar();

        if (!typeClass.isInstance(desugared)) {
            return desugared.desugarToTry(typeClass);
        }

        return Optional.of(typeClass.cast(desugared));

        /*
        // Check if current type is the asked type
        if (typeClass.isInstance(this)) {
        return Optional.of(typeClass.cast(this));
        }
        
        // If is sugared, desugar and call again
        if (hasSugar()) {
        return desugar().desugar(typeClass);
        }
        
        return Optional.empty();
        */
    }

    public <T extends Type> T desugarTo(Class<T> typeClass) {
        return desugarToTry(typeClass)
                .orElseThrow(() -> new RuntimeException("Could not desugar to type '" + typeClass + "':\n" + this));
    }

    public final Type desugar() {
        if (!getTypeData().hasSugar()) {
            return this;
        }

        return desugarImpl();
    }

    protected Type desugarImpl() {
        throw new NotImplementedException(getClass());
    }

    public final void setDesugar(Type desugaredType) {
        if (!hasSugar()) {
            throw new RuntimeException("Type does not have sugar:" + this);
        }

        setDesugarImpl(desugaredType);
    }

    protected void setDesugarImpl(Type desugaredType) {
        throw new NotImplementedException(getClass());
    }

    /*
    @Override
    public App getApp() {
        // App appNode = app.get();
        // if (appNode != null) {
        // return appNode;
        // }
    
        // If app not null, return it
        if (app != null) {
            return app;
        }
    
        // Return app of parent node
        if (hasParent()) {
            return getParent().getApp();
        }
    
        throw new RuntimeException(
                "Could not find an 'App' node associated with this type (id: " + getExtendedId().orElse(null) + ")");
    }
    */

    /**
     *
     * @return true if this type is const-qualified, false otherwise
     */
    public boolean isConst() {
        return false;
    }

    public boolean isArray() {
        return false;
    }

    public <T extends Type> T to(Class<T> type) {
        return toTry(type).orElseThrow(
                () -> new RuntimeException("Could not convert type '" + getClass() + "' to '" + type + "':\n" + this));
    }

    /**
     * Goes down the type tree looking for the given type. If a node has more than one child, descending stops.
     *
     * @param type
     * @return
     */
    public <T extends Type> Optional<T> toTry(Class<T> type) {
        if (type.isInstance(this)) {
            return Optional.of(type.cast(this));
        }

        // Continue if there is one child
        if (getNumChildren() == 1) {
            return ((Type) getChild(0)).toTry(type);
        }

        if (this instanceof AttributedType) {
            return ((AttributedType) this).getModifiedType().toTry(type);
        }

        // Stop, can go no further
        // SpecsLogs.msgWarn("Could not find type '" + type + "'");
        return Optional.empty();
    }

    @Override
    public int hashCode() {
        return getCode().hashCode();
        // final int prime = 31;
        // int result = prime;
        // result = prime * result + ((data == null) ? 0 : data.hashCode());
        // return result;
    }

    @Override
    public boolean equals(Object obj) {

        // if (this == obj) {
        // return true;
        // }
        //
        // if (!super.equals(obj)) {
        // return false;
        // }
        //
        // if (getClass() != obj.getClass()) {
        // return false;
        // }
        // Type other = (Type) obj;

        if (!(obj instanceof Type)) {
            return false;
        }
        // System.out.println("THIS CODE:" + getCode());
        // System.out.println("OTHER CODE:" + ((Type) obj).getCode());
        return getCode().equals(((Type) obj).getCode());
        /*
        
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        return true;
        */
    }

    /*
    @Override
    protected void setData(ClavaData data) {
        throw new RuntimeException(".setData() not allowed for Type nodes, they are considered immutable");
    }
    
    @Override
    public void setId(String newId) {
        throw new RuntimeException(".setId() not allowed for Type nodes, they are considered immutable");
    }
    */

}
