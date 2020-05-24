/**
 * Copyright 2016 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.weaver.importable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.LinkageSpecDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.TypedefDecl;
import pt.up.fe.specs.clava.ast.decl.ValueDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.enums.LanguageId;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.ParenExpr;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.FloatKind;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.omp.OmpDirectiveKind;
import pt.up.fe.specs.clava.ast.stmt.BreakStmt;
import pt.up.fe.specs.clava.ast.stmt.CaseStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.NullType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.ast.type.enums.ElaboratedTypeKeyword;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.parsing.omp.OmpParser;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ABinaryOp;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACall;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACast;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AElaboratedType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFile;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunctionType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AIf;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ANamedDecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStruct;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATypedefDecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVarref;
import pt.up.fe.specs.clava.weaver.joinpoints.CxxFunction;
import pt.up.fe.specs.util.Preconditions;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;

public class AstFactory {

    /**
     * Creates a joinpoint representing a variable declaration with the given name and initialization.
     *
     * @param varName
     * @param joinpoint
     * @return
     */
    public static AJoinPoint varDecl(String varName, AJoinPoint init) {

        // Check that init is an expression
        ClavaNode expr = init.getNode();
        if (!(expr instanceof Expr)) {
            SpecsLogs.msgInfo("CxxFactory.varDecl: parameter 'init' must be of type expression, it is of type '"
                    + expr.getNodeName() + "'");
            return null;
        }

        Expr initExpr = (Expr) expr;

        // boolean isUsed = true;
        // boolean isImplicit = false;
        // boolean isNrvo = false;
        Type initType = (Type) init.getTypeImpl().getNode();

        // System.out.println("INIT JP:" + init.getClass());
        // System.out.println("INIT expr:" + initExpr);
        // System.out.println("INIT expr TYPE:" + initExpr.getType());
        // System.out.println("INIT TYPE:" + initType);
        DataStore config = CxxWeaver.getCxxWeaver().getConfig();

        // Check if C or C++
        Standard standard = config.get(ClavaOptions.STANDARD);

        Type type = getVarDeclType(standard, initType);

        // VarDeclData varDeclData = new VarDeclData(StorageClass.NONE, TLSKind.NONE, false, isNrvo,
        // InitializationStyle.CINIT, false);
        // DeclData declData = new DeclData(false, isImplicit, isUsed, false, false, false);
        // VarDecl varDecl = ClavaNodeFactory.varDecl(varDeclData, varName, type, declData,
        // ClavaNodeInfo.undefinedInfo(),
        // initExpr);

        VarDecl varDecl = CxxWeaver.getFactory().varDecl(varName, type);
        varDecl.setInit(initExpr);

        return CxxJoinpoints.create(varDecl, AVardecl.class);
    }

    /**
     * Creates a joinpoint representing a variable declaration with the given name and initialization.
     *
     * @param varName
     * @param joinpoint
     * @return
     */
    public static AJoinPoint varDeclNoInit(String varName, AType type) {

        // VarDecl varDecl = ClavaNodeFactory.varDecl(varName, (Type) type.getNode());
        VarDecl varDecl = CxxWeaver.getFactory().varDecl(varName, (Type) type.getNode());
        return CxxJoinpoints.create(varDecl, AVardecl.class);
    }

    private static Type getVarDeclType(Standard standard, Type returnType) {
        // Special case, NullType
        if (returnType instanceof NullType) {
            if (!standard.isCxx()) {
                throw new RuntimeException("Found NullType and we are on C standard, cannot use 'auto'");
            }

            // If C++, use auto as type

            String autoCode = "auto";

            // Check if reference type
            //
            // if (returnType instanceof ReferenceType) {
            // autoCode = autoCode + "&";
            // }

            return CxxWeaver.getFactory().literalType(autoCode);
        }

        return returnType;
    }

    public static CxxFunction functionVoid(String name) {

        BuiltinType voidType = CxxWeaver.getFactory().builtinType(BuiltinKind.Void);
        FunctionProtoType functionType = CxxWeaver.getFactory().functionProtoType(voidType);
        // FunctionProtoType functionType = ClavaNodeFactory.functionProtoType(new FunctionProtoTypeData(),
        // new FunctionTypeData(), new TypeData("void(void)"), ClavaNodeInfo.undefinedInfo(), voidType,
        // Collections.emptyList());
        // FunctionDecl functionDecl = ClavaNodeFactory.functionDecl(name, Arrays.asList(), functionType,
        // new FunctionDeclData(), new DeclData(),
        // ClavaNodeInfo.undefinedInfo(),
        // CxxWeaver.getFactory().compoundStmt());

        FunctionDecl functionDecl = CxxWeaver.getFactory().functionDecl(name, functionType);
        functionDecl.setBody(CxxWeaver.getFactory().compoundStmt());

        return (CxxFunction) CxxJoinpoints.create(functionDecl);
    }

    public static AStatement stmtLiteral(String code) {
        return CxxJoinpoints.create(CxxWeaver.getSnippetParser().parseStmt(code), AStatement.class);
    }

    public static AType typeLiteral(String code) {
        return CxxJoinpoints.create(CxxWeaver.getFactory().literalType(code), AType.class);
    }

    public static ADecl declLiteral(String code) {
        return CxxJoinpoints.create(CxxWeaver.getFactory().literalDecl(code), ADecl.class);
    }

    public static AExpression exprLiteral(String code) {
        return exprLiteral(code,
                CxxJoinpoints.create(CxxWeaver.getFactory().nullType()));
    }

    public static AExpression exprLiteral(String code, AJoinPoint type) {
        Type astType = type instanceof AType ? (Type) type.getNode()
                : CxxWeaver.getFactory().nullType();

        // Type astType = type == null ? ClavaNodeFactory.nullType(ClavaNodeInfo.undefinedInfo()) : type.getNode();

        return CxxJoinpoints.create(CxxWeaver.getFactory().literalExpr(code, astType), AExpression.class);
    }

    public static ACall callFromFunction(AFunction function, AJoinPoint... args) {
        return call(function.getNameImpl(), function.getFunctionTypeImpl(), args);
    }

    public static ACall call(String functionName, AType typeJp, AJoinPoint... args) {

        Type returnType = (Type) typeJp.getNode();

        // DeclRefExpr declRef = ClavaNodeFactory.declRefExpr(functionName, returnType);
        DeclRefExpr declRef = CxxWeaver.getFactory().declRefExpr(functionName, returnType);

        List<Type> argTypes = Arrays.stream(args)
                .map(arg -> ((Typable) arg.getNode()).getType())
                .collect(Collectors.toList());

        // FunctionTypeData fData = new FunctionTypeData(Types.isVoid(returnType), false, false, null, CallingConv.C);
        // FunctionProtoType type = ClavaNodeFactory.functionProtoType(new FunctionProtoTypeData(), fData,
        // new TypeData(""), ClavaNodeInfo.undefinedInfo(), returnType, argTypes);

        FunctionProtoType type = CxxWeaver.getFactory().functionProtoType(returnType, argTypes);

        List<Expr> exprArgs = Arrays.stream(args)
                .map(arg -> (Expr) arg.getNode())
                .collect(Collectors.toList());

        CallExpr call = CxxWeaver.getFactory().callExpr(declRef, type, exprArgs);
        // CallExpr call = ClavaNodeFactory.callExpr(new ExprData(type, ValueKind.R_VALUE),
        // ClavaNodeInfo.undefinedInfo(),
        // declRef, exprArgs);

        return CxxJoinpoints.create(call, ACall.class);
    }

    /**
     * Creates a joinpoint representing an empty translation unit.
     *
     * @param varName
     * @param joinpoint
     * @return
     */
    public static AFile file(File file, String relativePath) {
        // Test if path is absolute
        if (relativePath != null && new File(relativePath).isAbsolute()) {
            ClavaLog.warning(
                    "Cannot use absolute paths for new 'file' join points, replacing '" + relativePath + "' with null");
            relativePath = null;
        }

        // TranslationUnit tUnit = ClavaNodeFactory.translationUnit(filename, path, Collections.emptyList());
        // New files do not have a path
        TranslationUnit tUnit = CxxWeaver.getFactory().translationUnit(file, Collections.emptyList());

        if (relativePath != null) {
            tUnit.setRelativePath(relativePath);
        }

        var fileJp = CxxJoinpoints.create(tUnit, AFile.class);

        // If file already exists, insert the code of the file literaly
        if (file.isFile()) {
            fileJp.getNode().setOptional(TranslationUnit.LITERAL_SOURCE, SpecsIo.read(file));
        }

        return fileJp;
    }

    /**
     * Overload that accepts the contents of the file, to be inserted literally.
     * 
     * @param filename
     * @param contents
     * @param relativePath
     * @return
     */
    public static AFile file(String filename, String contents, String relativePath) {
        var fileJp = file(new File(filename), relativePath);

        // Add contents
        fileJp.getNode().setOptional(TranslationUnit.LITERAL_SOURCE, contents);

        return fileJp;
    }

    /**
     * Overload that accepts a String instead of a File.
     *
     * @param filename
     * @param relativePath
     * @return
     */
    public static AFile file(String filename, String relativePath) {
        return file(new File(filename), relativePath);
    }

    public static AJoinPoint externC(AJoinPoint jpDecl) {
        // Allowed classes for now: CxxFunction
        // TODO: This might be expanded in the future
        boolean isFunction = jpDecl instanceof CxxFunction;
        if (!isFunction) {
            ClavaLog.warning(
                    "Constructor 'externC' does not support joinpoint of type '" + jpDecl.getJoinPointType() + "'");
            return null;
        }

        /*
        if (isFunction) {
            if (((CxxFunction) jpDecl).getNode().getFunctionDeclData().getStorageClass() == StorageClass.EXTERN) {
                return null;
            }
        }
        */

        // Check that node does not already has a parent LinkageSpecDecl
        ClavaNode decl = jpDecl.getNode();
        if (decl.getParent() instanceof LinkageSpecDecl) {
            ClavaLog.warning("Given joinpoint already is marked as 'extern \"C\"'");
            return null;
            // return CxxJoinpoints.newJoinpoint(decl.getParent(), null);
        }

        LinkageSpecDecl linkage = CxxWeaver.getFactory().linkageSpecDecl(LanguageId.C, (Decl) decl);
        // LinkageSpecDecl linkage = ClavaNodeFactory.linkageSpecialDecl(LanguageId.C, DeclData.empty(),
        // ClavaNodeInfo.undefinedInfo(), Arrays.asList(decl));

        return CxxJoinpoints.create(linkage);
    }

    // public static AJoinPoint whileLoop() {
    // ClavaNodeFactory.whileStmt(info, condition, thenStmt)
    // }

    // public static ACxxWeaverJoinPoint constArrayType(String typeCode, Standard standard, List<Integer> dims) {
    public static ACxxWeaverJoinPoint constArrayType(String typeCode, String standard, List<Integer> dims) {
        return constArrayType(CxxWeaver.getFactory().literalType(typeCode), standard, dims);
    }

    // public static ACxxWeaverJoinPoint constArrayType(Type outType, Standard standard, List<Integer> dims) {
    /**
     * TODO: Standard string not required
     *
     * @param outType
     * @param standardString
     * @param dims
     * @return
     */
    public static ACxxWeaverJoinPoint constArrayType(Type outType, String standardString, List<Integer> dims) {
        // Standard standard = Standard.getEnumHelper().fromValue(standardString);

        Preconditions.checkNotNull(dims);
        Preconditions.checkArgument(dims.size() > 0);

        Type inType = null;

        ListIterator<Integer> li = dims.listIterator(dims.size());
        while (li.hasPrevious()) {

            // ArrayTypeData arrayTypeData = new ArrayTypeData(ArraySizeType.NORMAL, Collections.emptyList(), standard);
            // TypeData typeData = new TypeData(outType.getCode());
            // ClavaNodeInfo info = ClavaNodeInfo.undefinedInfo();

            inType = outType;
            // outType = ClavaNodeFactory.constantArrayType(li.previous(), arrayTypeData, typeData, info, inType);
            outType = CxxWeaver.getFactory().constantArrayType(inType, li.previous());
        }

        return CxxJoinpoints.create(outType);
    }

    public static AType variableArrayType(AType elementType, AExpression sizeExpr) {
        Type variableArrayType = CxxWeaver.getFactory().variableArrayType((Type) elementType.getNode(),
                (Expr) sizeExpr.getNode());
        // Type variableArrayType = ClavaNodeFactory.variableArrayType((Type) elementType.getNode(),
        // (Expr) sizeExpr.getNode());

        return CxxJoinpoints.create(variableArrayType, AType.class);
    }

    public static AJoinPoint omp(String directiveName) {
        // Get directive
        OmpDirectiveKind kind = OmpDirectiveKind.getHelper().fromValue(directiveName);
        return CxxJoinpoints.create(OmpParser.newOmpPragma(kind, CxxWeaver.getContex()));

        // ClavaNodeFactory.wrapperStmt(ClavaNodeInfo.undefinedInfo(),
    }

    public static AStatement caseStmt(AExpression value, AStatement subStmt) {

        CaseStmt caseStmt = CxxWeaver.getFactory().caseStmt((Expr) value.getNode(),
                (Stmt) subStmt.getNode());

        return CxxJoinpoints.create(caseStmt, AStatement.class);
    }

    /**
     *
     * @param value
     * @param expr
     * @return a list with a case statement and a break statement
     */
    public static List<AStatement> caseFromExpr(AExpression value, AExpression expr) {
        // Create compound stmt
        ExprStmt exprStmt = CxxWeaver.getFactory().exprStmt((Expr) expr.getNode());
        BreakStmt breakStmt = CxxWeaver.getFactory().breakStmt();

        CompoundStmt compoundStmt = CxxWeaver.getFactory().compoundStmt(exprStmt);
        compoundStmt.setNaked(true);
        // InlineComment comment = ClavaNodeFactory.inlineComment("Case " + value.getCode(), false,
        // ClavaNodeInfo.undefinedInfo());
        // Stmt commentStmt = ClavaNodeFactory.wrapperStmt(ClavaNodeInfo.undefinedInfo(), comment);

        AStatement caseStmt = caseStmt(value, CxxJoinpoints.create(compoundStmt, AStatement.class));

        return Arrays.asList(caseStmt,
                CxxJoinpoints.create(breakStmt, AStatement.class));

    }

    public static AStatement switchStmt(AExpression condition, AStatement body) {
        Stmt switchStmt = CxxWeaver.getFactory().switchStmt((Expr) condition.getNode(), (Stmt) body.getNode());

        return CxxJoinpoints.create(switchStmt, AStatement.class);
    }

    public static AStatement switchStmt(AExpression condition, AExpression[] cases) {
        if (cases.length % 2 != 0) {
            ClavaLog.info("The number of join points for the cases must be even (expression-stmt pairs)");
            return null;
        }

        // boolean invalidInput = Arrays.stream(cases)
        // .filter(aCase -> !(aCase instanceof AExpression))
        // .findFirst()
        // .isPresent();

        // if (invalidInput) {
        // ClavaLog.info("Expected all inputs to be 'expression' join points");
        // return null;
        // }

        List<Stmt> statements = new ArrayList<>();

        for (int i = 0; i < cases.length; i += 2) {

            statements.addAll(caseFromExpr(cases[i], cases[i + 1]).stream()
                    .map(aStmt -> (Stmt) aStmt.getNode())
                    .collect(Collectors.toList()));
            /*
            if (cases[i] instanceof AExpression) {
                ClavaLog.info("Expected argument " + (i + 2) + " to be an expression join point");
                return null;
            }
            
            if (cases[i + 1] instanceof AStatement) {
                ClavaLog.info("Expected argument " + (i + 3) + " to be a statement join point");
                return null;
            }
            */
        }

        CompoundStmt body = CxxWeaver.getFactory().compoundStmt(statements);
        Stmt switchStmt = CxxWeaver.getFactory().switchStmt((Expr) condition.getNode(), body);

        return CxxJoinpoints.create(switchStmt, AStatement.class);
    }

    ////// Methods that only use ClavaFactory

    public static ACxxWeaverJoinPoint builtinType(String typeCode) {
        // BuiltinKind kind = BuiltinKind.getHelper().fromValue(typeCode);

        BuiltinType type = CxxWeaver.getFactory().builtinType(typeCode);

        return CxxJoinpoints.create(type);
    }

    public static AExpression doubleLiteral(String floating) {
        return doubleLiteral(Double.parseDouble(floating));
    }

    public static AExpression doubleLiteral(double floating) {
        FloatingLiteral floatingLiteral = CxxWeaver.getFactory()
                .floatingLiteral(FloatKind.DOUBLE, floating);
        // Type type = CxxWeaver.getFactory().builtinType(BuiltinKind.DOUBLE);
        // FloatingLiteral intLiteral = ClavaNodeFactory.floatingLiteral(FloatKind.DOUBLE, Double.toString(floating),
        // new ExprData(type), ClavaNodeInfo.undefinedInfo());

        return CxxJoinpoints.create(floatingLiteral, AExpression.class);
    }

    public static ACxxWeaverJoinPoint longType() {
        BuiltinType type = CxxWeaver.getFactory().builtinType(BuiltinKind.Long);
        return CxxJoinpoints.create(type);
    }

    public static AExpression integerLiteral(String integer) {
        return integerLiteral(Integer.parseInt(integer));
    }

    public static AExpression integerLiteral(int integer) {
        IntegerLiteral intLiteral = CxxWeaver.getFactory().integerLiteral(integer);
        // Type intType = ClavaNodeFactory.builtinType(BuiltinKind.INT);
        // IntegerLiteral intLiteral = ClavaNodeFactory.integerLiteral(Integer.toString(integer), new ExprData(intType),
        // ClavaNodeInfo.undefinedInfo());

        return CxxJoinpoints.create(intLiteral, AExpression.class);
    }

    public static AScope scope() {
        return scope(new ArrayList<>());
    }

    public static AScope scope(List<? extends AStatement> statements) {
        List<Stmt> stmtNodes = SpecsCollections.map(statements, stmt -> (Stmt) stmt.getNode());
        return CxxJoinpoints.create(CxxWeaver.getFactory().compoundStmt(stmtNodes), AScope.class);

    }

    public static AVarref varref(String declName, AType type) {
        Type typeNode = (Type) type.getNode();
        return CxxJoinpoints.create(CxxWeaver.getFactory().declRefExpr(declName, typeNode), AVarref.class);
    }

    public static AVarref varref(ANamedDecl namedDecl) {
        NamedDecl decl = (NamedDecl) namedDecl.getNode();

        if (!(decl instanceof ValueDecl)) {
            ClavaLog.info(
                    "AstFactory.varref: Given node '" + decl.getClass() + "' is not compatible as input of varref");
            return null;
        }

        return CxxJoinpoints.create(CxxWeaver.getFactory().declRefExpr((ValueDecl) decl), AVarref.class);
    }

    public static AStatement returnStmt(AExpression expr) {
        return CxxJoinpoints.create(CxxWeaver.getFactory().returnStmt((Expr) expr.getNode()), AStatement.class);
    }

    public static AStatement returnStmt() {
        return CxxJoinpoints.create(CxxWeaver.getFactory().returnStmt(), AStatement.class);
    }

    public static AFunctionType functionType(AType returnTypeJp, AType... argTypesJps) {

        Type returnType = (Type) returnTypeJp.getNode();

        List<Type> argTypes = Arrays.stream(argTypesJps)
                .map(arg -> ((Type) arg.getNode()))
                .collect(Collectors.toList());

        FunctionProtoType type = CxxWeaver.getFactory().functionProtoType(returnType, argTypes);

        return CxxJoinpoints.create(type, AFunctionType.class);
    }

    public static AFunction functionDeclFromType(String functionName, AFunctionType functionTypeJp) {
        FunctionType functionType = (FunctionType) functionTypeJp.getNode();
        return CxxJoinpoints.create(CxxWeaver.getFactory().functionDecl(functionName, functionType),
                AFunction.class);
    }

    public static AFunction functionDecl(String functionName, AType returnTypeJp, AJoinPoint... namedDeclJps) {

        Type returnType = (Type) returnTypeJp.getNode();

        // Get the arg types and create the parameters
        List<Type> argTypes = new ArrayList<>(namedDeclJps.length);
        List<ParmVarDecl> params = new ArrayList<>();

        for (AJoinPoint namedDeclJp : namedDeclJps) {
            ClavaNode node = namedDeclJp.getNode();
            if (!(node instanceof ValueDecl)) {
                ClavaLog.info("AstFactory.functionDecl: decl '" + node.getClass()
                        + "' is not compatible as parameter of function");
                return null;
            }

            ValueDecl valueDecl = (ValueDecl) node;
            argTypes.add(valueDecl.getType());
            params.add(CxxWeaver.getFactory().parmVarDecl(valueDecl.getDeclName(), valueDecl.getType()));
        }

        // Create the function type
        FunctionProtoType functionType = CxxWeaver.getFactory().functionProtoType(returnType, argTypes);

        // Create function decl
        FunctionDecl functionDecl = CxxWeaver.getFactory().functionDecl(functionName, functionType);

        // Add parameters
        functionDecl.addChildren(params);

        return CxxJoinpoints.create(functionDecl, AFunction.class);
    }

    public static ABinaryOp assignment(AExpression leftHand, AExpression rightHand) {
        Expr lhs = (Expr) leftHand.getNode();
        Expr rhs = (Expr) rightHand.getNode();

        BinaryOperator assign = CxxWeaver.getFactory().binaryOperator(BinaryOperatorKind.Assign, rhs.getType(), lhs,
                rhs);

        return CxxJoinpoints.create(assign, ABinaryOp.class);
    }

    public static AIf ifStmt(AExpression condition, AStatement thenBody, AStatement elseBody) {

        var thenNode = thenBody != null ? ClavaNodes.toCompoundStmt((Stmt) thenBody.getNode()) : null;
        var elseNode = elseBody != null ? ClavaNodes.toCompoundStmt((Stmt) elseBody.getNode()) : null;
        // CompoundStmt emptyBody = CxxWeaver.getFactory().compoundStmt();
        // IfStmt ifStmt = CxxWeaver.getFactory().ifStmt((Expr) condition.getNode(), emptyBody);
        IfStmt ifStmt = CxxWeaver.getFactory().ifStmt((Expr) condition.getNode(), thenNode, elseNode);
        return CxxJoinpoints.create(ifStmt, AIf.class);
    }

    public static ABinaryOp binaryOp(String op, AExpression left, AExpression right, AType type) {

        BinaryOperatorKind opKind = BinaryOperator.getOpTry(op).orElse(null);
        if (opKind == null) {
            ClavaLog.info("binaryOp: operator '" + op + "' is not valid");
            return null;
        }

        BinaryOperator opNode = CxxWeaver.getFactory().binaryOperator(opKind, (Type) type.getNode(),
                (Expr) left.getNode(), (Expr) right.getNode());

        return CxxJoinpoints.create(opNode, ABinaryOp.class);
    }

    public static AExpression parenthesis(AExpression expression) {
        ParenExpr parenExpr = CxxWeaver.getFactory().parenExpr((Expr) expression.getNode());
        return CxxJoinpoints.create(parenExpr, AExpression.class);
    }

    /**
     * Creates a join point representing a type of a typedef.
     *
     * @param varName
     * @param joinpoint
     * @return
     */
    public static AType typedefType(ATypedefDecl typedefDecl) {
        var typedefType = CxxWeaver.getFactory().typedefType((TypedefDecl) typedefDecl.getNode());
        return CxxJoinpoints.create(typedefType, AType.class);
    }

    public static ATypedefDecl typedefDecl(AType underlyingType, String identifier) {
        var typedefDecl = CxxWeaver.getFactory().typedefDecl((Type) underlyingType.getNode(), identifier);
        return CxxJoinpoints.create(typedefDecl, ATypedefDecl.class);
    }

    public static AElaboratedType structType(AStruct struct) {
        var namedType = (Type) struct.getTypeImpl().getNode();
        var elaboratedType = CxxWeaver.getFactory().elaboratedType(ElaboratedTypeKeyword.STRUCT, namedType);

        return CxxJoinpoints.create(elaboratedType, AElaboratedType.class);
    }

    public static ACast cStyleCast(AType type, AExpression expr) {
        var cast = CxxWeaver.getFactory().cStyleCastExpr((Type) type.getNode(), (Expr) expr.getNode());

        return CxxJoinpoints.create(cast, ACast.class);
    }

}
