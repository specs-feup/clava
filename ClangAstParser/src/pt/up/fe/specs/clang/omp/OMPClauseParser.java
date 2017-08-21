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

package pt.up.fe.specs.clang.omp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.omp.clause.OMPClause;
import pt.up.fe.specs.clava.omp.clause.OMPClauseFactory;
import pt.up.fe.specs.clava.omp.clause.OMPScheduleClause;
import pt.up.fe.specs.clava.omp.clause.OMPScheduleClause.ScheduleKind;
import pt.up.fe.specs.clava.omp.clause.OMPScheduleClause.ScheduleModifier;
import pt.up.fe.specs.clava.omp.clause.OMPSharedClause;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.lazy.ThreadSafeLazy;
import pt.up.fe.specs.util.stringparser.StringParser;

/**
 * @deprecated
 * @author Joao Bispo
 *
 */
@Deprecated
public class OMPClauseParser {

    private static final Lazy<Map<String, OMPClauseConverter>> CONVERTERS = new ThreadSafeLazy<>(
            () -> buildConverters());

    /**
     * Builds converters table.
     *
     * @return
     */
    private static Map<String, OMPClauseConverter> buildConverters() {
        Map<String, OMPClauseConverter> converters = new HashMap<>();

        converters.put("10", OMPClauseParser::parseShared);
        converters.put("17", OMPClauseParser::parseSchedule);

        return converters;
    }

    public OMPClause convert(String clauseKind, String clauseString) {
        OMPClauseConverter converter = CONVERTERS.get().get(clauseKind);
        if (converter == null) {
            throw new RuntimeException("Converter not implemented for clause kind '" + clauseKind + "'");
        }

        return converter.apply(new StringParser(clauseString.trim()));
    }

    /**
     * Parses a comma-separated list.
     *
     * @param list
     * @return
     */
    private static List<String> parseList(String list) {
        String[] array = list.toString().split(",");
        return Arrays.stream(array)
                .map(variable -> variable.trim())
                .collect(Collectors.toList());
    }

    /**
     * Expects code in the form 'shared(list)'.
     *
     * @param clause
     * @return
     */
    private static OMPSharedClause parseShared(StringParser clause) {

        clause.apply(ClangGenericParsers::checkStringStarts, "shared(");
        clause.apply(ClangGenericParsers::checkStringEnds, ")");

        List<String> variables = parseList(clause.toString());

        return OMPClauseFactory.shared(variables);
    }

    /**
     * Expects code in the form 'schedule([modifier [, modifier]:]kind[, chunk_size])'
     *
     * @param clause
     * @return
     */
    private static OMPScheduleClause parseSchedule(StringParser clause) {

        clause.apply(ClangGenericParsers::checkStringStarts, "schedule(");
        clause.apply(ClangGenericParsers::checkStringEnds, ")");

        String args = clause.toString();
        int colonIndex = args.indexOf(':');
        List<String> modifiersStrings = colonIndex == -1 ? Collections.emptyList()
                : parseList(args.substring(0, colonIndex));

        List<ScheduleModifier> modifiers = ScheduleModifier.getHelper().valueOf(modifiersStrings);

        args = colonIndex == -1 ? args : args.substring(colonIndex + 1);

        int commaIndex = args.indexOf(',');

        String scheduleString = commaIndex == -1 ? args : args.substring(0, commaIndex);
        ScheduleKind schedule = ScheduleKind.getHelper().valueOf(scheduleString.trim());

        Integer chunkSize = commaIndex == -1 ? null : Integer.decode(args.substring(commaIndex + 1).trim());

        return OMPClauseFactory.schedule(schedule, chunkSize, modifiers);
    }
}
