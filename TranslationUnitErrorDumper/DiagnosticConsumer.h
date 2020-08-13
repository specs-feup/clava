//
// Created by JoaoBispo on 07/08/2020.
//

#ifndef TRANSLATIONUNITERRORDUMPER_DIAGNOSTICCONSUMER_H
#define TRANSLATIONUNITERRORDUMPER_DIAGNOSTICCONSUMER_H

#include "clang/AST/DeclBase.h"
#include "clang/AST/DeclContextInternals.h"
#include <llvm/Support/raw_ostream.h>
#include "llvm/ADT/APFloat.h"
#include <iostream>
#include <clang/AST/Type.h>
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
                if(argKind == clang::DiagnosticsEngine::ak_std_string) {
                    llvm::errs() << "string\n";
                    llvm::errs() << Info.getArgStdStr(i);
                    llvm::errs() << "\n";
                }
                if(argKind == clang::DiagnosticsEngine::ak_c_string) {
                    llvm::errs() << "ak_c_string\n";
                    llvm::errs() << Info.getArgCStr(i);
                    llvm::errs() << "\n";
                }
                if(argKind == clang::DiagnosticsEngine::ak_sint) {
                    llvm::errs() << " ak_sint: " << Info.getArgSInt(i);
                }
                if(argKind == clang::DiagnosticsEngine::ak_uint) {
                    llvm::errs() << " ak_uint: " << Info.getArgUInt(i);
                }
                if(argKind == clang::DiagnosticsEngine::ArgumentKind::ak_qualtype) {
                    llvm::errs() << "qualtype\n";
                    llvm::errs() <<  ((clang::QualType *) Info.getRawArg(i))->getAsString();
                    llvm::errs() << "\n";
                }
                if(argKind == clang::DiagnosticsEngine::ArgumentKind::ak_declarationname) {
                    llvm::errs() << "declaration_name\n";
                    llvm::errs() <<  ((clang::DeclarationName *) Info.getRawArg(i))->getAsIdentifierInfo();
                 /*   llvm::errs() << "\n";
                    llvm::errs() <<  ((clang::DeclarationName *) Info.getRawArg(i))->getCXXLiteralIdentifier();
                    llvm::errs() << "\n";
                    llvm::errs() <<  ((clang::DeclarationName *) Info.getRawArg(i))->getCXXNameType().getAsString();
                    llvm::errs() << "\n";
                    llvm::errs() <<  ((clang::DeclarationName *) Info.getRawArg(i))->getNameKind();
                    llvm::errs() << "\n";*/
                  //  llvm::errs() <<  ((clang::DeclarationName *) Info.getRawArg(i))->getAsString();
                    llvm::errs() << "\n";
                }
                if(argKind == clang::DiagnosticsEngine::ArgumentKind::ak_declcontext) {
                    llvm::errs() << "declcontext\n";
                    llvm::errs() <<  ((clang::DeclContext *) Info.getRawArg(i))->getDeclKindName();
                    //llvm::errs() <<  ((clang::DeclContext *) Info.getRawArg(i))->get
                    //getIdentifier()->getName();
                    /*clang::SmallVectorImpl &OutStr = llvm::SmallVectorImpl<char> ();
                    Info.getDiags()->ConvertArgToString(clang::DiagnosticsEngine::ArgumentKind::ak_declcontext,
                                                        Info.getRawArg(i),
                                                        llvm::StringRef("Modifier", 5),
                                                        llvm::StringRef("Argument", 5),
                                                        clang::SmallVector<clang::DiagnosticsEngine::ArgumentValue, 8>(),
                                                        static_cast<clang::SmallVectorImpl<char> &>(clang::SmallVector<clang::DiagnosticsEngine::ArgumentValue, 8>()),
                                                        clang::SmallVector<intptr_t, 2>());*/
                    llvm::errs() << "\n";
                }
            }
        }

        llvm::errs() << "<Clang Error End>\n";
        llvm::errs() << " \n";

        llvm::SmallVector<char, 128> message;
        Info.FormatDiagnostic(message);
        llvm::errs() << message << '\n';

    }
};


#endif //TRANSLATIONUNITERRORDUMPER_DIAGNOSTICCONSUMER_H
