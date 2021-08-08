/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clang.parsers.data;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamParsers;

import pt.up.fe.specs.clang.codeparser.ClangParserData;
import pt.up.fe.specs.clang.parsers.NodeDataParser;
import pt.up.fe.specs.clava.ast.attr.AlignedAttr;
import pt.up.fe.specs.clava.ast.attr.AlignedExprAttr;
import pt.up.fe.specs.clava.ast.attr.AlignedTypeAttr;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.attr.FormatAttr;
import pt.up.fe.specs.clava.ast.attr.NonNullAttr;
import pt.up.fe.specs.clava.ast.attr.OpenCLUnrollHintAttr;
import pt.up.fe.specs.clava.ast.attr.VisibilityAttr;
import pt.up.fe.specs.clava.ast.attr.enums.AlignedAttrKind;
import pt.up.fe.specs.clava.ast.attr.enums.AttributeKind;
import pt.up.fe.specs.clava.ast.attr.enums.FormatAttrKind;
import pt.up.fe.specs.clava.ast.attr.enums.VisibilityType;
import pt.up.fe.specs.util.utilities.LineStream;

public class AttrDataParser {

    public static DataStore parseAttributeData(LineStream lines, ClangParserData dataStore) {

        DataStore clavaData = NodeDataParser.parseNodeData(lines, dataStore);

        clavaData.add(Attribute.KIND, LineStreamParsers.enumFromName(AttributeKind.getHelper(), lines));
        clavaData.add(Attribute.IS_IMPLICIT, LineStreamParsers.oneOrZero(lines));
        clavaData.add(Attribute.IS_INHERITED, LineStreamParsers.oneOrZero(lines));
        clavaData.add(Attribute.IS_LATE_PARSED, LineStreamParsers.oneOrZero(lines));
        clavaData.add(Attribute.IS_PACK_EXPANSION, LineStreamParsers.oneOrZero(lines));

        return clavaData;
    }

    public static DataStore parseAlignedAttrData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseAttributeData(lines, dataStore);

        data.add(AlignedAttr.SPELLING, lines.nextLine());

        boolean isExpr = LineStreamParsers.oneOrZero(lines);
        String nodeId = lines.nextLine();

        if (isExpr) {
            data.add(AlignedAttr.ALIGNED_ATTR_KIND, AlignedAttrKind.EXPR);
            // data.add(AlignedExprAttr.EXPR, dataStore.getClavaNodes().getExpr(nodeId));
            // data.add(AlignedExprAttr.EXPR, dataStore.getClavaNodes().getExpr(nodeId));
            dataStore.getClavaNodes().queueSetOptionalNode(data, AlignedExprAttr.EXPR, nodeId);
            // data.add(ClavaNodeI.NODE_CLASS, AlignedExprAttr.class);

            // data.setDefinition(AlignedExprAttr.class);
            // DataStore dummyData = data.copy();
            // dummyData.setDefinition(DummyAttr.class);
            // dummyData.set(DummyAttr.CLASSNAME, "<classname>");
            // System.out.println("DUMMY DATA:" + dummyData);
        } else {
            data.add(AlignedAttr.ALIGNED_ATTR_KIND, AlignedAttrKind.TYPE);
            // Type type = dataStore.getClavaNodes().getType(nodeId);
            // data.add(AlignedTypeAttr.TYPE, type);
            dataStore.getClavaNodes().queueSetNode(data, AlignedTypeAttr.TYPE, nodeId);
            // data.setDefinition(AlignedTypeAttr.class);
            // data.setStoreDefinition(StoreDefinition.fromInterface(AlignedTypeAttrI.class));
            // data.add(ClavaNodeI.NODE_CLASS, AlignedTypeAttr.class);
        }

        return data;

        // ClavaNode node = ClavaNodes.getNode(dataStore, nodeId);
        // ClavaNode node = dataStore.get(ClangParserKeys.CLAVA_NODES).get(nodeId);

        // Expr expr = isExpr ? (Expr) node : null;
        // Type type = isExpr ? null : (Type) node;

        // return new AlignedAttrData(spelling, isExpr, expr, type, data);
    }

    public static DataStore parseOpenCLUnrollHintAttrData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseAttributeData(lines, dataStore);

        data.add(OpenCLUnrollHintAttr.UNROLL_HINT, LineStreamParsers.integer(lines));

        return data;
    }

    public static DataStore parseFormatAttrData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseAttributeData(lines, dataStore);

        data.add(FormatAttr.TYPE, LineStreamParsers.enumFromName(FormatAttrKind.class, lines));
        data.add(FormatAttr.FORMAT_INDEX, LineStreamParsers.integer(lines));
        data.add(FormatAttr.FIRST_ARG, LineStreamParsers.integer(lines));

        return data;
    }

    public static DataStore parseNonNullAttrData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseAttributeData(lines, dataStore);

        data.add(NonNullAttr.ARGUMENTS, LineStreamParsers.list(lines, l -> Integer.valueOf(l.nextLine())));

        return data;
    }

    public static DataStore parseVisibilityAttrData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseAttributeData(lines, dataStore);

        data.add(VisibilityAttr.VISIBILITY_TYPE, LineStreamParsers.enumFromName(VisibilityType.class, lines));

        return data;
    }

}
