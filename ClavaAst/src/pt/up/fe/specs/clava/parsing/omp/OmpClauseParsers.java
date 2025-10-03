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

package pt.up.fe.specs.clava.parsing.omp;

import static pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ast.omp.OmpDirectiveKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpClauseKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpDefaultClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpDefaultClause.DefaultKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpIfClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpIntegerExpressionClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpListClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpProcBindClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpProcBindClause.ProcBindKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpReductionClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpReductionClause.ReductionKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpScheduleClause;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpScheduleClause.ScheduleKind;
import pt.up.fe.specs.clava.ast.omp.clauses.OmpScheduleClause.ScheduleModifier;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.stringparser.ParserResult;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;
import pt.up.fe.specs.util.utilities.StringSlice;

public class OmpClauseParsers {

    private static final Map<OmpClauseKind, Function<StringParser, OmpClause>> OMP_CLAUSE_PARSERS;
    static {
        OMP_CLAUSE_PARSERS = new HashMap<>();
        OMP_CLAUSE_PARSERS.put(SCHEDULE, OmpClauseParsers::parseSchedule);
        OMP_CLAUSE_PARSERS.put(REDUCTION, OmpClauseParsers::parseReduction);
        // OMP_CLAUSE_PARSERS.put(NUM_THREADS, OmpClauseParsers::parseNumThreads);

        OMP_CLAUSE_PARSERS.put(PROC_BIND, OmpClauseParsers::parseProcBind);
        OMP_CLAUSE_PARSERS.put(DEFAULT, OmpClauseParsers::parseDefault);

        OMP_CLAUSE_PARSERS.put(NUM_THREADS, parser -> OmpClauseParsers.parseInteger(parser, NUM_THREADS, false, false));
        OMP_CLAUSE_PARSERS.put(COLLAPSE, parser -> OmpClauseParsers.parseInteger(parser, COLLAPSE, false, true));
        OMP_CLAUSE_PARSERS.put(ORDERED, parser -> OmpClauseParsers.parseInteger(parser, ORDERED, true, true));

        OMP_CLAUSE_PARSERS.put(PRIVATE, parser -> OmpClauseParsers.parseListClause(parser, PRIVATE));
        OMP_CLAUSE_PARSERS.put(FIRSTPRIVATE, parser -> OmpClauseParsers.parseListClause(parser, FIRSTPRIVATE));
        OMP_CLAUSE_PARSERS.put(LASTPRIVATE, parser -> OmpClauseParsers.parseListClause(parser, LASTPRIVATE));
        OMP_CLAUSE_PARSERS.put(SHARED, parser -> OmpClauseParsers.parseListClause(parser, SHARED));
        OMP_CLAUSE_PARSERS.put(COPYIN, parser -> OmpClauseParsers.parseListClause(parser, COPYIN));
    }

    public static Optional<Map<OmpClauseKind, List<OmpClause>>> parse(StringParser pragmaParser,
            OmpDirectiveKind directive) {

        Map<OmpClauseKind, List<OmpClause>> clauses = new LinkedHashMap<>();

        // Apply rules while there are clauses
        while (!pragmaParser.isEmpty()) {

            Optional<OmpClause> parsedClause = pragmaParser.apply(OmpClauseParsers::parseOmpClause, directive);

            // If empty, means that clause is not supported, return empty
            if (!parsedClause.isPresent()) {
                return Optional.empty();
            }

            OmpClause clause = parsedClause.get();

            List<OmpClause> clausesList = clauses.get(clause.getKind());
            if (clausesList == null) {
                clausesList = new ArrayList<>();
                clauses.put(clause.getKind(), clausesList);
            }

            clausesList.add(clause);
            // parsedClause.ifPresent(clause -> clauses.put(clause.getKind(), clause));

        }

        return Optional.of(clauses);
    }

    private static ParserResult<Optional<OmpClause>> parseOmpClause(StringSlice string, OmpDirectiveKind directive) {
        // Identify kind of clause
        OmpClauseKind clauseKind = getClauseKind(string);
        StringParser pragmaParser = new StringParser(string);

        OmpClause clause = parse(pragmaParser, clauseKind, directive);

        if (clause == null) {
            return new ParserResult<>(string, Optional.empty());
        }
        // clauses.put(clauseKind, clause);

        // Remove unused spaces
        pragmaParser.trim();

        // If starts with ',' remove
        pragmaParser.apply(StringParsers::checkCharacter, ',');

        return new ParserResult<Optional<OmpClause>>(pragmaParser.getCurrentString(), Optional.of(clause));
    }

    private static OmpClause parse(StringParser pragmaParser, OmpClauseKind clauseKind, OmpDirectiveKind directive) {

        // If clause, needs directive
        if (clauseKind == OmpClauseKind.IF) {
            return parseIf(pragmaParser, clauseKind, directive);
        }

        // Clause parsers that only need the StringParser
        Function<StringParser, OmpClause> clauseParser = OMP_CLAUSE_PARSERS.get(clauseKind);

        if (clauseParser == null) {
            ClavaLog.info("Clause not implemented yet: " + clauseKind.getString());
            return null;
        }
        // Objects.requireNonNull(clauseParser, () -> "Clause not implemented yet: " + clauseKind);

        // Remove unused spaces
        // without this, the next call will fail to match any OmpClauseKind when parseClauseName is called
        // we can also hide this trim call in parseClauseName, but we need to make sure every parsing function will
        // call parseClauseName
        // pragmaParser.trim();

        // OmpClause clause = pragmaParser.applyFunction(clauseParser);
        return clauseParser.apply(pragmaParser);
    }

    private static OmpClauseKind getClauseKind(StringSlice currentPragma) {
        StringParser currentPragmaParser = new StringParser(currentPragma);

        // Remove unused spaces
        // without this, the next call will return an empty string if there are spaces between clauses
        // currentPragmaParser.trim();

        // Get up to the first space
        String currentPragmaPrefix = currentPragmaParser.apply(StringParsers::parseWord);

        // Check if clause has parenthesis
        int indexOfPar = currentPragmaPrefix.indexOf('(');

        String clauseName = indexOfPar == -1 ? currentPragmaPrefix : currentPragmaPrefix.substring(0, indexOfPar);

        return getHelper().fromValue(clauseName.toLowerCase().trim());
    }

    /**
     * Consumes the clause name and parameters from clauses, returns the contents of the parameters.
     * 
     * @param clauseKind
     * @param clauses
     * @return the contents of the parameters
     */
    private static StringParser parseClauseName(OmpClauseKind clauseKind, StringParser clauses) {
        return parseClauseName(clauseKind, clauses, false);
    }

    /**
     * Consumes the clause name and parameters from clauses, if present, returns the contents of the parameters or null
     * string if there are no parameters.
     * 
     * @param clauseKind
     * @param clauses
     * @param optionalParams
     * @return
     */
    private static StringParser parseClauseName(OmpClauseKind clauseKind, StringParser clauses,
            boolean optionalParams) {

        StringSlice currentString = clauses.getCurrentString();
        String clauseKindName = clauseKind.getString();

        boolean hasParameters = hasParameters(currentString, clauseKindName);

        if (!optionalParams) {
            Preconditions.checkArgument(hasParameters);
        }

        // If no parameters, just consume the clause name and return empty string
        if (!hasParameters) {
            clauses.apply(StringParsers::checkStringStarts, clauseKindName);
            return null;
        }

        // Find end parenthesis
        int closeParIndex = SpecsStrings.findCloseParenthesisIndex(clauses.getCurrentString().toString());
        // int closeParIndex = clauses.getCurrentString().indexOf(')');
        String scheduleClauseString = clauses.substring(closeParIndex + 1);
        StringParser clause = new StringParser(scheduleClauseString);

        clause.apply(StringParsers::checkStringStarts, clauseKindName);
        clause.trim();
        clause.apply(StringParsers::checkStringStarts, "(");
        clause.apply(StringParsers::checkStringEnds, ")");

        return clause;
    }

    /*
    private static int findCloseParIndex(String string) {
        int openParIndex = string.indexOf('(');
    
        int innerOpenPars = 0;
        for (int i = openParIndex + 1; i < string.length(); i++) {
            if (string.charAt(i) == ')') {
                if (innerOpenPars == 0) {
                    return i;
                } else {
                    innerOpenPars--;
                }
    
                continue;
            }
    
            if (string.charAt(i) == '(') {
                innerOpenPars++;
                continue;
            }
        }
    
        throw new RuntimeException("Could not find closing parenthesis for the OpenMP pragma portion '" + string + "'");
    }
    */

    private static boolean hasParameters(StringSlice currentString, String clauseKindName) {
        int openParIndex = currentString.indexOf('(');

        // No opening parenthesis, no parameters
        if (openParIndex == -1) {
            return false;
        }

        // Check if what appears before the parenthesis is the expected word
        return currentString.substring(0, openParIndex).trim().toString().toLowerCase().equals(clauseKindName);
    }

    /**
     * Expects code in the form 'schedule([modifier [, modifier]:]kind[, chunk_size])'
     *
     * @param clause
     * @return
     */
    public static OmpScheduleClause parseSchedule(StringParser clauses) {
        /*
        int closeParIndex = clauses.getCurrentString().indexOf(')');
        Preconditions.checkArgument(closeParIndex != -1);
        
        String scheduleClauseString = clauses.substring(closeParIndex + 1);
        StringParser clause = new StringParser(scheduleClauseString);
        
        clause.apply(ClangGenericParsers::checkStringStarts, "schedule(");
        clause.apply(ClangGenericParsers::checkStringEnds, ")");
        */
        StringParser clause = parseClauseName(OmpClauseKind.SCHEDULE, clauses);

        String args = clause.toString();
        int colonIndex = args.indexOf(':');
        List<String> modifiersStrings = colonIndex == -1 ? Collections.emptyList()
                : parseList(args.substring(0, colonIndex));

        List<ScheduleModifier> modifiers = ScheduleModifier.getHelper().fromValue(modifiersStrings);

        args = colonIndex == -1 ? args : args.substring(colonIndex + 1);

        int commaIndex = args.indexOf(',');

        String scheduleString = commaIndex == -1 ? args : args.substring(0, commaIndex);
        ScheduleKind schedule = ScheduleKind.getHelper().fromValue(scheduleString.trim());

        String chunkSize = commaIndex == -1 ? null : args.substring(commaIndex + 1).trim();
        // Integer chunkSize = commaIndex == -1 ? null : Integer.decode(args.substring(commaIndex + 1).trim());

        return new OmpScheduleClause(schedule, chunkSize, modifiers);
    }

    /**
     * Parses a comma-separated list.
     *
     * @param list
     * @return
     */
    public static List<String> parseList(String list) {
        String[] array = list.toString().split(",");
        return Arrays.stream(array)
                .map(variable -> variable.trim())
                .collect(Collectors.toList());
    }

    private static OmpClause parseListClause(StringParser clauses, OmpClauseKind kind) {

        StringParser clause = parseClauseName(kind, clauses);

        List<String> variables = parseList(clause.toString());

        return new OmpListClause(kind, variables);

    }

    private static OmpReductionClause parseReduction(StringParser clauses) {
        StringParser clause = parseClauseName(REDUCTION, clauses);

        String args = clause.toString();

        // kind of reduction
        int colonIndex = clause.toString().indexOf(':');

        if (colonIndex < 1) {
            throw new RuntimeException("Badly formed reduction clause: reduction(" + args + ")"); // need more
                                                                                                  // information
        }

        String kindString = args.substring(0, colonIndex).trim();
        ReductionKind reductionKind = ReductionKind.getHelper().fromValue(kindString);

        // variable list
        List<String> variables = parseList(args.substring(colonIndex + 1));

        return new OmpReductionClause(reductionKind, variables);
    }

    /*
    private static OmpNumThreadsClause parseNumThreads(StringParser clauses) {
    
        StringParser clause = parseClauseName(NUM_THREADS, clauses);
    
        // String expression = clause.apply(StringParsers::parseWord);
        String expression = clause.toString();
    
        return new OmpNumThreadsClause(expression);
    }
    */

    private static OmpIntegerExpressionClause parseInteger(StringParser clauses, OmpClauseKind kind,
            boolean isOptional, boolean isConstantPositive) {

        StringParser clause = parseClauseName(kind, clauses, isOptional);
        // If clause is not empty, parse word

        // Peek if starts with '('
        String expression = clause != null ? clause.toString() : null;

        return new OmpIntegerExpressionClause(kind, expression, isOptional, isConstantPositive);
    }

    private static OmpIfClause parseIf(StringParser clauses, OmpClauseKind kind, OmpDirectiveKind directive) {

        StringParser clause = parseClauseName(kind, clauses, false);

        // Test if it has the name directive, followed by a colon
        Optional<String> directiveIfName = directive.getIfName();

        // If no directive if name, check if CANCEL directive
        if (!directiveIfName.isPresent() && directive != OmpDirectiveKind.CANCEL) {
            throw new RuntimeException("Directive '" + directive + "' does not support the 'if' clause");
        }

        String directiveName = null;
        boolean hasColon = false;
        // System.out.println("DIRECTIVE IF NAME:" + directiveIfName);
        if (directiveIfName.isPresent()) {
            // System.out.println("CLAUSES BEFORE:" + clause);
            directiveName = clause.apply(StringParsers::checkStringStarts, directiveIfName.get(), false).orElse(null);
            // System.out.println("DIRECTIVE NAME:" + directiveName);
            // System.out.println("CLAUSES AFTER:" + clause);
            hasColon = clause.apply(StringParsers::checkStringStarts, ":").isPresent();
        }

        // If has colon, directive name must not be null, and this is an if clause with directive name
        if (hasColon) {
            Objects.requireNonNull(directiveName,
                    () -> "Since it has a colon, expected the directive name preceeding the colon:" + clauses.toString());

            String expression = clause.clear();
            return new OmpIfClause(directiveName, expression);
        }

        // If no colon, if clause just has expression
        String expression = "";

        // If directiveName is not null, use as prefix of expression
        if (directiveName != null) {
            expression += directiveName;
        }

        // Use remaining of the clause as suffix
        expression += clause.clear();

        return new OmpIfClause(expression);
    }

    private static OmpProcBindClause parseProcBind(StringParser clauses) {

        StringParser clause = parseClauseName(PROC_BIND, clauses);

        String arg = clause.toString().trim();
        ProcBindKind kind = ProcBindKind.getHelper().fromValue(arg);

        return new OmpProcBindClause(kind);
    }

    private static OmpDefaultClause parseDefault(StringParser clauses) {

        StringParser clause = parseClauseName(DEFAULT, clauses);

        String arg = clause.toString().trim();
        DefaultKind kind = DefaultKind.getHelper().fromValue(arg);

        return new OmpDefaultClause(kind);
    }
}
