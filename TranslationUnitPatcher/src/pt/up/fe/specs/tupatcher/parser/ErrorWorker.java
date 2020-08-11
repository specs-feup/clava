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

package pt.up.fe.specs.tupatcher.parser;

import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import pt.up.fe.specs.util.utilities.LineStream;

public class ExampleWorker implements LineStreamWorker<TUErrorData> {

    @Override
    public String getId() {
        return "<EXAMPLE_WORKER>";
    }

    @Override
    public void init(TUErrorData data) {
        // Do nothing
    }

    @Override
    public void apply(LineStream lineStream, TUErrorData data) {
        // Expects next line to be an integer, parse it and store
        var decodedInteger = Integer.decode(lineStream.nextLine());
        data.set(TUErrorData.INTEGER_EXAMPLE, decodedInteger);
    }

}
