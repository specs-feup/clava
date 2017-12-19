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
import java.util.Map.Entry;
import java.util.Optional;

import org.suikasoft.XStreamPlus.XStreamUtils;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
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
        App previousApp = apps.peek();
        apps.push(app);

        // Preserve previous user values
        Map<ClavaNode, Map<String, Object>> userValuesCopy = getUserValuesCopy(app, previousApp,
                userValuesStack.peek());

        userValuesStack.push(userValuesCopy);
        // userValuesStack.push(new HashMap<>());
    }

    public Map<ClavaNode, Map<String, Object>> getUserValues() {
        return userValuesStack.peek();
    }

    private Map<ClavaNode, Map<String, Object>> getUserValuesCopy(App app, App previousApp,
            Map<ClavaNode, Map<String, Object>> userValues) {

        // When there are no user values
        if (userValues == null || userValues.isEmpty()) {
            return new HashMap<>();
        }

        Map<ClavaNode, Map<String, Object>> userValuesCopy = new HashMap<>();

        // For each entry in the table, map the previous node to the current node and copy the object
        for (Entry<ClavaNode, Map<String, Object>> entry : userValues.entrySet()) {
            // Get node of the previous tree
            ClavaNode previousNode = entry.getKey();

            // Get corresponding node of the new tree
            ClavaNode newNode = getNewNode(app, previousNode);
            if (newNode == null) {
                ClavaLog.warning(
                        "Could not preserve user field for node at location '" + previousNode.getLocation() + "'");
                continue;
            }

            // Serialize value
            Map<String, Object> valueCopy = XStreamUtils.copy(entry.getValue());

            // Add to map
            userValuesCopy.put(newNode, valueCopy);
        }

        return userValuesCopy;
        // return userValuesStack.isEmpty() ? new HashMap<>() : XStreamUtils.copy(userValuesStack.peek());
    }

    private ClavaNode getNewNode(App app, ClavaNode previousNode) {
        // Special case: node App
        if (previousNode instanceof App) {
            return app;
        }

        return app.find(previousNode).orElse(null);
    }

    public App popAst() {
        userValuesStack.pop();
        return apps.pop();
    }

}
