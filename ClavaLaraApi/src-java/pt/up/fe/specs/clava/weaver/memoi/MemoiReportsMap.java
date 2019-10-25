package pt.up.fe.specs.clava.weaver.memoi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class MemoiReportsMap {

    Map<String, Map<String, List<String>>> map = new HashMap<String, Map<String, List<String>>>();

    public void put(String target, String callSite, String reportPath) {

        Map<String, List<String>> sitesMap = getSitesMap(target);
        put(sitesMap, callSite, reportPath);
    }

    private Map<String, List<String>> getSitesMap(String target) {

        if (map.containsKey(target)) {

            return map.get(target);
        } else {

            HashMap<String, List<String>> innerMap = new HashMap<String, List<String>>();
            map.put(target, innerMap);

            return innerMap;
        }
    }

    private void put(Map<String, List<String>> sitesMap, String callSite, String reportPath) {

        if (sitesMap.containsKey(callSite)) {

            sitesMap.get(callSite).add(reportPath);
        } else {

            List<String> reportList = new ArrayList<String>();
            reportList.add(reportPath);

            sitesMap.put(callSite, reportList);
        }
    }

    @Override
    public String toString() {

        return map.toString();
    }
}
