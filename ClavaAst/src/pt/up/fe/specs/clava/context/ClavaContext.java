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

package pt.up.fe.specs.clava.context;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ast.LegacyToDataStore;

public class ClavaContext extends ADataClass<ClavaContext> {

    /// DATAKEYS BEGIN

    /**
     * The arguments used to call the parser.
     */
    public final static DataKey<Map<File, List<String>>> ARGUMENTS = KeyFactory
            .generic("arguments", new HashMap<>());

    /**
     * IDs generator
     */
    public final static DataKey<ClavaIdGenerator> ID_GENERATOR = KeyFactory
            .object("idGenerator", ClavaIdGenerator.class)
            .setDefault(() -> new ClavaIdGenerator());

    public final static DataKey<ClavaFactory> FACTORY = KeyFactory
            .object("factory", ClavaFactory.class);

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

    public ClavaContext() {

        // this.data = DataStore.newInstance(getClass());

        // Set arguments
        set(ARGUMENTS, new HashMap<>());

        // Initialize factory
        set(FACTORY, new ClavaFactory(this));

        // Set ClavaNodeFactory
        // TODO: Temporary transition measure
        LegacyToDataStore.CLAVA_CONTEXT.set(this);
    }

    public ClavaContext addArguments(File sourceFile, List<String> arguments) {
        get(ARGUMENTS).put(sourceFile, arguments);
        return this;
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
    
    public ClavaFactory getFactory() {
        return factory;
        // return data.get(FACTORY);
    }
    
    public List<String> getParsingArguments() {
        return arguments;
        // return data.get(ARGUMENTS);
    }
    */

    @Override
    public String toString() {
        return "ClavaContext:" + Integer.toString(hashCode());
    }
}
