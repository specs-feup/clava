//
// Created by JoaoBispo on 18/03/2018.
//

#include "ClavaDataDumper.h"

#include "ClavaConstants.h"
#include "ClangNodes.h"

#include <sstream>

clava::ClavaDataDumper::ClavaDataDumper(ASTContext *Context, int id) : Context(Context), id(id)  {};

void clava::ClavaDataDumper::DumpHeader(const std::string tag, const void *pointer) {
    llvm::errs() << tag << "\n";
    llvm::errs() << getId(pointer) << "\n";
}

/*
void InfoDumper::DumpHeader(const Decl* D) {
    DumpHeader("<" + clava::getClassName(D) + "Data>", D);
}
 */

/*
void InfoDumper::DumpHeader(const Stmt* S) {
    DumpHeader("<" + clava::getClassName(S) + "Data>", S);
}

void InfoDumper::DumpHeader(const Type* T) {
    DumpHeader("<" + clava::getClassName(T) + "Data>", T);
}
 */

std::string clava::ClavaDataDumper::getId(const void* addr) {
    std::stringstream ss;
    ss <<  addr << "_" << id;

    return ss.str();
}

void clava::ClavaDataDumper::dump(bool boolean) {
    llvm::errs() << boolean << "\n";
}


void clava::ClavaDataDumper::dump(int integer) {
    llvm::errs() << integer << "\n";
}

void clava::ClavaDataDumper::dump(unsigned int integer) {
    llvm::errs() << integer << "\n";
}

void clava::ClavaDataDumper::dump(std::string string) {
    llvm::errs() << string << "\n";
}



const std::string clava::ClavaDataDumper::getDataName(std::string nodeName) {
    return "<" + nodeName + "Data>";
}

const std::string clava::ClavaDataDumper::getDataName(DeclNode node) {
    return getDataName(clava::getName(node));
}
const std::string clava::ClavaDataDumper::getDataName(StmtNode node) {
    return getDataName(clava::getName(node));
}

const std::string clava::ClavaDataDumper::getDataName(TypeNode node) {
    return getDataName(clava::getName(node));
}

/**
* Names of DeclData dumpers
*/
/*
static std::map<DeclNode,std::string>  DECL_DATA_NAMES = {
        {DeclNode::DECL, "<DeclData>"},
        {DeclNode::NAMED_DECL, "<NamedDeclData>"},
        {DeclNode::FUNCTION_DECL, "<FunctionDeclData>"},
        {DeclNode::CXX_METHOD_DECL, "<CXXMethodDeclData>"},
        {DeclNode::VAR_DECL, "<VarDeclData>"},
        {DeclNode::PARM_VAR_DECL, "<ParmVarDeclData>"},
        {DeclNode::CXX_RECORD_DECL, "<CXXRecordDeclData>"}

};
*/

/**
* Names of StmtData dumpers. In Clang Expr is a subclass of Stmt, but in Clava they are different classes.
*/
/*
static std::map<StmtNode,std::string>  STMT_DATA_NAMES = {
        // Stmts
        {StmtNode::STMT, "<StmtData>"},
        {StmtNode::COMPOUND_STMT, "<CompoundStmtData>"},
        {StmtNode::DECL_STMT, "<DeclStmtData>"},

        // Exprs
        {StmtNode::EXPR, "<ExprData>"}
};
*/
/**
* Names of ExprData dumpers. In Clang Expr is a subclass of Stmt, but in Clava they are different classes.
*/
/*
    static std::map<ExprNode,std::string>  EXPR_DATA_NAMES = {

    };
*/
/**
* Names of StmtData dumpers
*/
/*
static std::map<TypeNode,std::string>  TYPE_DATA_NAMES = {
        {TypeNode::TYPE, "<TypeData>"}
};
 */

