//
// Created by JoaoBispo on 30/03/2018.
//

#include "ClavaDataDumper.h"
#include "ClangNodes.h"

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

    clava::dump(T->getKind());
    clava::dump(T->isSugared());
}