package pt.up.fe.specs.clava.weaver.memoi;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsIo;

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

    public static Map<String, Map<String, List<String>>> fromDirName(String dirName) {

//        System.out.println("buildReportsMap(dirName)");

        File dir = new File(dirName);

        SpecsCheck.checkArgument(dir.exists(), () -> "the directory " + dirName + " doesn't exist");
        SpecsCheck.checkArgument(dir.isDirectory(), () -> dirName + " is not a directory");

        List<File> reportFiles = SpecsIo.getFiles(dir, "json");

        return fromFiles(reportFiles);
    }

    public static Map<String, Map<String, List<String>>> fromNames(String[] fileNames) {

//        System.out.println("buildReportsMapFromNames(fileNames)");

        return fromNames(Arrays.asList(fileNames));
    }

    public static Map<String, Map<String, List<String>>> fromNames(List<String> fileNames) {

//        System.out.println("buildReportsMapFromNames(L[fileNames)");

        List<File> files = new ArrayList<>();

        for (String fileName : fileNames) {

            File file = new File(fileName);

            SpecsCheck.checkArgument(file.exists(), () -> "the file " + fileName + " doesn't exist");
            SpecsCheck.checkArgument(file.isFile(), () -> fileName + " is not a file");

            files.add(file);
        }

        return fromFiles(files);
    }

    public static Map<String, Map<String, List<String>>> fromFiles(List<File> reportFiles) {

//        System.out.println("buildReportsMapFromFiles(reportFiles)");

        Map<String, Map<String, List<String>>> map = new HashMap<String, Map<String, List<String>>>();

        for (File reportFile : reportFiles) {

            MemoiReport report = MemoiReport.fromFile(reportFile);
            put(map, report.getFuncSig(), String.join("#", report.getCall_sites()), reportFile.getAbsolutePath());
        }

        return map;
    }

    private static void put(Map<String, Map<String, List<String>>> map, String target, String callSite,
            String reportPath) {

        Map<String, List<String>> sitesMap = getSitesMap(map, target);
        put(sitesMap, callSite, reportPath);
    }

    private static Map<String, List<String>> getSitesMap(Map<String, Map<String, List<String>>> map, String target) {

        if (map.containsKey(target)) {

            return map.get(target);
        } else {

            HashMap<String, List<String>> innerMap = new HashMap<String, List<String>>();
            map.put(target, innerMap);

            return innerMap;
        }
    }

    private static void put(Map<String, List<String>> sitesMap, String callSite, String reportPath) {

        if (sitesMap.containsKey(callSite)) {

            sitesMap.get(callSite).add(reportPath);
        } else {

            List<String> reportList = new ArrayList<String>();
            reportList.add(reportPath);

            sitesMap.put(callSite, reportList);
        }
    }
}
