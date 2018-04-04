//
// Created by JoaoBispo on 21/03/2017.
//

#ifndef CLANGASTDUMPER_CLANGASTDUMPERCONSTANTS_H
#define CLANGASTDUMPER_CLANGASTDUMPERCONSTANTS_H

#include <string>

static const std::string CXX_CTOR_INITIALIZER_BEGIN = "<CXXCtorInitializer>";
static const std::string CXX_CTOR_INITIALIZER_END = "<CXXCtorInitializer End>";

// Use in TemplateDecl
static const std::string DUMP_NUMBER_TEMPLATE_PARAMETERS = "<Number Template Parameters>";

// Used in FunctionDecl, such as FunctionTemplateDecl
//static const std::string DUMP_NUMBER_TEMPLATE_ARGUMENTS = "<Number Template Arguments>";
static const std::string DUMP_NAMESPACE_ALIAS_PREFIX = "<Namespace Alias Prefix>";
static const std::string DUMP_TEMPLATE_ARGS = "<Template Args>";

// Information about FieldDecl nodes
static const std::string DUMP_FIELD_DECL_INFO = "<Field Decl Info>";

// For NamedDecls, signals if they do not have a defined name (which is less common)
static const std::string DUMP_NAMED_DECL_WITHOUT_NAME = "<NamedDecl Without Name>";



// Maps a CXXMethodDecl to the corresponding declaration, if the method is a definition
//static const std::string DUMP_CXX_METHOD_DECL_DECLARATION = "<CXXMethodDecl Declaration>";

static const std::string DUMP_PARM_VAR_DECL_HAS_INHERITED_DEFAULT_ARG = "<ParmVarDecl Has Inherited Default Arg>";

// Information about OffsetOfExpr
static const std::string DUMP_OFFSET_OF_INFO = "<OFFSET_OF_INFO>";

// Exception information from FunctionProtoType
static const std::string FUNCTION_PROTO_TYPE_EXCEPTION = "<FUNCTION_PROTO_TYPE_EXCEPTION>";

static const std::string TYPEDEF_DECL_SOURCE = "<TypedefDecl Source>";

static const std::string CXX_MEMBER_EXPR_INFO = "<CXX Member Expr Info>";

static const std::string TYPE_AS_WRITTEN = "<Type As Written>";

static const std::string LAMBDA_EXPR_DATA = "<Lambda Expr Data>";

static const std::string TYPEID_DATA = "<Typeid Data>";

//static const std::string IS_CONST_EXPR = "<Is Constexpr>";

//static const std::string VARDECL_QUALIFIED_NAME = "<VarDecl Qualified Name>";


static const std::string INIT_LIST_EXPR_INFO = "<InitListExpr Info>";

static const std::string VISITED_CHILDREN = "<Visited Children>";

static const std::string ID_TO_CLASS_MAP = "<Id to Class Map>";

static const std::string TOP_LEVEL_NODES = "<Top Level Nodes>";

static const std::string TOP_LEVEL_TYPES = "<Top Level Types>";

#endif //CLANGASTDUMPER_CLANGASTDUMPERCONSTANTS_H
