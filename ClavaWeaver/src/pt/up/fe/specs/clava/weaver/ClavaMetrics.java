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

import org.lara.interpreter.profile.BasicWeaverProfiler;
import org.lara.interpreter.profile.ReportWriter;
import org.lara.interpreter.profile.WeavingReport;
import org.lara.interpreter.weaver.interf.events.data.ActionEvent;

@Deprecated
public class ClavaMetrics extends BasicWeaverProfiler {

    @Override
    public WeavingReport getReport() {
        return super.getReport();
    }

    @Override
    protected void buildReport(ReportWriter writer) {}

    @Override
    protected void onActionImpl(ActionEvent data) {}
}
