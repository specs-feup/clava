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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.type.enums.AddressSpaceQualifierV2;
import pt.up.fe.specs.clava.ast.type.enums.C99Qualifier;

/**
 * Represents a set of qualifiers for a type.
 *
 * @author JoaoBispo
 *
 */
public class QualType extends Type {

    /// DATAKEYs BEGIN

    public final static DataKey<List<C99Qualifier>> C99_QUALIFIERS = KeyFactory
            .generic("c99Qualifiers", new ArrayList<>());

    public final static DataKey<AddressSpaceQualifierV2> ADDRESS_SPACE_QUALIFIER = KeyFactory
            .enumeration("addressSpaceQualifier", AddressSpaceQualifierV2.class);

    public final static DataKey<Long> ADDRESS_SPACE = KeyFactory.longInt("addressSpace");

    public final static DataKey<Type> UNQUALIFIED_TYPE = KeyFactory.object("unqualifiedType", Type.class);

    /// DATAKEYS END

    public QualType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {
        String type = getUnqualifiedType().getCode(sourceNode, name);

        // If not a top-level qualifier, has to be put after the type, but before the name
        if (hasParent()) {

            // But only if there are no other QualType ancestors, or if there is,
            // the parent must be a Pointer or a Reference, to avoid invalid double 'const' qualifiers
            boolean hasQualTypeAncestor = getAncestorTry(QualType.class).isPresent();

            ClavaNode parent = getParent();

            boolean allowedTypes = parent instanceof PointerType || parent instanceof ReferenceType;

            if (hasQualTypeAncestor && !allowedTypes) {
                return type;
            }
        }

        return getCode(type, name, sourceNode);
    }

    private String getCode(String type, String name, ClavaNode sourceNode) {
        String addressQualifier = get(ADDRESS_SPACE_QUALIFIER).getCode(get(ADDRESS_SPACE));

        if (!addressQualifier.isEmpty()) {
            addressQualifier += " ";
        }

        String qualifiersCode = get(C99_QUALIFIERS).stream()
                .map(C99Qualifier::getCode)
                .collect(Collectors.joining(" "));

        // If constexpr, replace const with constexpr
        boolean isConstexpr = sourceNode != null
                && sourceNode instanceof VarDecl
                && sourceNode.get(VarDecl.IS_CONSTEXPR);
        if (isConstexpr) {
            qualifiersCode = qualifiersCode.replace("const", "constexpr");
        }

        // Prefix space, so that it can be concatenated
        if (!qualifiersCode.isEmpty()) {
            qualifiersCode = " " + qualifiersCode;
        }

        if (name != null) {
            int index = type.lastIndexOf(name);
            Preconditions.checkArgument(index != -1);

            String prefix = addressQualifier + type.substring(0, index).trim() + qualifiersCode;
            if (!prefix.isEmpty()) {
                prefix = prefix + " ";
            }

            return prefix + type.substring(index);
        }

        return addressQualifier + type + qualifiersCode;

    }

    public Type getUnqualifiedType() {
        return get(UNQUALIFIED_TYPE);
    }

    @Override
    public boolean isConst() {
        if (get(C99_QUALIFIERS).contains(C99Qualifier.CONST)) {
            return true;
        }

        return getUnqualifiedType().isConst();
    }

    @Override
    public void removeConst() {
        var qualifiers = get(QualType.C99_QUALIFIERS);

        if (qualifiers.contains(C99Qualifier.CONST)) {

            // Make copy of qualifiers
            var qualCopy = new ArrayList<>(qualifiers);
            set(QualType.C99_QUALIFIERS, qualCopy);
            qualCopy.remove(C99Qualifier.CONST);
            return;
        }
    }

    public List<String> getQualifierStrings() {
        return getQualifiersPrivate();
    }

    private List<String> getQualifiersPrivate() {
        List<String> qualifiers = new ArrayList<>();

        qualifiers.addAll(getData().get(C99_QUALIFIERS).stream()
                .map(C99Qualifier::getCode)
                .collect(Collectors.toList()));

        return qualifiers;
    }

    @Override
    protected List<DataKey<Type>> getUnderlyingTypeKeys() {
        return Arrays.asList(UNQUALIFIED_TYPE);
    }

    @Override
    public boolean isPointer() {
        return getUnqualifiedType().isPointer();
    }

    @Override
    public Type getPointeeType() {
        return getUnqualifiedType().getPointeeType();
    }

    @Override
    public boolean isArray() {
        return getUnqualifiedType().isArray();
    }
}
