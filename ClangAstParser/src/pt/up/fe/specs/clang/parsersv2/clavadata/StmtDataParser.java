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

package pt.up.fe.specs.clang.parsersv2.clavadata;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.parsersv2.ClavaDataParser;
import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.stmt.data.StmtData;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * ClavaData parsers for Stmt nodes.
 * 
 * 
 * @author JoaoBispo
 *
 */
public class StmtDataParser {

    public static StmtData parseStmtData(LineStream lines, DataStore dataStore) {
        ClavaData clavaData = ClavaDataParser.parseClavaData(lines, dataStore);

        return new StmtData(clavaData);
    }

}
