//
// Created by JoaoBispo on 07/08/2020.
//

#ifndef TRANSLATIONUNITERRORDUMPER_DIAGNOSTICCONSUMER_H
#define TRANSLATIONUNITERRORDUMPER_DIAGNOSTICCONSUMER_H

#include <llvm/Support/raw_ostream.h>
#include <iostream>
#include "clang/Basic/Diagnostic.h"

class DiagnosticConsumer : public clang::DiagnosticConsumer {

public:


    void HandleDiagnostic(clang::DiagnosticsEngine::Level DiagLevel,
                          const clang::Diagnostic& Info) override {
        unsigned diagId = Info.getID();
        if (diagId == 3822) //id for 'unknown type name'
            llvm::errs() << diagId << ' ';
        /*
        \\(a funcao getNumArgs sempre retorna 0 aqui)
        llvm::errs() << "Num args: " << Info.getNumArgs()  << '\n';
        for(unsigned argIndex=0; argIndex<Info.getNumArgs(); argIndex++) {
            llvm::errs() << "Arg kind: " <<  Info.getArgKind(argIndex) << "\n";
        }*/

/*
        llvm::SmallVector<char, 128> message;
        Info.FormatDiagnostic(message);
        llvm::errs() << message << '\n';*/

    }
};


#endif //TRANSLATIONUNITERRORDUMPER_DIAGNOSTICCONSUMER_H
