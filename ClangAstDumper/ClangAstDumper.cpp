//
// Created by JoaoBispo on 20/01/2017.
//

#include "ClangAstDumper.h"
#include "ClangAstDumperConstants.h"
#include "ClangNodes.h"
#include "ClangEnums.h"

#include "clang/AST/AST.h"
#include "clang/Lex/Lexer.h"


#include <iostream>
#include <sstream>

#include <assert.h>

//#define DEBUG

//#define VISIT_CHECK

using namespace clang;

ClangAstDumper::ClangAstDumper(ASTContext *Context, int id, int systemHeaderThreshold) : Context(Context), id(id),
    systemHeaderThreshold(systemHeaderThreshold), dataDumper(Context, id)  {};

// This method is equivalent to a VisitQualType() in ClangAstDumperTypes.cpp
void ClangAstDumper::VisitTypeTop(const QualType& T) {

    if(T.isNull()) {
        return;
    }



    // Check if QualType is the same as the underlying type
    if((void*) T.getTypePtr() == T.getAsOpaquePtr()) {
#ifdef VISIT_CHECK
        clava::dump(TOP_VISIT_START);
        clava::dump(clava::getId(T.getTypePtr(), id));
#endif

/*
        if(dumpType(T.getTypePtr())) {
            return;
        }
*/
        // TODO: AST dump method relies on visiting the nodes multiple times
        // For now, detect it to avoid visiting children more than once
        if(seenTypes.count(T.getTypePtr()) == 0) {
            /*
            if(dumpType(T.getTypePtr())) {
                return;
            }

            visitChildrenAndData(T.getTypePtr());
*/
            TypeVisitor::Visit(T.getTypePtr());
        }

        //llvm::errs() << "BRANCH 1 FOR " << T.getTypePtr() << "\n";

        //visitChildren(T.getTypePtr());
        //dataDumper.dump(T.getTypePtr());
        //dumpIdToClassMap(T.getTypePtr(), clava::getClassName(T.getTypePtr()));

        dumpType(T.getTypePtr());
#ifdef VISIT_CHECK
        clava::dump(TOP_VISIT_END);
        clava::dump(clava::getId(T.getTypePtr(), id));
#endif
        return;
    }

    // Dump QualType
    /*
    if(dumpType(T)) {
        return;
    }
    */


    // Visit children
    // TODO: AST dump method relies on visiting the nodes multiple times
    // For now, detect it to avoid visiting children more than once

/*
    if(seenTypes.count(T.getAsOpaquePtr()) == 0) {
        visitChildren(T);
    }
*/



    //dumpType(T);

    if(dumpType(T)) {
        return;
    }

#ifdef VISIT_CHECK
    clava::dump(TOP_VISIT_START);
    clava::dump(clava::getId(T, id));
#endif

    //llvm::errs() << "BRANCH 2 FOR " << T.getAsOpaquePtr() << "\n";

    visitChildren(T);
    dataDumper.dump(T);
    dumpIdToClassMap(T.getAsOpaquePtr(), "QualType");

#ifdef VISIT_CHECK
    clava::dump(TOP_VISIT_END);
    clava::dump(clava::getId(T, id));
#endif

    // Dump data
    //dataDumper.dump(clava::TypeNode::TYPE, T);

    // Visit underlying (unqualified) type
    //TypeVisitor::Visit(T.getTypePtr());
//llvm::errs() << "Opaque PTR: " << T.getAsOpaquePtr() << "\n";
//llvm::errs() << "Underlying PTR: " << T.getTypePtr() << "\n";

    //dumpType(T);

//    auto typeAddr = T.getTypePtrOrNull();
//    if(typeAddr == nullptr) {
//        return;
//    }


}

void ClangAstDumper::VisitTypeTop(const Type *T) {
    //std::cout << "HELLO\n";

    if(T == nullptr) {
        return;
    }
//    llvm::errs() << "TYPE TOP:" << T << "\n";
//    llvm::errs() << "TYPE TOP CLASS:" << T->getTypeClass() << "\n";
//    llvm::errs() << "TYPE TOP 2\n";
#ifdef VISIT_CHECK
    clava::dump(TOP_VISIT_START);
    clava::dump(clava::getId(T, id));
#endif

    TypeVisitor::Visit(T);
//    llvm::errs() << "TYPE TOP 3\n";
#ifdef VISIT_CHECK
    clava::dump(TOP_VISIT_END);
    clava::dump(clava::getId(T, id));
#endif
}

void ClangAstDumper::VisitStmtTop(const Stmt *Node) {
    //VisitStmt(Node);

    if(Node == nullptr) {
        return;
    }

#ifdef VISIT_CHECK
    clava::dump(TOP_VISIT_START);
    clava::dump(clava::getId(Node, id));
#endif

/*
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(Node);
*/
    ConstStmtVisitor::Visit(Node);

#ifdef VISIT_CHECK
    clava::dump(TOP_VISIT_END);
    clava::dump(clava::getId(Node, id));
#endif
}

void ClangAstDumper::VisitDeclTop(const Decl *Node) {
    if(Node == nullptr) {
        return;
    }

#ifdef VISIT_CHECK
    clava::dump(TOP_VISIT_START);
    clava::dump(clava::getId(Node, id));
#endif
/*
    if(dumpDecl(Node)) {
        return;
    }

    visitChildrenAndData(Node);
*/
    // Do not visit if in system header
    // If is in system header
    /*
    FullSourceLoc fullLocation = Context->getFullLoc(Node->getLocStart());
    if (fullLocation.isValid() && fullLocation.isInSystemHeader()) {
        return;
    }
     */

    ConstDeclVisitor::Visit(Node);

#ifdef VISIT_CHECK
    clava::dump(TOP_VISIT_END);
    clava::dump(clava::getId(Node, id));
#endif

}

void ClangAstDumper::VisitAttrTop(const Attr *Node) {
    if(Node == nullptr) {
        return;
    }

#ifdef VISIT_CHECK
    clava::dump(TOP_VISIT_START);
    clava::dump(clava::getId(Node, id));
#endif

    VisitAttr(Node);

#ifdef VISIT_CHECK
    clava::dump(TOP_VISIT_END);
    clava::dump(clava::getId(Node, id));
#endif
}

//void ClangAstDumper::log(const char* name, const void* addr) {
void ClangAstDumper::log(std::string name, const void* addr) {
#ifdef DEBUG
    llvm::errs() << name << " " << addr << "\n";
#endif

//    clava::dump(VISIT_START);
//    clava::dump(clava::getId(addr, id));
}

void ClangAstDumper::log(const Decl* D) {
    log(clava::getClassName(D), D);
}

void ClangAstDumper::log(const Stmt* S) {
    log(clava::getClassName(S), S);
}

void ClangAstDumper::log(const Type* T) {
    log(clava::getClassName(T), T);
}

void ClangAstDumper::log(const Attr* A) {
    log(clava::getClassName(A), A);
}


std::string ClangAstDumper::loc2str(SourceLocation locStart, SourceLocation locEnd) {

    clang::SourceManager *sm = &Context->getSourceManager();
    clang::LangOptions lopt = Context->getLangOpts();

    clang::SourceLocation b(locStart), _e(locEnd);
    clang::SourceLocation e(clang::Lexer::getLocForEndOfToken(_e, 0, *sm, lopt));


    std::string bChars(sm->getCharacterData(b));
    std::string eChars(sm->getCharacterData(e));

    if(bChars == "<<<<INVALID BUFFER>>>>") {
        return "";
    }

    if(eChars == "<<<<INVALID BUFFER>>>>") {
        return "";
    }

    return std::string(sm->getCharacterData(b), sm->getCharacterData(e)-sm->getCharacterData(b));
}


void ClangAstDumper::dumpSourceRange(std::string id, SourceLocation startLoc, SourceLocation endLoc) {
    llvm::errs() << "<SourceRange Dump>\n";

    llvm::errs() << id << "\n";
    // All components of the source range will be dumped

    const SourceManager& SM = Context->getSourceManager();

    SourceLocation startSpellingLoc = SM.getSpellingLoc(startLoc);
    PresumedLoc startPLoc = SM.getPresumedLoc(startSpellingLoc);

    if (startPLoc.isInvalid()) {
        llvm::errs() << "<invalid>\n";
        /*
        llvm::errs() << "startLoc:\n";
        startLoc.dump(SM);
        llvm::errs() << "endLoc:\n";
        endLoc.dump(SM);
        assert(startLoc == endLoc);
         */
        return;
    }


    // Dump start location
    llvm::errs() << startPLoc.getFilename() << "\n";
    llvm::errs() << startPLoc.getLine() << "\n";
    llvm::errs() << startPLoc.getColumn() << "\n";

    if(startLoc == endLoc) {
        llvm::errs() << "<end>\n";
        return;
    }

    SourceLocation endSpellingLoc = SM.getSpellingLoc(endLoc);
    PresumedLoc endPLoc = SM.getPresumedLoc(endSpellingLoc);

    if(endPLoc.isInvalid()) {
        llvm::errs() << "<end>\n";
        return;
    }

    const char* endFilename = endPLoc.getFilename();
    if(!endFilename) {
        endFilename = startPLoc.getFilename();
    }

    unsigned int endLine = endPLoc.getLine();
    if(!endLine) {
        endLine = startPLoc.getLine();
    }

    unsigned int endCol = endPLoc.getColumn();
    if(!endCol) {
        endCol = startPLoc.getColumn();
    }

    // Dump end location
    llvm::errs() << endFilename << "\n";
    llvm::errs() << endLine << "\n";
    llvm::errs() << endCol << "\n";
}

/*
std::string ClangAstDumper::getId(const void* addr) {
    std::stringstream ss;
    ss <<  addr << "_" << id;

    return ss.str();
}
*/
/*
std::string ClangAstDumper::getId(const Decl* addr) {

}

std::string ClangAstDumper::getId(const Stmt* addr) {

}

std::string ClangAstDumper::getId(const Expr* addr) {

}

std::string ClangAstDumper::getId(const Type* addr) {

}

std::string ClangAstDumper::getId(const Attr* addr) {

}
*/

std::string ClangAstDumper::toBoolString(int value) {
    return value ? "true" : "false";
}


const Type* getTypePtr(QualType T, std::string source)  {
    assert(!T.isNull() && "Cannot retrieve a NULL type pointer");

    return T.getTypePtr();
}

void ClangAstDumper::dumpVisitedChildren(const void *pointer, std::vector<std::string> children) {
    llvm::errs() << VISITED_CHILDREN << "\n";
    // If node has children, pointer will not be null
    llvm::errs() << clava::getId(pointer, id) << "\n";
    llvm::errs() << children.size() << "\n";

    for(auto child : children) {
        llvm::errs() << child << "\n";
    }
}

void ClangAstDumper::dumpIdToClassMap(const void* pointer, std::string className) {
    llvm::errs() << ID_TO_CLASS_MAP << "\n";
    llvm::errs() << clava::getId(pointer, id) << "\n";
    llvm::errs() << className << "\n";
}

void ClangAstDumper::dumpTopLevelType(const QualType &type) {
    llvm::errs() << TOP_LEVEL_TYPES << "\n";
    clava::dump(type, id);
}


void ClangAstDumper::dumpTopLevelAttr(const Attr *attr) {
    llvm::errs() << TOP_LEVEL_ATTRIBUTES << "\n";
    llvm::errs() << clava::getId(attr, id) << "\n";
}



void ClangAstDumper::emptyChildren(const void *pointer) {
    std::vector<std::string> noChildren;
    dumpVisitedChildren(pointer, noChildren);
}


const void ClangAstDumper::addChild(const Decl *addr, std::vector<std::string> &children) {

    // Do not add child if goes above system header threshold
    if(systemHeaderThreshold > 0 && currentSystemHeaderLevel > systemHeaderThreshold) {
        return;
    }

    std::string clavaId = clava::getId(addr, id);

#ifdef VISIT_CHECK
    if(addr != nullptr) {
        clava::dump(VISIT_START);
        clava::dump(clavaId);
    }
#endif

    VisitDeclTop(addr);
    children.push_back(clavaId);

#ifdef VISIT_CHECK
    if(addr != nullptr) {
        clava::dump(VISIT_END);
        clava::dump(clavaId);
    }
#endif

};

const void ClangAstDumper::addChildren(DeclContext::decl_range decls, std::vector<std::string> &children) {



    for (auto decl = decls.begin(), endDecl = decls.end(); decl != endDecl; ++decl) {

        // If CXXRecordDecl without definition, skip
        if (const CXXRecordDecl *recordDecl = dyn_cast<CXXRecordDecl>(*decl)) {
            if (!recordDecl->hasDefinition()) {
                continue;
            }
        }

        if (*decl == nullptr) {
            continue;
        }


        addChild(*decl, children);
    }

};

const void ClangAstDumper::addChild(const Stmt *addr, std::vector<std::string> &children) {

    // Do not add child if goes above system header threshold
    if(systemHeaderThreshold > 0 && currentSystemHeaderLevel > systemHeaderThreshold) {
        return;
    }

    std::string clavaId = clava::getId(addr, id);

#ifdef VISIT_CHECK
    if(addr != nullptr) {
        clava::dump(VISIT_START);
        clava::dump(clavaId);
    }
#endif

    VisitStmtTop(addr);
    children.push_back(clavaId);

#ifdef VISIT_CHECK
    if(addr != nullptr) {
        clava::dump(VISIT_END);
        clava::dump(clavaId);
    }
#endif

};

const void ClangAstDumper::addChild(const Expr *addr, std::vector<std::string> &children) {
    // Do not add child if goes above system header threshold
    if(systemHeaderThreshold > 0 && currentSystemHeaderLevel > systemHeaderThreshold) {
        return;
    }

    std::string clavaId = clava::getId(addr, id);

#ifdef VISIT_CHECK
    if(addr != nullptr) {
        clava::dump(VISIT_START);
        clava::dump(clavaId);
    }
#endif

    VisitStmtTop(addr);
    children.push_back(clavaId);

#ifdef VISIT_CHECK
    if(addr != nullptr) {
        clava::dump(VISIT_END);
        clava::dump(clavaId);
    }
#endif

};

const void ClangAstDumper::addChild(const Type *addr, std::vector<std::string> &children) {

    // Do not add child if goes above system header threshold
    if(systemHeaderThreshold > 0 && currentSystemHeaderLevel > systemHeaderThreshold) {
        return;
    }

    std::string clavaId = clava::getId(addr, id);

#ifdef VISIT_CHECK
    if(addr != nullptr) {
        clava::dump(VISIT_START);
        clava::dump(clavaId);
    }
#endif

    VisitTypeTop(addr);
    children.push_back(clavaId);

#ifdef VISIT_CHECK
    if(addr != nullptr) {
        clava::dump(VISIT_END);
        clava::dump(clavaId);
    }
#endif

};

const void ClangAstDumper::addChild(const QualType &addr, std::vector<std::string> &children) {

    // Do not add child if goes above system header threshold
    if(systemHeaderThreshold > 0 && currentSystemHeaderLevel > systemHeaderThreshold) {
        return;
    }

    std::string clavaId = clava::getId(addr, id);

#ifdef VISIT_CHECK
    clava::dump(VISIT_START);
    clava::dump(clavaId);
#endif

    VisitTypeTop(addr);
    children.push_back(clavaId);

#ifdef VISIT_CHECK
    clava::dump(VISIT_END);
    clava::dump(clavaId);
#endif

};

const void ClangAstDumper::addChild(const Attr *addr, std::vector<std::string> &children) {

    // Do not add child if goes above system header threshold
    if(systemHeaderThreshold > 0 && currentSystemHeaderLevel > systemHeaderThreshold) {
        return;
    }

    std::string clavaId = clava::getId(addr, id);

#ifdef VISIT_CHECK
    if(addr != nullptr) {
        clava::dump(VISIT_START);
        clava::dump(clavaId);
    }
#endif

    VisitAttrTop(addr);
    children.push_back(clavaId);

#ifdef VISIT_CHECK
    if(addr != nullptr) {
        clava::dump(VISIT_END);
        clava::dump(clavaId);
    }
#endif

};




void ClangAstDumper::VisitTemplateArgument(const TemplateArgument& templateArg) {
    switch(templateArg.getKind()) {
        case TemplateArgument::ArgKind::Type:
            VisitTypeTop(templateArg.getAsType());
            break;
        case TemplateArgument::ArgKind::Expression:
            VisitStmtTop(templateArg.getAsExpr());
            break;
        case TemplateArgument::ArgKind::Pack:
            // Do nothing
            break;
        case TemplateArgument::ArgKind::Integral:
            // Do nothing
            break;
        default: throw std::invalid_argument("ClangAstDumper::VisitTemplateArgument(): Case not implemented, '"+clava::TEMPLATE_ARG_KIND[templateArg.getKind()]+"'");
    }
};