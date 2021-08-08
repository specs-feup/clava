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

package pt.up.fe.specs.clang.includes;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.collections.MultiMap;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * @deprecated
 * @author JBispo
 *
 */
@Deprecated
public class ClangIncludes {

    // Maps the includes of each file
    private final MultiMap<String, Include> includes;

    private ClangIncludes() {
        includes = new MultiMap<>();
    }

    public static ClangIncludes newInstance(File includesFile) {
        ClangIncludes includes = new ClangIncludes();

        ClangIncludesParser parser = new ClangIncludesParser();
        try (LineStream lines = LineStream.newInstance(includesFile)) {
            lines.stream()
                    .flatMap(line -> SpecsCollections.toStream(parser.parse(line)))
                    // .filter(ClangIncludes::filterHeaderFile)
                    .forEach(include -> includes.add(include));
        }

        return includes;
    }

    // private static final boolean isValidInclude(Include include) {
    //
    // if (include.isAngled()) {
    // return true;
    // }
    //
    // return SourceType.isHeader(new File(include.getInclude()));
    //
    // // System.out.println("INCLUDE: " + include.getInclude());
    // // System.out.println("IS HEADER? " + isHeader);
    // // return isHeader;
    // // var isHeader = SourceType.isHeader(include.getSourceFile());
    //
    // //
    // // return isHeader;
    // }

    private void add(Include include) {
        // if (!isValidInclude(include)) {
        // ClavaLog.info(() -> "ClangIncludes: filtering out include '" + include.getInclude() + "' in source file "
        // + include.getSourceFile());
        // return;
        // }

        includes.put(SpecsIo.getCanonicalPath(include.getSourceFile()), include);
    }

    public List<Include> getIncludes(File sourceFile) {
        List<Include> sourceIncludes = includes.get(SpecsIo.getCanonicalPath(sourceFile));

        if (sourceIncludes == null) {
            throw new RuntimeException("Could not find includes for source file '" + sourceFile + "'");
        }

        return sourceIncludes;
    }

    /**
     * Creates a map from include string to the Include object, for all includes in this object.
     * 
     * @return
     */
    public Map<String, Include> buildIncludeMap() {
        Map<String, Include> allIncludes = new HashMap<>();

        for (List<Include> includesList : includes.values()) {
            for (Include include : includesList) {
                allIncludes.put(include.getInclude(), include);
            }
        }

        return allIncludes;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (List<Include> includes : includes.values()) {
            // Get filename from first include
            String source = includes.get(0).getSourceFile().getName();
            builder.append("Includes for '" + source + "':\n");

            builder.append(includes.stream()
                    .map(Include::toString)
                    .collect(Collectors.joining(", ")));

            builder.append("\n");
        }

        return builder.toString();
    }

}
