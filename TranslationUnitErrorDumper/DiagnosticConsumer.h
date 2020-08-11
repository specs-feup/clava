//
// Created by JoaoBispo on 07/08/2020.
//

#ifndef TRANSLATIONUNITERRORDUMPER_DIAGNOSTICCONSUMER_H
#define TRANSLATIONUNITERRORDUMPER_DIAGNOSTICCONSUMER_H

#include <llvm/Support/raw_ostream.h>
#include <iostream>
#include "clang/Basic/Diagnostic.h"
#include "clang/Basic/IdentifierTable.h"

class DiagnosticConsumer : public clang::DiagnosticConsumer {


public:


    void HandleDiagnostic(clang::DiagnosticsEngine::Level DiagLevel,
                          const clang::Diagnostic& Info) override {
        unsigned diagId = Info.getID();
        llvm::errs() << "<Clang Error>" << "\n";
        llvm::errs() << diagId << '\n';

        if (Info.getNumArgs() > 0) {
            for (unsigned int i = 0; i < Info.getNumArgs(); i++){
                auto argKind = Info.getArgKind(i);
                if(argKind == clang::DiagnosticsEngine::ak_identifierinfo){
                  //  llvm::errs() << "<STORE_KEY_VALUE>\n";
                    llvm::errs() << "identifier_name\n";
                    llvm::errs() << Info.getArgIdentifier(i)->getName();
                    llvm::errs() << "\n";
                }
                /*if(argKind == clang::DiagnosticsEngine::ak_std_string) {
                    llvm::errs() << " : " << D.getArgStdStr(i);
                }
                if(argKind == clang::DiagnosticsEngine::ak_c_string) {
                    llvm::errs() << " ak_c_string: " << D.getArgCStr(i);
                }
                if(argKind == clang::DiagnosticsEngine::ak_sint) {
                    llvm::errs() << " ak_sint: " << D.getArgSInt(i);
                }
                if(argKind == clang::DiagnosticsEngine::ak_uint) {
                    llvm::errs() << " ak_uint: " << D.getArgUInt(i);
                }
                if(argKind == clang::DiagnosticsEngine::ArgumentKind::ak_qualtype) {
                    llvm::errs() << " : " << "qualtype";
                }
                if(argKind == clang::DiagnosticsEngine::ArgumentKind::ak_declarationname) {
                    llvm::errs() << " : " << "declarationname";
                }
                if(argKind == clang::DiagnosticsEngine::ArgumentKind::ak_declcontext) {
                    llvm::errs() << " : " << "ak_declcontext";
                }*/
            }
        }

        llvm::errs() << "end\n";
        llvm::errs() << " \n";
/*
        llvm::SmallVector<char, 128> message;
        Info.FormatDiagnostic(message);
        llvm::errs() << message << '\n';*/

    }
};


#endif //TRANSLATIONUNITERRORDUMPER_DIAGNOSTICCONSUMER_H
