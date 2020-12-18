//
// Created by JoaoBispo on 18/03/2018.
//

#include "ClavaDataDumper.h"
#include "ClangNodes.h"
#include "ClavaConstants.h"
#include "ClangEnums.h"

#include <map>
#include <string>

const std::map<const std::string, clava::DeclNode> clava::DECL_DATA_MAP = {
        {"CXXConstructorDecl",                     clava::DeclNode::CXX_CONSTRUCTOR_DECL},
        {"CXXConversionDecl",                      clava::DeclNode::CXX_CONVERSION_DECL},
        {"CXXDestructorDecl",                      clava::DeclNode::CXX_METHOD_DECL},
        {"CXXMethodDecl",                          clava::DeclNode::CXX_METHOD_DECL},
        {"FieldDecl",                              clava::DeclNode::FIELD_DECL},
        {"FunctionDecl",                           clava::DeclNode::FUNCTION_DECL},
        //{"NamespaceAliasDecl",                     clava::DeclNode::NAMED_DECL},
        {"ObjCImplementationDecl",                 clava::DeclNode::NAMED_DECL},
        {"ParmVarDecl",                            clava::DeclNode::PARM_VAR_DECL},
        {"TemplateDecl",                           clava::DeclNode::NAMED_DECL},
        {"TemplateTypeParmDecl",                   clava::DeclNode::TEMPLATE_TYPE_PARM_DECL},
        //{"TypedefDecl", clava::DeclNode::NAMED_DECL},
        {"TypeDecl",                               clava::DeclNode::TYPE_DECL},
        {"UnresolvedUsingTypenameDecl",            clava::DeclNode::UNRESOLVED_USING_TYPENAME_DECL},
        {"EnumDecl",                               clava::DeclNode::ENUM_DECL},
        {"RecordDecl",                             clava::DeclNode::RECORD_DECL},
        {"CXXRecordDecl",                          clava::DeclNode::CXX_RECORD_DECL},
        {"ClassTemplateSpecializationDecl",        clava::DeclNode::CXX_RECORD_DECL},
        {"ClassTemplatePartialSpecializationDecl", clava::DeclNode::CXX_RECORD_DECL},
        {"VarDecl",                                clava::DeclNode::VAR_DECL},
        {"EnumConstantDecl",                       clava::DeclNode::VALUE_DECL},
        {"NonTypeTemplateParmDecl",                clava::DeclNode::NON_TYPE_TEMPLATE_PARM_DECL},
        {"UsingShadowDecl",                        clava::DeclNode::NAMED_DECL},
        {"TypeAliasDecl",                          clava::DeclNode::TYPEDEF_NAME_DECL},
        {"TypedefDecl",                            clava::DeclNode::TYPEDEF_NAME_DECL},
        {"AccessSpecDecl",                         clava::DeclNode::ACCESS_SPEC_DECL},
        {"UsingDirectiveDecl",                     clava::DeclNode::USING_DIRECTIVE_DECL},
        {"NamespaceDecl",                          clava::DeclNode::NAMESPACE_DECL},
        {"NamespaceAliasDecl",                     clava::DeclNode::NAMESPACE_ALIAS_DECL},
        {"LinkageSpecDecl",                        clava::DeclNode::LINKAGE_SPEC_DECL},
        {"LabelDecl",                              clava::DeclNode::NAMED_DECL},
        {"StaticAssertDecl",                       clava::DeclNode::STATIC_ASSERT_DECL},
        {"TemplateTemplateParmDecl",               clava::DeclNode::TEMPLATE_TEMPLATE_PARM_DECL},

};


void clava::ClavaDataDumper::dump(const Decl *D) {

    // Get classname
    const std::string classname = clava::getClassName(D);

    // Get corresponding DeclNode
    DeclNode declNode = DECL_DATA_MAP.count(classname) == 1 ? DECL_DATA_MAP.find(classname)->second : DeclNode::DECL;

    dump(declNode, D);

}

void clava::ClavaDataDumper::dump(clava::DeclNode declNode, const Decl *D) {
    // Dump header
    llvm::errs() << getDataName(declNode) << "\n";
    llvm::errs() << clava::getId(D, id) << "\n";
    llvm::errs() << clava::getClassName(D) << "\n";
    //DumpHeader(getDataName(declNode), D);

    switch (declNode) {
        case clava::DeclNode::DECL:
            DumpDeclData(D);
            break;
        case clava::DeclNode::NAMED_DECL:
            DumpNamedDeclData(static_cast<const NamedDecl *>(D));
            break;
        case clava::DeclNode::TYPE_DECL:
            DumpTypeDeclData(static_cast<const TypeDecl *>(D));
            break;
        case clava::DeclNode::UNRESOLVED_USING_TYPENAME_DECL:
            DumpUnresolvedUsingTypenameDeclData(static_cast<const UnresolvedUsingTypenameDecl *>(D));
            break;
        case clava::DeclNode::TAG_DECL:
            DumpTagDeclData(static_cast<const TagDecl *>(D));
            break;
        case clava::DeclNode::ENUM_DECL:
            DumpEnumDeclData(static_cast<const EnumDecl *>(D));
            break;
        case clava::DeclNode::RECORD_DECL:
            DumpRecordDeclData(static_cast<const RecordDecl *>(D));
            break;
        case clava::DeclNode::CXX_RECORD_DECL:
            DumpCXXRecordDeclData(static_cast<const CXXRecordDecl *>(D));
            break;
        case clava::DeclNode::VALUE_DECL:
            DumpValueDeclData(static_cast<const ValueDecl *>(D));
            break;
        case clava::DeclNode::DECLARATOR_DECL:
            DumpDeclaratorDeclData(static_cast<const DeclaratorDecl *>(D));
            break;
        case clava::DeclNode::FIELD_DECL:
            DumpFieldDeclData(static_cast<const FieldDecl *>(D));
            break;
        case clava::DeclNode::FUNCTION_DECL:
            DumpFunctionDeclData(static_cast<const FunctionDecl *>(D));
            break;
        case clava::DeclNode::CXX_METHOD_DECL:
            DumpCXXMethodDeclData(static_cast<const CXXMethodDecl *>(D));
            break;
        case clava::DeclNode::CXX_CONSTRUCTOR_DECL:
            DumpCXXConstructorDeclData(static_cast<const CXXConstructorDecl *>(D));
            break;
        case clava::DeclNode::CXX_CONVERSION_DECL:
            DumpCXXConversionDeclData(static_cast<const CXXConversionDecl *>(D));
            break;
        case clava::DeclNode::VAR_DECL:
            DumpVarDeclData(static_cast<const VarDecl *>(D));
            break;
        case clava::DeclNode::PARM_VAR_DECL:
            DumpParmVarDeclData(static_cast<const ParmVarDecl *>(D));
            break;
        case clava::DeclNode::TEMPLATE_TYPE_PARM_DECL:
            DumpTemplateTypeParmDeclData(static_cast<const TemplateTypeParmDecl *>(D));
            break;
        case clava::DeclNode::TYPEDEF_NAME_DECL:
            DumpTypedefNameDeclData(static_cast<const TypedefNameDecl *>(D));
            break;
        case clava::DeclNode::ACCESS_SPEC_DECL:
            DumpAccessSpecDeclData(static_cast<const AccessSpecDecl *>(D));
            break;
        case clava::DeclNode::USING_DECL:
            DumpUsingDeclData(static_cast<const UsingDecl *>(D));
            break;
        case clava::DeclNode::USING_DIRECTIVE_DECL:
            DumpUsingDirectiveDeclData(static_cast<const UsingDirectiveDecl *>(D));
            break;
        case clava::DeclNode::NAMESPACE_DECL:
            DumpNamespaceDeclData(static_cast<const NamespaceDecl *>(D));
            break;
        case clava::DeclNode::NAMESPACE_ALIAS_DECL:
            DumpNamespaceAliasDeclData(static_cast<const NamespaceAliasDecl *>(D));
            break;
        case clava::DeclNode::LINKAGE_SPEC_DECL:
            DumpLinkageSpecDeclData(static_cast<const LinkageSpecDecl *>(D));
            break;
        case clava::DeclNode::STATIC_ASSERT_DECL:
            DumpStaticAssertDeclData(static_cast<const StaticAssertDecl *>(D));
            break;
        case clava::DeclNode::TEMPLATE_TEMPLATE_PARM_DECL:
            DumpTemplateTemplateParmDeclData(static_cast<const TemplateTemplateParmDecl *>(D));
            break;
        case clava::DeclNode::NON_TYPE_TEMPLATE_PARM_DECL:
            DumpNonTypeTemplateParmDeclData(static_cast<const NonTypeTemplateParmDecl *>(D));
            break;
        default:
            throw std::invalid_argument(
                    "ClangDataDumper::dump(DeclNode):: Case not implemented, '" + getName(declNode) + "'");


    }
}


void clava::ClavaDataDumper::DumpDeclData(const Decl *D) {
    clava::dumpSourceInfo(Context, D->getLocStart(), D->getLocEnd());
    //clava::dumpSourceInfo(Context, D->getSourceRange().getBegin(), D->getSourceRange().getEnd());

    //clava::dumpSourceRange(Context,     D->getSourceRange().getBegin(),     D->getSourceRange().getEnd());



    // Print information about Decl
    clava::dump(D->isImplicit());
    clava::dump(D->isUsed());
    clava::dump(D->isReferenced());
    clava::dump(D->isInvalidDecl());
    clava::dump(D->isModulePrivate());


    // Attributes
    std::vector<std::string> attributesIds;
    for (Decl::attr_iterator I = D->attr_begin(), E = D->attr_end(); I != E;
         ++I) {
        attributesIds.push_back(clava::getId(*I, id));
    }
    clava::dump(attributesIds);

    // Decls inside DeclContext
    /*
    std::vector<std::string> declsInContext;
    for (auto I = D->getDeclContext()->decls().begin(), E = D->getDeclContext()->decls().end(); I != E;
         ++I) {
        declsInContext.push_back(clava::getId(*I, id));
    }
    clava::dump(declsInContext);
    */
    /*
    if(D->getDeclContext() != nullptr) {
        clava::dump(clava::getId(cast<Decl>(D->getDeclContext()), id));
    } else {
        clava::dump(clava::getId((const Decl *)nullptr, id));
    }
     */



    // Attributes
    /*
    std::vector<Attr*> attributes;
    for (Decl::attr_iterator I = D->attr_begin(), E = D->attr_end(); I != E;
         ++I) {
        attributes.push_back(*I);
    }
    clava::dump(attributes, id);
*/
}

void clava::ClavaDataDumper::DumpNamedDeclData(const NamedDecl *D) {
    // Hierarchy
    DumpDeclData(D);

    // Print information about NamedDecl
    //clava::dump(D->getQualifiedNameAsString());
    clava::dump(clava::getQualifiedPrefix(D));
    clava::dump(D->getDeclName().getAsString());
    clava::dump(D->getDeclName().getNameKind());
    clava::dump(D->isHidden());
    //llvm::errs() << D->getQualifiedNameAsString() << "\n";
    //llvm::errs() << D->getDeclName().getNameKind() << "\n";
    //llvm::errs() << D->isHidden() << "\n";

    clava::dump(D->isCXXClassMember());
    clava::dump(D->isCXXInstanceMember());
    clava::dump(clava::LINKAGE[D->getFormalLinkage()]);
    clava::dump(clava::VISIBILITY[D->getVisibility()]);
    //clava::dump(clava::getId(D->getUnderlyingDecl(), id));

    // hasLinkage () const
    // isCXXClassMember ()
    // isCXXInstanceMember ()
    // getFormalLinkage()
    // getVisibility ()
    //
    //
    // getUnderlyingDecl ()

}

void clava::ClavaDataDumper::DumpTypeDeclData(const TypeDecl *D) {
    // Hierarchy
    DumpNamedDeclData(D);

    //clava::dump(clava::getId(Context->getTypeDeclType(D), id));
    clava::dump(clava::getId(D->getTypeForDecl(), id));
}

void clava::ClavaDataDumper::DumpUnresolvedUsingTypenameDeclData(const UnresolvedUsingTypenameDecl *D) {
    // Hierarchy
    DumpTypeDeclData(D);

    clava::dump(D->getQualifier(), Context);
    clava::dump(D->isPackExpansion());
}


void clava::ClavaDataDumper::DumpTagDeclData(const TagDecl *D) {
    // Hierarchy
    DumpTypeDeclData(D);

    clava::dump(clava::TAG_KIND[D->getTagKind()]);
    clava::dump(D->isCompleteDefinition());

}

void clava::ClavaDataDumper::DumpEnumDeclData(const EnumDecl *D) {
    // Hierarchy
    DumpTagDeclData(D);

    // Dump EnumScopeType
    if (D->isScoped()) {
        if (D->isScopedUsingClassTag())
            clava::dump("CLASS");
        else
            clava::dump("STRUCT");
    } else {
        clava::dump("NO_SCOPE");
    }

    clava::dump(clava::getId(D->getIntegerType(), id));

}

void clava::ClavaDataDumper::DumpRecordDeclData(const RecordDecl *D) {
    // Hierarchy
    DumpTagDeclData(D);

    clava::dump(D->isAnonymousStructOrUnion());

    //D->isCompleteDefinition();
    //clava::dump(clava::TAG_KIND[D->getTagKind()]);
}

void clava::ClavaDataDumper::DumpCXXRecordDeclData(const CXXRecordDecl *D) {
    // Hierarchy
    DumpRecordDeclData(D);

    if (D->hasDefinition()) {
        clava::dump(D->getNumBases());
        for (const auto &I : D->bases()) {
            clava::dump(I, id);
        }
    } else {
        clava::dump(0);
    }

    //clava::dump(D->hasDefinition());
    clava::dump(clava::getId(D->getDefinition(), id));

/*
    if (D->hasDefinition()) {
        clava::dump(clava::getId(D->getDefinition(), id));
    } else {
        clava::dump(clava::getId((const Decl*)nullptr, id));
    }
  */

}


void clava::ClavaDataDumper::DumpValueDeclData(const ValueDecl *D) {
    // Hierarchy
    DumpNamedDeclData(D);

    clava::dump(D->getType(), id);
    clava::dump(D->isWeak());
}


void clava::ClavaDataDumper::DumpDeclaratorDeclData(const DeclaratorDecl *D) {
    // Hierarchy
    DumpValueDeclData(D);

    // Nothing for now
}


void clava::ClavaDataDumper::DumpFieldDeclData(const FieldDecl *D) {
    // Hierarchy
    DumpDeclaratorDeclData(D);

    clava::dump(D->isMutable());
}


void clava::ClavaDataDumper::DumpFunctionDeclData(const FunctionDecl *D) {
    // Hierarchy
    DumpDeclaratorDeclData(D);



    // Print information about FunctionDecl
    clava::dump(D->isConstexpr());
    clava::dump(D->getTemplatedKind());
    clava::dump(clava::STORAGE_CLASS[D->getStorageClass()]);
    clava::dump(D->isInlineSpecified());
    clava::dump(D->isVirtualAsWritten());
    clava::dump(D->isPure());
    clava::dump(D->isDeletedAsWritten());
    clava::dump(D->isExplicitlyDefaulted());

    clava::dump(clava::getId(D->getPreviousDecl(), id));
    clava::dump(clava::getId(D->getCanonicalDecl(), id));

    FunctionTemplateDecl* primaryTemplate =  D->getPrimaryTemplate();
    if(primaryTemplate != nullptr) {
        clava::dump(clava::getId(primaryTemplate->getTemplatedDecl(), id));
    } else {
        clava::dump(clava::getId((const Decl *) nullptr, id));
    }


    // Template specialization args
    auto templateSpecializationArgs = D->getTemplateSpecializationArgs();
    if (templateSpecializationArgs != nullptr) {
        clava::dump(templateSpecializationArgs->size());
        for (auto templateArg : templateSpecializationArgs->asArray()) {
            clava::dump(templateArg, id, Context);
        }
    } else {
        clava::dump(0);
    }


/*
    if (const FunctionTemplateSpecializationInfo* FTSI =
            D->getTemplateSpecializationInfo()) {

        for (unsigned i = 0, e = FTSI->size(); i < e; ++i) {

        }
            dumpTemplateArgumentLoc(TALI[i]);

    }
     */



/*
    if (D->getStorageClass() != SC_None) {
        std::string storageClassStr;
        llvm::raw_string_ostream scStream(storageClassStr);
        scStream << VarDecl::getStorageClassSpecifierString(D->getStorageClass());
        clava::dump(scStream.str());
    } else {
        clava::dump("none");
    }
*/
/*
  StorageClass SC = D->getStorageClass();
  if (SC != SC_None)
    OS << ' ' << VarDecl::getStorageClassSpecifierString(SC);
  if (D->isInlineSpecified())
    OS << " inline";
  if (D->isVirtualAsWritten())
    OS << " virtual";
  if (D->isModulePrivate())
    OS << " __module_private__";
*/
}

void clava::ClavaDataDumper::DumpCXXMethodDeclData(const CXXMethodDecl *D) {
    // Hierarchy
    DumpFunctionDeclData(D);

    clava::dump(clava::getId(D->getParent(), id));
    // Dump the corresponding CXXRecordDecl
//    llvm::errs() << DUMP_CXX_METHOD_DECL_PARENT << "\n";
//    llvm::errs() << getId(D) << "\n";
//    llvm::errs() << getId(D->getParent()) << "\n";

    // Visit type
    //llvm::errs() << "Visiting type " << dumper->getId(D->getType().getTypePtr()) << " for node " << dumper->getId(D) << "\n";
    //dumper->VisitTypeTop(D->getType().getTypePtr());
}

void clava::ClavaDataDumper::DumpCXXConstructorDeclData(const CXXConstructorDecl *D) {
    // Hierarchy
    DumpCXXMethodDeclData(D);

    // Dump CXXCtorInitializers
    clava::dump(D->getNumCtorInitializers());
    for (auto init = D->init_begin(), init_last = D->init_end(); init != init_last; ++init) {

        if ((*init)->isAnyMemberInitializer()) {
            clava::dump("ANY_MEMBER_INITIALIZER");
            clava::dump(clava::getId((*init)->getAnyMember(), id));
        } else if ((*init)->isBaseInitializer()) {
            clava::dump("BASE_INITIALIZER");
            clava::dump(clava::getId((*init)->getBaseClass(), id));
        } else if ((*init)->isDelegatingInitializer()) {
            clava::dump("DELEGATING_INITIALIZER");
            clava::dump(clava::getId((*init)->getTypeSourceInfo()->getType(), id));
        } else {
            throw std::invalid_argument(
                    "ClangDataDumper::DumpCXXConstructorDeclData():: CXXCtorInitializer case not implemented");
        }

        // Init expr
        clava::dump(clava::getId((*init)->getInit(), id));

        clava::dump((*init)->isInClassMemberInitializer());
        clava::dump((*init)->isWritten());

    }

    clava::dump(D->isDefaultConstructor());
    clava::dump(D->isExplicit());
    clava::dump(D->isExplicitSpecified());

}

void clava::ClavaDataDumper::DumpCXXConversionDeclData(const CXXConversionDecl *D) {
    // Hierarchy
    DumpCXXMethodDeclData(D);

    clava::dump(D->isExplicit());
    clava::dump(D->isLambdaToBlockPointerConversion());

    clava::dump(D->getConversionType(), id);
}


void clava::ClavaDataDumper::DumpVarDeclData(const VarDecl *D) {
    // Hierarchy
    //DumpNamedDeclData(D);
    DumpValueDeclData(D);

    // Print information about VarDecl
    clava::dump(clava::STORAGE_CLASS[D->getStorageClass()]);
    clava::dump(clava::TLS_KIND[D->getTLSKind()]);
    clava::dump(D->isNRVOVariable());
    clava::dump(clava::INIT_STYLE[D->getInitStyle()]);

    clava::dump(D->isConstexpr());
    clava::dump(D->isStaticDataMember());
    clava::dump(D->isOutOfLine());
    clava::dump(D->hasGlobalStorage());


    /**



  if (D->hasInit()) {
    switch (D->getInitStyle()) {
    case VarDecl::CInit: OS << " cinit"; break;
    case VarDecl::CallInit: OS << " callinit"; break;
    case VarDecl::ListInit: OS << " listinit"; break;
    }
    dumpStmt(D->getInit());
  }
     */

/*
    if(D->isConstexpr()) {
        llvm::errs() << IS_CONST_EXPR << "\n";
        llvm::errs() << getId(D) << "\n";
    }

    // Print qualified name for all VarDecls
    llvm::errs() << VARDECL_QUALIFIED_NAME << "\n";
    llvm::errs() << getId(D) << "\n";
    llvm::errs() << D->getQualifiedNameAsString() << "\n";

    llvm::errs() << "VARDECL: " << D->getNameAsString() << "\n";
    llvm::errs() << "IS OUT OF LINE: " << D->isOutOfLine() << "\n";
    llvm::errs() << "IS STATIC DATA MEMBER: " << D->isStaticDataMember() << "\n";
*/
}

void clava::ClavaDataDumper::DumpParmVarDeclData(const ParmVarDecl *D) {

    // Hierarchy
    DumpVarDeclData(D);

    // Print information about ParmVarDecl
    clava::dump(D->hasInheritedDefaultArg());
    //llvm::errs() << D->hasInheritedDefaultArg() << "\n";
}

void clava::ClavaDataDumper::DumpTemplateTypeParmDeclData(const TemplateTypeParmDecl *D) {

    // Hierarchy
    DumpTypeDeclData(D);

    // Kind
    if (D->wasDeclaredWithTypename()) {
        clava::dump("TYPENAME");
    } else {
        clava::dump("CLASS");
    }

    clava::dump(D->isParameterPack());

    if (D->hasDefaultArgument()) {
        clava::dump(clava::getId(D->getDefaultArgument(), id));
    } else {
        clava::dump(clava::getId((const Type *) nullptr, id));
    }

}


void clava::ClavaDataDumper::DumpTypedefNameDeclData(const TypedefNameDecl *D) {

    // Hierarchy
    DumpTypeDeclData(D);

    clava::dump(clava::getId(D->getUnderlyingType(), id));
}

void clava::ClavaDataDumper::DumpAccessSpecDeclData(const AccessSpecDecl *D) {

    // Hierarchy
    DumpDeclData(D);

    clava::dump(clava::ACCESS_SPECIFIER[D->getAccess()]);
}

void clava::ClavaDataDumper::DumpUsingDeclData(const UsingDecl *D) {

    // Hierarchy
    DumpNamedDeclData(D);

    clava::dump(clava::NESTED_NAMED_SPECIFIER[D->getQualifier()->getKind()]);
}

void clava::ClavaDataDumper::DumpUsingDirectiveDeclData(const UsingDirectiveDecl *D) {

    // Hierarchy
    DumpNamedDeclData(D);

    clava::dump(clava::getSource(Context, D->getSourceRange()));
    clava::dump(clava::getId(D->getNominatedNamespace(), id));
    clava::dump(clava::getId(D->getNominatedNamespaceAsWritten(), id));
}

void clava::ClavaDataDumper::DumpNamespaceDeclData(const NamespaceDecl *D) {

    // Hierarchy
    DumpNamedDeclData(D);

    clava::dump(clava::getSource(Context, D->getSourceRange()));
}

void clava::ClavaDataDumper::DumpNamespaceAliasDeclData(const NamespaceAliasDecl *D) {

    // Hierarchy
    DumpNamedDeclData(D);

    clava::dump(clava::getSource(Context, D->getQualifierLoc().getSourceRange()));
    clava::dump(clava::getId(D->getAliasedNamespace(), id));
}

void clava::ClavaDataDumper::DumpLinkageSpecDeclData(const LinkageSpecDecl *D) {

    // Hierarchy
    DumpDeclData(D);

    switch(D->getLanguage()) {
        case LinkageSpecDecl::LanguageIDs::lang_c:
            clava::dump(clava::LINKAGE_LANGUAGE[0]);
            break;
        case LinkageSpecDecl::LanguageIDs::lang_cxx:
            clava::dump(clava::LINKAGE_LANGUAGE[1]);
            break;
        default:
            throw std::invalid_argument(
                    "ClangDataDumper::DumpLinkageSpecDeclData():: Case not implemented, '" + std::to_string(D->getLanguage()) + "'");
    }
    //clava::dump(clava::getSource(Context, D->getQualifierLoc().getSourceRange()));
}

void clava::ClavaDataDumper::DumpStaticAssertDeclData(const StaticAssertDecl *D) {

    // Hierarchy
    DumpDeclData(D);

    //clava::dump(clava::getId(D->getAssertExpr(), id));
    //clava::dump(clava::getId(D->getMessage(), id));

    //clava::dump([&D](llvm::raw_string_ostream& stream){D->getMessage()->dump(stream);});
    clava::dump(D->isFailed());
}

void clava::ClavaDataDumper::DumpTemplateTemplateParmDeclData(const TemplateTemplateParmDecl *D) {

    // Hierarchy
    DumpNamedDeclData(D);

    clava::dump(D->hasDefaultArgument());
    if (D->hasDefaultArgument()) {
        clava::dump(D->getDefaultArgument().getArgument(), id, Context);
    }


    clava::dump(D->isParameterPack());
    clava::dump(D->isPackExpansion());
    clava::dump(D->isExpandedParameterPack());

    /*
    if(D->isExpandedParameterPack()) {
        // Dump number of expansion types
        clava::dump(D->getNumExpansionTemplateParameters());
        for(unsigned i=0; i<D->getNumExpansionTemplateParameters(); i++) {
            clava::dump(clava::getId(D->getExpansionTemplateParameters(i), id));
        }
    } else {
        // So that is always has a number
        clava::dump(0);
    }
     */

}

void clava::ClavaDataDumper::DumpNonTypeTemplateParmDeclData(const NonTypeTemplateParmDecl *D) {

    // Hierarchy
    DumpDeclaratorDeclData(D);

    if (D->hasDefaultArgument()) {
        clava::dump(clava::getId(D->getDefaultArgument(), id));
    } else {
        clava::dump(clava::getId((const Expr *) nullptr, id));
    }

    clava::dump(D->defaultArgumentWasInherited());
    clava::dump(D->isParameterPack());
    clava::dump(D->isPackExpansion());
    clava::dump(D->isExpandedParameterPack());

    if(D->isExpandedParameterPack()) {
        // Dump number of expansion types
        clava::dump(D->getNumExpansionTypes());
        for(unsigned i=0; i<D->getNumExpansionTypes(); i++) {
            clava::dump(clava::getId(D->getExpansionType(i), id));
        }
    } else {
        // So that is always has a number
        clava::dump(0);
    }


}

