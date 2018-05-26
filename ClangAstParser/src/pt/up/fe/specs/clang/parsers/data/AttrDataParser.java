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
import org.suikasoft.jOptions.streamparser.GeneralParsers;

import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clang.parsers.NodeDataParser;
import pt.up.fe.specs.clava.ast.attr.AlignedAttr;
import pt.up.fe.specs.clava.ast.attr.AlignedExprAttr;
import pt.up.fe.specs.clava.ast.attr.AlignedTypeAttr;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.attr.enums.AttributeKind;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.utilities.LineStream;

public class AttrDataParser {

    public static DataStore parseAttributeData(LineStream lines, DataStore dataStore) {

        DataStore clavaData = NodeDataParser.parseNodeData(lines, dataStore);

        clavaData.add(Attribute.KIND, GeneralParsers.enumFromName(AttributeKind.getHelper(), lines));
        clavaData.add(Attribute.IS_IMPLICIT, GeneralParsers.parseOneOrZero(lines));
        clavaData.add(Attribute.IS_INHERITED, GeneralParsers.parseOneOrZero(lines));
        clavaData.add(Attribute.IS_LATE_PARSED, GeneralParsers.parseOneOrZero(lines));
        clavaData.add(Attribute.IS_PACK_EXPANSION, GeneralParsers.parseOneOrZero(lines));

        return clavaData;
    }

    public static DataStore parseAlignedAttrData(LineStream lines, DataStore dataStore) {
        DataStore data = parseAttributeData(lines, dataStore);

        data.add(AlignedAttr.SPELLING, lines.nextLine());

        boolean isExpr = GeneralParsers.parseOneOrZero(lines);
        String nodeId = lines.nextLine();

        if (isExpr) {
            data.add(AlignedExprAttr.EXPR, ClavaNodes.getExpr(dataStore, nodeId));
            // data.add(ClavaNodeI.NODE_CLASS, AlignedExprAttr.class);

            // data.setDefinition(AlignedExprAttr.class);
            // DataStore dummyData = data.copy();
            // dummyData.setDefinition(DummyAttr.class);
            // dummyData.set(DummyAttr.CLASSNAME, "<classname>");
            // System.out.println("DUMMY DATA:" + dummyData);
        } else {
            Type type = ClavaNodes.getType(dataStore, nodeId);
            data.add(AlignedTypeAttr.TYPE, type);
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

}
