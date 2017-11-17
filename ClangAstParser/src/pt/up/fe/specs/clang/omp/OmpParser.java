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

package pt.up.fe.specs.clang.omp;

import static pt.up.fe.specs.clava.ast.omp.OmpDirectiveKind.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.pragma.PragmaParser;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.omp.OmpClausePragma;
import pt.up.fe.specs.clava.ast.omp.OmpDirectiveKind;
import pt.up.fe.specs.clava.ast.omp.OmpLiteralPragma;
import pt.up.fe.specs.clava.ast.omp.OmpPragma;
import pt.up.fe.specs.clava.ast.omp.SimpleOmpPragma;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind;
import pt.up.fe.specs.clava.ast.pragma.GenericPragma;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;

public class OmpParser implements PragmaParser {

    private static final Set<OmpDirectiveKind> SIMPLE_OMP_PRAGMA = new HashSet<>(
            Arrays.asList(DECLARE_TARGET, END_DECLARE_TARGET, TASKYIELD, MASTER, BARRIER, TASKWAIT, TASKGROUP,
                    ORDERED));

    private static final Set<OmpDirectiveKind> UNIMPLEMENTED_OMP_PRAGMA = new HashSet<>(
            Arrays.asList(CRITICAL, ATOMIC, FLUSH, CANCEL, CANCELATION_POINT, THREADPRIVATE,
                    DECLARE_REDUCTION));

    private static final Map<OmpDirectiveKind, Function<StringParser, OmpPragma>> SPECIFIC_OMP_PARSERS;
    static {
        SPECIFIC_OMP_PARSERS = new HashMap<>();
        // SPECIFIC_OMP_PARSERS.put(ATOMIC, value);
    }

    public OmpPragma parse(Pragma pragma) {

        String ompString = pragma.getFullContent();

        // Sanitize spaces

        String sanitizedString = Arrays.stream(ompString.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));

        StringParser pragmaParser = new StringParser(sanitizedString);

        // Check omp pragma
        Preconditions.checkArgument(pragmaParser.apply(StringParsers::parseWord).toLowerCase().equals("omp"));

        // Parse directive
        OmpDirectiveKind ompDirective = pragmaParser.apply(OmpDirectiveKind::parseOmpDirective);

        if (UNIMPLEMENTED_OMP_PRAGMA.contains(ompDirective)) {
            ClavaLog.info("OpenMP directive not implemented yet: " + ompDirective.getString());
            return new OmpLiteralPragma(ompDirective, pragma.getFullContent(), pragma.getInfo());
        }

        // Simple OpenMP Pragma
        if (SIMPLE_OMP_PRAGMA.contains(ompDirective)) {
            return new SimpleOmpPragma(ompDirective, pragma.getInfo());
        }

        // There are some special cases, directive 'critical' takes a name, some directives to not take clauses,
        // 'atomic' takes a list of pre-defined keywords
        Function<StringParser, OmpPragma> specificFunc = SPECIFIC_OMP_PARSERS.get(ompDirective);
        if (specificFunc != null) {
            return specificFunc.apply(pragmaParser);
        }

        // Check if there are clauses to parse
        boolean hasClauses = !pragmaParser.toString().trim().isEmpty();

        // Parse clauses
        Optional<Map<OmpClauseKind, List<OmpClause>>> clausesMap = OmpClauseParsers.parse(pragmaParser);

        // If no map and there were clauses to parse, at least one of the clauses could not be parsed. Return a literal
        // pragma
        if (hasClauses && !clausesMap.isPresent()) {
            return new OmpLiteralPragma(ompDirective, pragma.getFullContent(), pragma.getInfo());
        }

        return clausesMap.map(clauses -> (OmpPragma) new OmpClausePragma(ompDirective, clauses, pragma.getInfo()))
                .orElseGet(() -> new OmpLiteralPragma(ompDirective, pragma.getFullContent(), pragma.getInfo()));

        // Map<OmpClauseKind, OmpClause> clauses = OmpClauseParsers.parse(pragmaParser);
        //
        // return new OmpPragmaWithClauses(ompDirective, clauses, pragma.getInfo());
    }

    @Override
    public Pragma parse(StringParser contents, ClavaNodeInfo info) {
        Pragma pragma = parse(new GenericPragma(Arrays.asList("omp " + contents.toString()), info));
        contents.clear();
        return pragma;
    }

}
