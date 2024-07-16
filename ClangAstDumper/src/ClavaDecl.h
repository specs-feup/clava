//
// Created by JoaoBispo on 16/08/2018.
//

#ifndef CLANGASTDUMPER_CLAVADECL_H
#define CLANGASTDUMPER_CLAVADECL_H

#include <string>

namespace clava {


    /**
     * Represents decl node classes
     */
    enum class DeclNode {
        DECL,
        NAMED_DECL,
        VALUE_DECL,
        DECLARATOR_DECL,
        FIELD_DECL,
        TYPE_DECL,
        UNRESOLVED_USING_TYPENAME_DECL,
        FUNCTION_DECL,
        CXX_METHOD_DECL,
        CXX_CONSTRUCTOR_DECL,
        CXX_CONVERSION_DECL,
        VAR_DECL,
        PARM_VAR_DECL,
        TAG_DECL,
        ENUM_DECL,
        RECORD_DECL,
        CXX_RECORD_DECL,
        CLASS_TEMPLATE_SPECIALIZATION_DECL,
        CLASS_TEMPLATE_PARTIAL_SPECIALIZATION_DECL,
        TEMPLATE_DECL,
        //REDECLARABLE_TEMPLATE_DECL,
        //CLASS_TEMPLATE_DECL,
        TEMPLATE_TYPE_PARM_DECL,
        ENUM_CONSTANT_DECL,
        USING_SHADOW_DECL,
        TYPEDEF_NAME_DECL,
        TYPE_ALIAS_DECL,
        TYPEDEF_DECL,
        ACCESS_SPEC_DECL,
        USING_DECL,
        USING_DIRECTIVE_DECL,
        NAMESPACE_DECL,
        FRIEND_DECL,
        NAMESPACE_ALIAS_DECL,
        LINKAGE_SPEC_DECL,
        LABEL_DECL,
        STATIC_ASSERT_DECL,
        TEMPLATE_TEMPLATE_PARM_DECL,
        NON_TYPE_TEMPLATE_PARM_DECL,
        VAR_TEMPLATE_SPECIALIZATION_DECL,
        MS_PROPERTY_DECL,
    };

    const std::string getName(const DeclNode D);
}

#endif //CLANGASTDUMPER_CLAVADECL_H
