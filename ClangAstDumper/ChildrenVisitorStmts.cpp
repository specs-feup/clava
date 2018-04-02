//
// Created by JoaoBispo on 01/04/2018.
//

#include "ClangAstDumper.h"
#include "ClavaConstants.h"
#include "ClangNodes.h"

#include <string>

void ClangAstDumper::visitChildren(clava::StmtNode stmtNode, const Stmt* S) {

    std::vector<std::string> visitedChildren;

    switch(stmtNode) {
        //case clava::StmtNode::STMT:
        //    visitedChildren = VisitStmtChildren(static_cast<const Stmt *>(S)); break;
        case clava::StmtNode::COMPOUND_STMT:
            VisitCompoundStmtChildren(static_cast<const CompoundStmt *>(S), visitedChildren); break;
        case clava::StmtNode::DECL_STMT:
            VisitDeclStmtChildren(static_cast<const DeclStmt *>(S), visitedChildren); break;

        case clava::StmtNode::CAST_EXPR:
            VisitCastExprChildren(static_cast<const CastExpr *>(S), visitedChildren); break;


        default: throw std::invalid_argument("ClangDataDumper::visitChildren(StmtNode): Case not implemented, '"+clava::getName(stmtNode)+"'");

    }

    dumpVisitedChildren(S, visitedChildren);
}

void ClangAstDumper::VisitStmtChildren(const Stmt *S, std::vector<std::string> &children) {
    // Return empty vector
}


void ClangAstDumper::VisitCompoundStmtChildren(const CompoundStmt *S, std::vector<std::string> &children) {

    // Visit sub-statements
    for (auto &Arg : S->body()) {
        VisitStmtTop(Arg);
        children.push_back(getId(Arg));
    }
}


void ClangAstDumper::VisitDeclStmtChildren(const DeclStmt *S, std::vector<std::string> &children) {
    // Visit sub-statements

    for (DeclStmt::const_decl_iterator I = S->decl_begin(), E = S->decl_end(); I != E; ++I) {

        VisitDeclTop(*I);
        children.push_back(getId(*I));
    }

}


void ClangAstDumper::VisitCastExprChildren(const CastExpr *S, std::vector<std::string> &children) {

    // Sub-expression
    auto subExprAsWritten = S->getSubExprAsWritten();
    VisitStmtTop(subExprAsWritten);
    children.push_back(getId(subExprAsWritten));
}
