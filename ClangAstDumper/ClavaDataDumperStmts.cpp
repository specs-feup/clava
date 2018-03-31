//
// Created by JoaoBispo on 30/03/2018.
//

#include "ClavaDataDumper.h"
#include "ClangNodes.h"

void clava::ClavaDataDumper::dump(clava::StmtNode stmtNode, const Stmt* S) {
    DumpHeader(clava::STMT_DATA_NAMES[stmtNode], S);

    switch(stmtNode) {
        case clava::StmtNode::STMT:
            DumpStmtData(S); break;
        case clava::StmtNode::EXPR:
            DumpExprData(static_cast<const Expr *>(S)); break;
        default: throw std::invalid_argument("ClangDataDumper::dump: Case not implemented, '"+clava::STMT_DATA_NAMES[stmtNode]+"'");
    }
}


// STMTS


void clava::ClavaDataDumper::DumpStmtData(const Stmt *S) {

    clava::dumpSourceRange(Context, S->getLocStart(), S->getLocEnd());
    //void dumpSourceRange(std::string id, SourceLocation startLoc, SourceLocation endLoc);

}




// EXPRS


void clava::ClavaDataDumper::DumpExprData(const Expr *E) {
    DumpStmtData(E);
}