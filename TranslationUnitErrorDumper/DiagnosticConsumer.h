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
#include <string>
#include <fstream>
#include "clang/Frontend/CompilerInstance.h"





class DiagnosticConsumer : public clang::DiagnosticConsumer {

private:
    clang::CompilerInstance& CI;

public:
    DiagnosticConsumer(clang::CompilerInstance &CI_) : CI(CI_) {}
    void printRange(clang::Diagnostic Info, clang::CharSourceRange range, clang::SourceManager & SM);
    void HandleDiagnostic(clang::DiagnosticsEngine::Level DiagLevel,
                          const clang::Diagnostic& Info) override;
};


#endif //TRANSLATIONUNITERRORDUMPER_DIAGNOSTICCONSUMER_H
