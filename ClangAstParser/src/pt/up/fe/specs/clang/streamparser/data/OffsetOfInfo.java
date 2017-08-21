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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pt.up.fe.specs.clang.streamparser.StreamParser;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.lazy.ThreadSafeLazy;
import pt.up.fe.specs.util.utilities.LineStream;

public class OffsetOfInfo {

    private static final String STREAM_PARSER_HEADER = "<OFFSET_OF_INFO>";

    private static final Lazy<OffsetOfComponentKind[]> KIND_VALUES = new ThreadSafeLazy<>(
            () -> OffsetOfComponentKind.values());

    public static String getStreamParserHeader() {
        return STREAM_PARSER_HEADER;
    }

    private final String typeId;
    private final int numExpressions;
    private final List<OffsetOfClangComponent> components;

    public OffsetOfInfo(String typeId, int numExpressions, List<OffsetOfClangComponent> components) {
        this.typeId = typeId;
        this.numExpressions = numExpressions;
        this.components = components;
    }

    public String getTypeId() {
        return typeId;
    }

    public List<OffsetOfClangComponent> getComponents() {
        return components;
    }

    public int getNumExpressions() {
        return numExpressions;
    }

    @Override
    public String toString() {
        return "type:" + typeId + "; numExpressions:" + numExpressions + "; " + components;
    }

    public static void streamParser(LineStream lines, Map<String, OffsetOfInfo> offsetOfInfos) {
        // id
        String id = lines.nextLine();

        String typeId = lines.nextLine();

        // number of expressions
        // int numExpressions = StdErrParser.parseInt(lines, "numExpr:");
        int numExpressions = StreamParser.parseInt(lines);
        // number of components
        // int numComponents = StdErrParser.parseInt(lines, "numComp:");
        int numComponents = StreamParser.parseInt(lines);

        // Parse each component
        List<OffsetOfClangComponent> components = new ArrayList<>(numComponents);
        for (int i = 0; i < numComponents; i++) {
            components.add(parseComponent(lines));
        }

        offsetOfInfos.put(id, new OffsetOfInfo(typeId, numExpressions, components));
    }

    private static OffsetOfClangComponent parseComponent(LineStream lines) {
        // Kind
        // int kindOrdinal = StdErrParser.parseInt(lines, "kind:");
        int kindOrdinal = StreamParser.parseInt(lines);
        OffsetOfComponentKind kind = KIND_VALUES.get()[kindOrdinal];

        switch (kind) {
        case ARRAY:
            int expressionIndex = StreamParser.parseInt(lines);
            return OffsetOfClangComponent.newArrayComponent(expressionIndex);
        case FIELD:
            String fieldName = lines.nextLine();
            return OffsetOfClangComponent.newFieldComponent(fieldName);
        case IDENTIFIER:
            throw new NotImplementedException(kind);
        case BASE:
            throw new NotImplementedException(kind);
        default:
            throw new CaseNotDefinedException(kind);
        }
    }
}
