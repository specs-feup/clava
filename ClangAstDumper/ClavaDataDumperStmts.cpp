//
// Created by JoaoBispo on 30/03/2018.
//

#include "ClavaDataDumper.h"
#include "ClangNodes.h"

#include "clang/Lex/Lexer.h"

#include <map>

const std::map<const std::string, clava::StmtNode > clava::STMT_DATA_MAP = {

};

const std::map<const std::string, clava::StmtNode > clava::EXPR_DATA_MAP = {
        {"CharacterLiteral", clava::StmtNode::CHARACTER_LITERAL},
        {"IntegerLiteral", clava::StmtNode::INTEGER_LITERAL},
        {"CastExpr", clava::StmtNode::CAST_EXPR},
        {"ImplicitCastExpr", clava::StmtNode::CAST_EXPR},
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
        case clava::StmtNode ::INTEGER_LITERAL:
            DumpIntegerLiteralData(static_cast<const IntegerLiteral *>(S)); break;
        case clava::StmtNode ::FLOATING_LITERAL:
            DumpFloatingLiteralData(static_cast<const FloatingLiteral *>(S)); break;

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

void clava::ClavaDataDumper::DumpLiteralData(const Expr *E) {
    DumpExprData(E);

    // Source literal
    clava::dump(clava::getSource(Context, E->getSourceRange()));
}


void clava::ClavaDataDumper::DumpCharacterLiteralData(const CharacterLiteral *E) {
    DumpLiteralData(E);
//    DumpExprData(E);

//    clava::dump(clava::getSource(Context, E->getSourceRange()));
    clava::dump(E->getValue());
    clava::dump(E->getKind());
}

void clava::ClavaDataDumper::DumpIntegerLiteralData(const IntegerLiteral *E) {
    DumpLiteralData(E);
//    DumpExprData(E);

//    clava::dump(clava::getSource(Context, E->getSourceRange()));

    bool isSigned = E->getType()->isSignedIntegerType();

//    const std::string source = getSource(E);
//    clava::dump(source);
    clava::dump(E->getValue().toString(10, isSigned));

    //std::string source = clava::getSource(Context, E->getSourceRange());
    //clava::dump(source);

    /*
    const SourceManager &sm = Context->getSourceManager();
SourceRange sourceRange = E->getSourceRange();

    SourceLocation begin = sourceRange.getBegin();
    SourceLocation end = sourceRange.getEnd();
    if (begin.isMacroID()) {
        llvm::errs() << "Begin is macro:" << begin.printToString(sm) << "\n";
        begin = sm.getSpellingLoc(begin);
        llvm::errs() << "Begin spelling loc:" << begin.printToString(sm) << "\n";
    } else {
        llvm::errs() << "Begin is not macro:" << begin.printToString(sm) << "\n";
    }

    if (end.isMacroID()) {
        llvm::errs() << "End is macro:" << end.printToString(sm) << "\n";
        end = sm.getSpellingLoc(end);
        llvm::errs() << "End spelling loc:" << end.printToString(sm) << "\n";
    } else {
        llvm::errs() << "End is not macro:" << begin.printToString(sm) << "\n";
    }
*/
}

void clava::ClavaDataDumper::DumpFloatingLiteralData(const FloatingLiteral *E) {
    DumpLiteralData(E);
    //DumpExprData(E);

    //clava::dump(clava::getSource(Context, E->getSourceRange()));

    //clava::dump(getSource(E));

    /*
    const SourceManager &sm = Context->getSourceManager();
    clang::SourceLocation b(E->getLocStart()), _e(E->getLocEnd());

    clang::SourceLocation e(clang::Lexer::getLocForEndOfToken(_e, 0, sm, Context->getLangOpts()));
    std::string s = std::string(sm.getCharacterData(b),
                       sm.getCharacterData(e)-sm.getCharacterData(b));
*/
//    Context->getSourceManager().getP

//    Context->getSourceManager().sou
//    clava::dumpSourceRange()
    //E->getValue()->
    //bool isSigned = E->getType()->isSignedIntegerType();
    //clava::dump(E->getValue().toString(10, isSigned));
}

