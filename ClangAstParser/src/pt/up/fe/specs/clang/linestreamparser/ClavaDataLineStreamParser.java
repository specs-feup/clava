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

package pt.up.fe.specs.clang.linestreamparser;

import java.util.Collection;
import java.util.function.Function;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.clava.ast.decl.data2.ClavaData;
import pt.up.fe.specs.util.classmap.ClassMap;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * Parses
 * 
 * @author JoaoBispo
 *
 */
public class ClavaDataLineStreamParser implements LineStreamParser {

    private static final String BASE_CLAVA_AST_PACKAGE = DummyNode.class.getPackage().getName();

    private final ClassMap<ClavaData, Function<LineStream, ClavaData>> dataParsers;

    public ClavaDataLineStreamParser(ClassMap<ClavaData, Function<LineStream, ClavaData>> dataParsers) {
        this.dataParsers = dataParsers;
    }

    // @Override
    // public Optional<String> adaptsKey(String streamParserKey) {
    // // TODO Auto-generated method stub
    // return null;
    // }

    // s

    @Override
    public Collection<DataKey<?>> getKeys() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataStore getData() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean parse(String key, LineStream lineStream) {
        // TODO Auto-generated method stub
        return false;
    }

}
