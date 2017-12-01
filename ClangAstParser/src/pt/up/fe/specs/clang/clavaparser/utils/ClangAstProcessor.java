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

package pt.up.fe.specs.clang.clavaparser.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.decl.CXXRecordDeclParser;
import pt.up.fe.specs.clang.clavaparser.decl.NamespaceDeclParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.NamespaceDecl;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Process Clang nodes to add extra information (e.g., namespace and RecordDecl names to CXXMethodDecl).
 * 
 * @author JoaoBispo
 *
 */
public class ClangAstProcessor {

    private final ClangConverterTable converter;

    static class ClavaParserHelper<T extends AClangNodeParser, TC extends ClavaNode> {
        private final T parser;
        private final Class<TC> parsedClass;

        private final Map<String, TC> cache;

        ClavaParserHelper(T parser, Class<TC> parsedClass) {
            this.parser = parser;
            this.parsedClass = parsedClass;

            this.cache = new HashMap<>();
        }

        public TC parse(ClangNode node) {
            // Check if node was already parsed
            // TC cachedNode = cache.get(node.getAddressTry().orElse(null));
            TC cachedNode = cache.get(node.hasId() ? node.getExtendedId() : null);
            if (cachedNode != null) {
                return cachedNode;
            }

            // Parse node
            TC parsedNode = parsedClass.cast(parser.parse(node));

            // Add node to the cache
            if (node.hasId()) {
                cache.put(node.getExtendedId(), parsedNode);
            }
            // node.getAddressTry().ifPresent(address -> cache.put(address, parsedNode));

            return parsedNode;
        }

    }

    public ClangAstProcessor(ClangConverterTable converter) {
        this.converter = converter;
    }

    public void process(ClangRootNode clangDump) {
        addNamespaceAndRecordToMethods(clangDump);
    }

    /**
     * Adds information about namespaces and records (i.e., classes) to CXXMethodDecls.
     * 
     * @param clangDump
     */
    private void addNamespaceAndRecordToMethods(ClangRootNode clangDump) {

        // Collect all addresses of CXXRecordDecls
        // Get stream with all the nodes in the tree
        Map<String, ClangNode> records = clangDump.getDescendantsStream()
                // Get all CXXRecordDecl
                .filter(node -> node.getName().equals("CXXRecordDecl"))
                // Remove nodes without address
                .filter(node -> node.hasId())
                // Build map
                .collect(Collectors.toMap(node -> node.getExtendedId(), node -> node));

        // For each record, store the corresponding namespace
        Map<String, String> recordNamespaces = new HashMap<>();
        for (Entry<String, ClangNode> entry : records.entrySet()) {
            // If no parent, do not add key
            if (!entry.getValue().hasParent()) {
                continue;
            }

            // Get first parent that is a NamespaceDecl
            ClangNode namespaceDecl = entry.getValue().getAscendantsStream()
                    .filter(node -> node.getName().equals("NamespaceDecl"))
                    .findFirst().orElse(null);

            // If no parent, do not add key
            if (namespaceDecl == null) {
                continue;
            }

            // Get namespace
            NamespaceDecl namespaceNode = (NamespaceDecl) new NamespaceDeclParser(converter).parse(namespaceDecl);
            String namespace = namespaceNode.getDeclName();

            recordNamespaces.put(entry.getKey(), namespace);
        }

        // Go to all nodes derived from CXXMethodDecl, and add information about namespace and record
        Set<String> methodDecls = new HashSet<>(
                Arrays.asList("CXXMethodDecl", "CXXConstructorDecl", "CXXDestructorDecl"));

        List<ClangNode> methods = clangDump.getChildrenStream()
                // Get stream with all the nodes in the tree
                .flatMap(node -> node.getDescendantsAndSelfStream())
                // Get all CXXRecordDecl
                .filter(node -> methodDecls.contains(node.getName()))
                .collect(Collectors.toList());

        // Create parser to decode record name (i.e., class name)
        ClavaParserHelper<CXXRecordDeclParser, CXXRecordDecl> recordParser = new ClavaParserHelper<>(
                new CXXRecordDeclParser(converter), CXXRecordDecl.class);

        // Append namespace and record information at the end of the content
        TransformQueue<ClangNode> queue = new TransformQueue<>("CxxMethods to Delete");
        for (ClangNode method : methods) {

            Optional<String> recordIdTry = getParentId(method);

            // Lambdas do not have record
            if (!recordIdTry.isPresent()) {
                queue.delete(method);
                SpecsLogs.msgInfo("No record found, removing CXXMethod");
                // SpecsLogs.msgWarn("No record found, check what should be done here (probably this is a lambda)");
                continue;
            }

            // String recordId = getParentId(method);
            String recordId = recordIdTry.get();

            String namespace = recordNamespaces.get(recordId);

            String recordName = recordParser.parse(records.get(recordId)).getDeclName();

            // System.out.println("RECORD ID:" + recordId);
            // long parentId = method.toInfo().getId().get();
            // .getParent().get().getId();

            StringBuilder newContent = new StringBuilder(method.getContent());

            if (namespace != null) {
                newContent.append(" namespace ").append(namespace);
            }

            newContent.append(" record ").append(recordName);

            method.setContent(newContent.toString());
            // System.out.println("METHOD:" + method.getContent());
            // System.out.println("METHOD NEW:" + newContent);
        }

        // Delete nodes
        queue.apply();

    }

    public Optional<String> getParentId(ClangNode method) {

        // Check if it has a CXXRecordDecl as parent
        ClangNode cxxRecordDecl = method.getAscendantsStream()
                .filter(ascendent -> ascendent.getName().equals("CXXRecordDecl"))
                .findFirst()
                .orElse(null);

        if (cxxRecordDecl != null) {
            return Optional.of(cxxRecordDecl.getInfo().getIdLong());
        }

        // Get CXXRecordDecl address through parent info
        return method.getInfo().getParentIdLong();
    }
}
