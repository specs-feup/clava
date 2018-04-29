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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

public class ClavaContext {

    /// DATAKEYS BEGIN

    /**
     * IDs generator
     */
    public final static DataKey<ClavaIdGenerator> ID_GENERATOR = KeyFactory
            .object("idGenerator", ClavaIdGenerator.class)
            .setDefault(() -> new ClavaIdGenerator());

    public final static DataKey<ClavaFactory> FACTORY = KeyFactory
            .object("factory", ClavaFactory.class);

    /// DATAKEYS END

    private final DataStore data;

    public ClavaContext() {
        this.data = DataStore.newInstance(getClass());

        // Initialize factory
        this.data.add(FACTORY, new ClavaFactory(this));
    }
}
