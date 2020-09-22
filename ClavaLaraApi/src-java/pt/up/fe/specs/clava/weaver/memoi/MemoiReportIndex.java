package pt.up.fe.specs.clava.weaver.memoi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import com.google.gson.Gson;

import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsLogs;

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

public class MemoiReportIndex {

    private String id;
    private String funcSig;
    private List<String> call_sites;

    public static MemoiReportIndex fromFile(File file) {

        SpecsCheck.checkArgument(file.exists(), () -> "the file " + file + " doesn't exist");

        FileReader fr = null;
        try {
            fr = new FileReader(file);
        } catch (FileNotFoundException e) {
            SpecsLogs.warn("Could not find the file " + file.getAbsolutePath());
        }

        BufferedReader br = new BufferedReader(fr);
        MemoiReportIndex fromJson = new Gson().fromJson(br, MemoiReportIndex.class);
        return fromJson;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFuncSig() {
        return funcSig;
    }

    public void setFuncSig(String funcSig) {
        this.funcSig = funcSig;
    }

    public List<String> getCall_sites() {
        return call_sites;
    }

    public void setCall_sites(List<String> call_sites) {
        this.call_sites = call_sites;
    }
}
