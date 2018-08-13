//
// Created by JoaoBispo on 20/03/2018.
//

#include "ClangAstDumper.h"
#include "ClangNodes.h"
#include "ClavaConstants.h"
#include "ClangEnums.h"

#include <string>


const std::map<const std::string, clava::DeclNode > ClangAstDumper::DECL_CHILDREN_MAP = {
        {"CXXConstructorDecl", clava::DeclNode::CXX_CONSTRUCTOR_DECL},
        {"CXXConversionDecl", clava::DeclNode::CXX_METHOD_DECL},
        {"CXXDestructorDecl", clava::DeclNode::CXX_METHOD_DECL},
        {"CXXMethodDecl", clava::DeclNode::CXX_METHOD_DECL},
        {"EnumDecl", clava::DeclNode::ENUM_DECL},
        {"RecordDecl", clava::DeclNode::RECORD_DECL},
        {"CXXRecordDecl", clava::DeclNode::CXX_RECORD_DECL},
        {"FunctionDecl", clava::DeclNode::FUNCTION_DECL},
        {"VarDecl", clava::DeclNode::VAR_DECL},
        {"ParmVarDecl", clava::DeclNode::VAR_DECL},
        {"TypeDecl", clava::DeclNode::TYPE_DECL},
        {"FieldDecl", clava::DeclNode::FIELD_DECL},
        {"ClassTemplateDecl", clava::DeclNode::TEMPLATE_DECL},
        {"FunctionTemplateDecl", clava::DeclNode::TEMPLATE_DECL},
        {"TypeAliasTemplateDecl", clava::DeclNode::TEMPLATE_DECL},
        {"VarTemplateDecl", clava::DeclNode::TEMPLATE_DECL},
        {"TemplateTypeParmDecl", clava::DeclNode::TEMPLATE_TYPE_PARM_DECL},
        {"EnumConstantDecl", clava::DeclNode::ENUM_CONSTANT_DECL},
        {"NonTypeTemplateParmDecl", clava::DeclNode::VALUE_DECL},




};


void ClangAstDumper::visitChildren(const Decl* D) {
    // Get classname
    const std::string classname = clava::getClassName(D);

    // Get corresponding DeclNode
    clava::DeclNode declNode = DECL_CHILDREN_MAP.count(classname) == 1 ? DECL_CHILDREN_MAP.find(classname)->second :
                               clava::DeclNode::DECL;

    visitChildren(declNode, D);
}


void ClangAstDumper::visitChildren(clava::DeclNode declNode, const Decl* D) {

    std::vector<std::string> visitedChildren;

    switch(declNode) {
        case clava::DeclNode::DECL:
            VisitDeclChildren(D, visitedChildren); break;
        case clava::DeclNode::NAMED_DECL:
            VisitNamedDeclChildren(static_cast<const NamedDecl *>(D), visitedChildren); break;
        case clava::DeclNode::TYPE_DECL:
            VisitTypeDeclChildren(static_cast<const TypeDecl *>(D), visitedChildren); break;
        case clava::DeclNode::TAG_DECL:
            VisitTagDeclChildren(static_cast<const TagDecl *>(D), visitedChildren); break;
         case clava::DeclNode::ENUM_DECL:
            VisitEnumDeclChildren(static_cast<const EnumDecl *>(D), visitedChildren); break;
        case clava::DeclNode::RECORD_DECL:
            VisitRecordDeclChildren(static_cast<const RecordDecl *>(D), visitedChildren); break;
        case clava::DeclNode::VALUE_DECL:
            VisitValueDeclChildren(static_cast<const ValueDecl *>(D), visitedChildren); break;
        case clava::DeclNode::FIELD_DECL:
            VisitFieldDeclChildren(static_cast<const FieldDecl *>(D), visitedChildren); break;
        case clava::DeclNode::FUNCTION_DECL:
            VisitFunctionDeclChildren(static_cast<const FunctionDecl *>(D), visitedChildren); break;
        case clava::DeclNode::CXX_METHOD_DECL:
            VisitCXXMethodDeclChildren(static_cast<const CXXMethodDecl *>(D), visitedChildren); break;
         case clava::DeclNode::CXX_CONSTRUCTOR_DECL:
            VisitCXXConstructorDeclChildren(static_cast<const CXXConstructorDecl *>(D), visitedChildren); break;
        case clava::DeclNode::CXX_RECORD_DECL:
            VisitCXXRecordDeclChildren(static_cast<const CXXRecordDecl *>(D), visitedChildren); break;
        case clava::DeclNode::VAR_DECL:
            VisitVarDeclChildren(static_cast<const VarDecl *>(D), visitedChildren); break;
        case clava::DeclNode::TEMPLATE_DECL:
            VisitTemplateDeclChildren(static_cast<const TemplateDecl *>(D), visitedChildren); break;
        case clava::DeclNode::TEMPLATE_TYPE_PARM_DECL:
            VisitTemplateTypeParmDeclChildren(static_cast<const TemplateTypeParmDecl *>(D), visitedChildren); break;
        case clava::DeclNode::ENUM_CONSTANT_DECL:
            VisitEnumConstantDeclChildren(static_cast<const EnumConstantDecl *>(D), visitedChildren); break;
//        case clava::DeclNode::PARM_VAR_DECL:
//            visitedChildren = VisitParmVarDeclChildren(static_cast<const ParmVarDecl *>(D)); break;
        default: throw std::invalid_argument("ChildrenVisitorDecls::visitChildren: Case not implemented, '"+clava::getName(declNode)+"'");
    }

    dumpVisitedChildren(D, visitedChildren);
}


void ClangAstDumper::VisitDeclChildren(const Decl *D, std::vector<std::string> &children) {
    // Visit attributes
    for (Decl::attr_iterator I = D->attr_begin(), E = D->attr_end(); I != E;
         ++I) {
        Attr* attr = *I;
        VisitAttrTop(attr);
        dumpTopLevelAttr(attr);
    }

}

void ClangAstDumper::VisitNamedDeclChildren(const NamedDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitDeclChildren(D, children);

    // Just visit underlying decl
    //VisitDeclTop(D->getUnderlyingDecl());
    //llvm::errs() << "VISITING " << clava::getId(D->getUnderlyingDecl(), id) << " -> " << clava::getClassName(D->getUnderlyingDecl()) << "\n";
    //llvm::errs() << "ORIGINAL " << clava::getId(D, id) << "\n";
}

void ClangAstDumper::VisitTypeDeclChildren(const TypeDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitNamedDeclChildren(D, children);

    // Visit type
    VisitTypeTop(D->getTypeForDecl());
    dumpType(D->getTypeForDecl());

}

void ClangAstDumper::VisitTagDeclChildren(const TagDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitTypeDeclChildren(D, children);

    //std::string cxxRecordName = "CXXRecord";

    //for (auto I = D->getFirstDecl(), E = D->getDecl; I != E; ++I) {
    //llvm::errs() << "TagDecl id: " << clava::getId(D, id) << "\n";
    //int declCounter = 0;
    for(auto decl : D->decls()) {
    //for(auto decl : D->noload_decls()) {
        // If CXXRecordDecl without definition, skip
        if (const CXXRecordDecl *recordDecl = dyn_cast<CXXRecordDecl>(decl)) {
            if(!recordDecl->hasDefinition()) {
                continue;
            }
        }

        if(decl == nullptr) {
            continue;
        }


        //VisitDecl(decl);
        addChild(decl, children);
        //VisitDeclTop(decl);
        //children.push_back(clava::getId(decl, id));

        // Skip if the first child, and if current TagDecl is a CXXRecord, and

        //llvm::errs() << "TagDecl child: " << decl->getDeclKindName() << " " << clava::getId(decl, id) << "\n";


        //if(cxxRecordName.compare(decl->getDeclKindName()) == 0) {
        //    llvm::errs() << "Is record\n";
        //    llvm::errs() << "Has def? " << static_cast<const CXXRecordDecl *>(decl)->hasDefinition() << "\n";
        //}
        //VisitDeclTop(decl);
        //children.push_back(clava::getId(decl, id));
        //declCounter++;
    }

    /*
    for(auto decl : D->noload_decls()) {
        llvm::errs() << "No load TagDecl child: " << decl->getDeclKindName() << " " << clava::getId(decl, id) << "\n";

        //VisitDeclTop(decl);
        //children.push_back(clava::getId(decl, id));
    }
     */

}


void ClangAstDumper::VisitEnumDeclChildren(const EnumDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitTagDeclChildren(D, children);

    // Visit type
    VisitTypeTop(D->getIntegerType());
    //dumpTopLevelType(D->getType());


}

void ClangAstDumper::VisitValueDeclChildren(const ValueDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitNamedDeclChildren(D, children);

    // Visit type
    VisitTypeTop(D->getType());
    dumpTopLevelType(D->getType());

}
void ClangAstDumper::VisitFieldDeclChildren(const FieldDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitValueDeclChildren(D, children);

    // Add bitwidth
    addChild(D->getBitWidth(), children);

    // Add init
    addChild(D->getInClassInitializer(), children);
}

void ClangAstDumper::VisitFunctionDeclChildren(const FunctionDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitValueDeclChildren(D, children);

    // Visit canonical and previous decls
    VisitDeclTop(D->getPreviousDecl());
    VisitDeclTop(D->getCanonicalDecl());

    // Visit template arguments
    auto templateSpecializationArgs = D->getTemplateSpecializationArgs();
    if(templateSpecializationArgs != nullptr) {
        for(auto templateArg : templateSpecializationArgs->asArray()) {
            switch(templateArg.getKind()) {
                case TemplateArgument::ArgKind::Type:
                    VisitTypeTop(templateArg.getAsType());
                    break;
                case TemplateArgument::ArgKind::Expression:
                    VisitStmtTop(templateArg.getAsExpr());
                    break;
                default: throw std::invalid_argument("ClangNodes::dump(TemplateArgument&): Case not implemented, '"+clava::TEMPLATE_ARG_KIND[templateArg.getKind()]+"'");
            }
        }
    }



    // Visit parameters
    for(auto param : D->parameters()) {
        addChild(param, children);
        //VisitDeclTop(param);
        //children.push_back(clava::getId(param, id));
    }
    /*
    for (auto I = D->param_begin(), E = D->param_end(); I != E; ++I) {
        llvm::errs() << "PARAM: " <<  getId(I) << "\n";
        llvm::errs() << "PARAM CLASS: " <<  clava::getClassName(I) << "\n";

        VisitDeclTop(*I);
        children.push_back(getId(I));
    }
     */

    // Visit decls in prototype scope
/*
    for (ArrayRef<NamedDecl *>::iterator I = D->getDeclsInPrototypeScope().begin(),
                 E = D->getDeclsInPrototypeScope().end(); I != E; ++I) {

        addChild(*I, children);
        //VisitDeclTop(*I);
        //children.push_back(clava::getId(*I, id));
    }
*/
    // Visit body
    //if(D->hasBody()) {
    if (D->doesThisDeclarationHaveABody()) {
        //llvm::errs() << "BODY: " <<  getId(D->getBody()) << "\n";
        addChild(D->getBody(), children);
        //VisitStmtTop(D->getBody());
        //children.push_back(clava::getId(D->getBody(), id));
    }

}


void ClangAstDumper::VisitCXXMethodDeclChildren(const CXXMethodDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitFunctionDeclChildren(D, children);


    // Visit record decl
    VisitDeclTop(D->getParent());
}

void ClangAstDumper::VisitCXXConstructorDeclChildren(const CXXConstructorDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitCXXMethodDeclChildren(D, children);


    // Visit CXXCtorInitializers
    for (auto init = D->init_begin(), init_last = D->init_end(); init != init_last; ++init) {
        // Init expr
        VisitStmtTop((*init)->getInit());

        if ((*init)->isAnyMemberInitializer()) {
            VisitDeclTop((*init)->getAnyMember());
            continue;
        }

        if ((*init)->isBaseInitializer()) {
            VisitTypeTop((*init)->getBaseClass());
            continue;
        }

        if ((*init)->isDelegatingInitializer()) {
            VisitTypeTop((*init)->getTypeSourceInfo()->getType());
            continue;
        }

        throw std::invalid_argument(
                "ClangDataDumper::VisitCXXConstructorDeclChildren():: CXXCtorInitializer case not implemented");
    }
}

void ClangAstDumper::VisitRecordDeclChildren(const RecordDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitTagDeclChildren(D, children);

    //D->decls_begin()

    // Visit fields
    /*
    for (auto field : D->fields()) {
        addChild(field, children);
        //VisitDeclTop(field);
        //children.push_back(clava::getId(field, id));
    }
*/


}

void ClangAstDumper::VisitCXXRecordDeclChildren(const CXXRecordDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitRecordDeclChildren(D, children);

    if (D->hasDefinition()) {
        // Visit types in bases
        for (const auto &I : D->bases()) {
            VisitTypeTop(I.getType());
        }
        VisitDeclTop(D->getDefinition());
    }

    //llvm::errs() << "CXX RECORD HAS DEF: " << D->hasDefinition() << "\n";



    /*
    llvm::errs() << "CXXRECORDDECL CANONICAL: " << clava::getId(D->getCanonicalDecl(), id) << "\n";
    llvm::errs() << "CXXRECORDDECL PREVIOUS: " << clava::getId(D->getPreviousDecl(), id) << "\n";
    llvm::errs() << "CXXRECORDDECL MOST RECENT: " << clava::getId(D->getMostRecentDecl(), id) << "\n";
    llvm::errs() << "CXXRECORDDECL INSTANTIATED: " << clava::getId(D->getInstantiatedFromMemberClass(), id) << "\n";
    llvm::errs() << "CXXRECORDDECL TEMPLATE: " << clava::getId(D->getTemplateInstantiationPattern(), id) << "\n";
*/

        //llvm::errs() << "CXXRECORDDECL DEF: " << clava::getId(D->getDefinition(), id) << "\n";

    // Visit constructors
    /*
    for (auto ctor : D->ctors()) {
        VisitDeclTop(ctor);
        children.push_back(clava::getId(ctor, id));
    }
     */


    // Visit methods
    // This makes the program explode
/*
    for(auto method = D->method_begin(); method != D->method_end(); method++) {
        VisitDeclTop(*method);
        children.push_back(clava::getId(*method, id));
    }
    */
/*
    for (auto method : D->methods()) {
        VisitDeclTop(method);
        children.push_back(clava::getId(method, id));
    }
*/



}


/*
void ClangAstDumper::VisitCXXConstructorDeclChildren(const CXXConstructorDecl *D) {
    // Hierarchy
    VisitFunctionDeclChildren(D);


}
 */


void ClangAstDumper::VisitVarDeclChildren(const VarDecl *D, std::vector<std::string> &children) {
    // Hierarchy
    VisitValueDeclChildren(D, children);

    if (D->hasInit()) {
        addChild(D->getInit(), children);
        //VisitStmtTop(D->getInit());
        //children.push_back(clava::getId(D->getInit(), id));
    }

}

/*
std::vector<std::string> ClangAstDumper::VisitParmVarDeclChildren(const ParmVarDecl *D) {

    // Hierarchy
    std::vector<std::string> children = VisitVarDeclChildren(D);
    //children.push_back(getId(D));

    return children;
}
 */

void ClangAstDumper::VisitTemplateDeclChildren(const TemplateDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitNamedDeclChildren(D, children);

    auto templateParams = D->getTemplateParameters();
    if(templateParams) {
        for (auto I = templateParams->begin(), E = templateParams->end(); I != E; ++I)
            addChild(*I, children);
            //VisitDeclTop(*I);
    }


    addChild(D->getTemplatedDecl(), children);
}


void ClangAstDumper::VisitTemplateTypeParmDeclChildren(const TemplateTypeParmDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitTypeDeclChildren(D, children);

    //addChild(D->getDefaultArgument(), children);
    if(D->hasDefaultArgument()) {
        VisitTypeTop(D->getDefaultArgument());
    }

}

void ClangAstDumper::VisitEnumConstantDeclChildren(const EnumConstantDecl *D, std::vector<std::string> &children) {

    // Hierarchy
    VisitValueDeclChildren(D, children);

    addChild(D->getInitExpr(), children);
}
