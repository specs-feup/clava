//
// Created by JoaoBispo on 30/03/2018.
//

#include "ClavaDataDumper.h"


void clava::ClavaDataDumper::dump(clava::TypeNode typeNode, const Type* T) {
    DumpHeader(clava::TYPE_DATA_NAMES[typeNode], T);

    switch(typeNode) {
        case clava::TypeNode::TYPE:
            DumpTypeData(T); break;
        default: throw std::invalid_argument("ClangDataDumper::dump: Case not implemented, '"+clava::TYPE_DATA_NAMES[typeNode]+"'");
    }
}


void clava::ClavaDataDumper::DumpTypeData(const Type *T) {

}