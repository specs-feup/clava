package pt.up.fe.specs.clava.weaver.memoi.comparator;

import java.util.Comparator;

import pt.up.fe.specs.clava.weaver.memoi.MemoiUtils;
import pt.up.fe.specs.clava.weaver.memoi.MergedMemoiEntry;
import pt.up.fe.specs.clava.weaver.memoi.MergedMemoiReport;

/**
 * Copyright 2019 SPeCS.
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

public class MeanComparator implements Comparator<MergedMemoiEntry>, java.io.Serializable {

    private static final long serialVersionUID = -212004096753106698L;

    private MergedMemoiReport report;

    public MeanComparator() {
    }

    public MeanComparator(MergedMemoiReport report) {
        this.report = report;
    }

    // @SuppressWarnings("unchecked")
    // public final static Comparator<MergedMemoiEntry> mean(MergedMemoiReport report) {
    //
    // int total = report.getReportCount();
    //
    // return (MergedMemoiEntry e1, MergedMemoiEntry e2) -> {
    //
    // double mean1 = MemoiUtils.mean(e1.getCounter(), total);
    // double mean2 = MemoiUtils.mean(e2.getCounter(), total);
    //
    // if (mean1 > mean2) {
    // return 1;
    // } else if (mean1 < mean2) {
    // return -1;
    // } else {
    // return 0;
    // }
    // };
    // }

    @Override
    public int compare(MergedMemoiEntry o1, MergedMemoiEntry o2) {

        int total = report.getReportCount();

        double mean1 = MemoiUtils.mean(o1.getCounter(), total);
        double mean2 = MemoiUtils.mean(o2.getCounter(), total);

        if (mean1 > mean2) {
            return 1;
        } else if (mean1 < mean2) {
            return -1;
        } else {
            return 0;
        }
    }

    public MergedMemoiReport getReport() {
        return report;
    }

    public void setReport(MergedMemoiReport report) {
        this.report = report;
    }
}
