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

    // Dump data
    dataDumper.dump(clava::StmtNode::STMT, Node);

    for (const Stmt *SubStmt : Node->children()) {
        if (SubStmt) {
            VisitStmtTop(SubStmt);
        }
    }

}


void ClangAstDumper::VisitDeclStmt(const DeclStmt *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    // Dump data
    dataDumper.dump(clava::StmtNode::STMT , Node);

    // Visit children
    visitChildren(clava::StmtNode::DECL_STMT, Node);



}
void ClangAstDumper::VisitCXXForRangeStmt(const CXXForRangeStmt *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    VisitStmtTop(Node->getRangeStmt());
    VisitStmtTop(Node->getBeginEndStmt());
    VisitStmtTop(Node->getCond());
    VisitStmtTop(Node->getInc());
    VisitStmtTop(Node->getBody());
}

void ClangAstDumper::VisitCompoundStmt(const CompoundStmt *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    // Dump data
    dataDumper.dump(clava::StmtNode::STMT , Node);

    // Visit children
    visitChildren(clava::StmtNode::COMPOUND_STMT, Node);
}

void ClangAstDumper::VisitForStmt(const ForStmt *Node) {
    if(dumpStmt(Node)) {
        return;
    }
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

