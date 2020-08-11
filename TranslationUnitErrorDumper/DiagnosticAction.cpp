//
// Created by estudio on 06/08/2020.
//

#include "DiagnosticAction.h"
#include "DiagnosticConsumer.h"

void DiagnosticAction::ExecuteAction() {

    std::cout << "ExecuteAction" << std::endl;

    clang::CompilerInstance &CI = getCompilerInstance();
    clang::DiagnosticsEngine &DE = CI.getDiagnostics();

    DE.setClient(new DiagnosticConsumer(), /*ShouldOwnClient=*/true);
    SyntaxOnlyAction::ExecuteAction();

    //DE.setSeverity(4833, clang::diag::Severity::Ignored, clang::SourceLocation());


    if (DE.hasErrorOccurred()){

        std::cout << "Errors occurred\n";

        clang::Diagnostic D = clang::Diagnostic(&DE);

        for(auto diag : D.getDiags()->getDiagnosticMappings()){
            if(diag.getSecond().isErrorOrFatal()) {
                DE.Report(diag.getFirst());
                //std::cout << "Diag ID: " << diag.getFirst() << std::endl;
                //std::cout << "Diag Report: " << DE.Report(diag.getFirst()) << std::endl;
                //auto diagId = diag.getFirst();
                //std::cout << "Desc: " << DE.getDiagnosticIDs()->getDescription(diagId).str() << std::endl;

            }
                // std::cout << "\n    description: " << DE.getDiagnosticIDs()->getDescription(D.getArgKind(i)).str();
        }

    }

    /* Obter descricoes de todos os diagnosticos possiveis:

    const clang::IntrusiveRefCntPtr< clang::DiagnosticIDs > &IRCP = DE.getDiagnosticIDs();
    std::vector<clang::diag::kind> diags;
    clang::diag::Flavor flavor = static_cast<clang::diag::Flavor>(0);
    IRCP->getAllDiagnostics(flavor, diags);
    for (unsigned int i = 0; i < diags.size(); i++) {
        std::cout << "\n    " << diags.at(i) << "  diag description: " << DE.getDiagnosticIDs()->getDescription(diags.at(i)).str();
    }

    std::cout << "\n    diag description: " << DE.getDiagnosticIDs()->getDescription(D.getID()).str();
    std::cout << "\n    diag description: " << DE.getDiagnosticIDs()->getDescription(DE.getCurrentDiagID()).str();
    clang::LogDiagnosticPrinter LDP = clang::LogDiagnosticPrinter(, );
    std::cout << "\n    diag id: " << D.getID();
    handle_diag(D);
     */

}

