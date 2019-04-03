/**
 * Copyright 2019 SPeCS.
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

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.data.designator.Designator;

public class DesignatedInitExpr extends Expr {

    /// DATAKEYS BEGIN

    public static final DataKey<Boolean> USES_GNU_SYNTAX = KeyFactory.bool("usesGnuSyntax");
    public static final DataKey<List<Designator>> DESIGNATORS = KeyFactory.list("designators", Designator.class);

    /// DATAKEYS END

    public DesignatedInitExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Expr getInit() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {

        StringBuilder code = new StringBuilder();

        List<ClavaNode> children = getChildren();

        // Nodes for designators start at position 1
        int currentIndex = 1;
        // Write code for each designator
        for (Designator designator : get(DESIGNATORS)) {

            String designatorCode = designator
                    .getCode(children.subList(currentIndex, currentIndex + designator.getNumNodes()));
            currentIndex += designator.getNumNodes();

            code.append(designatorCode);
            //
            // if(designator.get(Designator.DESIGNATOR_KIND) == )
        }

        // Initialize value
        code.append(" = ").append(getInit().getCode());

        // String designatorsCode = get(DESIGNATORS).stream()
        // .map(designator -> designator.getCode(children))
        // .collect(Collectors.joining());

        // code.append(designatorsCode);

        // System.out.println("CODE: " + code);
        // System.out.println("CHILDREN:" + toTree());
        // System.out.println("DESIGNATORS: " + get(DESIGNATORS));
        return code.toString();

        // return "/* NOT IMPLEMENTED */";
    }
}
