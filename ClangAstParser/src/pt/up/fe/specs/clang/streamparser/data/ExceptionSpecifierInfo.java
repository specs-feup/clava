/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clang.streamparser.data;

import java.util.Map;

import pt.up.fe.specs.clava.ast.type.legacy.ExceptionSpecifier;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.lazy.ThreadSafeLazy;
import pt.up.fe.specs.util.utilities.LineStream;

public class ExceptionSpecifierInfo {

    private static final String STREAM_PARSER_HEADER = "<FUNCTION_PROTO_TYPE_EXCEPTION>";

    private static final Lazy<ExceptionSpecifier[]> EXCEPTION_SPECIFIER_VALUES = new ThreadSafeLazy<>(
            () -> ExceptionSpecifier.values());

    public static String getStreamParserHeader() {
        return STREAM_PARSER_HEADER;
    }

    public static void streamParser(LineStream lines, Map<String, ExceptionSpecifierInfo> exceptionInfos) {
        // id
        String id = lines.nextLine();

        int specifierIndex = Integer.parseInt(lines.nextLine());
        ExceptionSpecifier specifierId = EXCEPTION_SPECIFIER_VALUES.get()[specifierIndex];
        boolean hasExpr = lines.nextLine().equals("1");

        String exceptionExpr = null;
        if (hasExpr) {
            exceptionExpr = lines.nextLine();
        }

        ExceptionSpecifierInfo info = new ExceptionSpecifierInfo(specifierId, exceptionExpr);

        exceptionInfos.put(id, info);

    }

    private final ExceptionSpecifier specifier;
    private final String noexceptExpr;

    public ExceptionSpecifierInfo(ExceptionSpecifier specifier, String noexceptExpr) {
        this.specifier = specifier;
        this.noexceptExpr = noexceptExpr;
    }

    public ExceptionSpecifier getSpecifier() {
        return specifier;
    }

    public String getNoexceptExpr() {
        return noexceptExpr;
    }

    @Override
    public String toString() {
        return "specifier: " + specifier + "; noexceptExpr: " + noexceptExpr;
    }
}
