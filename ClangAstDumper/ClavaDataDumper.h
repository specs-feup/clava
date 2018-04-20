
//
// Created by JoaoBispo on 18/03/2018.
//

#ifndef CLANGASTDUMPER_CLAVADATADUMPER_H
#define CLANGASTDUMPER_CLAVADATADUMPER_H

#include "ClavaConstants.h"

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
        const std::string getId(const void* addr);

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
        void DumpHeader(const std::string tag, const void *pointer);

        // Generic value dumpers
        //void dump(bool boolean);
        //void dump(int integer);
        //void dump(unsigned int integer) ;
        //void dump(const std::string& string);
        //void dump(const QualType& type);

        // DECLS

        void DumpDeclData(const Decl *D);
        void DumpNamedDeclData(const NamedDecl *D); // Not being called yet in ClangAstDumper
        void DumpFunctionDeclData(const FunctionDecl *D);
        void DumpCXXMethodDeclData(const CXXMethodDecl *D);
        void DumpVarDeclData(const VarDecl *D);
        void DumpParmVarDeclData(const ParmVarDecl *D);


        // STMTS
        void DumpStmtData(const Stmt *S);


        // EXPRS
        void DumpExprData(const Expr *E);
        void DumpCastExprData(const CastExpr *E);
        void DumpLiteralData(const Expr *E);
        void DumpCharacterLiteralData(const CharacterLiteral *E);
        void DumpIntegerLiteralData(const IntegerLiteral *E);
        void DumpFloatingLiteralData(const FloatingLiteral *E);


        // TYPES
        void DumpTypeData(const Type *T);
        void DumpTypeData(const Type *T, Qualifiers &qualifiers);

            //void DumpQualTypeData(const QualType *T);
        void DumpBuiltinTypeData(const BuiltinType *T);


        // ATTRS

        void DumpAttrData(const Attr *A);
        void DumpAlignedAttrData(const AlignedAttr *A);

        const std::string getDataName(DeclNode node);
        const std::string getDataName(StmtNode node);
        const std::string getDataName(TypeNode node);
        const std::string getDataName(AttrNode node);
        const std::string getDataName(std::string nodeName);

    };



}

#endif //CLANGASTDUMPER_CLAVADATADUMPER_H
