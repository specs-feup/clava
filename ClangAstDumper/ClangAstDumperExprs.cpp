//
// Created by JoaoBispo on 31/03/2018.
//

#include "ClangAstDumper.h"
#include "ClangAstDumperConstants.h"
#include "ClangNodes.h"

#include "clang/AST/AST.h"

#include <iostream>
#include <sstream>

using namespace clang;

// EXPR

void ClangAstDumper::VisitExpr(const Expr *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(Node);
}


void ClangAstDumper::VisitCXXConstructExpr(const CXXConstructExpr *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(static_cast<const Expr*>(Node));
#ifdef OLD_OUTPUT
//    llvm::errs() << "DUMPING CXX CONST: " << getId(Node) << "\n";

    const Type* constructorType = Node->getConstructor()->getType().getTypePtrOrNull();
    if(constructorType != nullptr) {
        llvm::errs() << "CONSTRUCTOR_TYPE\n";
        llvm::errs() << clava::getId(static_cast<const Expr*>(Node), id) << "->" << clava::getId(constructorType, id) << "\n";
        //llvm::errs() << Node << "_" << id << "->" << constructorType << "_" << id << "\n";
        //VisitTypeTop(Node->getConstructor()->getType().getTypePtr());
        VisitTypeTop(constructorType);
    } else {
        llvm::errs() << "CXXConstructExpr " << Node->getConstructor()->getDeclName().getAsString() << " has null type\n";
    }
#endif
}

void ClangAstDumper::VisitUnaryExprOrTypeTraitExpr(const UnaryExprOrTypeTraitExpr *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(static_cast<const Expr*>(Node));
#ifdef OLD_OUTPUT
    // If argument type, dump mapping between node and type
    if (Node->isArgumentType()) {
        // Dump type
        VisitTypeTop(Node->getArgumentType().getTypePtr());

        llvm::errs() << "<UnaryExprOrTypeTraitExpr ArgType>\n";
        // First address is id of UnaryExprOrTypeTraitExpr, second is id of type of argument
        llvm::errs() << Node << "_" << id << "->" << Node->getArgumentType().getTypePtr() << "_" << id << "\n";
    }

    llvm::errs() << "<UnaryExprOrTypeTraitExpr Literal Code>\n";
    llvm::errs() << Node << "_" << id << "\n";
    llvm::errs() << clava::getSource(Context, Node->getSourceRange()) << "\n";
#endif
}


void ClangAstDumper::VisitDeclRefExpr(const DeclRefExpr *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(static_cast<const Expr*>(Node));
}


void ClangAstDumper::VisitOffsetOfExpr(const OffsetOfExpr *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(static_cast<const Expr*>(Node));
#ifdef OLD_OUTPUT
    // Dump offset set
    const Type* offsetType = Node->getTypeSourceInfo()->getType().getTypePtr();
    dumpType(offsetType);

    llvm::errs() << DUMP_OFFSET_OF_INFO<< "\n";

    // Node id
    llvm::errs() << clava::getId(static_cast<const Expr*>(Node), id) << "\n";



    // Offset type
    llvm::errs() << clava::getId(offsetType, id) << "\n";

    // Number of expressions
    llvm::errs() << Node->getNumExpressions() << "\n";

    // Number of components
    int numComp = Node->getNumComponents();
    llvm::errs() << numComp << "\n";

    for(int i=0; i<numComp; i++) {

        llvm::errs() << Node->getComponent(i).getKind() << "\n";

        switch(Node->getComponent(i).getKind()) {
            case OffsetOfNode::Array:
                llvm::errs() << Node->getComponent(i).getArrayExprIndex() << "\n";
                break;
            case OffsetOfNode::Field:
                llvm::errs() << Node->getComponent(i).getFieldName()->getName() << "\n";
                break;
            case OffsetOfNode::Identifier:
                //llvm::errs() << "OFFSET FIELD NAME:" << Node->getComponent(i).getFieldName()->getName() << "\n";
                //break;
            case OffsetOfNode::Base:
                //llvm::errs() << "OFFSET BASE TYPE START\n";
                //Node->getComponent(i).getBase()->getType().dump();
                //llvm::errs() << "OFFSET BASE TYPE END\n";
                //break;
            default:
                // Do nothing, cases not yet implemented
                break;
        }




    }
#endif
}

void ClangAstDumper::VisitCXXDependentScopeMemberExpr(const CXXDependentScopeMemberExpr *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(static_cast<const Expr*>(Node));
#ifdef OLD_OUTPUT
    llvm::errs() << CXX_MEMBER_EXPR_INFO << "\n";

    // Node id
    llvm::errs() << clava::getId(static_cast<const Expr*>(Node), id) << "\n";

    // Is arrow
    llvm::errs() << Node->isArrow() << "\n";

    // Member name
    llvm::errs() << Node->getMember().getAsString() << "\n";
#endif
}

void ClangAstDumper::VisitOverloadExpr(const OverloadExpr *Node) {
    if (dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(static_cast<const Expr*>(Node));
#ifdef OLD_OUTPUT
    if(Node->getQualifier() != nullptr) {
        // Can use the stream processor of decl ref expression qualifiers
        llvm::errs() << "DECL_REF_EXPR QUALIFIER BEGIN\n";
        llvm::errs() << clava::getId(static_cast<const Expr*>(Node), id) << "\n";
        Node->getQualifier()->dump();
        llvm::errs() << "\nDECL_REF_EXPR QUALIFIER END\n";
    }
#endif
}

void ClangAstDumper::VisitUnresolvedLookupExpr(const UnresolvedLookupExpr *Node) {

    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(static_cast<const Expr*>(Node));
#ifdef OLD_OUTPUT
    // Call parent in hierarchy
    //VisitOverloadExpr(Node, false);

    // Duplicated from ClangAstDumper::VisitOverloadExpr
    if(Node->getQualifier() != nullptr) {
        // Can use the stream processor of decl ref expression qualifiers
        llvm::errs() << "DECL_REF_EXPR QUALIFIER BEGIN\n";
        llvm::errs() << clava::getId(static_cast<const Expr*>(Node), id) << "\n";
        Node->getQualifier()->dump();
        llvm::errs() << "\nDECL_REF_EXPR QUALIFIER END\n";
    }
#endif
}

void ClangAstDumper::VisitUnresolvedMemberExpr(const UnresolvedMemberExpr *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(static_cast<const Expr*>(Node));
#ifdef OLD_OUTPUT
    // Call parent in hierarchy
    //VisitOverloadExpr(Node, false);

    // Duplicated from ClangAstDumper::VisitOverloadExpr
    if(Node->getQualifier() != nullptr) {
        // Can use the stream processor of decl ref expression qualifiers
        llvm::errs() << "DECL_REF_EXPR QUALIFIER BEGIN\n";
        llvm::errs() << clava::getId(static_cast<const Expr*>(Node), id) << "\n";
        Node->getQualifier()->dump();
        llvm::errs() << "\nDECL_REF_EXPR QUALIFIER END\n";
    }
#endif
}



void ClangAstDumper::VisitLambdaExpr(const LambdaExpr *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(static_cast<const Expr*>(Node));
#ifdef OLD_OUTPUT
//    llvm::errs() << "LAMBDA EXPR: " << getId(Node) << "\n";
//    llvm::errs() << "LAMBDA EXPR LAMBDA CLASS: " << getId(Node->getLambdaClass()) << "\n";

    // Visit Decl
    VisitDeclTop(Node->getLambdaClass());

    // Visit children
    // children() of LambdaExpr is not const?
    for (const Stmt *SubStmt : const_cast<LambdaExpr*>(Node)->children()) {
        if (SubStmt) {
            VisitStmtTop(SubStmt);
        }
    }

    // Dump lambda data
    llvm::errs() << LAMBDA_EXPR_DATA << "\n";
    llvm::errs() << clava::getId(static_cast<const Expr*>(Node), id) << "\n";
    llvm::errs() << Node->isGenericLambda() << "\n";
    llvm::errs() << Node->isMutable() << "\n";
    llvm::errs() << Node->hasExplicitParameters() << "\n";
    llvm::errs() << Node->hasExplicitResultType() << "\n";
    llvm::errs() << Node->getCaptureDefault() <<"\n";
    // Number of captures
    llvm::errs() << Node->capture_size() <<"\n";
    // Info about each capture
    for(auto lambdaCapture : Node->captures()) {
        // Capture kind
        llvm::errs()  << lambdaCapture.getCaptureKind ()  << "\n";
    }

#endif
}

void ClangAstDumper::VisitSizeOfPackExpr(const SizeOfPackExpr *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(static_cast<const Expr*>(Node));
#ifdef OLD_OUTPUT
    // Visit pack
    VisitDeclTop(Node->getPack());

    // Map expr to pack?
#endif
}

void ClangAstDumper::VisitCXXUnresolvedConstructExpr(const CXXUnresolvedConstructExpr *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(static_cast<const Expr*>(Node));
#ifdef OLD_OUTPUT
    // Visit type as written
    VisitTypeTop(Node->getTypeAsWritten().getTypePtr());

    // Map current address to address of 'type as written'
    llvm::errs() << TYPE_AS_WRITTEN << "\n";
    llvm::errs() << clava::getId(static_cast<const Expr*>(Node), id) << "\n";
    llvm::errs() << clava::getId(Node->getTypeAsWritten().getTypePtr(), id) << "\n";
#endif
}

void ClangAstDumper::VisitCXXTypeidExpr(const CXXTypeidExpr *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(static_cast<const Expr*>(Node));
#ifdef OLD_OUTPUT
    bool isTypeOperand = Node->isTypeOperand();
    llvm::errs() << TYPEID_DATA << "\n";
    llvm::errs() << clava::getId(static_cast<const Expr*>(Node), id) << "\n";

    llvm::errs() << isTypeOperand << "\n"; // True if is a type operand, false if it is an expression

    // Address of type/expr
    if(isTypeOperand) {
        QualType typeOperand = Node->getTypeOperand(*Context);
        // QUALTYPE EXP
        //llvm::errs() << getId(Node->getTypeOperand(*Context).getTypePtr()) << "\n";
        //llvm::errs() << getId(&typeOperand) << "\n";
        llvm::errs() << clava::getId(typeOperand, id) << "\n";
        VisitTypeTop(typeOperand);
    } else {
        llvm::errs() << clava::getId(Node->getExprOperand(), id) << "\n";
        VisitStmtTop(Node->getExprOperand());
    }
#endif
}

void ClangAstDumper::VisitInitListExpr(const InitListExpr *Node) {
    if(dumpStmt(Node)) {
        return;
    }

    visitChildrenAndData(static_cast<const Expr*>(Node));
#ifdef OLD_OUTPUT
    llvm::errs() << INIT_LIST_EXPR_INFO << "\n";
    llvm::errs() << clava::getId(static_cast<const Expr*>(Node), id) << "\n";

    // InitListExpr has method isExplicit(), but is not const
    bool isExplicit = Node->getLBraceLoc().isValid() && Node->getRBraceLoc().isValid();
    llvm::errs() << isExplicit << "\n";
#endif

}


