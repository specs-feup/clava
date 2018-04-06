//
// Created by JoaoBispo on 30/03/2018.
//

#include "ClavaDataDumper.h"
#include "ClangNodes.h"
#include "ClangEnums.h"

const std::map<const std::string, clava::TypeNode > clava::TYPE_DATA_MAP = {
        {"BuiltinType", clava::TypeNode::BUILTIN_TYPE}
};

void clava::ClavaDataDumper::dump(const Type* T) {

    // Get classname
    const std::string classname = clava::getClassName(T);

    // Get corresponding DeclNode
    TypeNode typeNode = TYPE_DATA_MAP.count(classname) == 1 ? TYPE_DATA_MAP.find(classname)->second : TypeNode::TYPE;

    dump(typeNode, T);
}

void clava::ClavaDataDumper::dump(clava::TypeNode typeNode, const Type* T) {
    DumpHeader(getDataName(typeNode), T);

    switch(typeNode) {
        case clava::TypeNode::TYPE:
            DumpTypeData(T); break;
//        case clava::TypeNode::QUAL_TYPE:
//            DumpQualTypeData(static_cast<const QualType *>(T)); break;
        case clava::TypeNode::BUILTIN_TYPE:
            DumpBuiltinTypeData(static_cast<const BuiltinType *>(T)); break;
        default: throw std::invalid_argument("ClangDataDumper::dump(TypeNode): Case not implemented, '"+ getName(typeNode) +"'");
    }
}


void clava::ClavaDataDumper::DumpTypeData(const Type *T) {

}

//void clava::ClavaDataDumper::DumpQualTypeData(const QualType *T) {

//}



void clava::ClavaDataDumper::DumpBuiltinTypeData(const BuiltinType *T) {
    DumpTypeData(T);

    clava::dump(T->getName(Context->getPrintingPolicy()));
    clava::dump(T->isSugared());
}