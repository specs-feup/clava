/**
 * Copyright 2016 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.importable;

import org.suikasoft.jOptions.Interfaces.DataStore;
import pt.up.fe.specs.clava.*;
import pt.up.fe.specs.clava.ast.decl.*;
import pt.up.fe.specs.clava.ast.decl.enums.LanguageId;
import pt.up.fe.specs.clava.ast.expr.*;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.expr.enums.FloatKind;
import pt.up.fe.specs.clava.ast.expr.enums.UnaryOperatorKind;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.omp.OmpDirectiveKind;
import pt.up.fe.specs.clava.ast.stmt.*;
import pt.up.fe.specs.clava.ast.type.*;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.ast.type.enums.ElaboratedTypeKeyword;
import pt.up.fe.specs.clava.language.AccessSpecifier;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.parsing.omp.OmpParser;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.*;
import pt.up.fe.specs.clava.weaver.joinpoints.CxxFunction;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsEnums;
import pt.up.fe.specs.util.Preconditions;
import pt.up.fe.specs.util.utilities.StringLines;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class AstFactory {

    /**
     * Creates a joinpoint representing a variable declaration with the given name and initialization.
     *
     * @param varName
     * @param joinpoint
     * @return
     */
    public static AJoinPoint varDecl(CxxWeaver weaver, String varName, AJoinPoint init) {

        // Check that init is an expression
        ClavaNode expr = init.getNode();
        if (!(expr instanceof Expr)) {
            SpecsLogs.msgInfo("CxxFactory.varDecl: parameter 'init' must be of type expression, it is of type '"
                    + expr.getNodeName() + "'");
            return null;
        }

        Expr initExpr = (Expr) expr;

        Type initType = (Type) init.getTypeImpl().getNode();

        DataStore config = weaver.getConfig();

        // Check if C or C++
        Standard standard = config.get(ClavaOptions.STANDARD);

        Type type = getVarDeclType(weaver, standard, initType);

        VarDecl varDecl = weaver.getFactory().varDecl(varName, type);
        varDecl.setInit(initExpr);

        return CxxJoinpoints.create(varDecl, weaver, AVardecl.class);
    }

    /**
     * Creates a joinpoint representing a variable declaration with the given name and initialization.
     *
     * @param varName
     * @param joinpoint
     * @return
     */
    public static AJoinPoint varDeclNoInit(CxxWeaver weaver, String varName, AType type) {
        VarDecl varDecl = weaver.getFactory().varDecl(varName, (Type) type.getNode());
        return CxxJoinpoints.create(varDecl, weaver, AVardecl.class);
    }

    private static Type getVarDeclType(CxxWeaver weaver, Standard standard, Type returnType) {

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

            return weaver.getFactory().literalType(autoCode);
        }

        return returnType;
    }

    public static CxxFunction functionVoid(CxxWeaver weaver, String name) {

        BuiltinType voidType = weaver.getFactory().builtinType(BuiltinKind.Void);
        FunctionProtoType functionType = weaver.getFactory().functionProtoType(voidType);

        FunctionDecl functionDecl = weaver.getFactory().functionDecl(name, functionType);
        functionDecl.setBody(weaver.getFactory().compoundStmt());

        return CxxJoinpoints.create(functionDecl, weaver, CxxFunction.class);
    }

    public static AStatement stmtLiteral(CxxWeaver weaver, String code) {
        return CxxJoinpoints.create(weaver.getSnippetParser().parseStmt(code), weaver, AStatement.class);
    }

    public static AType typeLiteral(CxxWeaver weaver, String code) {
        return CxxJoinpoints.create(weaver.getFactory().literalType(code), weaver, AType.class);
    }

    public static ADecl declLiteral(CxxWeaver weaver, String code) {
        return CxxJoinpoints.create(weaver.getFactory().literalDecl(code), weaver, ADecl.class);
    }

    public static AExpression exprLiteral(CxxWeaver weaver, String code) {
        return exprLiteral(weaver, code,
                CxxJoinpoints.create(weaver.getFactory().nullType(), weaver));
    }

    public static AExpression exprLiteral(CxxWeaver weaver, String code, AJoinPoint type) {
        Type astType = type instanceof AType ? (Type) type.getNode()
                : weaver.getFactory().nullType();

        return CxxJoinpoints.create(weaver.getFactory().literalExpr(code, astType), weaver, AExpression.class);
    }

    public static AExpression cxxConstructExpr(CxxWeaver weaver, AType type, Object[] constructorArguments) {
        return cxxConstructExpr(weaver, type, SpecsCollections.asListT(AJoinPoint.class, constructorArguments));
    }

    public static AExpression cxxConstructExpr(CxxWeaver weaver, AType type, List<AJoinPoint> constructorArguments) {
        List<Expr> exprArgs = constructorArguments.stream()
                .map(arg -> (Expr) arg.getNode())
                .collect(Collectors.toList());

        return CxxJoinpoints.create(weaver.getFactory().cxxConstructExpr((Type) type.getNode(), exprArgs), weaver, AExpression.class);
    }

    public static ACall callFromFunction(CxxWeaver weaver, AFunction function, Object[] args) {
        return callFromFunction(weaver, function, SpecsCollections.asListT(AJoinPoint.class, args));
    }

    public static ACall callFromFunction(CxxWeaver weaver, AFunction function, List<? extends AJoinPoint> args) {
        var functionDecl = (FunctionDecl) function.getNode();
        List<Expr> exprArgs = args.stream()
                .map(arg -> (Expr) arg.getNode())
                .collect(Collectors.toList());

        var call = weaver.getFactory().callExpr(functionDecl, exprArgs);

        return CxxJoinpoints.create(call, weaver, ACall.class);
    }

    public static ACall call(CxxWeaver weaver, String functionName, AType typeJp, Object[] args) {
        return call(weaver, functionName, typeJp, SpecsCollections.asListT(AJoinPoint.class, args));
    }


    public static ACall call(CxxWeaver weaver, String functionName, AType typeJp, List<AJoinPoint> args) {

        Type returnType = (Type) typeJp.getNode();

        DeclRefExpr declRef = weaver.getFactory().declRefExpr(functionName, returnType);
        List<Type> argTypes = args.stream()
                .map(arg -> ((Typable) arg.getNode()).getType())
                .collect(Collectors.toList());

        FunctionProtoType type = weaver.getFactory().functionProtoType(returnType, argTypes);

        List<Expr> exprArgs = args.stream()
                .map(arg -> (Expr) arg.getNode())
                .collect(Collectors.toList());

        CallExpr call = weaver.getFactory().callExpr(declRef, type, exprArgs);

        return CxxJoinpoints.create(call, weaver, ACall.class);
    }

    /**
     * Creates a joinpoint representing an empty translation unit.
     *
     * @param varName
     * @param joinpoint
     * @return
     */
    public static AFile file(CxxWeaver weaver, File file, String relativePath) {

        // Test if path is absolute
        if (relativePath != null && new File(relativePath).isAbsolute()) {
            ClavaLog.warning(
                    "Cannot use absolute paths for new 'file' join points, replacing '" + relativePath + "' with null");
            relativePath = null;
        }

        // New files do not have a path
        TranslationUnit tUnit = weaver.getFactory().translationUnit(file, Collections.emptyList());

        if (relativePath != null) {
            tUnit.setRelativePath(relativePath);
        }

        var fileJp = CxxJoinpoints.create(tUnit, weaver, AFile.class);

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
    public static AFile file(CxxWeaver weaver, String filename, String contents, String relativePath) {
        var fileJp = file(weaver, new File(filename), relativePath);

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
    public static AFile file(CxxWeaver weaver, String filename, String relativePath) {
        return file(weaver, new File(filename), relativePath);
    }

    public static AJoinPoint externC(CxxWeaver weaver, AJoinPoint jpDecl) {

        // Allowed classes for now: CxxFunction
        // TODO: This might be expanded in the future
        boolean isFunction = jpDecl instanceof CxxFunction;
        if (!isFunction) {
            ClavaLog.warning(
                    "Constructor 'externC' does not support joinpoint of type '" + jpDecl.getJoinPointType() + "'");
            return null;
        }

        // Check that node does not already has a parent LinkageSpecDecl
        ClavaNode decl = jpDecl.getNode();
        if (decl.getParent() instanceof LinkageSpecDecl) {
            ClavaLog.warning("Given joinpoint already is marked as 'extern \"C\"'");
            return null;
            // return CxxJoinpoints.newJoinpoint(decl.getParent(), null);
        }

        LinkageSpecDecl linkage = weaver.getFactory().linkageSpecDecl(LanguageId.C, (Decl) decl);

        return CxxJoinpoints.create(linkage, weaver);
    }

    public static ACxxWeaverJoinPoint constArrayType(CxxWeaver weaver,
            String typeCode, String standard, List<Integer> dims) {
        return constArrayType(weaver, weaver.getFactory().literalType(typeCode), standard, dims);
    }

    public static ACxxWeaverJoinPoint constArrayType(CxxWeaver weaver,
            String typeCode, String standard, Object[] dims) {
        return constArrayType(weaver, typeCode, standard, SpecsCollections.asListT(Integer.class, dims));
    }

    /**
     * TODO: Standard string not required
     *
     * @param outType
     * @param standardString
     * @param dims
     * @return
     */
    public static ACxxWeaverJoinPoint constArrayType(CxxWeaver weaver,
            Type outType, String standardString, List<Integer> dims) {

        Objects.requireNonNull(dims);
        Preconditions.checkArgument(dims.size() > 0);

        Type inType = null;

        ListIterator<Integer> li = dims.listIterator(dims.size());
        while (li.hasPrevious()) {
            inType = outType;
            outType = weaver.getFactory().constantArrayType(inType, li.previous());
        }

        return CxxJoinpoints.create(outType, weaver);
    }

    public static ACxxWeaverJoinPoint constArrayType(CxxWeaver weaver,
            Type outType, String standardString, Object[] dims) {
        return constArrayType(weaver, outType, standardString, SpecsCollections.asListT(Integer.class, dims));
    }


    public static AVariableArrayType variableArrayType(CxxWeaver weaver, AType elementType, AExpression sizeExpr) {
        Type variableArrayType = weaver.getFactory().variableArrayType((Type) elementType.getNode(),
                (Expr) sizeExpr.getNode());

        return CxxJoinpoints.create(variableArrayType, weaver, AVariableArrayType.class);
    }

    public static AIncompleteArrayType incompleteArrayType(CxxWeaver weaver, AType elementType) {
        Type incompleteArrayType = weaver.getFactory().incompleteArrayType(((Type) elementType.getNode()));
        return CxxJoinpoints.create(incompleteArrayType, weaver, AIncompleteArrayType.class);
    }

    public static AJoinPoint omp(CxxWeaver weaver, String directiveName) {

        // Get directive
        OmpDirectiveKind kind = OmpDirectiveKind.getHelper().fromValue(directiveName);
        return CxxJoinpoints.create(OmpParser.newOmpPragma(kind, weaver.getContex()), weaver);
    }

    public static AStatement caseStmt(CxxWeaver weaver, AExpression value) {

        CaseStmt caseStmt = weaver.getFactory().caseStmt((Expr) value.getNode());

        return CxxJoinpoints.create(caseStmt, weaver, AStatement.class);
    }

    public static AStatement defaultStmt(CxxWeaver weaver) {
        var defaultStmt = weaver.getFactory().defaultStmt();

        return CxxJoinpoints.create(defaultStmt, weaver, AStatement.class);
    }

    public static AStatement breakStmt(CxxWeaver weaver) {
        var breakStmt = weaver.getFactory().breakStmt();
        return CxxJoinpoints.create(breakStmt, weaver, AStatement.class);
    }

    /**
     * @param value
     * @param expr
     * @return a list with a case statement and a break statement
     */
    public static List<AStatement> caseFromExpr(CxxWeaver weaver, AExpression value, AExpression expr) {

        // Create compound stmt
        ExprStmt exprStmt = weaver.getFactory().exprStmt((Expr) expr.getNode());
        BreakStmt breakStmt = weaver.getFactory().breakStmt();
        var breakJp = CxxJoinpoints.create(breakStmt, weaver, AStatement.class);

        CompoundStmt compoundStmt = weaver.getFactory().compoundStmt(exprStmt);
        compoundStmt.setNaked(true);

        AStatement caseStmt = caseStmt(weaver, value);
        var compoundJp = CxxJoinpoints.create(compoundStmt, weaver, AStatement.class);
        return Arrays.asList(caseStmt, compoundJp, breakJp);

    }

    public static AStatement switchStmt(CxxWeaver weaver, AExpression condition, AStatement body) {
        Stmt switchStmt = weaver.getFactory().switchStmt((Expr) condition.getNode(), (Stmt) body.getNode());

        return CxxJoinpoints.create(switchStmt, weaver, AStatement.class);
    }

    public static AStatement switchStmt(CxxWeaver weaver, AExpression condition, Object[] casesArray) {
        var cases = SpecsCollections.cast(casesArray, AExpression.class);

        if (cases.length % 2 != 0) {
            ClavaLog.info("The number of join points for the cases must be even (expression-stmt pairs)");
            return null;
        }

        List<Stmt> statements = new ArrayList<>();

        for (int i = 0; i < cases.length; i += 2) {

            statements.addAll(caseFromExpr(weaver, cases[i], cases[i + 1]).stream()
                    .map(aStmt -> (Stmt) aStmt.getNode())
                    .collect(Collectors.toList()));
        }

        CompoundStmt body = weaver.getFactory().compoundStmt(statements);
        Stmt switchStmt = weaver.getFactory().switchStmt((Expr) condition.getNode(), body);

        return CxxJoinpoints.create(switchStmt, weaver, AStatement.class);
    }

    ////// Methods that only use ClavaFactory

    public static ACxxWeaverJoinPoint builtinType(CxxWeaver weaver, String typeCode) {
        BuiltinType type = weaver.getFactory().builtinType(typeCode);

        return CxxJoinpoints.create(type, weaver);
    }

    public static ACxxWeaverJoinPoint pointerTypeFromBuiltin(CxxWeaver weaver, String typeCode) {

        BuiltinType pointeeType = weaver.getFactory().builtinType(typeCode);
        PointerType pointerType = weaver.getFactory().pointerType(pointeeType);

        ACxxWeaverJoinPoint jp = CxxJoinpoints.create(pointerType, weaver);

        return jp;
    }

    public static ACxxWeaverJoinPoint pointerType(CxxWeaver weaver, AType pointeeType) {
        PointerType pointerType = weaver.getFactory().pointerType((Type) pointeeType.getNode());

        ACxxWeaverJoinPoint jp = CxxJoinpoints.create(pointerType, weaver);

        return jp;
    }

    public static AExpression doubleLiteral(CxxWeaver weaver, String floating) {
        return doubleLiteral(weaver, Double.parseDouble(floating));
    }

    public static AExpression doubleLiteral(CxxWeaver weaver, double floating) {
        FloatingLiteral floatingLiteral = weaver.getFactory()
                .floatingLiteral(FloatKind.DOUBLE, floating);

        return CxxJoinpoints.create(floatingLiteral, weaver, AExpression.class);
    }

    public static ACxxWeaverJoinPoint longType(CxxWeaver weaver) {
        BuiltinType type = weaver.getFactory().builtinType(BuiltinKind.Long);
        return CxxJoinpoints.create(type, weaver);
    }

    public static AExpression integerLiteral(CxxWeaver weaver, String integer) {
        return integerLiteral(weaver, Integer.parseInt(integer));
    }

    public static AExpression integerLiteral(CxxWeaver weaver, int integer) {
        IntegerLiteral intLiteral = weaver.getFactory().integerLiteral(integer);

        return CxxJoinpoints.create(intLiteral, weaver, AExpression.class);
    }

    public static AScope scope(CxxWeaver weaver) {
        return scope(weaver, Collections.emptyList());
    }

    public static AScope scope(CxxWeaver weaver, Object[] statements) {
        return scope(weaver, SpecsCollections.asListT(AStatement.class, statements));
    }

    public static AScope scope(CxxWeaver weaver, List<? extends AStatement> statements) {
        List<Stmt> stmtNodes = SpecsCollections.map(statements, stmt -> (Stmt) stmt.getNode());
        return CxxJoinpoints.create(weaver.getFactory().compoundStmt(stmtNodes), weaver, AScope.class);
    }

    public static AVarref varref(CxxWeaver weaver, String declName, AType type) {
        Type typeNode = (Type) type.getNode();
        return CxxJoinpoints.create(weaver.getFactory().declRefExpr(declName, typeNode), weaver, AVarref.class);
    }

    public static AVarref varref(CxxWeaver weaver, ANamedDecl namedDecl) {
        NamedDecl decl = (NamedDecl) namedDecl.getNode();

        if (!(decl instanceof ValueDecl)) {
            ClavaLog.info(
                    "AstFactory.varref: Given node '" + decl.getClass() + "' is not compatible as input of varref");
            return null;
        }

        return CxxJoinpoints.create(weaver.getFactory().declRefExpr((ValueDecl) decl), weaver, AVarref.class);
    }

    public static AStatement returnStmt(CxxWeaver weaver, AExpression expr) {
        return CxxJoinpoints.create(weaver.getFactory().returnStmt((Expr) expr.getNode()), weaver, AStatement.class);
    }

    public static AStatement returnStmt(CxxWeaver weaver) {
        return CxxJoinpoints.create(weaver.getFactory().returnStmt(), weaver, AStatement.class);
    }

    public static AFunctionType functionType(CxxWeaver weaver, AType returnTypeJp, Object[] argTypesJps) {
        return functionType(weaver, returnTypeJp, SpecsCollections.asListT(AType.class, argTypesJps));
    }


    public static AFunctionType functionType(CxxWeaver weaver, AType returnTypeJp, List<AType> argTypesJps) {

        Type returnType = (Type) returnTypeJp.getNode();

        List<Type> argTypes = argTypesJps.stream()
                .map(arg -> ((Type) arg.getNode()))
                .collect(Collectors.toList());

        FunctionProtoType type = weaver.getFactory().functionProtoType(returnType, argTypes);

        return CxxJoinpoints.create(type, weaver, AFunctionType.class);
    }

    public static AFunction functionDeclFromType(CxxWeaver weaver, String functionName, AFunctionType functionTypeJp) {
        FunctionType functionType = (FunctionType) functionTypeJp.getNode();
        return CxxJoinpoints.create(weaver.getFactory().functionDecl(functionName, functionType),
                weaver, AFunction.class);
    }

    public static AFunction functionDecl(CxxWeaver weaver, String functionName, AType returnTypeJp, List<AJoinPoint> namedDeclJps) {

        Type returnType = (Type) returnTypeJp.getNode();

        // Get the arg types and create the parameters
        List<Type> argTypes = new ArrayList<>(namedDeclJps.size());
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
            params.add(weaver.getFactory().parmVarDecl(valueDecl.getDeclName(), valueDecl.getType()));
        }

        // Create the function type
        FunctionProtoType functionType = weaver.getFactory().functionProtoType(returnType, argTypes);
        // Create function decl
        FunctionDecl functionDecl = weaver.getFactory().functionDecl(functionName, functionType);

        // Add parameters
        functionDecl.addChildren(params);

        return CxxJoinpoints.create(functionDecl, weaver, AFunction.class);
    }

    public static AFunction functionDecl(CxxWeaver weaver, String functionName, AType returnTypeJp, Object... namedDeclJps) {
        return functionDecl(weaver, functionName, returnTypeJp, SpecsCollections.asListT(AJoinPoint.class, namedDeclJps));
    }

    public static ABinaryOp assignment(CxxWeaver weaver, AExpression leftHand, AExpression rightHand) {
        Expr lhs = (Expr) leftHand.getNode();
        Expr rhs = (Expr) rightHand.getNode();

        BinaryOperator assign = weaver.getFactory().binaryOperator(BinaryOperatorKind.Assign, lhs.getType(), lhs,
                rhs);

        return CxxJoinpoints.create(assign, weaver, ABinaryOp.class);
    }

    public static AIf ifStmt(CxxWeaver weaver, AExpression condition, AStatement thenBody, AStatement elseBody) {
        var thenNode = thenBody != null ? ClavaNodes.toCompoundStmt((Stmt) thenBody.getNode()) : null;
        var elseNode = elseBody != null ? ClavaNodes.toCompoundStmt((Stmt) elseBody.getNode()) : null;

        IfStmt ifStmt = weaver.getFactory().ifStmt((Expr) condition.getNode(), thenNode, elseNode);
        return CxxJoinpoints.create(ifStmt, weaver, AIf.class);
    }

    public static ABinaryOp binaryOp(CxxWeaver weaver, String op, AExpression left, AExpression right, AType type) {

        BinaryOperatorKind opKind = BinaryOperator.getOpByNameOrSymbol(op);

        BinaryOperator opNode = weaver.getFactory().binaryOperator(opKind, (Type) type.getNode(),
                (Expr) left.getNode(), (Expr) right.getNode());

        return CxxJoinpoints.create(opNode, weaver, ABinaryOp.class);
    }

    public static ABinaryOp compoundAssignment(CxxWeaver weaver, String op, AExpression lhs, AExpression rhs) {
        var opKind = BinaryOperator.getOpByNameOrSymbol(op);
        var type = ((Expr) lhs.getNode()).getType();

        var opNode = weaver.getFactory().compoundAssignOperator(opKind, type, (Expr) lhs.getNode(),
                (Expr) rhs.getNode());

        return CxxJoinpoints.create(opNode, weaver, ABinaryOp.class);

    }

    public static AUnaryOp unaryOp(CxxWeaver weaver, String op, AExpression expr, AType type) {
        UnaryOperatorKind opKind = UnaryOperator.getOpByNameOrSymbol(op);

        // If type is null, try to infer type from operator
        var typeNode = type != null ? (Type) type.getNode()
                : Types.inferUnaryType(opKind, (Type) expr.getTypeImpl().getNode(), weaver.getFactory());

        UnaryOperator opNode = weaver.getFactory().unaryOperator(opKind, typeNode,
                (Expr) expr.getNode());

        return CxxJoinpoints.create(opNode, weaver, AUnaryOp.class);
    }

    public static ATernaryOp ternaryOp(CxxWeaver weaver, AExpression cond, AExpression trueExpr, AExpression falseExpr, AType type) {
        ConditionalOperator opNode = weaver.getFactory().conditionalOperator(
                (Type) type.getNode(),
                (Expr) cond.getNode(),
                (Expr) trueExpr.getNode(),
                (Expr) falseExpr.getNode());

        return CxxJoinpoints.create(opNode, weaver, ATernaryOp.class);
    }

    public static AExpression parenthesis(CxxWeaver weaver, AExpression expression) {
        ParenExpr parenExpr = weaver.getFactory().parenExpr((Expr) expression.getNode());
        return CxxJoinpoints.create(parenExpr, weaver, AExpression.class);
    }

    public static AArrayAccess arrayAccess(CxxWeaver weaver, AExpression base, List<AExpression> subscripts) {
        var subscriptsExpr = subscripts.stream()
                .map(arg -> ((Expr) arg.getNode()))
                .collect(Collectors.toList());

        var arraySubscriptExpr = weaver.getFactory().arraySubscriptExpr((Expr) base.getNode(), subscriptsExpr);
        return CxxJoinpoints.create(arraySubscriptExpr, weaver, AArrayAccess.class);
    }

    public static AArrayAccess arrayAccess(CxxWeaver weaver, AExpression base, Object[] subscripts) {
        return arrayAccess(weaver, base, SpecsCollections.asListT(AExpression.class, subscripts));
    }

    public static AInitList initList(CxxWeaver weaver, List<AExpression> values) {
        var valuesExpr = values.stream()
                .map(arg -> ((Expr) arg.getNode()))
                .collect(Collectors.toList());

        var initList = weaver.getFactory().initListExpr((valuesExpr));
        return CxxJoinpoints.create(initList, weaver, AInitList.class);
    }

    public static AInitList initList(CxxWeaver weaver, Object[] values) {
        return initList(weaver, SpecsCollections.asListT(AExpression.class, values));
    }

    /**
     * Creates a join point representing a type of a typedef.
     *
     * @param varName
     * @param joinpoint
     * @return
     */
    public static AType typedefType(CxxWeaver weaver, ATypedefDecl typedefDecl) {
        var typedefType = weaver.getFactory().typedefType((TypedefDecl) typedefDecl.getNode());
        return CxxJoinpoints.create(typedefType, weaver, AType.class);
    }

    public static ATypedefDecl typedefDecl(CxxWeaver weaver, AType underlyingType, String identifier) {
        var typedefDecl = weaver.getFactory().typedefDecl((Type) underlyingType.getNode(), identifier);
        return CxxJoinpoints.create(typedefDecl, weaver, ATypedefDecl.class);
    }

    public static AElaboratedType structType(CxxWeaver weaver, AStruct struct) {
        var namedType = (Type) struct.getTypeImpl().getNode();
        var elaboratedType = weaver.getFactory().elaboratedType(ElaboratedTypeKeyword.STRUCT, namedType);

        return CxxJoinpoints.create(elaboratedType, weaver, AElaboratedType.class);
    }

    public static ACast cStyleCast(CxxWeaver weaver, AType type, AExpression expr) {
        var cast = weaver.getFactory().cStyleCastExpr((Type) type.getNode(), (Expr) expr.getNode());

        return CxxJoinpoints.create(cast, weaver, ACast.class);
    }

    /**
     * Creates a join point representing a new class.
     *
     * @param varName
     * @param joinpoint
     * @return
     */
    public static AClass classDecl(CxxWeaver weaver, String className, List<AField> fields) {
        var fieldsNodes = fields.stream().map(field -> (FieldDecl) field.getNode())
                .collect(Collectors.toList());

        var classDecl = weaver.getFactory().cxxRecordDecl(className, fieldsNodes);
        return CxxJoinpoints.create(classDecl, weaver, AClass.class);
    }

    public static AClass classDecl(CxxWeaver weaver, String className, Object... fields) {
        return classDecl(weaver, className, SpecsCollections.asListT(AField.class, fields));
    }

    /**
     * Creates a join point representing a new field.
     *
     * @param fieldName
     * @param fieldType
     * @return
     */
    public static AField field(CxxWeaver weaver, String fieldName, AType fieldType) {
        var fieldDecl = weaver.getFactory().fieldDecl(fieldName, (Type) fieldType.getNode());
        return CxxJoinpoints.create(fieldDecl, weaver, AField.class);
    }

    /**
     * Creates a join point representing a new accessSpecifier.
     *
     * @param fieldName
     * @param fieldType
     * @return
     */
    public static AAccessSpecifier accessSpecifier(CxxWeaver weaver, String accessSpecifierString) {
        var accessSpecifier = SpecsEnums.fromName(AccessSpecifier.class, accessSpecifierString.toUpperCase());

        var accessSpecifierDecl = weaver.getFactory().accessSpecDecl(accessSpecifier);
        return CxxJoinpoints.create(accessSpecifierDecl, weaver, AAccessSpecifier.class);
    }

    public static ALoop forStmt(CxxWeaver weaver,
            AStatement init, AStatement condition, AStatement inc, AStatement body) {

        // If null, create NullStmt
        var initStmt = init != null ? (Stmt) init.getNode() : weaver.getFactory().nullStmt();
        var condStmt = condition != null ? (Stmt) condition.getNode() : weaver.getFactory().nullStmt();
        var incStmt = inc != null ? (Stmt) inc.getNode() : weaver.getFactory().nullStmt();
        var bodyStmt = body != null ? (Stmt) body.getNode() : weaver.getFactory().nullStmt();

        // If body is not a CompoundStmt, make it

        var compoundStmt = ClavaNodes.toCompoundStmt(bodyStmt);

        var forStmt = weaver.getFactory().forStmt(initStmt, condStmt, incStmt, compoundStmt);

        return CxxJoinpoints.create(forStmt, weaver, ALoop.class);
    }

    public static ALoop whileStmt(CxxWeaver weaver, AStatement condition, AStatement body) {
        var condStmt = condition != null ? (Stmt) condition.getNode() : weaver.getFactory().nullStmt();
        var bodyStmt = body != null ? (Stmt) body.getNode() : weaver.getFactory().nullStmt();

        var compoundStmt = ClavaNodes.toCompoundStmt(bodyStmt);

        var whileStmt = weaver.getFactory().whileStmt(condStmt, compoundStmt);
        return CxxJoinpoints.create(whileStmt, weaver, ALoop.class);
    }

    /**
     * Creates a join point representing a function parameter.
     *
     * @param name
     * @param type
     * @return
     */
    public static AParam param(CxxWeaver weaver, String name, AType type) {
        var param = weaver.getFactory().parmVarDecl(name, (Type) type.getNode());
        return CxxJoinpoints.create(param, weaver, AParam.class);
    }

    public static AComment comment(CxxWeaver weaver, String text) {

        // TODO: Detect C standard, to detect if inline comments are supported?

        var lines = StringLines.getLines(text);

        var comment = lines.size() < 2 ? weaver.getFactory().inlineComment(text, false)
                : weaver.getFactory().multiLineComment(lines);

        return CxxJoinpoints.create(comment, weaver, AComment.class);
    }

    public static AExprStmt exprStmt(CxxWeaver weaver, AExpression expr) {
        var exprStmt = weaver.getFactory().exprStmt((Expr) expr.getNode());
        return CxxJoinpoints.create(exprStmt, weaver, AExprStmt.class);
    }

    /**
     * Creates a join point representing a declStmt.
     *
     * @param varName
     * @param joinpoint
     * @return
     */
    public static ADeclStmt declStmt(CxxWeaver weaver, List<ADecl> decls) {
        var declNodes = decls.stream().map(decl -> (Decl) decl.getNode())
                .collect(Collectors.toList());

        var declStmt = weaver.getFactory().declStmt(declNodes);
        return CxxJoinpoints.create(declStmt, weaver, ADeclStmt.class);
    }

    public static ADeclStmt declStmt(CxxWeaver weaver, Object... decls) {
        return declStmt(weaver, SpecsCollections.asListT(ADecl.class, decls));
    }

    /**
     * Creates a joinpoint representing a declaration of a label. This is not a `label:` statement. For that, you must
     * create a `labelStmt` with returned `labelDecl`. This joinpoint is also used to create `gotoStmt`s.
     *
     * @param name Name of the label
     * @return The created label declaration
     */
    public static ALabelDecl labelDecl(CxxWeaver weaver, String name) {
        var decl = weaver.getFactory().labelDecl(name);
        return CxxJoinpoints.create(decl, weaver, ALabelDecl.class);
    }

    /**
     * Creates a join point representing a label statement in the code.
     *
     * @param decl The declaration for this statement
     * @return The label statement to be used in the code.
     */
    public static ALabelStmt labelStmt(CxxWeaver weaver, ALabelDecl decl) {
        var stmt = decl.getFactory().labelStmt((LabelDecl) decl.getNode());
        return CxxJoinpoints.create(stmt, weaver, ALabelStmt.class);
    }

    /**
     * Convenience method to create at once a label statement and its declaration
     *
     * @param name Name of the label
     * @return The created
     */
    public static ALabelStmt labelStmt(CxxWeaver weaver, String name) {
        return labelStmt(weaver, labelDecl(weaver, name));
    }

    /**
     * Create a joinpoint representing a goto statement.
     *
     * @param label The declaration of the label to jump to
     * @return The created goto statement
     */
    public static AGotoStmt gotoStmt(CxxWeaver weaver, ALabelDecl label) {
        var stmt = label.getFactory().gotoStmt((LabelDecl) label.getNode());
        return CxxJoinpoints.create(stmt, weaver, AGotoStmt.class);
    }


    public static AEmptyStmt emptyStmt(CxxWeaver weaver) {
        var stmt = weaver.getFactory().emptyStmt();
        return CxxJoinpoints.create(stmt, weaver, AEmptyStmt.class);
    }

    public static AProgram program(CxxWeaver weaver) {
        var app = weaver.getFactory().app(Collections.emptyList());
        return CxxJoinpoints.create(app, weaver, AProgram.class);
    }

    public static AMemberAccess memberAccess(CxxWeaver weaver, AExpression baseExpr, AField field) {
        var fieldNode = (FieldDecl) field.getNode();

        var memberAccess = weaver.getFactory().memberExpr(fieldNode.get(FieldDecl.DECL_NAME), fieldNode.get(FieldDecl.TYPE), (Expr) baseExpr.getNode());
        return CxxJoinpoints.create(memberAccess, weaver, AMemberAccess.class);
    }

    public static AMemberAccess memberAccess(CxxWeaver weaver, AExpression baseExpr, String fieldName, AType fieldType) {
        var memberAccess = weaver.getFactory().memberExpr(fieldName, (Type) fieldType.getNode(), (Expr) baseExpr.getNode());
        return CxxJoinpoints.create(memberAccess, weaver, AMemberAccess.class);
    }

    public static AUnaryExprOrType sizeof(CxxWeaver weaver, AExpression exprArg) {
        var sizeof = weaver.getFactory().sizeof((Expr) exprArg.getNode());
        return CxxJoinpoints.create(sizeof, weaver, AUnaryExprOrType.class);
    }

    public static AUnaryExprOrType sizeof(CxxWeaver weaver, AType typeArg) {
        var sizeof = weaver.getFactory().sizeof((Type) typeArg.getNode());
        return CxxJoinpoints.create(sizeof, weaver, AUnaryExprOrType.class);
    }

}
