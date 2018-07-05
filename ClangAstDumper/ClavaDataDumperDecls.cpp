//
// Created by JoaoBispo on 18/03/2018.
//

#include "ClavaDataDumper.h"
#include "ClangNodes.h"
#include "ClavaConstants.h"
#include "ClangEnums.h"

#include <map>

const std::map<const std::string, clava::DeclNode > clava::DECL_DATA_MAP = {
        {"CXXConstructorDecl", clava::DeclNode::CXX_METHOD_DECL},
        {"CXXConversionDecl", clava::DeclNode::CXX_METHOD_DECL},
        {"CXXDestructorDecl", clava::DeclNode::CXX_METHOD_DECL},
        {"CXXMethodDecl", clava::DeclNode::CXX_METHOD_DECL},
        {"CXXRecordDecl", clava::DeclNode::TYPE_DECL},
        {"FieldDecl", clava::DeclNode::NAMED_DECL},
        {"FunctionDecl", clava::DeclNode::FUNCTION_DECL},
        {"NamespaceAliasDecl", clava::DeclNode::NAMED_DECL},
        {"ObjCImplementationDecl", clava::DeclNode::NAMED_DECL},
        {"ParmVarDecl", clava::DeclNode::PARM_VAR_DECL},
        {"TemplateDecl", clava::DeclNode::NAMED_DECL},
        {"TemplateTypeParmDecl", clava::DeclNode::NAMED_DECL},
        {"TypedefDecl", clava::DeclNode::NAMED_DECL},
        {"TypeDecl", clava::DeclNode::TYPE_DECL},
        {"EnumDecl", clava::DeclNode::TYPE_DECL},
        {"VarDecl", clava::DeclNode::VAR_DECL}
};



void clava::ClavaDataDumper::dump(const Decl* D) {

    // Get classname
    const std::string classname = clava::getClassName(D);

    // Get corresponding DeclNode
    DeclNode declNode = DECL_DATA_MAP.count(classname) == 1 ? DECL_DATA_MAP.find(classname)->second : DeclNode::DECL;

    dump(declNode, D);
}

void clava::ClavaDataDumper::dump(clava::DeclNode declNode, const Decl* D) {
    // Dump header
    llvm::errs() << getDataName(declNode) << "\n";
    llvm::errs() << clava::getId(D, id) << "\n";
    //DumpHeader(getDataName(declNode), D);

    switch(declNode) {
        case clava::DeclNode::DECL:
            DumpDeclData(D); break;
        case clava::DeclNode::NAMED_DECL:
            DumpNamedDeclData(static_cast<const NamedDecl *>(D)); break;
        case clava::DeclNode::TYPE_DECL:
            DumpTypeDeclData(static_cast<const TypeDecl *>(D)); break;
        case clava::DeclNode::VALUE_DECL:
            DumpValueDeclData(static_cast<const ValueDecl *>(D)); break;
        case clava::DeclNode::FUNCTION_DECL:
            DumpFunctionDeclData(static_cast<const FunctionDecl *>(D)); break;
        case clava::DeclNode::CXX_METHOD_DECL:
            DumpCXXMethodDeclData(static_cast<const CXXMethodDecl *>(D)); break;
        case clava::DeclNode::VAR_DECL:
            DumpVarDeclData(static_cast<const VarDecl *>(D)); break;
        case clava::DeclNode::PARM_VAR_DECL:
            DumpParmVarDeclData(static_cast<const ParmVarDecl *>(D)); break;
        default:
            throw std::invalid_argument("ClangDataDumper::dump(DeclNode):: Case not implemented, '" + getName(declNode) + "'");


    }
}



void clava::ClavaDataDumper::DumpDeclData(const Decl *D) {
    clava::dumpSourceInfo(Context, D->getLocStart(), D->getLocEnd());

    // Print information about Decl
    clava::dump(D->isImplicit());
    clava::dump(D->isUsed());
    clava::dump(D->isReferenced());
    clava::dump(D->isInvalidDecl());


    // Attributes
    std::vector<std::string> attributesIds;
    for (Decl::attr_iterator I = D->attr_begin(), E = D->attr_end(); I != E;
         ++I) {
        attributesIds.push_back(clava::getId(*I, id));
    }
    clava::dump(attributesIds);

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
    clava::dump(D->getQualifiedNameAsString());
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

    clava::dump(clava::getId(D->getTypeForDecl(), id));
}



void clava::ClavaDataDumper::DumpValueDeclData(const ValueDecl *D) {
    // Hierarchy
    DumpNamedDeclData(D);

    clava::dump(D->getType(), id);
    clava::dump(D->isWeak());
}




void clava::ClavaDataDumper::DumpFunctionDeclData(const FunctionDecl *D) {
    // Hierarchy
    DumpValueDeclData(D);

    // Print information about FunctionDecl
    clava::dump(D->isConstexpr());
    clava::dump(D->getTemplatedKind());
//    llvm::errs() << D->isConstexpr() << "\n";
//    llvm::errs() << D->getTemplatedKind() << "\n";


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


void clava::ClavaDataDumper::DumpVarDeclData(const VarDecl *D) {
    // Hierarchy
    //DumpNamedDeclData(D);
    DumpValueDeclData(D);

    // Print information about VarDecl
    clava::dump(D->getStorageClass());
    clava::dump(D->getTLSKind());
    clava::dump(D->isModulePrivate());
    clava::dump(D->isNRVOVariable());
    clava::dump(D->getInitStyle());

    clava::dump(D->isConstexpr());
    clava::dump(D->isStaticDataMember() );
    clava::dump(D->isOutOfLine());
    clava::dump(D->hasGlobalStorage());
//    llvm::errs() << D->isConstexpr() << "\n";
//    llvm::errs() << D->isStaticDataMember() << "\n";
//    llvm::errs() << D->isOutOfLine() << "\n";
//    llvm::errs() << D->hasGlobalStorage() << "\n";


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