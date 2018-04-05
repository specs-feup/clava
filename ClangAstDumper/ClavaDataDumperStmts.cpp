//
// Created by JoaoBispo on 30/03/2018.
//

#include "ClavaDataDumper.h"
#include "ClangNodes.h"

void clava::ClavaDataDumper::dump(clava::StmtNode stmtNode, const Stmt* S) {
    DumpHeader(getDataName(stmtNode), S);

    switch(stmtNode) {
        case clava::StmtNode::STMT:
            DumpStmtData(S); break;
        case clava::StmtNode::EXPR:
            DumpExprData(static_cast<const Expr *>(S)); break;
        case clava::StmtNode::CAST_EXPR:
            DumpCastExprData(static_cast<const CastExpr *>(S)); break;
        case clava::StmtNode ::CHARACTER_LITERAL:
            DumpCharacterLiteralData(static_cast<const CharacterLiteral *>(S)); break;
        default: throw std::invalid_argument("ClangDataDumper::dump(StmtNode): Case not implemented, '"+getName(stmtNode)+"'");
    }
}


// STMTS


void clava::ClavaDataDumper::DumpStmtData(const Stmt *S) {

    clava::dumpSourceRange(Context, S->getLocStart(), S->getLocEnd());

}




// EXPRS


void clava::ClavaDataDumper::DumpExprData(const Expr *E) {
    DumpStmtData(E);

    clava::dump(E->getType(), id);
    clava::dump(E->getValueKind());
    clava::dump(E->getObjectKind());
}

void clava::ClavaDataDumper::DumpCastExprData(const CastExpr *E) {
    DumpExprData(E);

    clava::dump(E->getCastKindName());
}

void clava::ClavaDataDumper::DumpCharacterLiteralData(const CharacterLiteral *E) {
    DumpExprData(E);

    clava::dump(E->getValue());
    clava::dump(E->getKind());
}

