//
// Created by JoaoBispo on 20/01/2017.
//

#include "ClangAstDumper.h"
#include "ClangAstDumperConstants.h"
#include "ClangNodes.h"

#include "clang/AST/AST.h"

#include <iostream>
#include <sstream>

using namespace clang;

void ClangAstDumper::visitChildrenAndData(const Stmt *S) {
    // Visit children
    visitChildren(S);

    // Dump data
    dataDumper.dump(S);
}

void ClangAstDumper::visitChildrenAndData(const Expr *E) {
    // Visit children
    visitChildren(E);

    // Dump data
    dataDumper.dump(E);
}

/*
 * STMTS
 */

bool ClangAstDumper::dumpStmt(const Stmt* stmtAddr) {

    if(seenStmts.count(stmtAddr) != 0) {
        return true;
    }

    log(stmtAddr);

    // A StmtDumper is created for each context,
    // no need to use id to disambiguate
    seenStmts.insert(stmtAddr);

    std::ostringstream extendedId;
    extendedId << stmtAddr << "_" << id;

    // Dump location
    dumpSourceRange(extendedId.str(), stmtAddr->getLocStart(), stmtAddr->getLocEnd());

    dumpIdToClassMap(stmtAddr, clava::getClassName(stmtAddr));

    return false;
}

void ClangAstDumper::VisitStmt(const Stmt *Node) {
    if(dumpStmt(Node)) {
        return;
    }

//    llvm::errs() << "DUMPING STMT: " << getId(Node) << "\n";

/*
    for (const Stmt *SubStmt : Node->children()) {
        if (SubStmt) {
            VisitStmtTop(SubStmt);
        }
    }
*/
    visitChildrenAndData(Node);
    /*
    // Visit children
    visitChildren(clava::StmtNode::STMT, Node);

    // Dump data
    dataDumper.dump(clava::StmtNode::STMT, Node);
*/
}

/*
void ClangAstDumper::VisitDeclStmt(const DeclStmt *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    // Visit children
    visitChildren(clava::StmtNode::DECL_STMT, Node);

    // Dump data
    dataDumper.dump(clava::StmtNode::STMT , Node);

}
 */
void ClangAstDumper::VisitCXXForRangeStmt(const CXXForRangeStmt *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(Node);

    VisitStmtTop(Node->getRangeStmt());
    VisitStmtTop(Node->getBeginEndStmt());
    VisitStmtTop(Node->getCond());
    VisitStmtTop(Node->getInc());
    VisitStmtTop(Node->getBody());
}

/*
void ClangAstDumper::VisitCompoundStmt(const CompoundStmt *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    // Visit children
    visitChildren(clava::StmtNode::STMT, Node);

    // Dump data
    dataDumper.dump(clava::StmtNode::STMT , Node);
}
 */

void ClangAstDumper::VisitForStmt(const ForStmt *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(Node);

    if(Node->getInit() != nullptr) {
        VisitStmtTop(Node->getInit());
    }

    if(Node->getCond() != nullptr) {
        VisitStmtTop(Node->getCond());
    }

    if(Node->getInc() != nullptr) {
        VisitStmtTop(Node->getInc());
    }

    if(Node->getConditionVariable()!= nullptr) {
        VisitDeclTop(Node->getConditionVariable());
    }

    if(Node->getBody() != nullptr) {
        VisitStmtTop(Node->getBody());
    }




}

