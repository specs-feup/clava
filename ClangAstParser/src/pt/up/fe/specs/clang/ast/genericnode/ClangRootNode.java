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

package pt.up.fe.specs.clang.ast.genericnode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.includes.ClangIncludes;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.omp.OMPDirective;
import pt.up.fe.specs.util.collections.MultiMap;

public class ClangRootNode extends ClangNode {

    /**
     * @deprecated Gradually replace it with DataStore generated with StdErrKeys/StdErrParser
     * @author JoaoBispo
     *
     */
    @Deprecated
    public static class ClangRootData {
        // User configurations
        private final DataStore config;
        // Dumped includes
        private final ClangIncludes includes;
        // List of all dumped types, can have repeated nodes
        private final List<ClangNode> clangTypes;
        // Maps node addresses to the address of the corresponding type
        private final Map<String, String> nodeToTypes;
        // Indicates which nodes have template arguments
        // private final Set<String> hasTemplateArguments;
        // Indicates which nodes are temporary
        private final Set<String> isTemporary;
        // OpenMP directives
        private final Map<String, OMPDirective> ompDirectives;
        // Enum integer types
        private final Map<String, String> enumToIntegerType;

        // namespace qualifiers, such as 'std::'
        private final DataStore stdErr;

        // new ClangRootData(config, includes, clangTypes, nodeToTypes,
        // stderr.get(StdErrKeys.DECLREFEXPR_QUALS), typeQualifiers, hasTemplateArguments, isTemporary,
        // ompDirectives, enumToIntegerType, stderr.get(StdErrKeys.TEMPLATE_NAMES),
        // stderr.get(StdErrKeys.TEMPLATE_ARGS), stderr.get(StdErrKeys.CONSTRUCTOR_TYPES));

        public ClangRootData(DataStore config, ClangIncludes includes, List<ClangNode> clangTypes,
                Map<String, String> nodeToTypes,
                // Set<String> hasTemplateArguments,
                Set<String> isTemporary, Map<String, OMPDirective> ompDirectives,
                Map<String, String> enumToIntegerType, DataStore stdErr) {

            this.config = config;
            this.includes = includes;
            this.clangTypes = clangTypes;
            this.nodeToTypes = nodeToTypes;
            // this.hasTemplateArguments = hasTemplateArguments;
            this.isTemporary = isTemporary;
            this.ompDirectives = ompDirectives;
            this.enumToIntegerType = enumToIntegerType;
            this.stdErr = stdErr;
        }

        public DataStore getStdErr() {
            return stdErr;
        }

        public DataStore getConfig() {
            return config;
        }

        public ClangIncludes getIncludes() {
            return includes;
        }

        public List<ClangNode> getClangTypes() {
            return clangTypes;
        }

        public Map<String, String> getNodeToTypes() {
            return nodeToTypes;
        }

        public Map<String, String> getDeclRefExprQualifiers() {
            return stdErr.get(StreamKeys.DECLREFEXPR_QUALS);
        }

        // public Set<String> getTemplateArguments() {
        // return hasTemplateArguments;
        // }

        // public boolean hasTemplateArguments(String id) {
        // return hasTemplateArguments.contains(id);
        // }

        public boolean isTemporary(String id) {
            return isTemporary.contains(id);
        }

        public Map<String, OMPDirective> getOmpDirectives() {
            return ompDirectives;
        }

        public Map<String, String> getEnumToIntegerType() {
            return enumToIntegerType;
        }

        public MultiMap<String, String> getTemplateNames() {
            return stdErr.get(StreamKeys.TEMPLATE_NAMES);
        }

        public MultiMap<String, String> getTemplateArgTypes() {
            return stdErr.get(StreamKeys.TEMPLATE_ARGUMENT_TYPES);
        }

        public Map<String, String> getConstructorTypes() {
            return stdErr.get(StreamKeys.CONSTRUCTOR_TYPES);
        }
    }

    private final static String ROOT_NAME = "Root";

    // private final String clangOutput;
    private final ClangRootData clangRootData;

    // public ClangRootNode(String clangOutput, ClangRootData clangRootData, List<ClangNode> children) {
    public ClangRootNode(ClangRootData clangRootData, List<ClangNode> children) {

        super(ROOT_NAME);

        addChildren(children);

        // this.clangOutput = clangOutput;
        this.clangRootData = clangRootData;
    }

    public static String getRootName() {
        return ROOT_NAME;
    }

    // public String getClangOutput() {
    // return clangOutput;
    // }

    public ClangIncludes getIncludes() {
        return clangRootData.getIncludes();
    }

    public ClangRootData getClangRootData() {
        return clangRootData;
    }

    @Override
    protected ClangNode copyPrivate() {
        // return new ClangRootNode(clangOutput, clangRootData, Collections.emptyList());
        return new ClangRootNode(clangRootData, Collections.emptyList());
    }

    @Override
    public Optional<String> getContentTry() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getIdRawTry() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getLocationString() {
        return Optional.empty();
    }

    @Override
    public SourceRange getLocation() {
        return new SourceRange(null, -1, -1, -1, -1);
    }

    public List<ClangNode> getClangTypes() {
        return clangRootData.getClangTypes();
    }

    public Map<String, String> getNodeToTypes() {
        return clangRootData.getNodeToTypes();
    }

    public DataStore getConfig() {
        return clangRootData.getConfig();
    }

}
