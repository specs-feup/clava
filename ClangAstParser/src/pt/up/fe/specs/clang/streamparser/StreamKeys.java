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

package pt.up.fe.specs.clang.streamparser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;
import org.suikasoft.jOptions.storedefinition.StoreDefinitionBuilder;

import pt.up.fe.specs.clang.streamparser.data.CxxMemberExprInfo;
import pt.up.fe.specs.clang.streamparser.data.ExceptionSpecifierInfo;
import pt.up.fe.specs.clang.streamparser.data.FieldDeclInfo;
import pt.up.fe.specs.clang.streamparser.data.OffsetOfInfo;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.expr.data.LambdaExprData;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.collections.MultiMap;

public interface StreamKeys {

    // This is an auxiliary key
    DataKey<String> COUNTER = KeyFactory.string("stderr_counter");
    DataKey<String> TYPES = KeyFactory.string("stderr_types");

    DataKey<MultiMap<String, String>> TEMPLATE_NAMES = KeyFactory.generic("stderr_template_names",
            new MultiMap<String, String>());

    DataKey<MultiMap<String, String>> TEMPLATE_ARGUMENT_TYPES = KeyFactory.generic("stderr_template_argument_types",
            new MultiMap<String, String>());
    /**
     * namespace qualifiers, such as 'std::'
     */
    DataKey<Map<String, String>> DECLREFEXPR_QUALS = KeyFactory.generic("stderr_declrefexpr_qualifiers",
            new HashMap<String, String>());

    DataKey<Map<String, String>> CONSTRUCTOR_TYPES = KeyFactory.generic("stderr_constructor_types",
            new HashMap<String, String>());

    DataKey<MultiMap<String, String>> BASES_TYPES = KeyFactory.generic("stderr_bases_types",
            new MultiMap<String, String>());

    DataKey<Map<String, String>> UNARY_OR_TYPE_TRAIT_ARG_TYPES = KeyFactory.generic("stderr_uett_arg_types",
            new HashMap<String, String>());

    DataKey<MultiMap<String, String>> CXX_CTOR_INITIALIZERS = KeyFactory.generic("stderr_cxx_ctor_initializers",
            new MultiMap<String, String>());

    DataKey<Map<String, SourceRange>> SOURCE_RANGES = KeyFactory.generic("stderr_source_ranges",
            // new HashMap<String, SourceRange>())
            SpecsCollections.<String, SourceRange> newHashMap())
            // (Map<String, SourceRange>) new HashMap<String, SourceRange>())
            .setDefault(() -> new HashMap<>());

    DataKey<Map<String, Integer>> NUMBER_TEMPLATE_PARAMETERS = KeyFactory.generic("stderr_number_template_parameters",
            SpecsCollections.<String, Integer> newHashMap())
            .setDefault(() -> new HashMap<>());
    DataKey<Map<String, Integer>> NUMBER_TEMPLATE_ARGUMENTS = KeyFactory.generic("stderr_number_template_arguments",
            SpecsCollections.<String, Integer> newHashMap())
            .setDefault(() -> new HashMap<>());

    DataKey<Map<String, String>> NAMESPACE_ALIAS_PREFIX = KeyFactory.generic("stderr_namespace_alias_prefix",
            SpecsCollections.<String, String> newHashMap())
            .setDefault(() -> new HashMap<>());

    DataKey<MultiMap<String, String>> TEMPLATE_ARGUMENTS = KeyFactory.generic("stderr_template_arguments",
            new MultiMap<String, String>());

    DataKey<Map<String, FieldDeclInfo>> FIELD_DECL_INFO = KeyFactory.generic("stderr_FIELD_DECL_INFO",
            new HashMap<String, FieldDeclInfo>());

    DataKey<Map<String, String>> NAMED_DECL_WITHOUT_NAME = KeyFactory.generic("stderr_NAMED_DECL_WITHOUT_NAME",
            // Using SpecsCollections in order to invoke .setDefault()
            SpecsCollections.<String, String> newHashMap())
            // Setting default, in case all named decls have names
            .setDefault(() -> new HashMap<>());

    DataKey<Map<String, String>> CXX_METHOD_DECL_PARENT = KeyFactory.generic("stderr_cxx_method_decl_parent",
            new HashMap<String, String>());

    DataKey<Set<String>> PARM_VAR_DECL_HAS_INHERITED_DEFAULT_ARG = KeyFactory
            .generic("stderr_parm_var_decl_has_inherited_default_arg",
                    SpecsCollections.<String> newHashSet())
            // Setting default, in case no parm var decl has inherited arg
            .setDefault(() -> new HashSet<String>());

    DataKey<Map<String, OffsetOfInfo>> OFFSET_OF_INFO = KeyFactory.generic("stderr_offset_of_info", new HashMap<>());

    DataKey<Map<String, ExceptionSpecifierInfo>> FUNCTION_PROTOTYPE_EXCEPTION = KeyFactory
            .generic("stderr_function_prototype_exception", new HashMap<>());

    DataKey<Map<String, String>> TYPEDEF_DECL_SOURCE = KeyFactory.generic("stderr_typedef_decl_source",
            new HashMap<String, String>());

    DataKey<Map<String, CxxMemberExprInfo>> CXX_MEMBER_EXPR_INFO = KeyFactory.generic("stderr_cxx_member_expr_info",
            new HashMap<String, CxxMemberExprInfo>());

    DataKey<Map<String, String>> TYPE_AS_WRITTEN = KeyFactory.generic("stderr_type_as_written",
            new HashMap<String, String>());

    DataKey<Map<String, LambdaExprData>> LAMBDA_EXPR_DATA = KeyFactory.generic("stderr_lambda_expr_data",
            new HashMap<String, LambdaExprData>());

    // DataKey<Map<String, String>> CXX_METHOD_DECL_DECLARATION =
    // KeyFactory.generic("stderr_cxx_method_decl_declaration",
    // SpecsCollections.<String, String> newHashMap())
    // .setDefault(() -> new HashMap<String, String>());

    // DataKey<Set<String>> INTEGER_LITERALS_BUILTIN = KeyFactory.generic("stderr_integer_literals",
    // new HashSet<String>());
    //
    // DataKey<Set<String>> FLOATING_LITERALS_BUILTIN = KeyFactory.generic("stderr_floating_literals",
    // new HashSet<String>());

    DataKey<String> WARNINGS = KeyFactory.string("stderr_warnings");

    StoreDefinition STORE_DEFINITION = new StoreDefinitionBuilder("StdErr")
            .addKey(TYPES)
            .addKey(TEMPLATE_NAMES)
            .addKey(TEMPLATE_ARGUMENT_TYPES)
            .addKey(DECLREFEXPR_QUALS)
            .addKey(CONSTRUCTOR_TYPES)
            .addKey(BASES_TYPES)
            .addKey(UNARY_OR_TYPE_TRAIT_ARG_TYPES)
            .addKey(CXX_CTOR_INITIALIZERS)
            .addKey(SOURCE_RANGES)
            .addKey(NUMBER_TEMPLATE_PARAMETERS)
            .addKey(NUMBER_TEMPLATE_ARGUMENTS)
            .addKey(NAMESPACE_ALIAS_PREFIX)
            .addKey(TEMPLATE_ARGUMENTS)
            .addKey(FIELD_DECL_INFO)
            .addKey(NAMED_DECL_WITHOUT_NAME)
            .addKey(CXX_METHOD_DECL_PARENT)
            .addKey(PARM_VAR_DECL_HAS_INHERITED_DEFAULT_ARG)
            .addKey(OFFSET_OF_INFO)
            .addKey(FUNCTION_PROTOTYPE_EXCEPTION)
            .addKey(TYPEDEF_DECL_SOURCE)
            .addKey(CXX_MEMBER_EXPR_INFO)
            .addKey(TYPE_AS_WRITTEN)
            .addKey(LAMBDA_EXPR_DATA)
            // .addKey(CXX_METHOD_DECL_DECLARATION)
            // .addKey(INTEGER_LITERALS_BUILTIN)
            // .addKey(FLOATING_LITERALS_BUILTIN)
            .addKey(WARNINGS)
            .build();
}
