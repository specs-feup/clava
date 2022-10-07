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

package pt.up.fe.specs.clava.weaver.gears;

import org.lara.interpreter.weaver.interf.AGear;
import org.lara.interpreter.weaver.interf.events.data.ApplyEvent;

import com.google.common.base.Preconditions;

public class InsideApplyGear extends AGear {

    // State
    private int insideApplyCounter;

    public InsideApplyGear() {
        insideApplyCounter = 0;
    }

    public boolean isInsideApply() {
        return insideApplyCounter > 0;
    }

    @Override
    public void onApply(ApplyEvent data) {
        switch (data.getStage()) {
        case BEGIN:
            insideApplyCounter++;
            break;
        case END:
            Preconditions.checkArgument(insideApplyCounter > 0,
                    "Leaving apply, expected apply counter to be a number greater than zero, is " + insideApplyCounter);
            insideApplyCounter--;
        default:
            // Do nothing
            break;
        }
    }
}
