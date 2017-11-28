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

package pt.up.fe.specs.clava.transform.call;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.extra.data.IdNormalizer;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

/**
 * Extracts the list of statements from function that can be inlined.
 * 
 * @author JoaoBispo
 *
 */
public class FunctionExtractorForInlining {

    // If there are function declarations in header files,
    // avoids extracting multiple times the same function
    private final IdNormalizer idNormalizer;
    private final Map<String, List<Stmt>> functionCache;

    public FunctionExtractorForInlining() {
        this(null);
    }

    public FunctionExtractorForInlining(IdNormalizer idNormalizer) {
        this.idNormalizer = idNormalizer;
        this.functionCache = new HashMap<>();
    }

    public Optional<List<Stmt>> extractFunction(FunctionDecl function) {
        // Check if function has been already extracted
        List<Stmt> cachedStmts = getCachedStatements(function);
        if (cachedStmts != null) {
            return Optional.of(cachedStmts);
        }

        CompoundStmt compoundStmt = function.getBody().get();

        // Check if function is not recursive

        // Check if it has multiple return points
        if (CallAnalysis.hasMultipleReturnPoints(compoundStmt)) {
            return Optional.empty();
        }

        // Check if nodes are valid
        if (!CallAnalysis.canBeInlined(compoundStmt)) {
            return Optional.empty();
        }

        // Statements can be inlined, store it
        return Optional.of(cacheStatements(function, compoundStmt));
    }

    private List<Stmt> cacheStatements(FunctionDecl function, CompoundStmt compoundStmt) {
        String id = function.getExtendedId().get();

        // Normalize, if present
        if (idNormalizer != null) {
            id = idNormalizer.normalize(id);
        }

        functionCache.put(id, compoundStmt.getStatements());

        return compoundStmt.getStatements();
    }

    private List<Stmt> getCachedStatements(FunctionDecl function) {
        String id = function.getExtendedId().get();

        // Normalize, if present
        if (idNormalizer != null) {
            id = idNormalizer.normalize(id);
        }

        return functionCache.get(id);
    }
}
