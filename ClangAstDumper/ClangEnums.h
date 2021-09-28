//
// Created by JoaoBispo on 06/04/2018.
//
// Arrays that map Clang enums positions with Clava enums names.
//

#ifndef CLANGASTDUMPER_CLANGENUMS_H
#define CLANGASTDUMPER_CLANGENUMS_H

#include <string>
#include <vector>

namespace clava {

    /* Clava Enums */
    //extern const std::string CXX_CTOR_INIT_KIND[];

    /* Clang Enums */

    extern const std::string CAST_KIND[];
    extern const std::string ATTRIBUTES[];
    extern const std::string CALLING_CONVENTION[];
    extern const std::string REFERENCE_QUALIFIER[];
    extern const std::string EXCEPTION_SPECIFICATION_TYPE[];
    extern const std::string LINKAGE[];
    extern const std::string VISIBILITY[];
    extern const std::string BUILTIN_KIND[];
    extern const std::string ARRAY_SIZE_MODIFIER[];
    extern const std::string TEMPLATE_ARG_KIND[];
    extern const std::string TAG_KIND[];
    extern const std::string ACCESS_SPECIFIER[];
    extern const std::string INIT_STYLE[];
    extern const std::string NEW_INIT_STYLE[];
    extern const std::string STORAGE_CLASS[];
    extern const std::string TLS_KIND[];
    extern const std::string ELABORATED_TYPE_KEYWORD[];
    extern const std::string CONSTRUCTION_KIND[];
    extern const std::string BINARY_OPERATOR_KIND[];
    extern const std::string UTT_KIND[];
    extern const std::string UNARY_OPERATOR_KIND[];
    extern const std::string UETT_KIND[];
    extern const std::string NESTED_NAMED_SPECIFIER[];
    extern const std::string OFFSET_OF_NODE_KIND[];
    extern const std::string LINKAGE_LANGUAGE[];
    extern const std::string LAMBDA_CAPTURE_DEFAULT[];
    extern const std::string LAMBDA_CAPTURE_KIND[];
    extern const std::string PREDEFINED_ID_TYPE[];
    extern const std::string STRING_KIND[];
    extern const std::string TEMPLATE_NAME_KIND[];
    extern const std::string DESIGNATOR_KIND[];
    extern const std::string VISIBILITY_ATTR_TYPE[];
    extern const std::string EXPLICIT_SPEC_KIND[];
    extern const std::string TEMPLATE_SPECIALIZATION_KIND[];

}
#endif //CLANGASTDUMPER_CLANGENUMS_H
