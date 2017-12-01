//
// Created by JoaoBispo on 20/01/2017.
//

#ifndef CLANGASTDUMPER_CLANGASTDUMPER_H
#define CLANGASTDUMPER_CLANGASTDUMPER_H

#include "ClangAstDumperConstants.h"

#include "clang/AST/TypeVisitor.h"
#include "clang/AST/StmtVisitor.h"
#include "clang/AST/DeclVisitor.h"

#include <set>
#include <string>

//#define DEBUG

using namespace clang;


class ClangAstDumper : public TypeVisitor<ClangAstDumper>,  public ConstStmtVisitor<ClangAstDumper>, public ConstDeclVisitor<ClangAstDumper> {

private:
    ASTContext *Context;
    int id;

    std::set<const void*> seenTypes;
    std::set<const Stmt*> seenStmts;
    std::set<const Decl*> seenDecls;

    std::set<const CXXCtorInitializer*> seenInits;

public:
    explicit ClangAstDumper(ASTContext *Context, int id);

    void VisitTypeTop(const Type *T);
    void VisitTypeTop(const QualType& T);
    void VisitStmtTop(const Stmt *Node);
    void VisitDeclTop(const Decl *Node);


    /*
     * TYPES
     */
    void VisitType(const Type *T);
    void VisitPointerType(const PointerType *T);
    void VisitTemplateSpecializationType(const TemplateSpecializationType *T);
    void VisitFunctionProtoType(const FunctionProtoType *T);
    void VisitTypedefType(const TypedefType *T);
    void VisitElaboratedType(const ElaboratedType *T);
    void VisitLValueReferenceType(const LValueReferenceType *T);

    /*
     * STMTS
     */
    void VisitStmt(const Stmt *T);
    void VisitDeclStmt(const DeclStmt *Node);
    void VisitCXXForRangeStmt(const CXXForRangeStmt *Node);
    void VisitCompoundStmt(const CompoundStmt *Node);
    void VisitForStmt(const ForStmt *Node);

    void VisitUnaryExprOrTypeTraitExpr(const UnaryExprOrTypeTraitExpr *Node);
    void VisitCXXConstructExpr(const CXXConstructExpr *Node);
    void VisitDeclRefExpr(const DeclRefExpr *Node);
    void VisitOffsetOfExpr(const OffsetOfExpr *Node);
    void VisitCXXDependentScopeMemberExpr(const CXXDependentScopeMemberExpr *Node);
    void VisitOverloadExpr(const OverloadExpr *Node, bool isTopCall = true);
    void VisitUnresolvedLookupExpr(const UnresolvedLookupExpr *Node);
    void VisitUnresolvedMemberExpr(const UnresolvedMemberExpr *Node);



    /*
     * DELCS
     */
    void VisitDecl(const Decl *D);
    void VisitVarDecl(const VarDecl *D);
    void VisitCXXMethodDecl(const CXXMethodDecl *D);
    void VisitCXXRecordDecl(const CXXRecordDecl *D);
    void VisitCXXConstructorDecl(const CXXConstructorDecl *D);
    void VisitObjCImplementationDecl(const ObjCImplementationDecl *D);
    void VisitTemplateDecl(const TemplateDecl *D);
    void VisitTemplateTypeParmDecl(const TemplateTypeParmDecl *D);
    void VisitNamespaceAliasDecl(const NamespaceAliasDecl *D);
    void VisitFieldDecl(const FieldDecl *D);
    void VisitParmVarDecl(const ParmVarDecl *D);
    void VisitTypedefDecl(const TypedefDecl *D);



    // Utility methods
    std::string loc2str(SourceLocation locStart, SourceLocation locEnd);
    std::string getId(const void* addr);
    std::string toBoolString(int value);


    /* Utility methods for DECLS */
    void dumpNumberTemplateParameters(const Decl *D, const TemplateParameterList *TPL);

    // Private functions
private:
    // These methods return true if the node had been already visited

    bool dumpType(const Type* typeAddr);
    bool dumpType(const QualType& type);
    bool dumpStmt(const Stmt* stmtAddr);
    bool dumpDecl(const Decl* declAddr);

    void dumpSourceRange(std::string id, SourceLocation startLoc, SourceLocation endLoc);

    /* EXTRA */
    void dumpCXXCtorInitializer(const CXXCtorInitializer *Init);


    void log(const char* name, const void* addr);


};


#endif //CLANGASTDUMPER_CLANGASTDUMPER_H
