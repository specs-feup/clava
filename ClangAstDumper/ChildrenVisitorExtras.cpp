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
        default: throw std::invalid_argument("ClangNodes::dump(TemplateArgument&): Case not implemented, '"+clava::TEMPLATE_ARG_KIND[templateArg.getKind()]+"'");
    }
}