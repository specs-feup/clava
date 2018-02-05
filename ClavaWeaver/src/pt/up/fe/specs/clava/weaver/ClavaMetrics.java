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

import java.util.Set;

import org.lara.interpreter.profile.BasicWeaverProfiler;
import org.lara.interpreter.profile.ReportWriter;
import org.lara.interpreter.weaver.interf.events.Stage;
import org.lara.interpreter.weaver.interf.events.data.ActionEvent;

import pt.up.fe.specs.util.SpecsCollections;

public class ClavaMetrics extends BasicWeaverProfiler {

    private static final Set<String> ADDITIONAL_INSERT_ACTIONS = SpecsCollections.newHashSet("insertBefore",
            "insertAfter", "replaceWith", "insertBegin", "insertEnd");

    @Override
    protected void buildReport(ReportWriter writer) {
        // System.out.println("REPORT:" + writer);
    }

    @Override
    protected void onActionImpl(ActionEvent data) {
        // Update insert actions, after the action
        if (data.getStage().equals(Stage.END)) {
            boolean isAdditionalInsert = ADDITIONAL_INSERT_ACTIONS.contains(data.getActionName());
            boolean isInsert = data.getActionName().equals("insert");

            // TEMP: Only while native loc are not updated by the default profiler
            if (isInsert) {
                reportNativeLoc(data.getArguments().get(1), true);
            }

            // Update stats for additional inserts
            if (isAdditionalInsert) {
                getReport().incInserts();
                reportNativeLoc(data.getArguments().get(0), true);
            }
        }

    }

    /*
    private String currentClass;
    private String currentMethod;
    private String currentField;
    
    private AccumulatorMap<String> actionsPerformed = new AccumulatorMap<>();
    
    @Override
    protected void onApplyImpl(ApplyIterationEvent data) {
        if (data.getStage().equals(Stage.BEGIN)) {
            for (JoinPoint jp : data.getPointcutChain()) {
                if (jp instanceof AClass) {
                    currentClass = ((AClass) jp).getQualifiedNameImpl();
                    continue;
                }
                if (jp instanceof AMethod) {
                    currentMethod = ((AMethod) jp).getNameImpl();
                    continue;
                }
                if (jp instanceof AField) {
                    currentField = ((AMethod) jp).getNameImpl();
                    continue;
                }
            }
        } else {
            currentClass = null;
            currentMethod = null;
        }
    }
    
    @Override
    protected void onActionImpl(ActionEvent data) {
    
        if (data.getStage().equals(Stage.END)) {
            String name = "";
            if (currentClass != null) {
                name = currentClass;
            }
            if (currentMethod != null) {
                name += (currentClass != null ? "#" : "") + currentMethod;
            } else if (currentField != null) {
                name += (currentClass != null ? "#" : "") + currentField;
            }
            if (name.isEmpty()) {
                return; // What to do?
            }
            actionsPerformed.add(name);
        }
    }
    
    @Override
    protected void buildReport(ReportWriter writer) {
        writer.report("ActionsPerTarget", actionsPerformed.getAccMap());
    }
    */

}
