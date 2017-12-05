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

package pt.up.fe.specs.clava.weaver;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.weaver.options.CxxWeaverOption;
import pt.up.fe.specs.util.SpecsLogs;

public class ClavaWeaverData {

    // Options
    private final DataStore data;

    // Parsed program state
    private final Deque<App> apps;
    private final Deque<Map<ClavaNode, Map<String, Object>>> userValuesStack;

    public ClavaWeaverData(DataStore data) {
        this.data = data;

        this.apps = new ArrayDeque<>();
        this.userValuesStack = new ArrayDeque<>();
    }

    public Optional<App> getAst() {

        App app = apps.peek();

        if (app == null) {
            // Verify if weaving is disabled
            if (data.get(CxxWeaverOption.DISABLE_WEAVING)) {
                SpecsLogs.msgInfo("'Disable weaving' option is set, cannot use AST-related code (e.g., 'select')");
                return Optional.empty();
            }

            SpecsLogs.msgInfo("No parsed tree available");
            return Optional.empty();
        }

        return Optional.of(app);
    }

    public void pushAst(App app) {
        apps.push(app);
        userValuesStack.push(new HashMap<>());
    }

    public Map<ClavaNode, Map<String, Object>> getUserValues() {
        return userValuesStack.peek();
    }

    public App popAst() {
        userValuesStack.pop();
        return apps.pop();
    }

}
