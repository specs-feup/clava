//
// Created by JoaoBispo on 21/03/2018.
//

#include "ClangNodes.h"
#include "../ClangEnums/ClangEnums.h"

#include "clang/AST/Attr.h"
#include "clang/Basic/SourceManager.h"
#include "clang/Lex/Lexer.h"

#include <bitset>
#include <iostream>

using namespace clang;

const std::string clava::getClassName(const Decl *D) {
    const std::string kindName = D->getDeclKindName();
    return kindName + "Decl";
}

const std::string clava::getClassName(const Stmt *S) {
    const std::string className = S->getStmtClassName();
    return className;
}

const std::string clava::getClassName(const Type *T) {
    const std::string kindName = T->getTypeClassName();
    return kindName + "Type";
}

const std::string clava::getAttrKind(const Attr *A) {
    {
        switch (A->getKind()) {
#define ATTR(X)                                                                \
    case attr::X:                                                              \
        return #X;                                                             \
        break;
#include "clang/Basic/AttrList.inc"
        }
    }

    return "<undefined_attribute>";
}

const std::string clava::getClassName(const Attr *A) {
    const std::string kindName = clava::getAttrKind(A);

    return kindName + "Attr";
}

void clava::dumpSourceRange(ASTContext *Context, SourceLocation startLoc,
                            SourceLocation endLoc) {
    // All components of the source range will be dumped
    const SourceManager &SM = Context->getSourceManager();

    SourceLocation startSpellingLoc = SM.getSpellingLoc(startLoc);

    if (startSpellingLoc.isInvalid()) {
        llvm::errs() << "<invalid>\n";
        return;
    }

    // Dump start location
    llvm::errs() << SM.getFilename(startSpellingLoc) << "\n";
    llvm::errs() << SM.getSpellingLineNumber(startSpellingLoc) << "\n";
    llvm::errs() << SM.getSpellingColumnNumber(startSpellingLoc) << "\n";

    if (startLoc == endLoc) {
        llvm::errs() << "<end>\n";
        return;
    }

    SourceLocation endSpellingLoc = SM.getSpellingLoc(endLoc);

    if (endSpellingLoc.isInvalid()) {
        // if(endPLoc.isInvalid()) {
        llvm::errs() << "<end>\n";
        return;
    }

    StringRef endFilename = SM.getFilename(endSpellingLoc);
    if (endFilename.size() == 0) {
        endFilename = SM.getFilename(startSpellingLoc);
    }

    unsigned int endLine = SM.getSpellingLineNumber(endSpellingLoc);
    if (!endLine) {
        endLine = SM.getSpellingLineNumber(startSpellingLoc);
    }

    unsigned int endCol = SM.getSpellingColumnNumber(endSpellingLoc);
    if (!endCol) {
        endCol = SM.getSpellingColumnNumber(startSpellingLoc);
    }

    // Dump end location
    llvm::errs() << endFilename << "\n";
    llvm::errs() << endLine << "\n";
    llvm::errs() << endCol << "\n";
}

void clava::dumpSourceInfo(ASTContext *Context, SourceLocation begin,
                           SourceLocation end) {

    // Original source range
    clava::dumpSourceRange(Context,
                           Context->getSourceManager().getExpansionLoc(begin),
                           Context->getSourceManager().getExpansionLoc(end));

    // ISMACRO: Disable this when updating
    // If it is a macro
    bool isMacro = begin.isMacroID() || end.isMacroID();
    clava::dump(isMacro);

    // ISMACRO: Disable this when updating
    // Spelling location, if macro
    if (isMacro) {
        clava::dumpSourceRange(
            Context, Context->getSourceManager().getSpellingLoc(begin),
            Context->getSourceManager().getSpellingLoc(end));
    }

    // If is in system header
    FullSourceLoc fullLocation = Context->getFullLoc(begin);

    if (fullLocation.isValid() && fullLocation.isInSystemHeader()) {
        clava::dump(true);
    } else {
        clava::dump(false);
    }
}

const std::string clava::getId(const void *addr, int id) {
    std::stringstream ss;
    ss << addr << "_" << id;

    return ss.str();
}

const std::string clava::getId(const Decl *addr, int id) {
    if (addr == nullptr) {
        return "nullptr_decl";
    }

    return getId((void *)addr, id);
}

const std::string clava::getId(const Stmt *addr, int id) {
    if (addr == nullptr) {
        return "nullptr_stmt";
    }

    return getId((void *)addr, id);
}

const std::string clava::getId(const Expr *addr, int id) {
    if (addr == nullptr) {
        return "nullptr_expr";
    }

    return getId((void *)addr, id);
}

const std::string clava::getId(Optional<const Expr *> addr, int id) {
    if (!addr.hasValue()) {
        return "nullptr_expr";
    }

    return getId(addr.getValue(), id);
}

const std::string clava::getId(const Type *addr, int id) {
    if (addr == nullptr) {
        return "nullptr_type";
    }

    return getId((void *)addr, id);
}

const std::string clava::getId(const QualType &addr, int id) {
    if (addr.isNull()) {
        return "nullptr_type";
    }

    return getId(addr.getAsOpaquePtr(), id);
}

const std::string clava::getId(const Attr *addr, int id) {
    if (addr == nullptr) {
        return "nullptr_attr";
    }

    return getId((void *)addr, id);
}

void clava::dump(bool boolean) { llvm::errs() << boolean << "\n"; }

void clava::dump(int integer) { llvm::errs() << integer << "\n"; }

void clava::dump(double aDouble) { llvm::errs() << aDouble << "\n"; }

void clava::dump(unsigned int integer) { llvm::errs() << integer << "\n"; }

void clava::dumpSize(size_t integer) { llvm::errs() << integer << "\n"; }

void clava::dump(const std::string &string) { llvm::errs() << string << "\n"; }

void clava::dump(const llvm::StringRef string) {
    llvm::errs() << string << "\n";
}

void clava::dump(const char string[]) { dump(std::string(string)); }

void clava::dump(const std::vector<std::string> &strings) {
    // Number of attributes
    dump((unsigned int)strings.size());

    // Dump each attribute address
    for (auto string : strings) {
        dump(string);
    }
}

void clava::dump(const std::vector<Attr *> &attributes, const int id) {
    // Number of attributes
    dump((unsigned int)attributes.size());

    // Dump each attribute address
    for (auto attr : attributes) {
        dump(clava::getId(attr, id));
        dump(attr->isImplicit());
        dump(attr->isInherited());
        dump(attr->isLateParsed());
        dump(attr->isPackExpansion());
    }
}

void clava::dump(const QualType &type, int id) {
    //  QUALTYPE EXP
    dump(getId(type, id));
}

void clava::dump(const Qualifiers &qualifiers, ASTContext *Context) {
    auto c99Qualifiers = qualifiers.getCVRQualifiers();
    const int numBits = std::numeric_limits<decltype(c99Qualifiers)>::digits;
    size_t numSetBits = std::bitset<numBits>(c99Qualifiers).count();

    // Dumps the number of C99 qualifiers, and then the name of the qualifiers
    clava::dump((int)numSetBits);
    if (qualifiers.hasConst()) {
        clava::dump("CONST");
    }
    if (qualifiers.hasRestrict()) {
        if (Context->getPrintingPolicy().Restrict)
            clava::dump("RESTRICT_C99");
        else
            clava::dump("RESTRICT");
    }
    if (qualifiers.hasVolatile()) {
        clava::dump("VOLATILE");
    }
}

/**
 * Taken from:
 * https://stackoverflow.com/questions/11083066/getting-the-source-behind-clangs-ast
 * @param Context
 * @param start
 * @param end
 * @return
 */
const std::string clava::getSource(ASTContext *Context,
                                   SourceRange sourceRange) {

    // Mark beginning of source
    clava::dump("%CLAVA_SOURCE_BEGIN%");

    const SourceManager &sm = Context->getSourceManager();

    SourceLocation begin = sourceRange.getBegin();
    SourceLocation end = sourceRange.getEnd();
    if (begin.isMacroID()) {
        begin = sm.getSpellingLoc(begin);
    } else {
    }

    if (end.isMacroID()) {
        end = sm.getSpellingLoc(end);
    } else {
    }

    std::string text =
        Lexer::getSourceText(CharSourceRange::getTokenRange(begin, end), sm,
                             LangOptions(), 0)
            .str();
    if (text.size() > 0 &&
        (text.at(text.size() - 1) == ',')) { // the text can be ""
        std::string otherText =
            Lexer::getSourceText(CharSourceRange::getCharRange(begin, end), sm,
                                 LangOptions(), 0)
                .str();
        return otherText + "\n%CLAVA_SOURCE_END%";
    }

    return text + "\n%CLAVA_SOURCE_END%";
}

void clava::dump(NestedNameSpecifier *qualifier, ASTContext *Context) {

    // Dump qualifier
    if (qualifier != nullptr) {
        std::string qualifierStr;
        llvm::raw_string_ostream qualifierStream(qualifierStr);
        qualifier->print(qualifierStream, Context->getPrintingPolicy());
        clava::dump(qualifierStream.str());
    } else {
        clava::dump("");
    }
}

void clava::dump(NestedNameSpecifier *qualifier, int id) {
    auto qualifierKind = qualifier->getKind();

    // Dump name
    clava::dump(clava::NESTED_NAMED_SPECIFIER[qualifier->getKind()]);
    switch (qualifierKind) {
    case clang::NestedNameSpecifier::Namespace:
        clava::dump(getId(qualifier->getAsNamespace(), id));
        break;
    case clang::NestedNameSpecifier::NamespaceAlias:
        clava::dump(getId(qualifier->getAsNamespaceAlias(), id));
        break;
    case clang::NestedNameSpecifier::TypeSpec:
        clava::dump(getId(qualifier->getAsType(), id));
        break;
    case clang::NestedNameSpecifier::TypeSpecWithTemplate:
        clava::dump(getId(qualifier->getAsType(), id));
        break;
    case clang::NestedNameSpecifier::Global:
        break;
    case clang::NestedNameSpecifier::Super:
        clava::dump(getId(qualifier->getAsRecordDecl(), id));
        break;
    default:
        throw std::invalid_argument(
            "ClangNodes::dump(NestedNameSpecifier):: Case not implemented, '" +
            clava::NESTED_NAMED_SPECIFIER[qualifier->getKind()] + "'");
    }
}

void clava::dump(const TemplateArgument &templateArg, int id,
                 ASTContext *Context) {
    SmallString<0> str;

    clava::dump(clava::TEMPLATE_ARG_KIND[templateArg.getKind()]);

    switch (templateArg.getKind()) {
    case TemplateArgument::ArgKind::Type:
        clava::dump(clava::getId(templateArg.getAsType(), id));
        break;
    case TemplateArgument::ArgKind::Expression:
        clava::dump(clava::getId(templateArg.getAsExpr(), id));
        break;
    case TemplateArgument::ArgKind::Pack:
        clava::dump(templateArg.pack_size());
        for (auto currentArg = templateArg.pack_begin(),
                  endArg = templateArg.pack_end();
             currentArg != endArg; ++currentArg) {
            clava::dump(*currentArg, id, Context);
        }
        break;
    case TemplateArgument::ArgKind::Integral:
        templateArg.getAsIntegral().toString(str, 10);
        clava::dump(str);
        break;
    case TemplateArgument::ArgKind::Template:
        clava::dump(templateArg.getAsTemplate(), id, Context);
        break;

    default:
        throw std::invalid_argument(
            "ClangNodes::dump(TemplateArgument&): Case not implemented, '" +
            clava::TEMPLATE_ARG_KIND[templateArg.getKind()] + "' (" +
            std::to_string(templateArg.getKind()) + ")");
    }
}

void clava::dump(const TemplateName &templateName, int id,
                 ASTContext *Context) {
    clava::dump(clava::TEMPLATE_NAME_KIND[templateName.getKind()]);

    switch (templateName.getKind()) {
    case TemplateName::NameKind::Template:
        clava::dump(clava::getId(templateName.getAsTemplateDecl(), id));
        break;
    case TemplateName::NameKind::QualifiedTemplate:
        clava::dump(templateName.getAsQualifiedTemplateName()->getQualifier(),
                    Context);
        clava::dump(
            templateName.getAsQualifiedTemplateName()->hasTemplateKeyword());
        clava::dump(clava::getId(
            templateName.getAsQualifiedTemplateName()->getTemplateDecl(), id));
        break;
    case TemplateName::NameKind::SubstTemplateTemplateParm:
        clava::dump(clava::getId(
            templateName.getAsSubstTemplateTemplateParm()->getParameter(), id));
        clava::dump(
            templateName.getAsSubstTemplateTemplateParm()->getReplacement(), id,
            Context);
        break;
    default:
        throw std::invalid_argument(
            "ClangNodes::dump(TemplateName&): TemplateName case in kind "
            "'Template' not implemented, '" +
            clava::TEMPLATE_NAME_KIND[templateName.getKind()] + "'");
    }
}

void clava::dump(const CXXBaseSpecifier &base, int id) {
    clava::dump(base.isVirtual());
    clava::dump(base.isPackExpansion());
    clava::dump(ACCESS_SPECIFIER[base.getAccessSpecifierAsWritten()]);
    clava::dump(ACCESS_SPECIFIER[base.getAccessSpecifier()]);
    clava::dump(clava::getId(base.getType(), id));
}

void clava::dump(std::function<void(llvm::raw_string_ostream &)> dumper) {

    // Example of call to this function:
    // clava::dump([&T](llvm::raw_string_ostream&
    // stream){T->getTemplateName().dump(stream);});

    std::string string;
    llvm::raw_string_ostream stream(string);
    dumper(stream);
    clava::dump(stream.str());
}

bool clava::isSystemHeader(const Stmt *S, ASTContext *Context) {
    FullSourceLoc fullLocation = Context->getFullLoc(S->getBeginLoc());
    return fullLocation.isValid() && fullLocation.isInSystemHeader();
}

bool clava::isSystemHeader(const Decl *D, ASTContext *Context) {
    FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());
    return fullLocation.isValid() && fullLocation.isInSystemHeader();
}

/**
 * Taken from here:
 * https://stackoverflow.com/questions/874134/find-if-string-ends-with-another-string-in-c#874160
 *
 * @param str
 * @param suffix
 * @return
 */
static bool endsWith(const std::string &str, const std::string &suffix) {
    return str.size() >= suffix.size() &&
           0 == str.compare(str.size() - suffix.size(), suffix.size(), suffix);
}

/**
 *  Taken from here:
 * https://stackoverflow.com/questions/874134/find-if-string-ends-with-another-string-in-c#874160
 *
 * @param str
 * @param prefix
 * @return
 */
static bool startsWith(const std::string &str, const std::string &prefix) {
    return str.size() >= prefix.size() &&
           0 == str.compare(0, prefix.size(), prefix);
}

const std::string clava::getQualifiedPrefix(const NamedDecl *D) {
    const std::string qualifiedName = D->getQualifiedNameAsString();
    const std::string declName = D->getDeclName().getAsString();

    // If declName is empty, return empty string
    if (declName.empty()) {
        return "";
    }

    // If declName is the same as the qualified name, return empty string
    if (declName == qualifiedName) {
        return "";
    }

    // Remove decl name and :: from qualified name
    const std::string expectedSuffix = "::" + declName;
    if (!endsWith(qualifiedName, expectedSuffix)) {
        std::string message = "ClangNodes::getQualifiedPrefix(const NamedDecl "
                              "*): Expected string '" +
                              qualifiedName + "' to have the suffix '" +
                              expectedSuffix + "'";
        throw std::invalid_argument(message);
    }

    int endIndex = qualifiedName.length() - declName.length() - 2;

    return qualifiedName.substr(0, endIndex);
}

void clava::dump(const clang::DesignatedInitExpr::Designator *designator) {

    if (designator->isFieldDesignator()) {
        // Dump kind
        clava::dump(clava::DESIGNATOR_KIND[0]);
        clava::dump(designator->getFieldName()->getName());
    } else if (designator->isArrayDesignator()) {
        // Dump kind
        clava::dump(clava::DESIGNATOR_KIND[1]);
        clava::dump(designator->getFirstExprIndex());
    } else if (designator->isArrayRangeDesignator()) {
        // Dump kind
        clava::dump(clava::DESIGNATOR_KIND[2]);
        clava::dump(designator->getFirstExprIndex());
    } else {

        throw std::invalid_argument(
            "ClangNodes::dump(const clang::DesignatedInitExpr::Designator*): "
            "Case not implemented");
    }
}

void clava::dump(const ExplicitSpecifier &specifier, int id) {

    clava::dump(clava::EXPLICIT_SPEC_KIND, specifier.getKind());
    clava::dump(clava::getId(specifier.getExpr(), id));
    clava::dump(specifier.isSpecified());
}

void clava::throwNotImplemented(const std::string &source,
                                const std::string &caseNotImplemented,
                                ASTContext *Context, SourceLocation startLoc,
                                SourceLocation endLoc) {
    llvm::errs() << "Dumping source range of code that caused exception:\n";
    clava::dumpSourceRange(Context, startLoc, endLoc);
    throw std::invalid_argument(source + ": Case not implemented, '" +
                                caseNotImplemented +
                                "', source range has been dumped");
}

void clava::throwNotImplemented(const std::string &source,
                                const std::string &caseNotImplemented,
                                ASTContext *Context, SourceRange range) {
    throw std::invalid_argument(source + ": Case not implemented, '" +
                                caseNotImplemented +
                                "', source code that triggered the problem:\n" +
                                clava::getSource(Context, range));
}
