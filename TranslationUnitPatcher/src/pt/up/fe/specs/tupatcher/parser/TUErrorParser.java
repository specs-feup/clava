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

import java.util.HashMap;
import java.util.Map;

import org.suikasoft.jOptions.streamparser.LineStreamParser;
import org.suikasoft.jOptions.streamparser.LineStreamWorker;

public class TUErrorParser {

    /**
     * Workers for the LineStreamParser.
     */
    private static final Map<String, LineStreamWorker<TUErrorsData>> WORKERS;
    static {
        WORKERS = new HashMap<>();
        for (int i = 0; i < 5; i++)
            addWorker(new ErrorWorker());
        //addWorker(ExampleWorkers.newStoreKeyValueWorker());
    }

    /**
     * Helper method for adding workers to the Worker map.
     * 
     * @param worker
     */
    private static void addWorker(LineStreamWorker<TUErrorsData> worker) {
        // Add worker
        WORKERS.put(worker.getId(), worker);
    }

    /**
     * Creates a new instance of a LineStreamParser for parsing data about Clang errors.
     * 
     * @return
     */
    public static LineStreamParser<TUErrorsData> newInstance() {
        TUErrorsData errorsData = new TUErrorsData();

        LineStreamParser<TUErrorsData> streamParser = LineStreamParser.newInstance(errorsData, WORKERS);
        streamParser.setLineIgnore(TUErrorParser::ignoreLine);

        return streamParser;
    }

    /**
     * Custom rules for ignoring lines.
     * 
     * @param line
     * @return
     */
    private static boolean ignoreLine(String line) {
        // By default, ignores nothing
        return false;
    }

}
