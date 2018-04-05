//
// Created by JoaoBispo on 30/03/2018.
//

#include "ClavaDataDumper.h"
#include "ClangNodes.h"

#include <map>

const std::map<const std::string, clava::StmtNode > clava::STMT_DATA_MAP = {

};

const std::map<const std::string, clava::StmtNode > clava::EXPR_DATA_MAP = {
        {"CharacterLiteral", clava::StmtNode::CHARACTER_LITERAL},
        {"CastExpr", clava::StmtNode::CAST_EXPR}
};


void clava::ClavaDataDumper::dump(const Stmt* S) {

    // Get classname
    const std::string classname = clava::getClassName(S);

    // Get corresponding DeclNode
    StmtNode stmtNode = STMT_DATA_MAP.count(classname) == 1 ? STMT_DATA_MAP.find(classname)->second : StmtNode::STMT;

    dump(stmtNode, S);
}

void clava::ClavaDataDumper::dump(const Expr* E) {

    // Get classname
    const std::string classname = clava::getClassName(E);

    // Get corresponding DeclNode
    StmtNode exprNode = EXPR_DATA_MAP.count(classname) == 1 ? EXPR_DATA_MAP.find(classname)->second : StmtNode::EXPR;

    dump(exprNode, E);
}

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

