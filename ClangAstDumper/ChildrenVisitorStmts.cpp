//
// Created by JoaoBispo on 01/04/2018.
//

#include "ClangAstDumper.h"
#include "ClavaConstants.h"
#include "ClangNodes.h"

#include <string>

const std::map<const std::string, clava::StmtNode > ClangAstDumper::STMT_CHILDREN_MAP = {
        {"DeclStmt", clava::StmtNode::DECL_STMT},
        {"IfStmt", clava::StmtNode::IF_STMT},
};

const std::map<const std::string, clava::StmtNode > ClangAstDumper::EXPR_CHILDREN_MAP = {
        {"InitListExpr", clava::StmtNode::INIT_LIST_EXPR},
        {"DeclRefExpr", clava::StmtNode::DECL_REF_EXPR},
        {"OffsetOfExpr", clava::StmtNode::OFFSET_OF_EXPR},
        {"UnresolvedLookupExpr", clava::StmtNode::UNRESOLVED_LOOKUP_EXPR},
        {"CallExpr", clava::StmtNode::CALL_EXPR},
        {"CXXMemberCallExpr", clava::StmtNode::CXX_MEMBER_CALL_EXPR},
        {"CXXOperatorCallExpr", clava::StmtNode::CALL_EXPR},
        {"UserDefinedLiteral", clava::StmtNode::CALL_EXPR},
        {"CXXTypeidExpr", clava::StmtNode::CXX_TYPEID_EXPR},
        {"CStyleCastExpr", clava::StmtNode::EXPLICIT_CAST_EXPR},
        {"CXXConstCastExpr", clava::StmtNode::EXPLICIT_CAST_EXPR},
        {"CXXReinterpretCastExpr", clava::StmtNode::EXPLICIT_CAST_EXPR},
        {"CXXStaticCastExpr", clava::StmtNode::EXPLICIT_CAST_EXPR},
        {"OpaqueValueExpr", clava::StmtNode::OPAQUE_VALUE_EXPR},
};

void ClangAstDumper::visitChildren(const Stmt* S) {
    // Get classname
    const std::string classname = clava::getClassName(S);

    // Get corresponding StmtNode
    clava::StmtNode stmtNode = STMT_CHILDREN_MAP.count(classname) == 1 ? STMT_CHILDREN_MAP.find(classname)->second :
                               clava::StmtNode::STMT;

    visitChildren(stmtNode, S);
}

void ClangAstDumper::visitChildren(const Expr* E) {
    // Get classname
    const std::string classname = clava::getClassName(E);

    // Get corresponding ExprNode
    clava::StmtNode exprNode = EXPR_CHILDREN_MAP.count(classname) == 1 ? EXPR_CHILDREN_MAP.find(classname)->second :
                               clava::StmtNode::EXPR;

    visitChildren(exprNode, E);
}

void ClangAstDumper::visitChildren(clava::StmtNode stmtNode, const Stmt* S) {

    std::vector<std::string> visitedChildren;

    switch(stmtNode) {
        case clava::StmtNode::STMT:
            VisitStmtChildren(S, visitedChildren); break;
//        case clava::StmtNode::COMPOUND_STMT:
//            VisitCompoundStmtChildren(static_cast<const CompoundStmt *>(S), visitedChildren); break;
        case clava::StmtNode::DECL_STMT:
            VisitDeclStmtChildren(static_cast<const DeclStmt *>(S), visitedChildren); break;
        case clava::StmtNode::IF_STMT:
            VisitIfStmtChildren(static_cast<const IfStmt *>(S), visitedChildren); break;


        case clava::StmtNode::EXPR:
            VisitExprChildren(static_cast<const Expr *>(S), visitedChildren); break;
        case clava::StmtNode::INIT_LIST_EXPR:
            VisitInitListExprChildren(static_cast<const InitListExpr *>(S), visitedChildren); break;
        case clava::StmtNode::DECL_REF_EXPR:
            VisitDeclRefExprChildren(static_cast<const DeclRefExpr *>(S), visitedChildren); break;
//        case clava::StmtNode::CAST_EXPR:
//            VisitCastExprChildren(static_cast<const CastExpr *>(S), visitedChildren); break;
        case clava::StmtNode::OFFSET_OF_EXPR:
            VisitOffsetOfExprChildren(static_cast<const OffsetOfExpr *>(S), visitedChildren); break;
        case clava::StmtNode::MATERIALIZE_TEMPORARY_EXPR:
            VisitMaterializeTemporaryExprChildren(static_cast<const MaterializeTemporaryExpr *>(S), visitedChildren); break;
        case clava::StmtNode::UNRESOLVED_LOOKUP_EXPR:
            VisitUnresolvedLookupExprChildren(static_cast<const UnresolvedLookupExpr *>(S), visitedChildren); break;
        case clava::StmtNode::CALL_EXPR:
            VisitCallExprChildren(static_cast<const CallExpr *>(S), visitedChildren); break;
        case clava::StmtNode::CXX_MEMBER_CALL_EXPR:
            VisitCXXMemberCallExprChildren(static_cast<const CXXMemberCallExpr *>(S), visitedChildren); break;
        case clava::StmtNode::CXX_TYPEID_EXPR:
            VisitCXXTypeidExprChildren(static_cast<const CXXTypeidExpr *>(S), visitedChildren); break;
        case clava::StmtNode::EXPLICIT_CAST_EXPR:
            VisitExplicitCastExprChildren(static_cast<const ExplicitCastExpr *>(S), visitedChildren); break;
        case clava::StmtNode::OPAQUE_VALUE_EXPR:
            VisitOpaqueValueExprChildren(static_cast<const OpaqueValueExpr *>(S), visitedChildren); break;
        case clava::StmtNode::UNARY_EXPR_OR_TYPE_TRAIT_EXPR:
            VisitUnaryExprOrTypeTraitExprChildren(static_cast<const UnaryExprOrTypeTraitExpr *>(S), visitedChildren); break;


        default: throw std::invalid_argument("ChildrenVisitorStmts::visitChildren(StmtNode): Case not implemented, '"+clava::getName(stmtNode)+"'");

    }

    dumpVisitedChildren(S, visitedChildren);
}

void ClangAstDumper::VisitStmtChildren(const Stmt *S, std::vector<std::string> &children) {
    // Visit Stmt children
    for (const Stmt *SubStmt : S->children()) {
        if (SubStmt) {
            addChild(SubStmt, children);
            //VisitStmtTop(SubStmt);
            //children.push_back(clava::getId(SubStmt, id));
        }
    }
}


/*
void ClangAstDumper::VisitCompoundStmtChildren(const CompoundStmt *S, std::vector<std::string> &children) {

    // Visit sub-statements
    for (auto &Arg : S->body()) {
        VisitStmtTop(Arg);
        children.push_back(getId(Arg));
    }
}
 */


void ClangAstDumper::VisitDeclStmtChildren(const DeclStmt *S, std::vector<std::string> &children) {
    // Do not visit sub-statements, only decls
    //VisitStmtChildren(S, children);

    // Visit decls
    for (DeclStmt::const_decl_iterator I = S->decl_begin(), E = S->decl_end(); I != E; ++I) {
        addChild(*I, children);
        //VisitDeclTop(*I);
        //children.push_back(clava::getId(*I, id));
    }

}

void ClangAstDumper::VisitIfStmtChildren(const IfStmt *S, std::vector<std::string> &children) {
    // Do not visit sub-statements automatically, visit the if stmts in a controlled manner
    //VisitStmtChildren(S, children);

    //addChild(S->getConditionVariableDeclStmt(), children);
    addChild(S->getConditionVariable(), children);
    addChild(S->getCond(), children);
    addChild(S->getThen(), children);
    addChild(S->getElse(), children);
}




void ClangAstDumper::VisitExprChildren(const Expr *E, std::vector<std::string> &children) {
    // Visit sub-statements
    VisitStmtChildren(E, children);

    // Visit type
    VisitTypeTop(E->getType());
    dumpTopLevelType(E->getType());
}

void ClangAstDumper::VisitInitListExprChildren(const InitListExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    // Visit array filler
    VisitStmtTop(E->getArrayFiller());

    // Visit field
    //VisitDeclTop(E->getInitializedFieldInUnion());
}


/*
void ClangAstDumper::VisitCastExprChildren(const CastExpr *S, std::vector<std::string> &children) {

    // Sub-expression
    auto subExprAsWritten = S->getSubExprAsWritten();
    VisitStmtTop(subExprAsWritten);
    children.push_back(getId(subExprAsWritten));
}
 */


void ClangAstDumper::VisitDeclRefExprChildren(const DeclRefExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    VisitDeclTop(E->getDecl());
    // Visit decl
    //VisitDeclTop(E->getDecl());
    //children.push_back(clava::getId(E->getDecl(), id));

    // Visit found decl as child
    //VisitDeclTop(E->getFoundDecl());
    //children.push_back(clava::getId(E->getFoundDecl(), id));
}

void ClangAstDumper::VisitOffsetOfExprChildren(const OffsetOfExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    // Visit type
    VisitTypeTop(E->getTypeSourceInfo()->getType().getTypePtr());
}

void ClangAstDumper::VisitMaterializeTemporaryExprChildren(const MaterializeTemporaryExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    // Visit type
    VisitDeclTop(E->getExtendingDecl());
}

void ClangAstDumper::VisitUnresolvedLookupExprChildren(const UnresolvedLookupExpr *E, std::vector<std::string> &children) {
    // Hierarchy - direct parent is OverloadExpr
    VisitExprChildren(E, children);

    // Visit decls
    auto currentDecl = E->decls_begin(), declsEnd = E->decls_end();
    for (; currentDecl != declsEnd; ++currentDecl) {
        VisitDeclTop(*currentDecl);
    }

}

void ClangAstDumper::VisitCallExprChildren(const CallExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    VisitDeclTop(E->getDirectCallee());

}

void ClangAstDumper::VisitCXXMemberCallExprChildren(const CXXMemberCallExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitCallExprChildren(E, children);

    VisitDeclTop(E->getMethodDecl());

}

void ClangAstDumper::VisitCXXTypeidExprChildren(const CXXTypeidExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    if(E->isTypeOperand()) {
        VisitTypeTop(E->getTypeOperand(*Context));
    } else {
        VisitStmtTop(E->getExprOperand());
    }

}

void ClangAstDumper::VisitExplicitCastExprChildren(const ExplicitCastExpr *E, std::vector<std::string> &children) {
    // Hierarchy - direct parent is CastExpr
    VisitExprChildren(E, children);


    VisitTypeTop(E->getTypeAsWritten());
}

void ClangAstDumper::VisitOpaqueValueExprChildren(const OpaqueValueExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    addChild(E->getSourceExpr(), children);

}

void ClangAstDumper::VisitUnaryExprOrTypeTraitExprChildren(const UnaryExprOrTypeTraitExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    if(E->isArgumentType()) {
        VisitTypeTop(E->getArgumentType());
    }


}