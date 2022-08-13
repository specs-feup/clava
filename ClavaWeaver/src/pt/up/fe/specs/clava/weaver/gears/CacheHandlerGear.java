/**
 * Copyright 2022 SPeCS.
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

package pt.up.fe.specs.clava.weaver.gears;

import org.lara.interpreter.weaver.interf.AGear;
import org.lara.interpreter.weaver.interf.events.data.ActionEvent;

import pt.up.fe.specs.clava.weaver.CxxWeaver;

public class CacheHandlerGear extends AGear {

    /**
     * Every time an action is called, clear cache
     */
    @Override
    public void onAction(ActionEvent data) {
        CxxWeaver.getCxxWeaver().getApp().clearCache();
    }

}
