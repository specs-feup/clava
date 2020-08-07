//
// Created by estudio on 06/08/2020.
//

#include "DiagnosticAction.h"
#include "DiagnosticConsumer.h"

void DiagnosticAction::ExecuteAction() {





    std::cout << "ExecuteAction" << std::endl;

    SyntaxOnlyAction::ExecuteAction();

    clang::CompilerInstance &CI = getCompilerInstance();
    clang::DiagnosticsEngine &DE = CI.getDiagnostics();

    DE.setClient(new DiagnosticConsumer(), /*ShouldOwnClient=*/true);


    if (DE.hasErrorOccurred()){

        clang::Diagnostic D = clang::Diagnostic(&DE);

//        std::cout << "Diags: " << DE.getDiagnosticIDs()->getAllDiagnostics(clang::diag::Flavor::WarningOrError,... ) << std::endl;

        for(auto diag : D.getDiags()->getDiagnosticMappings()) {

            //std::cout << "Diag level: " << DE.getDiagnosticLevel(diag.getFirst(), clang_getNullLocation());
            if(diag.getSecond().isErrorOrFatal()) {
                DE.Report(diag.getFirst());
                //std::cout << "Diag ID: " << diag.getFirst() << std::endl;
                //std::cout << "Diag Report: " << DE.Report(diag.getFirst()) << std::endl;
                //auto diagId = diag.getFirst();
                //std::cout << "Desc: " << DE.getDiagnosticIDs()->getDescription(diagId).str() << std::endl;

            }

            //auto second = diag.getSecond();

            //   std::cout << "Diag: " << static_cast<std::underlying_type<clang::diag::Severity>::type>(diag.second.getSeverity())  << "\n";
            //std::cout << "Is error or fatal: " << diag.getSecond().isErrorOrFatal() << std::endl;

        }

/*
        std::cout << "Errors occurred\n";

        //Argumentos do diagnostico
        std::cout << std::endl << "args " << D.getNumArgs();
        for (unsigned int i = 0; i < D.getNumArgs(); i++){
            auto argKind = D.getArgKind(i);
            std::cout << "\n  arg kind " << argKind;
            if(argKind == clang::DiagnosticsEngine::ak_std_string) {
                std::cout << " : " << D.getArgStdStr(i);
            }

            if(argKind == clang::DiagnosticsEngine::ak_c_string) {
                std::cout << " : " << D.getArgCStr(i);
            }

            if(argKind == clang::DiagnosticsEngine::ak_sint) {
                std::cout << " : " << D.getArgSInt(i);
            }

            if(argKind == clang::DiagnosticsEngine::ak_uint) {
                std::cout << " : " << D.getArgUInt(i);
            }

            if(argKind == clang::DiagnosticsEngine::ak_identifierinfo) {
                std::cout << " : " << D.getArgIdentifier(i);
            }


            // std::cout << "\n    description: " << DE.getDiagnosticIDs()->getDescription(D.getArgKind(i)).str();
        }
*/

        // Obter descricoes de todos os diagnosticos possiveis:
        //
		//const clang::IntrusiveRefCntPtr< clang::DiagnosticIDs > &IRCP = DE.getDiagnosticIDs();
        //std::vector<clang::diag::kind> diags;
        //clang::diag::Flavor flavor = static_cast<clang::diag::Flavor>(0);
        //IRCP->getAllDiagnostics(flavor, diags);
        //for (unsigned int i = 0; i < diags.size(); i++) {
        //    std::cout << "\n    " << diags.at(i) << "  diag description: " << DE.getDiagnosticIDs()->getDescription(diags.at(i)).str();
        //}

        //std::cout << "\n    diag description: " << DE.getDiagnosticIDs()->getDescription(D.getID()).str();
        //std::cout << "\n    diag description: " << DE.getDiagnosticIDs()->getDescription(DE.getCurrentDiagID()).str();
        //clang::LogDiagnosticPrinter LDP = clang::LogDiagnosticPrinter(, );
        //std::cout << "\n    diag id: " << D.getID();
        //handle_diag(D);

}



}
