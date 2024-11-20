/**
 * Copyright 2018 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.context;

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.utilities.CachedItems;
import pt.up.fe.specs.util.utilities.IdGenerator;

import java.io.File;
import java.util.*;

public class ClavaContext extends ADataClass<ClavaContext> {

    /// DATAKEYS BEGIN

    /**
     * The arguments used to call the parser.
     */
    public final static DataKey<Map<File, List<String>>> ARGUMENTS = KeyFactory
            .generic("arguments", (Map<File, List<String>>) new HashMap<File, List<String>>())
            .setCopyFunction(map -> new HashMap<>(map));

    /**
     * IDs generator
     */
    public final static DataKey<IdGenerator> ID_GENERATOR = KeyFactory
            .object("idGenerator", IdGenerator.class)
            .setDefault(() -> new IdGenerator())
            .setCopyFunction(id -> new IdGenerator(id));

    public final static DataKey<ClavaFactory> FACTORY = KeyFactory
            .object("factory", ClavaFactory.class);

    // public final static DataKey<App> APP = KeyFactory
    // .object("app", App.class);

    public final static DataKey<ClavaMetrics> METRICS = KeyFactory
            .object("metrics", ClavaMetrics.class);

    public final static DataKey<CachedItems<String, String>> CACHED_FILEPATHS = KeyFactory
            .generic("cachedFilepaths", () -> new CachedItems<String, String>(string -> string, true));

    /**
     * If set, represents the root folder where we are working on.
     */
    // public final static DataKey<Optional<File>> ROOT_FOLDER = KeyFactory.optional("rootFolder");

    /**
     * Temporary measure due to TextParser being called more than once over the same nodes.
     */
    // public final static DataKey<Set<ClavaNode>> ASSOCIATED_COMMENTS = KeyFactory.generic(
    // "associatedComments", (Set<ClavaNode>) new HashSet<ClavaNode>());

    /// DATAKEYS END

    // private final DataStore data;
    // private final Standard standard;
    // private final List<String> arguments;
    // private final ClavaIdGenerator idGenerator;
    // private final ClavaFactory factory;

    private final List<App> appStack;

    public ClavaContext() {

        // this.data = DataStore.newInstance(getClass());

        // Set arguments
        set(ARGUMENTS, new HashMap<>());

        // Initialize factory
        set(FACTORY, new ClavaFactory(this));

        set(METRICS, new ClavaMetrics());

        set(CACHED_FILEPATHS, new CachedItems<>(string -> string, true));

        appStack = new ArrayList<>();
    }

    // public ClavaContext(ClavaContext context) {
    // set(ARGUMENTS, ARGUMENTS.copy(context.get(ARGUMENTS)));
    // set(ID_GENERATOR, ID_GENERATOR.copy(context.get(ID_GENERATOR)));
    // set(METRICS, METRICS.copy(context.get(METRICS)));
    //
    // // Initialize factory
    // set(FACTORY, new ClavaFactory(this));
    // }

    public ClavaContext addArguments(File sourceFile, List<String> arguments) {
        get(ARGUMENTS).put(sourceFile, arguments);
        return this;
    }

    public ClavaFactory getFactory() {
        return get(FACTORY);
    }

    public Optional<App> pushApp(App newApp) {
        // Verify if apps have the same context
        SpecsCheck.checkArgument(newApp.getContext() == this, () -> "Expected new App to have the same context");
        Optional<App> previousApp = SpecsCollections.lastTry(appStack);
        appStack.add(newApp);

        // ClavaLog.info("APP PUSH: " + appStack.size() + " pushed app: " + newApp.hashCode());

        return previousApp;
    }

    /**
     * Only last element of the app list remains.
     */
    public void clearAppHistory() {
        if (appStack.size() < 2) {
            return;
        }

        var lastApp = SpecsCollections.last(appStack);
        appStack.clear();
        appStack.add(lastApp);
    }

    public App popApp() {
        App app = appStack.remove(appStack.size() - 1);

        // ClavaLog.info("APP POP: " + appStack.size() + " popped app: " + app.hashCode());

        return app;
    }

    public boolean isStackEmpty() {
        return appStack.isEmpty();
    }

    public int getStackSize() {
        return appStack.size();
    }

    public App getApp() {
        if (appStack.isEmpty()) {
            throw new RuntimeException("No App has been set yet");
        }

        return SpecsCollections.last(appStack);
    }

    /**
     * Returns the nth app from the top of the stack.<br>
     * <br>
     * E.g., if index is 1, returns the App immediately under the top of the stack.
     *
     * @param index
     * @return the nth app from the top of the stack, or empty if none exists
     */
    public Optional<App> getApp(int index) {
        if (index >= appStack.size()) {
            return Optional.empty();
        }

        return Optional.of(appStack.get(appStack.size() - index - 1));
    }

    // public <T> T get(DataKey<T> key) {
    // return this.data.get(key);
    // }

    // public Standard getStandard() {
    // return standard;
    // }

    /*
    public ClavaIdGenerator getIds() {
        return idGenerator;
        // return data.get(ID_GENERATOR);
    }
    
    
    
    public List<String> getParsingArguments() {
        return arguments;
        // return data.get(ARGUMENTS);
    }
    */

    // @Override
    // public String toString() {
    // return "ClavaContext:" + Integer.toString(hashCode());
    // }
}
