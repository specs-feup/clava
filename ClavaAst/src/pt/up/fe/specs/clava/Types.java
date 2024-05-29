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

package pt.up.fe.specs.clava;

import java.util.Optional;

import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.type.AdjustedType;
import pt.up.fe.specs.clava.ast.type.ArrayType;
import pt.up.fe.specs.clava.ast.type.AttributedType;
import pt.up.fe.specs.clava.ast.type.AutoType;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.DecayedType;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.NullType;
import pt.up.fe.specs.clava.ast.type.ParenType;
import pt.up.fe.specs.clava.ast.type.PointerType;
import pt.up.fe.specs.clava.ast.type.QualType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.TypedefType;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class Types {

    private static final FunctionClassMap<Type, FunctionType> TO_FUNCTION_TYPE;

    static {
        TO_FUNCTION_TYPE = new FunctionClassMap<>();
        TO_FUNCTION_TYPE.put(FunctionType.class, type -> type);
        TO_FUNCTION_TYPE.put(FunctionProtoType.class, type -> type);
        TO_FUNCTION_TYPE.put(AttributedType.class, type -> TO_FUNCTION_TYPE.apply(type.getModifiedType()));
        TO_FUNCTION_TYPE.put(PointerType.class, type -> TO_FUNCTION_TYPE.apply(type.getPointeeType()));
        TO_FUNCTION_TYPE.put(ParenType.class, type -> TO_FUNCTION_TYPE.apply(type.getInnerType()));
        // TO_FUNCTION_TYPE.put(QualType.class, type -> TO_FUNCTION_TYPE.apply(type.getQualifiedType()));
        TO_FUNCTION_TYPE.put(NullType.class, type -> null);
    }

    public static boolean isVoid(Type type) {
        if (!(type instanceof BuiltinType)) {
            return false;
        }

        return type.getBareType().equals("void");
    }

    /**
     * Removes stray occurrences of 'class ' and 'struct '.
     * 
     * @param type
     * @return
     */
    public static String cleanElaborated(String type) {
        String currentType = type;

        if (!currentType.startsWith("class ")) {
            currentType = SpecsStrings.remove(currentType, "class ");
        }

        if (!currentType.startsWith("struct ")) {
            currentType = SpecsStrings.remove(currentType, "struct ");
        }

        return currentType;
    }

    /**
     * Tries to convert the type into a FunctionType. If it is not possible, returns null.
     * 
     * @param type
     * @return
     */
    public static FunctionType getFunctionType(Type type) {
        // System.out.println("TYPE:" + type);
        return TO_FUNCTION_TYPE.apply(type);
        /*
        if (type instanceof FunctionType) {
        return (FunctionType) type;
        }
        
        if (type instanceof AttributedType) {
        return getFunctionType(((AttributedType) type).getModifiedType());
        }
        
        if (type instanceof NullType) {
        return null;
        }
        
        throw new RuntimeException("Could not convert type to FunctionType:" + type.getClass().getSimpleName());
        */
    }

    // public static boolean isPointer(Type type) {
    // if (type instanceof PointerType) {
    // return true;
    // }
    //
    // if (type instanceof QualType) {
    // return isPointer(type.get(QualType.UNQUALIFIED_TYPE));
    // }
    //
    // return false;
    // }

    // public static Type getPointeeType(Type type) {
    // if (type instanceof PointerType) {
    // return ((PointerType) type).getPointeeType();
    // }
    //
    // throw new RuntimeException("Not implemented for type '" + type.getClass().getSimpleName() + "'");
    // }

    // public static Type normalize(Type type) {
    // if (type instanceof AdjustedType) {
    // return normalize(((AdjustedType) type).getAdjustedType());
    // }
    //
    // return type;
    // }

    /**
     * Number pointer levels of type (including array dimensions)
     * 
     * @return
     */
    public static int getPointerArity(Type type) {
        int typeArity = 0;
        if (type instanceof PointerType || type instanceof ArrayType) {
            typeArity = 1;
        }

        Type elementType = getSingleElement(type);

        // If the element is not the same node, continue building arity
        return elementType == null ? typeArity : typeArity + getPointerArity(elementType);

        // return typeArity + getArity(getSingleElement(type));
        /*        
        if (type instanceof AdjustedType) {
            return 0 + getArity(getSingleElement(type));
        }
        
        if (type instanceof PointerType) {
            return 1 + getArity(((PointerType) type).getPointeeType());
        }
        
        if (type instanceof ArrayType) {
            return 1 + getArity(((ArrayType) type).getElementType());
        }
        
        return 0;
        */
    }

    public static Type getElement(Type type) {
        Type elementType = getSingleElement(type);

        // If the element is not the same node, continue searching
        return elementType == null ? type : getElement(elementType);
    }

    public static Type getSingleElement(Type type) {
        if (type instanceof DecayedType) {
            return ((DecayedType) type).getOriginalType();
        }

        if (type instanceof AdjustedType) {
            return ((AdjustedType) type).getAdjustedType();
        }

        if (type instanceof PointerType) {
            return ((PointerType) type).getPointeeType();
        }

        if (type instanceof ArrayType) {
            return ((ArrayType) type).getElementType();
        }

        if (type instanceof QualType) {
            return ((QualType) type).getUnqualifiedType();
        }

        if (type instanceof TypedefType) {
            return ((TypedefType) type).getTypeClass();
        }

        return null;
    }

    public static <K extends Type> Optional<K> getElement(Type type, Class<K> targetType) {
        Type elementType = type;

        while (elementType != null) {
            if (targetType.isInstance(elementType)) {
                return Optional.of(targetType.cast(elementType));
            }

            elementType = getSingleElement(elementType);
        }

        return Optional.empty();
    }

    /**
     * To be called when a sugared type is modified.
     * 
     * @param sugaredType
     */
    public static void updateSugaredType(Type sugaredType) {
        // System.out.println("UPDATING " + sugaredType);

        // No sugar, nothing to do
        if (!sugaredType.hasSugar()) {
            return;
        }

        Type underlyingType = sugaredType.desugar();
        // System.out.println("UNDERLYING " + underlyingType);

        // If underlyingType type is a TypedefType, to reflect changes replace with the underlying type
        if (underlyingType instanceof TypedefType) {
            Type typeClass = ((TypedefType) underlyingType).getTypeClass();
            // System.out.println("IS TYPEDEF TYPE, TYPE CLASS:" + typeClass);
            // Optimization: detach to avoid copy
            // typeClass.detach();

            sugaredType.setDesugar(typeClass);
        }

    }

    public static boolean isEqual(Type type1, Type type2) {

        if (type1 == null && type2 == null) {
            return true;
        }

        if (type1 == null || type2 == null) {
            return false;
        }

        type1 = toComparable(type1);
        type2 = toComparable(type2);

        return type1.getCode().equals(type2.getCode());

    }

    public static int hashCode(Type type) {
        return toComparable(type).getCode().hashCode();
    }

    private static Type toComparable(Type type) {
        if (type instanceof AutoType) {
            // return toComparable(((AutoType) type).getDeducedType().orElse(null));
            return ((AutoType) type).getDeducedType().map(Types::toComparable).orElse(type);
        }

        return type;
    }

    /**
     * Tries to infer the return type for a given unary operator.
     * 
     * @param op
     * @param exprType
     * @return
     */
    public static Type inferUnaryType(UnaryOperatorKind op, Type exprType, ClavaFactory factory) {
        switch (op) {
        case PostInc:
        case PostDec:
        case PreInc:
        case PreDec:
        case Not:
        case Plus:
        case Minus:
            return exprType;
        case LNot:
            return factory.builtinType(BuiltinKind.Bool);
        case AddrOf:
            return factory.pointerType(exprType);
        case Deref:
            var unqualifiedType = exprType.desugarAll().unqualifiedType();

            SpecsCheck.checkArgument(unqualifiedType instanceof PointerType,
                    () -> "Expected type to be a pointer: " + unqualifiedType);

            return ((PointerType) unqualifiedType).getPointeeType();
        default:
            throw new NotImplementedException("Unary op return type inference not implemented for '" + op + "'");
        }
    }
}
