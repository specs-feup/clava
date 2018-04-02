//
// Created by JoaoBispo on 30/03/2018.
//

#include "ClavaDataDumper.h"


void clava::ClavaDataDumper::dump(clava::TypeNode typeNode, const Type* T) {
    DumpHeader(getDataName(typeNode), T);

    switch(typeNode) {
        case clava::TypeNode::TYPE:
            DumpTypeData(T); break;
        default: throw std::invalid_argument("ClangDataDumper::dump(TypeNode): Case not implemented, '"+ getName(typeNode) +"'");
    }
}


void clava::ClavaDataDumper::DumpTypeData(const Type *T) {

}