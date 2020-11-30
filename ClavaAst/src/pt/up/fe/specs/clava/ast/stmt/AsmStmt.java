/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.clava.ast.stmt;

import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.stmt.data.AsmInput;
import pt.up.fe.specs.clava.ast.stmt.data.AsmOutput;

/**
 * Represents a section of inline-assembly.
 * 
 * @author JBispo
 *
 */
public abstract class AsmStmt extends Stmt {

    public static final DataKey<Boolean> IS_SIMPLE = KeyFactory.bool("isSimple");
    public static final DataKey<Boolean> IS_VOLATILE = KeyFactory.bool("isVolatile");
    public static final DataKey<List<String>> CLOBBERS = KeyFactory.list("clobbers", String.class);
    public static final DataKey<List<AsmOutput>> OUTPUTS = KeyFactory.list("outputs", AsmOutput.class);
    public static final DataKey<List<AsmInput>> INPUTS = KeyFactory.list("inputs", AsmInput.class);

    public AsmStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

}
