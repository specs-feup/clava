//
// Created by estudio on 06/08/2020.
//

#ifndef TEST_DIAGNOSTICACTION_H
#define TEST_DIAGNOSTICACTION_H
#include "clang/Frontend/FrontendActions.h"
#include "clang/Frontend/CompilerInstance.h"
#include "clang/Frontend/LogDiagnosticPrinter.h"
#include "clang/Basic/DiagnosticIDs.h"
#include <iostream>
#include <vector>


class DiagnosticAction : public clang::SyntaxOnlyAction {

public:
    void ExecuteAction();

};


#endif //TEST_DIAGNOSTICACTION_H
