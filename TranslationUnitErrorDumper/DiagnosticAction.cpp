//
// Created by estudio on 06/08/2020.
//

#include "DiagnosticAction.h"

void DiagnosticAction::ExecuteAction() {

    std::cout << "ExecuteAction";

    SyntaxOnlyAction::ExecuteAction();

    clang::CompilerInstance &CI = getCompilerInstance();
    clang::DiagnosticsEngine &DE = CI.getDiagnostics();

    if (DE.hasErrorOccurred()){
        clang::Diagnostic D = clang::Diagnostic(&DE);

        std::cout << "Errors occurred\n";

        //Argumentos do diagnostico
        std::cout << std::endl << "args " << D.getNumArgs();
        for (unsigned int i = 0; i < D.getNumArgs(); i++){
            std::cout << "\n  arg kind " << D.getArgKind(i);

            // std::cout << "\n    description: " << DE.getDiagnosticIDs()->getDescription(D.getArgKind(i)).str();
        }


        // Obter descricoes de todos os diagnosticos possiveis:
        const clang::IntrusiveRefCntPtr< clang::DiagnosticIDs > &IRCP = DE.getDiagnosticIDs();
        std::vector<clang::diag::kind> diags;
        clang::diag::Flavor flavor = static_cast<clang::diag::Flavor>(0);
        IRCP->getAllDiagnostics(flavor, diags);
        for (unsigned int i = 0; i < diags.size(); i++) {
            std::cout << "\n    " << diags.at(i) << "  diag description: " << DE.getDiagnosticIDs()->getDescription(diags.at(i)).str();
        }
        //std::cout << "\n    diag description: " << DE.getDiagnosticIDs()->getDescription(D.getID()).str();
        //std::cout << "\n    diag description: " << DE.getDiagnosticIDs()->getDescription(DE.getCurrentDiagID()).str();
        //clang::LogDiagnosticPrinter LDP = clang::LogDiagnosticPrinter(, );
        //std::cout << "\n    diag id: " << D.getID();
        //handle_diag(D);

}



}
