//
// Created by JoaoBispo on 01/04/2018.
//

#include "ClangAstDumper.h"
#include "ClavaConstants.h"
#include "ClangNodes.h"
#include "ClangEnums.h"

#include <string>


const std::map<const std::string, clava::StmtNode > ClangAstDumper::STMT_CHILDREN_MAP = {
        {"DeclStmt", clava::StmtNode::DECL_STMT},
        {"IfStmt", clava::StmtNode::IF_STMT},
        {"ForStmt", clava::StmtNode::FOR_STMT},
        {"WhileStmt", clava::StmtNode::WHILE_STMT},
        {"DoStmt", clava::StmtNode::DO_STMT},
        {"CXXForRangeStmt", clava::StmtNode::CXX_FOR_RANGE_STMT},
        {"CXXCatchStmt", clava::StmtNode::CXX_CATCH_STMT},
        {"CXXTryStmt", clava::StmtNode::CXX_TRY_STMT},
        {"CaseStmt", clava::StmtNode::CASE_STMT},
        {"DefaultStmt", clava::StmtNode::DEFAULT_STMT},
        {"GotoStmt", clava::StmtNode::GOTO_STMT},
        {"AttributedStmt", clava::StmtNode::ATTRIBUTED_STMT},
        {"CapturedStmt", clava::StmtNode::CAPTURED_STMT},
};

const std::map<const std::string, clava::StmtNode > ClangAstDumper::EXPR_CHILDREN_MAP = {
        {"InitListExpr", clava::StmtNode::INIT_LIST_EXPR},
        {"DeclRefExpr", clava::StmtNode::DECL_REF_EXPR},
        {"DependentScopeDeclRefExpr", clava::StmtNode::DEPENDENT_SCOPE_DECL_REF_EXPR},
        {"OffsetOfExpr", clava::StmtNode::OFFSET_OF_EXPR},
        {"MemberExpr", clava::StmtNode::MEMBER_EXPR},
        {"MaterializeTemporaryExpr", clava::StmtNode::MATERIALIZE_TEMPORARY_EXPR},
        {"UnresolvedLookupExpr", clava::StmtNode::OVERLOAD_EXPR},
        {"UnresolvedMemberExpr", clava::StmtNode::OVERLOAD_EXPR},
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
        {"CXXNewExpr", clava::StmtNode::CXX_NEW_EXPR},
        {"CXXDeleteExpr", clava::StmtNode::CXX_DELETE_EXPR},
        {"LambdaExpr", clava::StmtNode::LAMBDA_EXPR},
        {"SizeOfPackExpr", clava::StmtNode::SIZE_OF_PACK_EXPR},
        {"UnaryExprOrTypeTraitExpr", clava::StmtNode::UNARY_EXPR_OR_TYPE_TRAIT_EXPR},
        {"DesignatedInitExpr", clava::StmtNode::DESIGNATED_INIT_EXPR},
        {"CXXConstructExpr", clava::StmtNode::CXX_CONSTRUCT_EXPR},
        {"CXXTemporaryObjectExpr", clava::StmtNode::CXX_TEMPORARY_OBJECT_EXPR},
        {"CXXDependentScopeMemberExpr", clava::StmtNode::CXX_DEPENDENT_SCOPE_MEMBER_EXPR},
        //{"CXXNoexceptExpr", clava::StmtNode::CXX_NOEXCEPT_EXPR},

        //{"SubstNonTypeTemplateParmExpr", clava::StmtNode::SUBST_NON_TYPE_TEMPLATE_PARM_EXPR},
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
        case clava::StmtNode::FOR_STMT:
            VisitForStmtChildren(static_cast<const ForStmt *>(S), visitedChildren); break;
        case clava::StmtNode::WHILE_STMT:
            VisitWhileStmtChildren(static_cast<const WhileStmt *>(S), visitedChildren); break;
        case clava::StmtNode::DO_STMT:
            VisitDoStmtChildren(static_cast<const DoStmt *>(S), visitedChildren); break;
       case clava::StmtNode::CXX_FOR_RANGE_STMT:
            VisitCXXForRangeStmtChildren(static_cast<const CXXForRangeStmt *>(S), visitedChildren); break;
       case clava::StmtNode::CXX_CATCH_STMT:
            VisitCXXCatchStmtChildren(static_cast<const CXXCatchStmt *>(S), visitedChildren); break;
       case clava::StmtNode::CXX_TRY_STMT:
            VisitCXXTryStmtChildren(static_cast<const CXXTryStmt *>(S), visitedChildren); break;
       case clava::StmtNode::CASE_STMT:
            VisitCaseStmtChildren(static_cast<const CaseStmt *>(S), visitedChildren); break;
       case clava::StmtNode::DEFAULT_STMT:
            VisitDefaultStmtChildren(static_cast<const DefaultStmt *>(S), visitedChildren); break;
       case clava::StmtNode::GOTO_STMT:
            VisitGotoStmtChildren(static_cast<const GotoStmt *>(S), visitedChildren); break;
       case clava::StmtNode::ATTRIBUTED_STMT:
            VisitAttributedStmtChildren(static_cast<const AttributedStmt *>(S), visitedChildren); break;
       case clava::StmtNode::CAPTURED_STMT:
            VisitCapturedStmtChildren(static_cast<const CapturedStmt *>(S), visitedChildren); break;




        case clava::StmtNode::EXPR:
            VisitExprChildren(static_cast<const Expr *>(S), visitedChildren); break;
        case clava::StmtNode::INIT_LIST_EXPR:
            VisitInitListExprChildren(static_cast<const InitListExpr *>(S), visitedChildren); break;
        case clava::StmtNode::DECL_REF_EXPR:
            VisitDeclRefExprChildren(static_cast<const DeclRefExpr *>(S), visitedChildren); break;
        case clava::StmtNode::DEPENDENT_SCOPE_DECL_REF_EXPR:
            VisitDependentScopeDeclRefExprChildren(static_cast<const DependentScopeDeclRefExpr *>(S), visitedChildren); break;
//        case clava::StmtNode::CAST_EXPR:
//            VisitCastExprChildren(static_cast<const CastExpr *>(S), visitedChildren); break;
        case clava::StmtNode::OFFSET_OF_EXPR:
            VisitOffsetOfExprChildren(static_cast<const OffsetOfExpr *>(S), visitedChildren); break;
        case clava::StmtNode::MEMBER_EXPR:
            VisitMemberExprChildren(static_cast<const MemberExpr *>(S), visitedChildren); break;
        case clava::StmtNode::MATERIALIZE_TEMPORARY_EXPR:
            VisitMaterializeTemporaryExprChildren(static_cast<const MaterializeTemporaryExpr *>(S), visitedChildren); break;
//        case clava::StmtNode::UNRESOLVED_LOOKUP_EXPR:
//            VisitUnresolvedLookupExprChildren(static_cast<const UnresolvedLookupExpr *>(S), visitedChildren); break;
        case clava::StmtNode::OVERLOAD_EXPR:
            VisitOverloadExprChildren(static_cast<const OverloadExpr *>(S), visitedChildren); break;
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
        case clava::StmtNode::CXX_NEW_EXPR:
            VisitCXXNewExprChildren(static_cast<const CXXNewExpr *>(S), visitedChildren); break;
        case clava::StmtNode::CXX_DELETE_EXPR:
            VisitCXXDeleteExprChildren(static_cast<const CXXDeleteExpr *>(S), visitedChildren); break;
        case clava::StmtNode::LAMBDA_EXPR:
            VisitLambdaExprChildren(static_cast<const LambdaExpr *>(S), visitedChildren); break;
        case clava::StmtNode::SIZE_OF_PACK_EXPR:
            VisitSizeOfPackExprChildren(static_cast<const SizeOfPackExpr *>(S), visitedChildren); break;
        case clava::StmtNode::DESIGNATED_INIT_EXPR:
            VisitDesignatedInitExprChildren(static_cast<const DesignatedInitExpr *>(S), visitedChildren); break;
        case clava::StmtNode::CXX_CONSTRUCT_EXPR:
            VisitCXXConstructExprChildren(static_cast<const CXXConstructExpr *>(S), visitedChildren); break;
        case clava::StmtNode::CXX_TEMPORARY_OBJECT_EXPR:
            VisitCXXTemporaryObjectExprChildren(static_cast<const CXXTemporaryObjectExpr *>(S), visitedChildren); break;
        case clava::StmtNode::CXX_DEPENDENT_SCOPE_MEMBER_EXPR:
            VisitCXXDependentScopeMemberExprChildren(static_cast<const CXXDependentScopeMemberExpr *>(S), visitedChildren); break;
//        case clava::StmtNode::CXX_NOEXCEPT_EXPR:
//            VisitCXXNoexceptExprChildren(static_cast<const CXXNoexceptExpr *>(S), visitedChildren); break;


            //        case clava::StmtNode::SUBST_NON_TYPE_TEMPLATE_PARM_EXPR:
//            VisitSubstNonTypeTemplateParmExprChildren(static_cast<const SubstNonTypeTemplateParmExpr *>(S), visitedChildren); break;


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

void ClangAstDumper::VisitForStmtChildren(const ForStmt *S, std::vector<std::string> &children) {
    // Do not visit sub-statements automatically, visit the if stmts in a controlled manner
    //VisitStmtChildren(S, children);

    //addChild(S->getConditionVariableDeclStmt(), children);
    addChild(S->getInit(), children);
    addChild(S->getCond(), children);
    addChild(S->getInc(), children);
    addChild(S->getBody(), children);
}

void ClangAstDumper::VisitWhileStmtChildren(const WhileStmt *S, std::vector<std::string> &children) {
    // Do not visit sub-statements automatically, visit the if stmts in a controlled manner
    //VisitStmtChildren(S, children);

    addChild(S->getConditionVariable(), children);
    addChild(S->getCond(), children);
    addChild(S->getBody(), children);
}

void ClangAstDumper::VisitDoStmtChildren(const DoStmt *S, std::vector<std::string> &children) {
    // Do not visit sub-statements automatically, visit the if stmts in a controlled manner
    //VisitStmtChildren(S, children);

    addChild(S->getBody(), children);
    addChild(S->getCond(), children);
}

void ClangAstDumper::VisitCXXForRangeStmtChildren(const CXXForRangeStmt *S, std::vector<std::string> &children) {
    // Do not visit sub-statements automatically, visit the if stmts in a controlled manner
    //VisitStmtChildren(S, children);

    addChild(S->getRangeStmt(), children);
    //addChild(S->getBeginEndStmt(), children); LLVM3.8
    addChild(S->getBeginStmt(), children);
    addChild(S->getEndStmt(), children);
    addChild(S->getCond(), children);
    addChild(S->getInc(), children);
    addChild(S->getLoopVarStmt(), children);
    addChild(S->getBody(), children);
}

void ClangAstDumper::VisitCXXCatchStmtChildren(const CXXCatchStmt *S, std::vector<std::string> &children) {
    // Do not visit sub-statements automatically, visit the if stmts in a controlled manner
    //VisitStmtChildren(S, children);

    addChild(S->getExceptionDecl(), children);
    addChild(S->getHandlerBlock(), children);
}

void ClangAstDumper::VisitCXXTryStmtChildren(const CXXTryStmt *S, std::vector<std::string> &children) {
    // Do not visit sub-statements automatically, visit the if stmts in a controlled manner
    //VisitStmtChildren(S, children);

    addChild(S->getTryBlock(), children);
    for(unsigned i=0; i<S->getNumHandlers();i++) {
        addChild(S->getHandler(i), children);
    }

    //llvm::errs() << "TRY STMT: " << S << "\n";
    //llvm::errs() << "TRY BLOCK: " << S->getTryBlock() << "\n";
}

void ClangAstDumper::VisitCaseStmtChildren(const CaseStmt *S, std::vector<std::string> &children) {
    // Do not visit sub-statements automatically, visit the if stmts in a controlled manner
    //VisitStmtChildren(S, children);

    addChild(S->getLHS(), children);
    addChild(S->getRHS(), children);
    addChild(S->getSubStmt(), children);
}

void ClangAstDumper::VisitDefaultStmtChildren(const DefaultStmt *S, std::vector<std::string> &children) {
    // Do not visit sub-statements automatically, visit the if stmts in a controlled manner
    //VisitStmtChildren(S, children);

    addChild(S->getSubStmt(), children);
}


void ClangAstDumper::VisitGotoStmtChildren(const GotoStmt *S, std::vector<std::string> &children) {
    // Do not visit sub-statements automatically, visit the if stmts in a controlled manner
    //VisitStmtChildren(S, children);

    VisitDeclTop(S->getLabel());
}


void ClangAstDumper::VisitAttributedStmtChildren(const AttributedStmt *S, std::vector<std::string> &children) {
    // Do not visit sub-statements automatically, visit the if stmts in a controlled manner
    //VisitStmtChildren(S, children);

    // Visit attributes
    for (auto attr : S->getAttrs()) {
        VisitAttrTop(attr);
        dumpTopLevelAttr(attr);
    }

    addChild(S->getSubStmt(), children);
}

void ClangAstDumper::VisitCapturedStmtChildren(const CapturedStmt *S, std::vector<std::string> &children) {
    // Do not visit sub-statements automatically, visit the if stmts in a controlled manner
    //VisitStmtChildren(S, children);

    addChild(S->getCapturedStmt(), children);
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

    // Visit syntatic form
    VisitStmtTop(E->getSyntacticForm());
    VisitStmtTop(E->getSemanticForm());
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

    auto templateArgs = E->getTemplateArgs();
    for (unsigned i = 0; i < E->getNumTemplateArgs(); ++i) {
        auto templateArg = templateArgs + i;
        VisitTemplateArgument(templateArg->getArgument());
    }

    // Visit decl
    //VisitDeclTop(E->getDecl());
    //children.push_back(clava::getId(E->getDecl(), id));

    // Visit found decl as child
    //VisitDeclTop(E->getFoundDecl());
    //children.push_back(clava::getId(E->getFoundDecl(), id));
}

void ClangAstDumper::VisitDependentScopeDeclRefExprChildren(const DependentScopeDeclRefExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    auto templateArgs = E->getTemplateArgs();
    for (unsigned i = 0; i < E->getNumTemplateArgs(); ++i) {
        auto templateArg = templateArgs + i;
        VisitTemplateArgument(templateArg->getArgument());
    }

}


void ClangAstDumper::VisitOffsetOfExprChildren(const OffsetOfExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    // Visit type
    VisitTypeTop(E->getTypeSourceInfo()->getType().getTypePtr());

    for(unsigned i = 0; i < E->getNumComponents(); i++) {
        // Dump each component
        OffsetOfNode node = E->getComponent(i);
        switch (node.getKind()) {
            case OffsetOfNode::Kind::Array:
                VisitStmtTop(E->getIndexExpr(node.getArrayExprIndex()));
                break;
            case OffsetOfNode::Kind::Field:
                // Nothing to visit
                break;
            default:
                throw std::invalid_argument("ClangDataDumper::DumpOffsetOfExprData(): Case not implemented, '" +
                                            clava::OFFSET_OF_NODE_KIND[node.getKind()] + "'");
        }
    }
}

void ClangAstDumper::VisitMemberExprChildren(const MemberExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    // Visit decls
    VisitDeclTop(E->getMemberDecl());
    VisitDeclTop(E->getFoundDecl().getDecl());
}

void ClangAstDumper::VisitMaterializeTemporaryExprChildren(const MaterializeTemporaryExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    // Visit type
    VisitDeclTop(E->getExtendingDecl());
}

//void ClangAstDumper::VisitUnresolvedLookupExprChildren(const UnresolvedLookupExpr *E, std::vector<std::string> &children) {
void ClangAstDumper::VisitOverloadExprChildren(const OverloadExpr *E, std::vector<std::string> &children) {
    // Hierarchy - direct parent is OverloadExpr
    VisitExprChildren(E, children);

    // Visit decls
    auto currentDecl = E->decls_begin(), declsEnd = E->decls_end();
    for (; currentDecl != declsEnd; ++currentDecl) {
        VisitDeclTop(*currentDecl);
    }

    // Visit template arguments
    auto templateArgs = E->getTemplateArgs();
    for (unsigned i = 0; i < E->getNumTemplateArgs(); ++i) {
        auto templateArg = templateArgs + i;
        VisitTemplateArgument(templateArg->getArgument());
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
        //llvm::errs() << "UNARY ARG TYPE: " << clava::getId(E->getArgumentType(), id) << "\n";
    }

}

void ClangAstDumper::VisitCXXNewExprChildren(const CXXNewExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    VisitStmtTop(E->getInitializer());
    VisitStmtTop(E->getArraySize());
    VisitDeclTop(E->getOperatorNew());

}

void ClangAstDumper::VisitCXXDeleteExprChildren(const CXXDeleteExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    addChild(E->getArgument(), children);
    //VisitStmtTop(E->getArgument());
}

void ClangAstDumper::VisitLambdaExprChildren(const LambdaExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    VisitDeclTop(E->getLambdaClass());
}

void ClangAstDumper::VisitSizeOfPackExprChildren(const SizeOfPackExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    VisitDeclTop(E->getPack());

    if(E->isPartiallySubstituted()) {
        for (auto templateArg : E->getPartialArguments()) {
            VisitTemplateArgument(templateArg);
        }
    }
}

void ClangAstDumper::VisitDesignatedInitExprChildren(const DesignatedInitExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    //for(unsigned int i=0; i<E->getNumSubExprs(); i++) {
    //    addChild(E->getSubExpr(i), children);
    //}

}

void ClangAstDumper::VisitCXXConstructExprChildren(const CXXConstructExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);


    VisitDeclTop(E->getConstructor());
}

void ClangAstDumper::VisitCXXTemporaryObjectExprChildren(const CXXTemporaryObjectExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitCXXConstructExprChildren(E, children);
}

void ClangAstDumper::VisitCXXDependentScopeMemberExprChildren(const CXXDependentScopeMemberExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

    auto templateArgs = E->getTemplateArgs();
    for (unsigned i = 0; i < E->getNumTemplateArgs(); ++i) {
        auto templateArg = templateArgs + i;
        VisitTemplateArgument(templateArg->getArgument());
    }
}



/*
void ClangAstDumper::VisitCXXNoexceptExprChildren(const CXXNoexceptExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);

//    VisitExpr(E->getOperand());
}
 */



/*
void ClangAstDumper::VisitSubstNonTypeTemplateParmExprChildren(const SubstNonTypeTemplateParmExpr *E, std::vector<std::string> &children) {
    // Hierarchy
    VisitExprChildren(E, children);


    addChild(E->getReplacement(), children);
    //VisitStmtTop(E->getArgument());
}
*/