
//
// Created by JoaoBispo on 18/03/2018.
//

#ifndef CLANGASTDUMPER_CLAVADATADUMPER_H
#define CLANGASTDUMPER_CLAVADATADUMPER_H

#include "ClavaConstants.h"
#include "ClavaDecl.h"

#include "clang/AST/AST.h"
#include "clang/AST/Decl.h"
#include "clang/AST/Stmt.h"
#include "clang/AST/Type.h"

/*
#include "clang/AST/TypeVisitor.h"
#include "clang/AST/StmtVisitor.h"
#include "clang/AST/DeclVisitor.h"
*/
///#include "clang/AST/Decl.h"
//#include "clang/AST/DeclCXX.h"

#include <string>
#include <map>

//#include "ClangAstDumper.h"


using namespace clang;

namespace clava {

    /**
     * Maps what kind of Data dumper each node should use
     */
    extern const std::map<const std::string, clava::DeclNode > DECL_DATA_MAP;
    extern const std::map<const std::string, clava::StmtNode > STMT_DATA_MAP;
    extern const std::map<const std::string, clava::StmtNode > EXPR_DATA_MAP;
    extern const std::map<const std::string, clava::TypeNode > TYPE_DATA_MAP;
    extern const std::map<const std::string, clava::AttrNode > ATTR_DATA_MAP;

    /**
    * Dumps information about each node
    */
    class ClavaDataDumper  {

    private:
        //ClangAstDumper dumper;
        ASTContext *Context;
        int id;

    public:
        // Constructor
        explicit ClavaDataDumper(ASTContext *Context, int id);

        // Utility methods
        //const std::string getId(const void* addr);

        void dump(const Decl* D);
        void dump(const Stmt* S);
        void dump(const Expr* E);
        void dump(const Type* T);
        void dump(const QualType& T);
        void dump(const Attr* A);

        void dump(clava::DeclNode declNode, const Decl* D);
        void dump(clava::StmtNode stmtNode, const Stmt* S);
        void dump(clava::TypeNode typeNode, const Type* T);
        void dump(clava::AttrNode attrNode, const Attr* A);

        //void DumpHeader(const Decl* D);
        //void DumpHeader(const Stmt* S);
        //void DumpHeader(const Type* T);



        // void DumpHeader(const std::string tag, const void *pointer);
        //void dumpHeader(const std::string tag, Stmt *D);
        //void dumpHeader(const std::string tag, Type *D);




    private:
        //void dumpHeaderAny(const std::string tag, void *D);
        //void DumpHeader(const std::string tag, const void *pointer);

        // Generic value dumpers
        //void dump(bool boolean);
        //void dump(int integer);
        //void dump(unsigned int integer) ;
        //void dump(const std::string& string);
        //void dump(const QualType& type);

        // DECLS

        void DumpDeclData(const Decl *D);
        void DumpNamedDeclData(const NamedDecl *D);
        void DumpTypeDeclData(const TypeDecl *D);
        void DumpTagDeclData(const TagDecl *D);
        void DumpEnumDeclData(const EnumDecl *D);
        void DumpRecordDeclData(const RecordDecl *D);
        void DumpCXXRecordDeclData(const CXXRecordDecl *D);
        void DumpValueDeclData(const ValueDecl *D);
        void DumpDeclaratorDeclData(const DeclaratorDecl *D);
        void DumpFieldDeclData(const FieldDecl *D);
        void DumpFunctionDeclData(const FunctionDecl *D);
        void DumpCXXMethodDeclData(const CXXMethodDecl *D);
        void DumpCXXConstructorDeclData(const CXXConstructorDecl *D);
        void DumpVarDeclData(const VarDecl *D);
        void DumpParmVarDeclData(const ParmVarDecl *D);
        void DumpTemplateTypeParmDeclData(const TemplateTypeParmDecl *D);
        void DumpTypedefNameDeclData(const TypedefNameDecl *D);
        void DumpAccessSpecDeclData(const AccessSpecDecl *D);
        void DumpUsingDeclData(const UsingDecl *D);
        void DumpUsingDirectiveDeclData(const UsingDirectiveDecl *D);
        void DumpNamespaceDeclData(const NamespaceDecl *D);
        void DumpNamespaceAliasDeclData(const NamespaceAliasDecl *D);
        void DumpLinkageSpecDeclData(const LinkageSpecDecl *D);
        void DumpStaticAssertDeclData(const StaticAssertDecl *D);


        // STMTS
        void DumpStmtData(const Stmt *S);
        void DumpLabelStmtData(const LabelStmt *S);
        void DumpGotoStmtData(const GotoStmt *S);
        void DumpAttributedStmtData(const AttributedStmt *S);

        //void DumpCapturedStmtData(const CapturedStmt *S);
        //void DumpCxxForRangeStmtData(const CXXForRangeStmt *S);

        // EXPRS
        void DumpExprData(const Expr *E);
        void DumpCastExprData(const CastExpr *E);
        //void DumpCXXFunctionalCastExprData(const CXXFunctionalCastExpr *E);
        void DumpLiteralData(const Expr *E);
        void DumpCharacterLiteralData(const CharacterLiteral *E);
        void DumpIntegerLiteralData(const IntegerLiteral *E);
        void DumpFloatingLiteralData(const FloatingLiteral *E);
        void DumpStringLiteralData(const StringLiteral *E);
        void DumpCXXBoolLiteralExprData(const CXXBoolLiteralExpr *E);
        void DumpCompoundLiteralExprData(const CompoundLiteralExpr *E);
        void DumpInitListExprData(const InitListExpr *E);
        void DumpDeclRefExprData(const DeclRefExpr *E);
        void DumpOverloadExprData(const OverloadExpr *E);
        void DumpUnresolvedMemberExprData(const UnresolvedMemberExpr *E);
        void DumpUnresolvedLookupExprData(const UnresolvedLookupExpr *E);
        void DumpCXXConstructExprData(const CXXConstructExpr *E);
        void DumpMemberExprData(const MemberExpr *E);
        void DumpMaterializeTemporaryExprData(const MaterializeTemporaryExpr *E);
        void DumpBinaryOperatorData(const BinaryOperator *E);
        void DumpCallExprData(const CallExpr *E);
        void DumpCXXMemberCallExprData(const CXXMemberCallExpr *E);
        void DumpCXXTypeidExprData(const CXXTypeidExpr *E);
        void DumpExplicitCastExprData(const ExplicitCastExpr *E);
        void DumpCXXNamedCastExprData(const CXXNamedCastExpr *E);
        void DumpCXXDependentScopeMemberExprData(const CXXDependentScopeMemberExpr *E);
        void DumpUnaryOperatorData(const UnaryOperator *E);
        void DumpUnaryExprOrTypeTraitExprData(const UnaryExprOrTypeTraitExpr *E);
        void DumpCXXNewExprData(const CXXNewExpr *E);
        void DumpCXXDeleteExprData(const CXXDeleteExpr *E);
        void DumpOffsetOfExprData(const OffsetOfExpr *E);
        void DumpLambdaExprData(const LambdaExpr *E);
        void DumpPredefinedExprData(const PredefinedExpr *E);
        void DumpSizeOfPackExprData(const SizeOfPackExpr *E);
        void DumpArrayInitLoopExprData(const ArrayInitLoopExpr *E);



            // TYPES
        void DumpTypeData(const Type *T);
        void DumpTypeData(const Type *T, Qualifiers &qualifiers);
        //void DumpTypeData(const QualType &T);

            //void DumpQualTypeData(const QualType *T);
        void DumpBuiltinTypeData(const BuiltinType *T);
        void DumpPointerTypeData(const PointerType *T);
        void DumpFunctionTypeData(const FunctionType *T);
        void DumpFunctionProtoTypeData(const FunctionProtoType *T);
        void DumpTagTypeData(const TagType *T);
        //void DumpRecordTypeData(const RecordType *T);
        void DumpArrayTypeData(const ArrayType *T);
        void DumpConstantArrayTypeData(const ConstantArrayType *T);
        void DumpVariableArrayTypeData(const VariableArrayType *T);
        void DumpDependentSizedArrayTypeData(const DependentSizedArrayType *T);
        void DumpTypeWithKeywordData(const TypeWithKeyword *T);
        void DumpElaboratedTypeData(const ElaboratedType *T);
        void DumpTemplateTypeParmTypeData(const TemplateTypeParmType *T);
        void DumpTemplateSpecializationTypeData(const TemplateSpecializationType *T);
        void DumpTypedefTypeData(const TypedefType *T);
        void DumpAdjustedTypeData(const AdjustedType *T);
        void DumpDecayedTypeData(const DecayedType *T);
        void DumpDecltypeTypeData(const DecltypeType *T);
        void DumpAutoTypeData(const AutoType *T);
        void DumpReferenceTypeData(const ReferenceType *T);
        void DumpPackExpansionTypeData(const PackExpansionType *T);
        void DumpTypeOfExprTypeData(const TypeOfExprType *T);
        void DumpAttributedTypeData(const AttributedType *T);
        void DumpUnaryTransformTypeData(const UnaryTransformType  *T);
        void DumpSubstTemplateTypeParmTypeData(const SubstTemplateTypeParmType *T);
        void DumpComplexTypeData(const ComplexType *T);

            // ATTRS

        void DumpAttrData(const Attr *A);
        void DumpAlignedAttrData(const AlignedAttr *A);
        void DumpOpenCLUnrollHintAttrData(const OpenCLUnrollHintAttr *A);

        const std::string getDataName(DeclNode node);
        const std::string getDataName(StmtNode node);
        const std::string getDataName(TypeNode node);
        const std::string getDataName(AttrNode node);
        const std::string getDataName(std::string nodeName);

    };



}

#endif //CLANGASTDUMPER_CLAVADATADUMPER_H
