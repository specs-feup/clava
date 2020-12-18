//
// Created by JoaoBispo on 20/01/2017.
//

#ifndef CLANGASTDUMPER_CLANGASTDUMPER_H
#define CLANGASTDUMPER_CLANGASTDUMPER_H

#include "ClavaDataDumper.h"

#include "clang/AST/TypeVisitor.h"
#include "clang/AST/StmtVisitor.h"
#include "clang/AST/DeclVisitor.h"
//#include "ClangAst.h"

#include <set>
#include <string>
#include <vector>

//#define DEBUG

using namespace clang;


class ClangAstDumper : public TypeVisitor<ClangAstDumper>,  public ConstStmtVisitor<ClangAstDumper>, public ConstDeclVisitor<ClangAstDumper> {

private:
    ASTContext *Context;
    //const ASTContext& constContext;
    int id;

    int systemHeaderThreshold = 2;
    int currentSystemHeaderLevel = 0;

    std::set<const void*> seenTypes;
    std::set<const Stmt*> seenStmts;
    std::set<const Decl*> seenDecls;
    std::set<const Attr*> seenAttrs;

    std::set<const CXXCtorInitializer*> seenInits;

    clava::ClavaDataDumper dataDumper;

    static const std::map<const std::string, clava::DeclNode > DECL_CHILDREN_MAP;
    static const std::map<const std::string, clava::StmtNode > STMT_CHILDREN_MAP;
    static const std::map<const std::string, clava::StmtNode > EXPR_CHILDREN_MAP;
    static const std::map<const std::string, clava::TypeNode > TYPE_CHILDREN_MAP;
    static const std::map<const std::string, clava::AttrNode > ATTR_CHILDREN_MAP;


public:
    explicit ClangAstDumper(ASTContext *Context, int id, int systemHeaderThreashold);
    //explicit ClangAstDumper(ASTContext *Context, const ASTContext& constContext, int id, int systemHeaderThreashold);

    void VisitTypeTop(const Type *T);
    void VisitTypeTop(const QualType& T);
    void VisitStmtTop(const Stmt *Node);
    void VisitDeclTop(const Decl *Node);
    void VisitAttrTop(const Attr *Node);


    /*
     * TYPES
     */
    void VisitType(const Type *T); // Should not be manually called, instead call VisitTypeTop()
/*
    // TODO: We should only need to call VisitType, children/data maps should handle all cases
    //void VisitBuiltinType(const BuiltinType *T);
    void VisitPointerType(const PointerType *T);
    //void VisitTemplateSpecializationType(const TemplateSpecializationType *T);
    void VisitFunctionProtoType(const FunctionProtoType *T);
    void VisitTypedefType(const TypedefType *T);
    void VisitElaboratedType(const ElaboratedType *T);
    void VisitLValueReferenceType(const LValueReferenceType *T);
    void VisitDependentSizedArrayType(const DependentSizedArrayType *T);
*/

        /*
         * STMTS
         */
    void VisitStmt(const Stmt *T); // Should not be manually called, instead call VisitStmtTop()
    /*
    //void VisitDeclStmt(const DeclStmt *Node);
    void VisitCXXForRangeStmt(const CXXForRangeStmt *Node);
    //void VisitCompoundStmt(const CompoundStmt *Node);
    void VisitForStmt(const ForStmt *Node);
    */

    /*
     * EXPRS
     */

    void VisitExpr(const Expr *Node);
    /*
    void VisitUnaryExprOrTypeTraitExpr(const UnaryExprOrTypeTraitExpr *Node);
    void VisitCXXConstructExpr(const CXXConstructExpr *Node);
    void VisitDeclRefExpr(const DeclRefExpr *Node);
    void VisitOffsetOfExpr(const OffsetOfExpr *Node);
    void VisitCXXDependentScopeMemberExpr(const CXXDependentScopeMemberExpr *Node);
    void VisitOverloadExpr(const OverloadExpr *Node);
    void VisitUnresolvedLookupExpr(const UnresolvedLookupExpr *Node);
    void VisitUnresolvedMemberExpr(const UnresolvedMemberExpr *Node);
    void VisitLambdaExpr(const LambdaExpr *Node);
    void VisitSizeOfPackExpr(const SizeOfPackExpr *Node);
    void VisitCXXUnresolvedConstructExpr(const CXXUnresolvedConstructExpr *Node);
    void VisitCXXTypeidExpr(const CXXTypeidExpr *Node);
    void VisitInitListExpr(const InitListExpr *Node);
    //void VisitCastExpr(const CastExpr *Node); // Works for ImplicitCastExpr, etc
    //void VisitCharacterLiteral(const CharacterLiteral *Node);

    //void VisitImplicitCastExpr(const ImplicitCastExpr *Node);
*/



    /*
     * DELCS
     */
    void VisitDecl(const Decl *D); // Should not be manually called, instead call VisitDeclTop()
/*
    //void VisitVarDecl(const VarDecl *D);
    //void VisitFunctionDecl(const FunctionDecl *D);
    //void VisitCXXMethodDecl(const CXXMethodDecl *D);
    void VisitCXXConstructorDecl(const CXXConstructorDecl *D);
    //void VisitCXXConversionDecl(const CXXConversionDecl *D);
    //void VisitCXXDestructorDecl(const CXXDestructorDecl *D);
    void VisitCXXRecordDecl(const CXXRecordDecl *D);
    void VisitObjCImplementationDecl(const ObjCImplementationDecl *D);
    void VisitTemplateDecl(const TemplateDecl *D);
    void VisitTemplateTypeParmDecl(const TemplateTypeParmDecl *D);
    void VisitNamespaceAliasDecl(const NamespaceAliasDecl *D);
    void VisitFieldDecl(const FieldDecl *D);
    void VisitParmVarDecl(const ParmVarDecl *D);
    void VisitTypedefDecl(const TypedefDecl *D);
*/
    /*
     * ATTR
     */
    void VisitAttr(const Attr* A);


  /*
   * EXTRA
   */


    /*
     * Utility methods
     */

    std::string loc2str(SourceLocation locStart, SourceLocation locEnd);
    //std::string getId(const void* addr);
    /*
    std::string getId(const Decl* addr);
    std::string getId(const Stmt* addr);
    std::string getId(const Expr* addr);
    std::string getId(const Type* addr);
    std::string getId(const Attr* addr);
    */

    std::string toBoolString(int value);
    const Type* getTypePtr(QualType T, std::string source);

    //void log(const char* name, const void* addr);
    void log(const Decl* D);
    void log(const Stmt* S);
    void log(const Type* T);
    void log(const Attr* A);



    /**
     * Adds a child.
     *
     * @param addr
     * @param id
     * @return
     */
    const void addChild(const Decl *addr, std::vector<std::string> &children);
    const void addChildren(DeclContext::decl_range declRange, std::vector<std::string> &children);
    //const void addChildren(DeclContext::decl_range declRange, std::vector<std::string> &children, bool ignoreClassDefinitions);
    const void addChild(const Stmt *addr, std::vector<std::string> &children);
    const void addChild(const Expr *addr, std::vector<std::string> &children);
    const void addChild(const Type *addr, std::vector<std::string> &children);
    const void addChild(const QualType &addr, std::vector<std::string> &children);
    const void addChild(const Attr *addr, std::vector<std::string> &children);



    // Private functions
private:

    void log(std::string name, const void* addr);

    // Children and data
    void visitChildrenAndData(const Decl *D);
    void visitChildrenAndData(const Stmt *S);
    void visitChildrenAndData(const Expr *E);
    void visitChildrenAndData(const Type *T);
    void visitChildrenAndData(const Attr *A);


    // Children visitors
    void dumpVisitedChildren(const void *pointer, std::vector<std::string> children);

    void visitChildren(const Decl* D);
    void visitChildren(const Stmt* S);
    void visitChildren(const Expr* E);
    void visitChildren(const Type* T);
    void visitChildren(const Attr* A);

    void visitChildren(clava::DeclNode declNode, const Decl* D);
    void visitChildren(clava::StmtNode stmtNode, const Stmt* S);
    void visitChildren(clava::TypeNode typeNode, const Type* T);
    void visitChildren(const QualType &T);
    void visitChildren(clava::AttrNode attrNode, const Attr* A);
    void emptyChildren(const void *pointer);

    // Children visitors for Decls
    void VisitDeclChildren(const Decl *D, std::vector<std::string> &children);
    void VisitNamedDeclChildren(const NamedDecl *D, std::vector<std::string> &children);
    void VisitTypeDeclChildren(const TypeDecl *D, std::vector<std::string> &children);
    void VisitTagDeclChildren(const TagDecl *D, std::vector<std::string> &children);
    void VisitEnumDeclChildren(const EnumDecl *D, std::vector<std::string> &children);
    void VisitRecordDeclChildren(const RecordDecl *D, std::vector<std::string> &children);
    void VisitCXXRecordDeclChildren(const CXXRecordDecl *D, std::vector<std::string> &children);
    void VisitValueDeclChildren(const ValueDecl *D, std::vector<std::string> &children);
    void VisitFieldDeclChildren(const FieldDecl *D, std::vector<std::string> &children);
    void VisitFunctionDeclChildren(const FunctionDecl *D, std::vector<std::string> &children);
    void VisitCXXMethodDeclChildren(const CXXMethodDecl *D, std::vector<std::string> &children);
    void VisitCXXConstructorDeclChildren(const CXXConstructorDecl *D, std::vector<std::string> &children);
    void VisitCXXConversionDeclChildren(const CXXConversionDecl *D, std::vector<std::string> &children);

    void VisitVarDeclChildren(const VarDecl *D, std::vector<std::string> &children);
    void VisitParmVarDeclChildren(const ParmVarDecl *D, std::vector<std::string> &children);

    void VisitTemplateDeclChildren(const TemplateDecl *D, std::vector<std::string> &children);
    void VisitTemplateTemplateParmDeclChildren(const TemplateTemplateParmDecl * D, std::vector<std::string> &children);
    void VisitTemplateTypeParmDeclChildren(const TemplateTypeParmDecl *D, std::vector<std::string> &children);
    void VisitEnumConstantDeclChildren(const EnumConstantDecl *D, std::vector<std::string> &children);
    void VisitTypedefNameDeclChildren(const TypedefNameDecl *D, std::vector<std::string> &children);
    void VisitUsingDirectiveDeclChildren(const UsingDirectiveDecl *D, std::vector<std::string> &children);
    void VisitNamespaceDeclChildren(const NamespaceDecl *D, std::vector<std::string> &children);
    void VisitFriendDeclChildren(const FriendDecl *D, std::vector<std::string> &children);
    void VisitNamespaceAliasDeclChildren(const NamespaceAliasDecl *D, std::vector<std::string> &children);
    void VisitLinkageSpecDeclChildren(const LinkageSpecDecl *D, std::vector<std::string> &children);
    void VisitStaticAssertDeclChildren(const StaticAssertDecl *D, std::vector<std::string> &children);
    void VisitNonTypeTemplateParmDeclChildren(const NonTypeTemplateParmDecl *D, std::vector<std::string> &children);


    // Children visitors for Stmts
    void VisitStmtChildren(const Stmt *S, std::vector<std::string> &children);
    //void VisitCompoundStmtChildren(const CompoundStmt *S, std::vector<std::string> &children);
    void VisitDeclStmtChildren(const DeclStmt *S, std::vector<std::string> &children);
    void VisitIfStmtChildren(const IfStmt *S, std::vector<std::string> &children);
    void VisitForStmtChildren(const ForStmt *S, std::vector<std::string> &children);
    void VisitWhileStmtChildren(const WhileStmt *S, std::vector<std::string> &children);
    void VisitDoStmtChildren(const DoStmt *S, std::vector<std::string> &children);
    void VisitCXXForRangeStmtChildren(const CXXForRangeStmt *S, std::vector<std::string> &children);
    void VisitCXXCatchStmtChildren(const CXXCatchStmt *S, std::vector<std::string> &children);
    void VisitCXXTryStmtChildren(const CXXTryStmt *S, std::vector<std::string> &children);
    void VisitCaseStmtChildren(const CaseStmt *S, std::vector<std::string> &children);
    void VisitDefaultStmtChildren(const DefaultStmt *S, std::vector<std::string> &children);
    void VisitGotoStmtChildren(const GotoStmt *S, std::vector<std::string> &children);
    void VisitAttributedStmtChildren(const AttributedStmt *S, std::vector<std::string> &children);
    void VisitCapturedStmtChildren(const CapturedStmt *S, std::vector<std::string> &children);

    // Children visitors for Exprs
    void VisitExprChildren(const Expr *S, std::vector<std::string> &children);
    void VisitInitListExprChildren(const InitListExpr *E, std::vector<std::string> &children);
    void VisitDeclRefExprChildren(const DeclRefExpr *E, std::vector<std::string> &children);
    void VisitDependentScopeDeclRefExprChildren(const DependentScopeDeclRefExpr *E, std::vector<std::string> &children);
    void VisitOffsetOfExprChildren(const OffsetOfExpr *E, std::vector<std::string> &children);
    void VisitMemberExprChildren(const MemberExpr *E, std::vector<std::string> &children);
    void VisitMaterializeTemporaryExprChildren(const MaterializeTemporaryExpr *E, std::vector<std::string> &children);
    //void VisitUnresolvedLookupExprChildren(const UnresolvedLookupExpr *E, std::vector<std::string> &children);
    void VisitOverloadExprChildren(const OverloadExpr *E, std::vector<std::string> &children);
    void VisitCallExprChildren(const CallExpr *E, std::vector<std::string> &children);
    void VisitCXXMemberCallExprChildren(const CXXMemberCallExpr *E, std::vector<std::string> &children);
    void VisitCXXTypeidExprChildren(const CXXTypeidExpr *E, std::vector<std::string> &children);
    void VisitExplicitCastExprChildren(const ExplicitCastExpr *E, std::vector<std::string> &children);
    void VisitOpaqueValueExprChildren(const OpaqueValueExpr *E, std::vector<std::string> &children);
    void VisitUnaryExprOrTypeTraitExprChildren(const UnaryExprOrTypeTraitExpr *E, std::vector<std::string> &children);
    void VisitCXXNewExprChildren(const CXXNewExpr *E, std::vector<std::string> &children);
    void VisitCXXDeleteExprChildren(const CXXDeleteExpr *E, std::vector<std::string> &children);
    void VisitLambdaExprChildren(const LambdaExpr *E, std::vector<std::string> &children);
    void VisitSizeOfPackExprChildren(const SizeOfPackExpr *E, std::vector<std::string> &children);
    void VisitDesignatedInitExprChildren(const DesignatedInitExpr *E, std::vector<std::string> &children);
    void VisitCXXConstructExprChildren(const CXXConstructExpr *E, std::vector<std::string> &children);
    void VisitCXXTemporaryObjectExprChildren(const CXXTemporaryObjectExpr *E, std::vector<std::string> &children);
    void VisitCXXDependentScopeMemberExprChildren(const CXXDependentScopeMemberExpr *E, std::vector<std::string> &children);

    //void VisitCXXNoexceptExprChildren(const CXXNoexceptExpr *E, std::vector<std::string> &children);



    //void VisitSubstNonTypeTemplateParmExprChildren(const SubstNonTypeTemplateParmExpr *E, std::vector<std::string> &children);


        //void VisitCastExprChildren(const CastExpr *S, std::vector<std::string> &children);


    // Children visitors for Attributes
    void VisitAlignedAttrChildren(const AlignedAttr * A, std::vector<std::string> &children);
    void VisitTemplateArgument(const TemplateArgument& templateArg);
    void VisitTemplateName(const TemplateName& templateArg);



        // Dumpers of other kinds of information
    void dumpIdToClassMap(const void* pointer, std::string className);
    void dumpTopLevelType(const QualType &type);
    void dumpTopLevelAttr(const Attr *attr);


    // Children visitors for Types
    void VisitTypeChildren(const Type *T, std::vector<std::string> &children);
    void VisitFunctionTypeChildren(const FunctionType *T, std::vector<std::string> &visitedChildren);
    void VisitFunctionProtoTypeChildren(const FunctionProtoType *T, std::vector<std::string> &visitedChildren);
    void VisitTagTypeChildren(const TagType *T, std::vector<std::string> &visitedChildren);
    //void VisitEnumTypeChildren(const EnumType *T, std::vector<std::string> &visitedChildren);
    void VisitArrayTypeChildren(const ArrayType *T, std::vector<std::string> &visitedChildren);
    void VisitVariableArrayTypeChildren(const VariableArrayType *T, std::vector<std::string> &visitedChildren);
    void VisitDependentSizedArrayTypeChildren(const DependentSizedArrayType *T, std::vector<std::string> &visitedChildren);
    void VisitPointerTypeChildren(const PointerType *T, std::vector<std::string> &visitedChildren);
    void VisitElaboratedTypeChildren(const ElaboratedType *T, std::vector<std::string> &visitedChildren);
    void VisitReferenceTypeChildren(const ReferenceType *T, std::vector<std::string> &visitedChildren);
    void VisitInjectedClassNameTypeChildren(const InjectedClassNameType *T, std::vector<std::string> &visitedChildren);
    void VisitTemplateTypeParmTypeChildren(const TemplateTypeParmType *T, std::vector<std::string> &visitedChildren);
    //void VisitTypedefTypeChildren(const TypedefType *T, std::vector<std::string> &visitedChildren);
    void VisitSubstTemplateTypeParmTypeChildren(const SubstTemplateTypeParmType *T, std::vector<std::string> &visitedChildren);
    void VisitTemplateSpecializationTypeChildren(const TemplateSpecializationType *T, std::vector<std::string> &visitedChildren);
    void VisitTypedefTypeChildren(const TypedefType *T, std::vector<std::string> &visitedChildren);
    void VisitAdjustedTypeChildren(const AdjustedType *T, std::vector<std::string> &visitedChildren);
    void VisitDecayedTypeChildren(const DecayedType *T, std::vector<std::string> &visitedChildren);
    void VisitDecltypeTypeChildren(const DecltypeType *T, std::vector<std::string> &visitedChildren);
    void VisitAutoTypeChildren(const AutoType *T, std::vector<std::string> &visitedChildren);
    void VisitPackExpansionTypeChildren(const PackExpansionType *T, std::vector<std::string> &visitedChildren);
    void VisitTypeOfExprTypeChildren(const TypeOfExprType *T, std::vector<std::string> &visitedChildren);
    void VisitAttributedTypeChildren(const AttributedType *T, std::vector<std::string> &visitedChildren);
    void VisitUnaryTransformTypeChildren(const UnaryTransformType *T, std::vector<std::string> &visitedChildren);
    void VisitComplexTypeChildren(const ComplexType *T, std::vector<std::string> &visitedChildren);

    // Children visitors for other types of classes
    void VisitTemplateArgChildren(const TemplateArgument& arg);

    /* Utility methods for DECLS */
    void dumpNumberTemplateParameters(const Decl *D, const TemplateParameterList *TPL);

    void dumpSourceRange(std::string id, SourceLocation startLoc, SourceLocation endLoc);

    // These methods return true if the node had been already visited

    bool dumpType(const Type* typeAddr);
    bool dumpType(const QualType& type);
    bool dumpStmt(const Stmt* stmtAddr);
    bool dumpDecl(const Decl* declAddr);
    bool dumpAttr(const Attr* attrAddr);

    /* EXTRA */
    void dumpCXXCtorInitializer(const CXXCtorInitializer *Init);




};


#endif //CLANGASTDUMPER_CLANGASTDUMPER_H
