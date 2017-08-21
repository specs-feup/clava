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

import java.util.function.BiConsumer;
import java.util.function.Function;

import pt.up.fe.specs.util.utilities.LineStream;

class GenericSnippetParser<I, T> implements SnippetParser<I, T> {

    private final String id;
    private final I current;
    private final BiConsumer<LineStream, I> parser;
    private final Function<I, T> conversor;

    public GenericSnippetParser(String id, I current, BiConsumer<LineStream, I> parser,
            Function<I, T> conversor) {

        this.id = id;
        this.current = current;
        this.parser = parser;
        this.conversor = conversor;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public I getCurrent() {
        return current;
    }

    @Override
    public T getResult() {
        return conversor.apply(getCurrent());
    }

    public Function<I, T> getConversor() {
        return conversor;
    }

    @Override
    public void parse(LineStream lines) {
        parser.accept(lines, getCurrent());
    }

    @Override
    public String toString() {
        return getResult().toString();
    }

}
