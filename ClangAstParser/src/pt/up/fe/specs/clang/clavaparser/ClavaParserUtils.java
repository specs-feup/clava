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

package pt.up.fe.specs.clang.clavaparser;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.expr.enums.ValueKind;
import pt.up.fe.specs.clava.ast.extra.Undefined;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;

public class ClavaParserUtils {

    public static <T extends ClavaNode> T cast(ClavaNode node, Class<T> aClass, Function<ClavaNode, T> dummySupplier) {
        // Replace with DummyStmt if Undefined
        if (node instanceof Undefined) {
            node = dummySupplier.apply(node);
        }

        if (!aClass.isInstance(node)) {
            throw new RuntimeException("Expected an '" + aClass.getSimpleName() + "':" + node);
        }

        return aClass.cast(node);
    }

    /**
     * @deprecated replaced by ValueKind.getHelper()
     * @param valueKind
     * @return
     */
    @Deprecated
    public static Optional<ValueKind> parseValueKind(String valueKind) {
        switch (valueKind) {
        case "lvalue":
            return Optional.of(ValueKind.L_VALUE);
        case "rvalue":
            return Optional.of(ValueKind.R_VALUE);
        case "xvalue":
            return Optional.of(ValueKind.X_VALUE);
        default:
            return Optional.empty();
        }
    }

    /**
     * @deprecated replaced by InitializationStyle.getHelper()
     * @param kindString
     * @return
     */
    @Deprecated
    public static Optional<InitializationStyle> parseInitializationStyle(String kindString) {

        switch (kindString) {
        case "":
            return Optional.of(InitializationStyle.NO_INIT);
        case "cinit":
            return Optional.of(InitializationStyle.CINIT);
        case "callinit":
            return Optional.of(InitializationStyle.CALL_INIT);
        default:
            return Optional.empty();
        }
    }

    public static String createAnonName(ClangNode node) {
        if (!node.getLocation().isValid()) {
            return "anon_" + UUID.randomUUID().toString();
        }

        String sanitizedFilename = node.getLocation().getFilename().replace('.', '_');
        return "anon_" + sanitizedFilename + "_" + node.getLocation().getStartLine();
    }

    /**
     * Used for parsing C++ conditions, which can contain declarations.
     * 
     * @param isDeclCondition
     * @param condition
     * @return
     */
    public static ClavaNode getDeclCondition(boolean isDeclCondition, ClavaNode condition) {
        if (!isDeclCondition) {
            return condition;

        }

        // Confirm that it is a DeclStmt
        Preconditions.checkArgument(condition instanceof DeclStmt);
        return condition.getChild(0);
    }
}
