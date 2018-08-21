/**
 * EXTRA Visitors
 */

#include "ClangAstDumper.h"
#include "ClavaConstants.h"
#include "ClangNodes.h"
#include "ClangEnums.h"

#include <string>

void ClangAstDumper::VisitTemplateArgChildren(const TemplateArgument& templateArg) {
    switch(templateArg.getKind()) {
        case TemplateArgument::ArgKind::Type:
            VisitTypeTop(templateArg.getAsType());
            break;
        case TemplateArgument::ArgKind::Expression:
            VisitStmtTop(templateArg.getAsExpr());
            break;
        case TemplateArgument::ArgKind::Pack:
            for (auto currentArg = templateArg.pack_begin(), endArg = templateArg.pack_end(); currentArg != endArg; ++currentArg) {
                VisitTemplateArgChildren(*currentArg);
            }
            break;
        case TemplateArgument::ArgKind::Integral:
            // Do nothing
            break;
        default: throw std::invalid_argument("ClangAstDumper::VisitTemplateArgChildren(const TemplateArgument&): Case not implemented, '"+clava::TEMPLATE_ARG_KIND[templateArg.getKind()]+"'");
    }
}