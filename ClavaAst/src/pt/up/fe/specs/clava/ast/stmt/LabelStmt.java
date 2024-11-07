/**
 * Copyright 2016 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.stmt;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.decl.LabelDecl;

import java.util.Collection;
import java.util.Optional;

public class LabelStmt extends Stmt {
    /// DATAKEYS BEGIN

    public final static DataKey<LabelDecl> LABEL = KeyFactory.object("label", LabelDecl.class);

    /// DATAKEYS END

    public LabelStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public String getLabel() {
        return get(LABEL).getDeclName();
    }

    public LabelDecl getLabelDecl() {
        return get(LABEL);
    }

    public void setLabelDecl(LabelDecl labelDecl) {
        set(LABEL, labelDecl);

        // Update LabelStmt in LabelDecl
        labelDecl.set(LabelDecl.LABEL_STMT, Optional.of(this));

    }

    public Optional<Stmt> getSubStmt() {
        return ClavaNodes.nextNode(this).map(node -> (Stmt) node);
    }

    @Override
    public String getCode() {
        String code = getLabel() + ":";

        return code;
    }

}
