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
import java.util.Map.Entry;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.streamparser.SnippetParser;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * 
 * @author JoaoBispo
 *
 */
public class GenericLineStreamParser implements LineStreamParser {

    private final Map<DataKey<?>, SnippetParser<?, ?>> parsers;
    private final Map<String, SnippetParser<?, ?>> ids;
    // private final DataStore data;

    // public GenericLineStreamParser(Collection<SnippetParser<?, ?>> parsers) {
    // this(parsers.stream().collect(Collectors.toMap(SnippetParser::getId, parser -> parser)));
    // }

    public GenericLineStreamParser(Map<DataKey<?>, SnippetParser<?, ?>> parsers) {
        this.ids = new HashMap<>();
        this.parsers = new HashMap<>();
        // Make sure all parsers have different IDs
        for (Entry<DataKey<?>, SnippetParser<?, ?>> entry : parsers.entrySet()) {
            SnippetParser<?, ?> previousValue = this.ids.put(entry.getValue().getId(), entry.getValue());
            if (previousValue != null) {
                throw new RuntimeException(
                        "Found duplicated id '" + entry.getValue().getId() + "' in given parsers map:\n" + parsers);
            }

            this.parsers.put(entry.getKey(), entry.getValue());
        }
        // this.parsers = parsers;
        // this.data = DataStore.newInstance("GenericLineStreamParser Data");
    }

    @Override
    public DataStore buildData() {

        DataStore data = DataStore.newInstance("Generic LineStream Parser Data");

        for (Entry<DataKey<?>, SnippetParser<?, ?>> entry : parsers.entrySet()) {
            data.setRaw(entry.getKey(), entry.getValue().getResult());
        }

        return data;
    }

    @Override
    public boolean parse(String id, LineStream lineStream) {
        SnippetParser<?, ?> parser = ids.get(id);

        if (parser == null) {
            return false;
        }

        parser.parse(lineStream);

        return true;
    }

    @Override
    public Collection<String> getIds() {
        return ids.keySet();
        // return parsers.values().stream()
        // .map(SnippetParser::getId)
        // .collect(Collectors.toSet());
    }

}
