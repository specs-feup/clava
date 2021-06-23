//
// Created by JoaoBispo on 21/03/2018.
//

#include "ClangNodes.h"
#include "ClangEnums.h"

#include "clang/Lex/Lexer.h"
#include "clang/AST/Attr.h"
#include "clang/Basic/SourceManager.h"


#include <bitset>


using namespace clang;

const std::string clava::getClassName(const Decl* D) {
    const std::string kindName = D->getDeclKindName();
    return kindName + "Decl";
}

const std::string clava::getClassName(const Stmt* S) {
    const std::string className = S->getStmtClassName();
    return className;
}

const std::string clava::getClassName(const Type* T) {
    const std::string kindName = T->getTypeClassName();
    return kindName + "Type";
}

const std::string clava::getClassName(const Attr* A) {
    const std::string kindName =  clava::ATTRIBUTES[A->getKind()];
    return kindName + "Attr";
}


void clava::dumpSourceRange(ASTContext *Context, SourceLocation startLoc, SourceLocation endLoc) {
    //llvm::errs() << "<SourceRange Dump>\n";

    //llvm::errs() << id << "\n";

    // All components of the source range will be dumped
    const SourceManager& SM = Context->getSourceManager();

    SourceLocation startSpellingLoc = SM.getSpellingLoc(startLoc);
    //PresumedLoc startPLoc = SM.getPresumedLoc(startSpellingLoc);

    if(startSpellingLoc.isInvalid()) {
    //if (startPLoc.isInvalid()) {
        llvm::errs() << "<invalid>\n";
        return;
    }


    // Dump start location
    llvm::errs() << SM.getFilename(startSpellingLoc) << "\n";
    llvm::errs() << SM.getSpellingLineNumber(startSpellingLoc) << "\n";
    llvm::errs() << SM.getSpellingColumnNumber(startSpellingLoc) << "\n";

//    llvm::errs() << startPLoc.getFilename() << "\n";
//    llvm::errs() << startPLoc.getLine() << "\n";
//    llvm::errs() << startPLoc.getColumn() << "\n";
    // ISMACRO: Temporarily disabled
	//llvm::errs() << startLoc.isMacroID() << "\n";


    if(startLoc == endLoc) {
        llvm::errs() << "<end>\n";
        return;
    }

    SourceLocation endSpellingLoc = SM.getSpellingLoc(endLoc);
    //PresumedLoc endPLoc = SM.getPresumedLoc(endSpellingLoc);

    if(endSpellingLoc.isInvalid()) {
    //if(endPLoc.isInvalid()) {
        llvm::errs() << "<end>\n";
        return;
    }

    //const char* endFilename = endPLoc.getFilename();
    StringRef endFilename = SM.getFilename(endSpellingLoc);
    //if(!endFilename) {
    if(endFilename.size() == 0) {
        endFilename = SM.getFilename(startSpellingLoc);
        //endFilename = startPLoc.getFilename();
    }

    //unsigned int endLine = endPLoc.getLine();
    unsigned int endLine = SM.getSpellingLineNumber(endSpellingLoc);
    if(!endLine) {
        //endLine = startPLoc.getLine();
        endLine = SM.getSpellingLineNumber(startSpellingLoc);
    }

    //unsigned int endCol = endPLoc.getColumn();
    unsigned int endCol = SM.getSpellingColumnNumber(endSpellingLoc);
    if(!endCol) {
        //endCol = startPLoc.getColumn();
        endCol = SM.getSpellingColumnNumber(startSpellingLoc);
    }

    // Dump end location
    llvm::errs() << endFilename << "\n";
    llvm::errs() << endLine << "\n";
    llvm::errs() << endCol << "\n";
	
	// ISMACRO: Temporarily disabled
    //llvm::errs() << endLoc.isMacroID() << "\n";
}

void clava::dumpSourceInfo(ASTContext *Context, SourceLocation begin, SourceLocation end) {


    // Original source range
    //clava::dumpSourceRange(Context, begin, end);
    clava::dumpSourceRange(Context, Context->getSourceManager().getExpansionLoc(begin), Context->getSourceManager().getExpansionLoc(end));

    // ISMACRO: Disable this when updating
    // If it is a macro
    bool isMacro = begin.isMacroID() || end.isMacroID();
    clava::dump(isMacro);

//    bool beginIsMacro = begin.isMacroID();
//    bool endIsMacro = end.isMacroID();
//    clava::dump(beginIsMacro);
//    clava::dump(endIsMacro);

	// ISMACRO: Disable this when updating
    // Spelling location, if macro
    if(isMacro) {
        clava::dumpSourceRange(Context, Context->getSourceManager().getSpellingLoc(begin), Context->getSourceManager().getSpellingLoc(end));
    }

    // If is in system header
    FullSourceLoc fullLocation = Context->getFullLoc(begin);


    if (fullLocation.isValid() && fullLocation.isInSystemHeader()) {
        clava::dump(true);
    } else {
        clava::dump(false);
    }

    //clava::dumpSourceRange(Context, Context->getFullLoc(begin).getExpansionLoc(), Context->getFullLoc(end).getExpansionLoc());
    //clava::dumpSourceRange(Context, Context->getSourceManager().getImmediateMacroCallerLoc(begin), Context->getSourceManager().getImmediateMacroCallerLoc(end));
    //clava::dumpSourceRange(Context, Context->getSourceManager().getExpansionLoc(begin), Context->getSourceManager().getExpansionLoc(end));

    //Context->getSourceManager().getCharacterData(begin)
    //const char *getCharacterData(SourceLocation SL, bool *Invalid = nullptr) const;
    // Original source range

}


const std::string clava::getId(const void* addr, int id) {
    //if(addr == nullptr || addr == 0) {
    //    return "nullptr";
    //}

    std::stringstream ss;
    ss <<  addr << "_" << id;

    return ss.str();
}

const std::string clava::getId(const Decl* addr, int id) {
    if(addr == nullptr) {
        return "nullptr_decl";
    }

    return getId((void*) addr, id);
}
/*
const std::string clava::getId(Decl* addr, int id) {
    return getId(const_cast<const Decl*>(addr), id);
}
 */

const std::string clava::getId(const Stmt* addr, int id) {
    if(addr == nullptr) {
        return "nullptr_stmt";
    }

    return getId((void*) addr, id);
}

const std::string clava::getId(const Expr* addr, int id) {
    if(addr == nullptr) {
        return "nullptr_expr";
    }

    return getId((void*) addr, id);
}

const std::string clava::getId(const Type* addr, int id) {
    if(addr == nullptr) {
        return "nullptr_type";
    }

    return getId((void*) addr, id);
}

const std::string clava::getId(const QualType &addr, int id) {
    if(addr.isNull()) {
        return "nullptr_type";
    }

    return getId(addr.getAsOpaquePtr(), id);
}

const std::string clava::getId(const Attr* addr, int id) {
    if(addr == nullptr) {
        return "nullptr_attr";
    }

    return getId((void*) addr, id);
}


void clava::dump(bool boolean) {
    llvm::errs() << boolean << "\n";
}

void clava::dump(int integer) {
    llvm::errs() << integer << "\n";
}

void clava::dump(double aDouble) {
    llvm::errs() << aDouble << "\n";
}

void clava::dump(unsigned int integer) {
    llvm::errs() << integer << "\n";
}

void clava::dumpSize(size_t integer) {
    llvm::errs() << integer << "\n";
}

void clava::dump(const std::string& string) {
    llvm::errs() << string << "\n";
}

void clava::dump(const char string[]) {
    dump(std::string(string));
}

void clava::dump(const std::vector<std::string> &strings) {
    // Number of attributes
    dump((unsigned int)strings.size());

    // Dump each attribute address
    for(auto string : strings) {
        dump(string);
    }
}

void clava::dump(const std::vector<Attr*> &attributes, const int id) {
    // Number of attributes
    dump((unsigned int)attributes.size());

    // Dump each attribute address
    for(auto attr : attributes) {
        dump(clava::getId(attr, id));
        dump(attr->isImplicit());
        dump(attr->isInherited());
        dump(attr->isLateParsed());
        dump(attr->isPackExpansion());
    }
/*
    // Dump each attribute
    for(auto attr : attributes) {
        dump(clava::ATTRIBUTES[attr->getKind()]);
        dump(attr->isImplicit());
        dump(attr->isInherited());
        dump(attr->isLateParsed());
        dump(attr->isPackExpansion());
    }
*/
}


void clava::dump(const QualType& type, int id) {
/*
    if(type.isNull()) {
        dump("nullptr");
    }
*/
    //dump(getId(type.getTypePtr(), id));
    // QUALTYPE EXP
    //dump(getId(type.getAsOpaquePtr(), id));
    dump(getId(type, id));

    // Check if QualType is the same as the underlying type
    /*
    if((void*) type.getTypePtr() == type.getAsOpaquePtr()) {

        llvm::errs() << "QualType " << getId(type.getAsOpaquePtr(), id) << " opaque ptr and type ptr are the same\n";
    } else {
        llvm::errs() << "QualType " << getId(type.getAsOpaquePtr(), id) << " type ptr is different -> " << getId(type.getTypePtr(), id) << "\n";
    }
     */

    // Dump QualType
    /*
    if(dumpType(T)) {
        return;
    }
    */




}

void clava::dump(const Qualifiers& qualifiers, ASTContext* Context) {
    auto c99Qualifiers = qualifiers.getCVRQualifiers();
    const int numBits = std::numeric_limits<decltype(c99Qualifiers)>::digits;
    size_t numSetBits = std::bitset<numBits>(c99Qualifiers).count();

    // Dumps the number of C99 qualifiers, and then the name of the qualifiers
    clava::dump((int) numSetBits);
    if(qualifiers.hasConst()) {clava::dump("CONST");}
    if(qualifiers.hasRestrict()) {
        //if(Context->getPrintingPolicy().LangOpts.C99) // LLVM 3.8
        if(Context->getPrintingPolicy().Restrict)
            clava::dump("RESTRICT_C99");
        else
            clava::dump("RESTRICT");
    }
    if(qualifiers.hasVolatile()) {clava::dump("VOLATILE");}

}



/**
 * Taken from: https://stackoverflow.com/questions/11083066/getting-the-source-behind-clangs-ast
 * @param Context
 * @param start
 * @param end
 * @return
 */
const std::string clava::getSource(ASTContext *Context, SourceRange sourceRange) {

    // Mark beginning of source
    clava::dump("%CLAVA_SOURCE_BEGIN%");

    //const SourceManager &sm = Context->getSourceManager();

  /*

    clang::SourceLocation b(start), _e(end);

    clang::SourceLocation e(clang::Lexer::getLocForEndOfToken(_e, 0, sm, Context->getLangOpts()));

    return std::string(sm.getCharacterData(b),
                                sm.getCharacterData(e)-sm.getCharacterData(b));
*/
    // (T, U) => "T,,"

    const SourceManager &sm = Context->getSourceManager();


    SourceLocation begin = sourceRange.getBegin();
    SourceLocation end = sourceRange.getEnd();
    if (begin.isMacroID()) {
        //llvm::errs() << "Begin is macro:" << begin.printToString(sm) << "\n";
        begin = sm.getSpellingLoc(begin);
        //llvm::errs() << "Begin spelling loc:" << begin.printToString(sm) << "\n";
    } else {
        //llvm::errs() << "Begin is not macro:" << begin.printToString(sm) << "\n";
    }

    if (end.isMacroID()) {
        //llvm::errs() << "End is macro:" << end.printToString(sm) << "\n";
        end = sm.getSpellingLoc(end);
        //llvm::errs() << "End spelling loc:" << end.printToString(sm) << "\n";
    } else {
        //llvm::errs() << "End is not macro:" << begin.printToString(sm) << "\n";
    }



    std::string text = Lexer::getSourceText(CharSourceRange::getTokenRange(begin, end), sm, LangOptions(), 0);
    if (text.size() > 0 && (text.at(text.size()-1) == ',')) { //the text can be ""
        std::string otherText = Lexer::getSourceText(CharSourceRange::getCharRange(begin, end), sm, LangOptions(), 0);
        return  otherText + "\n%CLAVA_SOURCE_END%";
    }

    return text + "\n%CLAVA_SOURCE_END%";


}

void clava::dump(NestedNameSpecifier* qualifier, ASTContext* Context) {

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

void clava::dump(const TemplateArgument &templateArg, int id, ASTContext* Context) {
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
            for (auto currentArg = templateArg.pack_begin(), endArg = templateArg.pack_end();
                 currentArg != endArg; ++currentArg) {
                clava::dump(*currentArg, id, Context);
            }
            break;
        case TemplateArgument::ArgKind::Integral:
            //bool isSigned = templateArg.getAsIntegral().isSigned();
            clava::dump(templateArg.getAsIntegral().toString(10));
//    const std::string source = getSource(E);
//    clava::dump(source);
            break;
        case TemplateArgument::ArgKind::Template:
            clava::dump(templateArg.getAsTemplate(), id, Context);
            break;
            /*
        {
            TemplateName templateName = templateArg.getAsTemplate();


            break;
        }
             */
        //case TemplateArgument::ArgKind::Qualified
        default:
            throw std::invalid_argument("ClangNodes::dump(TemplateArgument&): Case not implemented, '" +
                                        clava::TEMPLATE_ARG_KIND[templateArg.getKind()] + "' (" + std::to_string(templateArg.getKind()) +")");
    }
}

void clava::dump(const TemplateName &templateName, int id, ASTContext* Context) {
    clava::dump(clava::TEMPLATE_NAME_KIND[templateName.getKind()]);

    switch(templateName.getKind()) {
        case TemplateName::NameKind::Template:
            clava::dump(clava::getId(templateName.getAsTemplateDecl(), id));
            break;
        case TemplateName::NameKind::QualifiedTemplate:
            clava::dump(templateName.getAsQualifiedTemplateName()->getQualifier(), Context);
            clava::dump(templateName.getAsQualifiedTemplateName()->hasTemplateKeyword());
            clava::dump(clava::getId(templateName.getAsQualifiedTemplateName()->getTemplateDecl(), id));
            break;
        case TemplateName::NameKind::SubstTemplateTemplateParm:
            clava::dump(clava::getId(templateName.getAsSubstTemplateTemplateParm()->getParameter(), id));
            clava::dump(templateName.getAsSubstTemplateTemplateParm()->getReplacement(), id, Context);
            break;
        default:
            throw std::invalid_argument("ClangNodes::dump(TemplateName&): TemplateName case in kind 'Template' not implemented, '" +
                                        clava::TEMPLATE_NAME_KIND[templateName.getKind()] + "'");
    }
}



void clava::dump(const CXXBaseSpecifier& base, int id) {
     clava::dump(base.isVirtual());
     clava::dump(base.isPackExpansion());
     clava::dump(ACCESS_SPECIFIER[base.getAccessSpecifierAsWritten()]);
     clava::dump(ACCESS_SPECIFIER[base.getAccessSpecifier()]);
     clava::dump(clava::getId(base.getType(), id));
}

/*
void dump(Consumer<Stream> consumer) {
    std::string string;
    llvm::raw_string_ostream stream(string);
    consumer.apply(stream);
    clava::dump(stream.str());
}
*/

void clava::dump(std::function<void(llvm::raw_string_ostream&)> dumper) {

    // Example of call to this function:
    // clava::dump([&T](llvm::raw_string_ostream& stream){T->getTemplateName().dump(stream);});

    std::string string;
    llvm::raw_string_ostream stream(string);
    dumper(stream);
    clava::dump(stream.str());

}

/*
void clava::dump(const TemplateName& templateName) {

    //dump(stream -> templateName.(stream));

    std::string string;
    llvm::raw_string_ostream stream(string);
    templateName.dump(stream);
    clava::dump(stream.str());
}
 */


/*
llvm::raw_ostream clava::stringStream() {
    std::string stringStream;
    llvm::raw_string_ostream llvmStringStream(stringStream);
    return llvmStringStream;
}
 */
/*
void clava::dump(llvm::raw_string_ostream llvmStringStream) {
    dump(llvmStringStream.str());
}
*/

bool clava::isSystemHeader(const Stmt* S, ASTContext* Context) {
    FullSourceLoc fullLocation = Context->getFullLoc(S->getBeginLoc());
    return fullLocation.isValid() && fullLocation.isInSystemHeader();
}


bool clava::isSystemHeader(const Decl* D, ASTContext* Context) {
    FullSourceLoc fullLocation = Context->getFullLoc(D->getBeginLoc());
    return fullLocation.isValid() && fullLocation.isInSystemHeader();
}

/**
 * Taken from here: https://stackoverflow.com/questions/874134/find-if-string-ends-with-another-string-in-c#874160
 *
 * @param str
 * @param suffix
 * @return
 */
static bool endsWith(const std::string& str, const std::string& suffix)
{
    return str.size() >= suffix.size() && 0 == str.compare(str.size()-suffix.size(), suffix.size(), suffix);
}

/**
 *  Taken from here: https://stackoverflow.com/questions/874134/find-if-string-ends-with-another-string-in-c#874160
 *
 * @param str
 * @param prefix
 * @return
 */
static bool startsWith(const std::string& str, const std::string& prefix)
{
    return str.size() >= prefix.size() && 0 == str.compare(0, prefix.size(), prefix);
}

const std::string clava::getQualifiedPrefix(const NamedDecl *D) {
    const std::string qualifiedName = D->getQualifiedNameAsString();
    const std::string declName = D->getDeclName().getAsString();

    //llvm::errs() << "QUALIFIED NAME: " << qualifiedName << "\n";
    //llvm::errs() << "DECL NAME: " << declName << "\n";

    // If declName is empty, return empty string
    if(declName.empty()) {
        return "";
    }

    // If declName is the same as the qualified name, return empty string
    if(declName == qualifiedName) {
        return "";
    }



    // Remove decl name and :: from qualified name
    const std::string expectedSuffix = "::" + declName;
    if(!endsWith(qualifiedName, expectedSuffix)) {
        std::string message = "ClangNodes::getQualifiedPrefix(const NamedDecl *): Expected string '" + qualifiedName +
                "' to have the suffix '" + expectedSuffix + "'";
        throw std::invalid_argument(message);
        //throw std::invalid_argument("ClangNodes::getQualifiedPrefix(const NamedDecl *): Expected string '"+qualifiedName+"' to have the suffix '" +
         //                                   expectedSuffix + "'");
    }

    int endIndex = qualifiedName.length() - declName.length() - 2;

    return qualifiedName.substr(0, endIndex);
}

void clava::dump(const clang::DesignatedInitExpr::Designator* designator) {

    if(designator->isFieldDesignator()) {
        // Dump kind
        clava::dump(clava::DESIGNATOR_KIND[0]);
        clava::dump(designator->getFieldName()->getName());
    } else if(designator->isArrayDesignator()) {
        // Dump kind
        clava::dump(clava::DESIGNATOR_KIND[1]);
        clava::dump(designator->getFirstExprIndex());
    } else if(designator->isArrayRangeDesignator()) {
        // Dump kind
        clava::dump(clava::DESIGNATOR_KIND[2]);
        clava::dump(designator->getFirstExprIndex());
    } else {

        throw  std::invalid_argument("ClangNodes::dump(const clang::DesignatedInitExpr::Designator*): Case not implemented");
    }
}

void clava::dump(const ExplicitSpecifier& specifier) {
    // TODO
    specifier.getKind();
    specifier.getExpr();
    specifier.isSpecified();
    specifier.isExplicit();
    specifier.isInvalid();
}


