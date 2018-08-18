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
        {"CXXFunctionalCastExpr", clava::StmtNode::CAST_EXPR},
        {"CStyleCastExpr", clava::StmtNode::EXPLICIT_CAST_EXPR},
        {"CXXConstCastExpr", clava::StmtNode::CXX_NAMED_CAST_EXPR},
        {"CXXReinterpretCastExpr", clava::StmtNode::CXX_NAMED_CAST_EXPR},
        {"CXXStaticCastExpr", clava::StmtNode::CXX_NAMED_CAST_EXPR},

        {"CXXBoolLiteralExpr", clava::StmtNode::CXX_BOOL_LITERAL_EXPR},
        {"CompoundLiteralExpr", clava::StmtNode::COMPOUND_LITERAL_EXPR},
        {"InitListExpr", clava::StmtNode::INIT_LIST_EXPR},
        {"StringLiteral", clava::StmtNode::STRING_LITERAL},
        {"DeclRefExpr", clava::StmtNode::DECL_REF_EXPR},
        {"UnresolvedLookupExpr", clava::StmtNode::UNRESOLVED_LOOKUP_EXPR},
        {"UnresolvedMemberExpr", clava::StmtNode::OVERLOAD_EXPR},
        {"CXXConstructExpr", clava::StmtNode::CXX_CONSTRUCT_EXPR},
        {"MemberExpr", clava::StmtNode::MEMBER_EXPR},
        {"MaterializeTemporaryExpr", clava::StmtNode::MATERIALIZE_TEMPORARY_EXPR},
        {"BinaryOperator", clava::StmtNode::BINARY_OPERATOR},
        {"UnaryOperator", clava::StmtNode::UNARY_OPERATOR},
        {"CompoundAssignOperator", clava::StmtNode::BINARY_OPERATOR},
        {"CallExpr", clava::StmtNode::CALL_EXPR},
        {"CXXMemberCallExpr", clava::StmtNode::CXX_MEMBER_CALL_EXPR},
        {"CXXOperatorCallExpr", clava::StmtNode::CALL_EXPR},
        {"UserDefinedLiteral", clava::StmtNode::CALL_EXPR},
        {"CXXTypeidExpr", clava::StmtNode::CXX_TYPEID_EXPR},
        {"CXXDependentScopeMemberExpr", clava::StmtNode::CXX_DEPENDENT_SCOPE_MEMBER_EXPR},
        {"UnaryExprOrTypeTraitExpr", clava::StmtNode::UNARY_EXPR_OR_TYPE_TRAIT_EXPR},
        {"CXXNewExpr", clava::StmtNode::CXX_NEW_EXPR},
        {"CXXDeleteExpr", clava::StmtNode::CXX_DELETE_EXPR},
        {"OffsetOfExpr", clava::StmtNode::OFFSET_OF_EXPR},
        {"LambdaExpr", clava::StmtNode::LAMBDA_EXPR},
        {"PredefinedExpr", clava::StmtNode::PREDEFINED_EXPR},
        {"SizeOfPackExpr", clava::StmtNode::SIZE_OF_PACK_EXPR},

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
        case clava::StmtNode ::OVERLOAD_EXPR:
            DumpOverloadExprData(static_cast<const OverloadExpr *>(S)); break;
        case clava::StmtNode ::UNRESOLVED_LOOKUP_EXPR:
            DumpUnresolvedLookupExprData(static_cast<const UnresolvedLookupExpr *>(S)); break;
        case clava::StmtNode ::CXX_CONSTRUCT_EXPR:
            DumpCXXConstructExprData(static_cast<const CXXConstructExpr *>(S)); break;
        case clava::StmtNode ::MEMBER_EXPR:
            DumpMemberExprData(static_cast<const MemberExpr *>(S)); break;
        case clava::StmtNode ::MATERIALIZE_TEMPORARY_EXPR:
            DumpMaterializeTemporaryExprData(static_cast<const MaterializeTemporaryExpr *>(S)); break;
        case clava::StmtNode ::BINARY_OPERATOR:
            DumpBinaryOperatorData(static_cast<const BinaryOperator *>(S)); break;
        case clava::StmtNode ::CALL_EXPR:
            DumpCallExprData(static_cast<const CallExpr *>(S)); break;
        case clava::StmtNode ::CXX_MEMBER_CALL_EXPR:
            DumpCXXMemberCallExprData(static_cast<const CXXMemberCallExpr *>(S)); break;
        case clava::StmtNode ::CXX_TYPEID_EXPR:
            DumpCXXTypeidExprData(static_cast<const CXXTypeidExpr *>(S)); break;
        case clava::StmtNode ::EXPLICIT_CAST_EXPR:
            DumpExplicitCastExprData(static_cast<const ExplicitCastExpr *>(S)); break;
        case clava::StmtNode ::CXX_NAMED_CAST_EXPR:
            DumpCXXNamedCastExprData(static_cast<const CXXNamedCastExpr *>(S)); break;
        case clava::StmtNode ::CXX_DEPENDENT_SCOPE_MEMBER_EXPR:
            DumpCXXDependentScopeMemberExprData(static_cast<const CXXDependentScopeMemberExpr *>(S)); break;
        case clava::StmtNode ::UNARY_OPERATOR:
            DumpUnaryOperatorData(static_cast<const UnaryOperator *>(S)); break;
        case clava::StmtNode ::UNARY_EXPR_OR_TYPE_TRAIT_EXPR:
            DumpUnaryExprOrTypeTraitExprData(static_cast<const UnaryExprOrTypeTraitExpr *>(S)); break;
        case clava::StmtNode ::CXX_NEW_EXPR:
            DumpCXXNewExprData(static_cast<const CXXNewExpr *>(S)); break;
        case clava::StmtNode ::CXX_DELETE_EXPR:
            DumpCXXDeleteExprData(static_cast<const CXXDeleteExpr *>(S)); break;
        case clava::StmtNode ::OFFSET_OF_EXPR:
            DumpOffsetOfExprData(static_cast<const OffsetOfExpr *>(S)); break;
        case clava::StmtNode ::LAMBDA_EXPR:
            DumpLambdaExprData(static_cast<const LambdaExpr *>(S)); break;
        case clava::StmtNode ::PREDEFINED_EXPR:
            DumpPredefinedExprData(static_cast<const PredefinedExpr *>(S)); break;
        case clava::StmtNode ::SIZE_OF_PACK_EXPR:
            DumpSizeOfPackExprData(static_cast<const SizeOfPackExpr *>(S)); break;


            //        case clava::StmtNode ::COMPOUND_ASSIGN_OPERATOR:
//            DumpCompoundAssignOperatorData(static_cast<const CompoundAssignOperator *>(S)); break;

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
    clava::dump(E->isDefaultArgument());
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
    clava::dump(E->getQualifier(), Context);
    /*
    if(E->getQualifier() != nullptr) {
        std::string qualifierStr;
        llvm::raw_string_ostream qualifierStream(qualifierStr);
        E->getQualifier()->print(qualifierStream, Context->getPrintingPolicy());
        clava::dump(qualifierStream.str());
    } else {
        clava::dump("");
    }
    */

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

    /*
    std::string declNameStr;
    llvm::raw_string_ostream declNameStream(declNameStr);
    declNameStream << E->getDecl()->getDeclName();
    clava::dump(declNameStream.str());
    */

    clava::dump(clava::getId(E->getDecl(), id));
}

void clava::ClavaDataDumper::DumpOverloadExprData(const OverloadExpr *E) {
    DumpExprData(E);

    // Dump qualifier
    clava::dump(E->getQualifier(), Context);
}

void clava::ClavaDataDumper::DumpUnresolvedLookupExprData(const UnresolvedLookupExpr *E) {
    DumpOverloadExprData(E);

    clava::dump(E->requiresADL());
    clava::dump([&E](llvm::raw_string_ostream& stream){stream << E->getName();});
/*
    if(E->requiresADL()) {
        clava::dump([&E](llvm::raw_string_ostream& stream){stream << E->getName();});
    } else {
        clava::dump("");
    }
*/
    // Number of decls
    clava::dump(E->getNumDecls());
    auto currentDecl = E->decls_begin(), declsEnd = E->decls_end();
    for (; currentDecl != declsEnd; ++currentDecl) {
        clava::dump(clava::getId(*currentDecl, id));
    }


}

void clava::ClavaDataDumper::DumpCXXConstructExprData(const CXXConstructExpr *E) {
    DumpExprData(E);

    // Dump qualifier
    clava::dump(E->isElidable());
    //clava::dump(E->isDefaultArgument());
    clava::dump(E->requiresZeroInitialization());
    clava::dump(E->isListInitialization());
    clava::dump(E->isStdInitListInitialization());
    clava::dump(clava::CONSTRUCTION_KIND[E->getConstructionKind()]);
}

void clava::ClavaDataDumper::DumpMemberExprData(const MemberExpr *E) {
    DumpExprData(E);

    clava::dump(E->isArrow());
    clava::dump(E->getMemberNameInfo().getAsString());
    //clava::dump(clava::getId(E->getMemberDecl(), id));
}

void clava::ClavaDataDumper::DumpMaterializeTemporaryExprData(const MaterializeTemporaryExpr *E) {
    DumpExprData(E);

    clava::dump(getId(E->getExtendingDecl(), id));
}


void clava::ClavaDataDumper::DumpBinaryOperatorData(const BinaryOperator *E) {
    DumpExprData(E);

    clava::dump(clava::BINARY_OPERATOR_KIND[E->getOpcode()]);
}

void clava::ClavaDataDumper::DumpCallExprData(const CallExpr *E) {
    DumpExprData(E);

    clava::dump(clava::getId(E->getDirectCallee(), id));
}

void clava::ClavaDataDumper::DumpCXXMemberCallExprData(const CXXMemberCallExpr *E) {
    DumpCallExprData(E);

    clava::dump(clava::getId(E->getMethodDecl(), id));
}

//void clava::ClavaDataDumper::DumpCompoundAssignOperatorData(const CompoundAssignOperator *E) {
//    DumpBinaryOperatorData(E);

//    clava::dump();
//}

void clava::ClavaDataDumper::DumpCXXTypeidExprData(const CXXTypeidExpr *E) {
    DumpExprData(E);

    clava::dump(E->isTypeOperand());
    if(E->isTypeOperand()) {
        clava::dump(clava::getId(E->getTypeOperand(*Context), id));
    } else {
        clava::dump(getId(E->getExprOperand(), id));
    }

}

void clava::ClavaDataDumper::DumpExplicitCastExprData(const ExplicitCastExpr *E) {
    DumpCastExprData(E);

    clava::dump(clava::getId(E->getTypeAsWritten(), id));
}

void clava::ClavaDataDumper::DumpCXXNamedCastExprData(const CXXNamedCastExpr *E) {
    DumpExplicitCastExprData(E);

    clava::dump(E->getCastName());
 }

void clava::ClavaDataDumper::DumpCXXDependentScopeMemberExprData(const CXXDependentScopeMemberExpr *E) {
    DumpExprData(E);

    clava::dump(E->isArrow());
    clava::dump(E->getMemberNameInfo().getAsString());

 }

void clava::ClavaDataDumper::DumpUnaryOperatorData(const UnaryOperator *E) {
    DumpExprData(E);

    clava::dump(clava::UNARY_OPERATOR_KIND[E->getOpcode()]);
    if (E->isPostfix()) {
        clava::dump("POSTFIX");
    } else {
        clava::dump("PREFIX");
    }


 }

void clava::ClavaDataDumper::DumpUnaryExprOrTypeTraitExprData(const UnaryExprOrTypeTraitExpr *E) {
    DumpExprData(E);

    clava::dump(clava::UETT_KIND[E->getKind()]);
    clava::dump(E->isArgumentType());
    if(E->isArgumentType()) {
        clava::dump(getId(E->getArgumentType(), id));
    } else {
        clava::dump(getId((const Type *)nullptr, id));
    }

    clava::dump(clava::getSource(Context, E->getSourceRange()));
    //llvm::errs() << "UNARY " << clava::getId(E, id) << " is argument type: " << E->isArgumentType() << "\n";
 }

void clava::ClavaDataDumper::DumpCXXNewExprData(const CXXNewExpr *E) {
    DumpExprData(E);

    clava::dump(E->isGlobalNew());
    clava::dump(E->isArray());
    clava::dump(E->hasInitializer());
    clava::dump(clava::NEW_INIT_STYLE[E->getInitializationStyle()]);
    clava::dump(clava::getId(E->getInitializer(), id));
    clava::dump(clava::getId(E->getConstructExpr(), id));
    clava::dump(clava::getId(E->getArraySize(), id));
    clava::dump(clava::getId(E->getOperatorNew(), id));

 }

void clava::ClavaDataDumper::DumpCXXDeleteExprData(const CXXDeleteExpr *E) {
    DumpExprData(E);

    clava::dump(E->isGlobalDelete());
    clava::dump(E->isArrayForm());
    clava::dump(E->isArrayFormAsWritten());
    //clava::dump(clava::getId(E->getArgument(), id));

 }

void clava::ClavaDataDumper::DumpOffsetOfExprData(const OffsetOfExpr *E) {
    DumpExprData(E);

    clava::dump(clava::getId(E->getTypeSourceInfo()->getType(), id));
    clava::dump(E->getNumComponents());
    for(int i = 0; i < E->getNumComponents(); i++) {
        // Dump each component
        OffsetOfNode node = E->getComponent(i);
        clava::dump(clava::OFFSET_OF_NODE_KIND[node.getKind()]);
        switch (node.getKind()) {
            case OffsetOfNode::Kind::Array:
                clava::dump(clava::getId(E->getIndexExpr(node.getArrayExprIndex()), id));
                break;
            case OffsetOfNode::Kind::Field:
                clava::dump(node.getFieldName()->getName().str());
                break;
            default:
                throw std::invalid_argument("ClangDataDumper::DumpOffsetOfExprData(): Case not implemented, '" +
                                                    clava::OFFSET_OF_NODE_KIND[node.getKind()] + "'");
        }
    }
 }

void clava::ClavaDataDumper::DumpLambdaExprData(const LambdaExpr *E) {
    DumpExprData(E);

    clava::dump(E->isGenericLambda());
    clava::dump(E->isMutable());
    clava::dump(E->hasExplicitParameters());
    clava::dump(E->hasExplicitResultType());
    clava::dump(clava::LAMBDA_CAPTURE_DEFAULT[E->getCaptureDefault()]);

    clava::dump(clava::getId(E->getLambdaClass(), id));

    clava::dump(E->capture_size());
    for(auto capture : E->captures()) {
        clava::dump(clava::LAMBDA_CAPTURE_KIND[capture.getCaptureKind()]);
    }


    /*
    for(int i=0; i<E->capture_size(); i++) {
        E->getC
    }
     */
 }

 void clava::ClavaDataDumper::DumpPredefinedExprData(const PredefinedExpr *E) {
    DumpExprData(E);

    clava::dump(clava::PREDEFINED_ID_TYPE[E->getIdentType()]);
 }

 void clava::ClavaDataDumper::DumpSizeOfPackExprData(const SizeOfPackExpr *E) {
    DumpExprData(E);

    clava::dump(E->isPartiallySubstituted());
    clava::dump(clava::getId(E->getPack(), id));

    if(E->isPartiallySubstituted()) {
        // Template args
        clava::dump(E->getPartialArguments().size());
        for(auto partialArg : E->getPartialArguments()) {
            clava::dump(partialArg, id);
        }
    } else {
        clava::dump(0);
    }


 }