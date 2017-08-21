/**
 * Copyright 2016 SPeCS.
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

import java.util.List;

import org.lara.interpreter.joptions.gui.LaraLauncher;

import pt.up.fe.specs.util.SpecsSystem;

public class ClavaWeaverLauncher {

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();
        // System.out.println("Press any key to proceed");
        // try {
        // System.in.read();
        // } catch (IOException e) {
        // SpecsLogs.msgWarn("Error message:\n", e);
        // }
        execute(args);
    }

    public static boolean execute(String[] args) {
        // To profile using VisualVM
        // try {
        // System.out.println("PRESS ENTER");
        // System.in.read();
        // } catch (IOException e) {
        // LoggingUtils.msgWarn("Error message:\n", e);
        // }
        return LaraLauncher.launch(args, new CxxWeaver());
    }

    public static boolean execute(List<String> args) {
        return execute(args.toArray(new String[0]));
    }
}
