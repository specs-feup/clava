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
        case TemplateArgument::ArgKind::Template:
            VisitTemplateNameChildren(templateArg.getAsTemplate());
            break;
        default: throw std::invalid_argument("ClangAstDumper::VisitTemplateArgChildren(const TemplateArgument&): Case not implemented, '"+clava::TEMPLATE_ARG_KIND[templateArg.getKind()]+"'");
    }
}

void ClangAstDumper::VisitTemplateNameChildren(const TemplateName& templateName) {
    switch(templateName.getKind()) {
        case TemplateName::NameKind::Template:
            VisitDeclTop(templateName.getAsTemplateDecl());
            break;
        case TemplateName::NameKind::QualifiedTemplate:
            VisitDeclTop(templateName.getAsQualifiedTemplateName()->getTemplateDecl());
            break;
        case TemplateName::NameKind::SubstTemplateTemplateParm:
            VisitDecl(templateName.getAsSubstTemplateTemplateParm()->getParameter());
            VisitTemplateNameChildren(templateName.getAsSubstTemplateTemplateParm()->getReplacement());
            break;
        default:
            throw std::invalid_argument("ClangAstDumper::VisitTemplateNameChildren(TemplateName&): TemplateName case in kind 'Template' not implemented, '" +
                                        clava::TEMPLATE_NAME_KIND[templateName.getKind()] + "'");
    }
}
