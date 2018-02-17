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
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

/**
 * The base class of the type hierarchy.
 *
 * @author JoaoBispo
 *
 */
public abstract class Type extends ClavaNode {

    private TypeData data;

    public Type(TypeData data, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);

        this.data = data;
    }

    public TypeData getTypeData() {
        return data;
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
        return super.toContentString() + getCode();
    }

    /**
     * By default returns false.
     *
     * @return true if type is considered anonymous (e.g., anonymous struct)
     */
    public boolean isAnonymous() {
        return getCode().contains("(anonymous ");
        // return false;
    }

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
    public List<String> getTemplateArgs() {
        if (!hasSugar()) {
            return Collections.emptyList();
        }

        return desugar().getTemplateArgs();
    }

    public List<Type> getTemplateArgTypes() {
        if (!hasSugar()) {
            return Collections.emptyList();
        }

        return desugar().getTemplateArgTypes();
    }

    /**
     * Sets all argument types of the template.
     * 
     * @param newTemplateArgTypes
     */
    public void setTemplateArgTypes(List<Type> newTemplateArgTypes) {
        // If no sugar, do nothing
        if (!hasSugar()) {
            return;
        }

        desugar().setTemplateArgTypes(newTemplateArgTypes);
    }

    /**
     * Sets a single argument type of the template. Cannot use an index larger than the number of original arguments.
     * 
     * @param index
     * @param newTemplateArgType
     */
    public void setTemplateArgType(int index, Type newTemplateArgType) {
        // If no sugar, do nothing
        if (!hasSugar()) {
            return;
        }

        desugar().setTemplateArgType(index, newTemplateArgType);
    }

    /**
     *
     *
     * @return true if there are template arguments, false otherise
     */
    public boolean hasTemplateArgs() {
        return !getTemplateArgs().isEmpty();
    }

    public boolean hasTemplateArgTypes() {
        return !getTemplateArgTypes().isEmpty();
    }

    /**
     * 
     * @return true if this type updated its template argument types, false otherwise
     */
    public boolean hasUpdatedTemplateArgTypes() {
        return false;
    }

    public boolean hasSugar() {
        return getTypeData().hasSugar();
    }

    public Type desugar() {
        if (!getTypeData().hasSugar()) {
            return this;
        }

        return desugarImpl();
    }

    protected Type desugarImpl() {
        throw new NotImplementedException(getClass());
    }

    @Override
    public App getApp() {
        throw new RuntimeException("Type nodes are detached from the App tree, this method is not implemented");
    }

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

    /**
     * Goes down the type tree looking for the given type. If a node has more than one child, descending stops.
     *
     * @param type
     * @return
     */
    public <T extends Type> Optional<T> to(Class<T> type) {
        if (type.isInstance(this)) {
            return Optional.of(type.cast(this));
        }

        // Continue if there is one child
        if (getNumChildren() == 1) {
            return ((Type) getChild(0)).to(type);
        }

        if (this instanceof AttributedType) {
            return ((AttributedType) this).getModifiedType().to(type);
        }

        // Stop, can go no further
        SpecsLogs.msgWarn("Could not find type '" + type + "'");
        return Optional.empty();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = prime;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        /*
        if (!super.equals(obj)) {
            return false;
        }
        */

        if (getClass() != obj.getClass()) {
            return false;
        }
        Type other = (Type) obj;
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        return true;
    }

}
