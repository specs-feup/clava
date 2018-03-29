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
import java.util.HashMap;
import java.util.Map;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.streamparser.SnippetParser;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * Collects information from a LineStream into a DataStore.
 * 
 * @author JoaoBispo
 *
 */
public interface LineStreamParser {

    /**
     * Builds a DataStore with the current parsed values.
     * 
     * @return DataStore with parsed data
     */
    public DataStore buildData();

    /**
     * Parsers the LineStream section corresponding to the given id.
     * 
     * @param id
     * @param lineStream
     * @return true if the id was valid, false otherwise. When returning false, the LineStream remains unmodified
     */
    public boolean parse(String id, LineStream lineStream);

    /**
     * Each section of LineStream lines is associated to a given id. This function returns the ids supported by this
     * parser.
     * 
     * @return the LineStream ids supported by this parser
     */
    public Collection<String> getIds();

    static LineStreamParser newInstance(DataKey<?> key, SnippetParser<?, ?> parser) {

        Map<DataKey<?>, SnippetParser<?, ?>> parsers = new HashMap<>();
        parsers.put(key, parser);

        return new GenericLineStreamParser(parsers);
    }
}
