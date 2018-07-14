//
// Created by JoaoBispo on 30/03/2018.
//

#include "ClavaDataDumper.h"
#include "ClangNodes.h"
#include "ClangEnums.h"

#include "clang/Lex/Lexer.h"

#include <map>

const std::map<const std::string, clava::StmtNode > clava::STMT_DATA_MAP = {

};

const std::map<const std::string, clava::StmtNode > clava::EXPR_DATA_MAP = {
        {"CharacterLiteral", clava::StmtNode::CHARACTER_LITERAL},
        {"IntegerLiteral", clava::StmtNode::INTEGER_LITERAL},
        {"FloatingLiteral", clava::StmtNode::FLOATING_LITERAL},
        //{"FloatingLiteral", clava::StmtNode::CXX_BOOL_LITERAL_EXPR},
        {"CastExpr", clava::StmtNode::CAST_EXPR},
        //{"CXXFunctionalCastExpr", clava::StmtNode::CXX_FUNCTIONAL_CAST_EXPR},
        {"CXXBoolLiteralExpr", clava::StmtNode::CXX_BOOL_LITERAL_EXPR},
        {"CompoundLiteralExpr", clava::StmtNode::COMPOUND_LITERAL_EXPR},
        {"InitListExpr", clava::StmtNode::INIT_LIST_EXPR},
        {"StringLiteral", clava::StmtNode::STRING_LITERAL},
        {"DeclRefExpr", clava::StmtNode::DECL_REF_EXPR},
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
    // Dump header
    llvm::errs() << getDataName(stmtNode) << "\n";
    llvm::errs() << clava::getId(S, id) << "\n";
    //DumpHeader(getDataName(stmtNode), S);

    switch(stmtNode) {
        case clava::StmtNode::STMT:
            DumpStmtData(S); break;
        case clava::StmtNode::EXPR:
            DumpExprData(static_cast<const Expr *>(S)); break;
        case clava::StmtNode::CAST_EXPR:
            DumpCastExprData(static_cast<const CastExpr *>(S)); break;
//        case clava::StmtNode::CXX_FUNCTIONAL_CAST_EXPR:
//            DumpCXXFunctionalCastExprData(static_cast<const CXXFunctionalCastExpr *>(S)); break;
        case clava::StmtNode ::CHARACTER_LITERAL:
            DumpCharacterLiteralData(static_cast<const CharacterLiteral *>(S)); break;
        case clava::StmtNode ::INTEGER_LITERAL:
            DumpIntegerLiteralData(static_cast<const IntegerLiteral *>(S)); break;
        case clava::StmtNode ::FLOATING_LITERAL:
            DumpFloatingLiteralData(static_cast<const FloatingLiteral *>(S)); break;
        case clava::StmtNode ::STRING_LITERAL:
            DumpStringLiteralData(static_cast<const StringLiteral *>(S)); break;
        case clava::StmtNode ::CXX_BOOL_LITERAL_EXPR:
            DumpCXXBoolLiteralExprData(static_cast<const CXXBoolLiteralExpr *>(S)); break;
        case clava::StmtNode ::COMPOUND_LITERAL_EXPR:
            DumpCompoundLiteralExprData(static_cast<const CompoundLiteralExpr *>(S)); break;
        case clava::StmtNode ::INIT_LIST_EXPR:
            DumpInitListExprData(static_cast<const InitListExpr *>(S)); break;
        case clava::StmtNode ::DECL_REF_EXPR:
            DumpDeclRefExprData(static_cast<const DeclRefExpr *>(S)); break;

        default: throw std::invalid_argument("ClangDataDumper::dump(StmtNode): Case not implemented, '"+getName(stmtNode)+"'");
    }
}


// STMTS


void clava::ClavaDataDumper::DumpStmtData(const Stmt *S) {

    // Original source range
    clava::dumpSourceInfo(Context, S->getLocStart(), S->getLocEnd());

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

    clava::dump(clava::CAST_KIND[E->getCastKind()]);
}

/*
void clava::ClavaDataDumper::DumpCXXFunctionalCastExprData(const CXXFunctionalCastExpr *E) {
    DumpCastExprData(E);

    clava::dump(clava::getId(E->get));
}
 */

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



    std::string text = Lexer::getSourceText(CharSourceRange::getTokenRange(begin, end), sm, LangOptions(), 0);
    if (text.size() > 0 && (text.at(text.size()-1) == ',')) //the text can be ""
        text = Lexer::getSourceText(CharSourceRange::getCharRange(begin, end), sm, LangOptions(), 0);

    llvm::errs() << "Source:" << text << "\n";
*/
}

void clava::ClavaDataDumper::DumpFloatingLiteralData(const FloatingLiteral *E) {
    DumpLiteralData(E);

    clava::dump(E->getValueAsApproximateDouble());
    /*
    llvm::errs() << "Source range:" << clava::getSource(Context, E->getSourceRange()) << "\n";
    llvm::errs() << "Source loc start/end:" << clava::getSource(Context, SourceRange(E->getLocStart(), E->getLocEnd())) << "\n";
    llvm::errs() << "Source expr:" << clava::getSource(Context, SourceRange(E->getLocStart(), E->getExprLoc())) << "\n";
    llvm::errs() << "Source location:" << clava::getSource(Context, SourceRange(E->getLocStart(), E->getLocation())) << "\n";
*/
}

void clava::ClavaDataDumper::DumpStringLiteralData(const StringLiteral *E) {
    DumpLiteralData(E);

    //E->getString() cannot be used when literal is not a single char wide
//    clava::dump(E->getString().str());
//    clava::dump("\n%CLAVA_SOURCE_END%");
}


void clava::ClavaDataDumper::DumpCXXBoolLiteralExprData(const CXXBoolLiteralExpr *E) {
    DumpLiteralData(E);

    clava::dump(E->getValue());
}


void clava::ClavaDataDumper::DumpCompoundLiteralExprData(const CompoundLiteralExpr *E) {
    DumpLiteralData(E);

    clava::dump(E->isFileScope());
}

void clava::ClavaDataDumper::DumpInitListExprData(const InitListExpr *E) {
    DumpExprData(E);
    //std::cout << "Hello\n";
    //std::cout << "INIT FIELD: " << E->getInitializedFieldInUnion() << "\n";
    clava::dump(clava::getId(E->getArrayFiller(), id));
    //clava::dump(clava::getId(E->getInitializedFieldInUnion(), id)); // Apparently not supported in old parser
    clava::dump(const_cast<InitListExpr*>(E)->isExplicit()); // isExplicit() could be const
    clava::dump(E->isStringLiteralInit()); // isExplicit() could be const
}

void clava::ClavaDataDumper::DumpDeclRefExprData(const DeclRefExpr *E) {
    DumpExprData(E);

    // Dump qualifier
    if(E->getQualifier() != nullptr) {
        std::string qualifierStr;
        llvm::raw_string_ostream qualifierStream(qualifierStr);
        E->getQualifier()->print(qualifierStream, Context->getPrintingPolicy());
        clava::dump(qualifierStream.str());
    } else {
        clava::dump("");
    }

    // Dump template arguments
    if(E->hasExplicitTemplateArgs()) {
        // Number of template args
        clava::dump(E->getNumTemplateArgs());

        auto templateArgs = E->getTemplateArgs();
        for (unsigned i = 0; i < E->getNumTemplateArgs(); ++i) {
            auto templateArg = templateArgs + i;
            clava::dump(clava::getSource(Context, templateArg->getSourceRange()));
        }
    } else {
        clava::dump(0);
    }

    std::string declNameStr;
    llvm::raw_string_ostream declNameStream(declNameStr);
    declNameStream << E->getDecl()->getDeclName();
    clava::dump(declNameStream.str());

    clava::dump(clava::getId(E->getDecl(), id));
}

