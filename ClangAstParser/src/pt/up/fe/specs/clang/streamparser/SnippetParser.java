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

package pt.up.fe.specs.clang.streamparser;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import pt.up.fe.specs.clava.ast.decl.data2.ClavaData;
import pt.up.fe.specs.util.utilities.LineStream;

public interface SnippetParser<I, T> {

    String getId();

    I getCurrent();

    T getResult();

    void parse(LineStream lines);

    static <I, T> SnippetParser<I, T> newInstance(String id, I init, BiConsumer<LineStream, I> parser,
            Function<I, T> conversor) {
        return new GenericSnippetParser<I, T>(id, init, parser, conversor);
    }

    static <T> SnippetParser<T, T> newInstance(String id, T resultInit, BiConsumer<LineStream, T> parser) {
        return new GenericSnippetParser<>(id, resultInit, parser, current -> current);
    }

    static <D extends ClavaData> SnippetParser<Map<String, D>, Map<String, D>> newInstance(String id,
            Function<LineStream, D> dataParser) {

        Map<String, D> resultInit = new HashMap<>();

        BiConsumer<LineStream, Map<String, D>> parser = (lineStream, map) -> StreamDataParser.parseNodeData(dataParser,
                lineStream, map);

        return new GenericSnippetParser<>(id, resultInit, parser, current -> current);
    }

    // public static <D extends ClavaData> BiConsumer<LineStream, Map<String, D>> parseNodeData(
    // Function<LineStream, D> dataParser) {
    //
    // return (lineStream, map) -> parseNodeData(dataParser, lineStream, map);
    // }

}
