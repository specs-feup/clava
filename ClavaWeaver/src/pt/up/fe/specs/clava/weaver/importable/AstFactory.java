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
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ClavaNodeParser;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.ClavaNodesLegacy;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.LinkageSpecDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.FunctionDeclData;
import pt.up.fe.specs.clava.ast.decl.data.VarDeclData;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.enums.LanguageId;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.FloatingLiteral;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.enums.BuiltinKind;
import pt.up.fe.specs.clava.ast.expr.enums.ValueKind;
import pt.up.fe.specs.clava.ast.expr.legacy.FloatingLiteralLegacy.FloatKind;
import pt.up.fe.specs.clava.ast.omp.OmpDirectiveKind;
import pt.up.fe.specs.clava.ast.stmt.BreakStmt;
import pt.up.fe.specs.clava.ast.stmt.CaseStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.ReferenceType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data.ArrayTypeData;
import pt.up.fe.specs.clava.ast.type.data.FunctionProtoTypeData;
import pt.up.fe.specs.clava.ast.type.data.FunctionTypeData;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.enums.ArraySizeType;
import pt.up.fe.specs.clava.ast.type.enums.CallingConv;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.language.TLSKind;
import pt.up.fe.specs.clava.parsing.omp.OmpParser;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACall;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFile;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.clava.weaver.joinpoints.CxxFunction;
import pt.up.fe.specs.clava.weaver.joinpoints.types.CxxType;
import pt.up.fe.specs.util.Preconditions;
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
        System.out.println("Init expr:" + initExpr);
        System.out.println("Init expr code:" + initExpr.getCode());

        boolean isUsed = true;
        boolean isImplicit = false;
        boolean isNrvo = false;

        Type initType = (Type) init.getTypeImpl().getNode();

        DataStore config = CxxWeaver.getCxxWeaver().getConfig();

        // Check if C or C++
        Standard standard = config.get(ClavaOptions.STANDARD);

        Type type = getVarDeclType(standard, initType);

        VarDeclData varDeclData = new VarDeclData(StorageClass.NONE, TLSKind.NONE, false, isNrvo,
                InitializationStyle.CINIT, false);
        DeclData declData = new DeclData(false, isImplicit, isUsed, false, false, false);
        VarDecl varDecl = ClavaNodeFactory.varDecl(varDeclData, varName, type, declData, ClavaNodeInfo.undefinedInfo(),
                initExpr);

        return CxxJoinpoints.create(varDecl, null, AVardecl.class);
    }

    /**
     * Creates a joinpoint representing a variable declaration with the given name and initialization.
     *
     * @param varName
     * @param joinpoint
     * @return
     */
    public static AJoinPoint varDeclNoInit(String varName, AType type) {

        VarDecl varDecl = ClavaNodeFactory.varDecl(varName, (Type) type.getNode());
        return CxxJoinpoints.create(varDecl, null, AVardecl.class);
    }

    private static Type getVarDeclType(Standard standard, Type returnType) {
        // If C, use the return type
        if (!standard.isCxx()) {
            return returnType;
        }

        // If C++, use auto as type

        String autoCode = "auto";

        // Check if reference type

        if (returnType instanceof ReferenceType) {
            autoCode = autoCode + "&";
        }

        return ClavaNodeFactory.literalType(autoCode);
    }

    public static CxxFunction functionVoid(String name) {
        BuiltinType voidType = CxxWeaver.getFactory().builtinType(BuiltinKind.VOID);
        FunctionProtoType functionType = ClavaNodeFactory.functionProtoType(new FunctionProtoTypeData(),
                new FunctionTypeData(), new TypeData("void(void)"), ClavaNodeInfo.undefinedInfo(), voidType,
                Collections.emptyList());
        FunctionDecl functionDecl = ClavaNodeFactory.functionDecl(name, Arrays.asList(), functionType,
                new FunctionDeclData(), new DeclData(),
                ClavaNodeInfo.undefinedInfo(),
                ClavaNodeFactory.compoundStmt(ClavaNodeInfo.undefinedInfo(), Collections.emptyList()));

        return (CxxFunction) CxxJoinpoints.create(functionDecl, null);
    }

    public static AStatement stmtLiteral(String code) {
        return stmtLiteral(code, null);
    }

    public static AStatement stmtLiteral(String code, ACxxWeaverJoinPoint parent) {
        // return CxxJoinpoints.create(ClavaNodeFactory.literalStmt(code), parent, AStatement.class);
        return CxxJoinpoints.create(ClavaNodeParser.parseStmt(code), parent, AStatement.class);
    }

    public static AType typeLiteral(String code) {
        return CxxJoinpoints.create(ClavaNodeFactory.literalType(code), null, AType.class);
    }

    public static ADecl declLiteral(String code) {
        return CxxJoinpoints.create(ClavaNodeFactory.literalDecl(code), null, ADecl.class);
    }

    public static AExpression exprLiteral(String code) {
        return exprLiteral(code,
                CxxJoinpoints.create(ClavaNodeFactory.nullType(ClavaNodeInfo.undefinedInfo()), null));
    }

    public static AExpression exprLiteral(String code, AJoinPoint type) {
        Type astType = type instanceof CxxType ? ((CxxType) type).getNode()
                : ClavaNodeFactory.nullType(ClavaNodeInfo.undefinedInfo());

        // Type astType = type == null ? ClavaNodeFactory.nullType(ClavaNodeInfo.undefinedInfo()) : type.getNode();

        return CxxJoinpoints.create(ClavaNodeFactory.literalExpr(code, astType), null, AExpression.class);
    }

    public static ACall callFromFunction(AFunction function, AJoinPoint... args) {
        return call(function.getNameImpl(), function.getFunctionTypeImpl(), args);
    }

    public static ACall call(String functionName, AType typeJp, AJoinPoint... args) {
        Type returnType = (Type) typeJp.getNode();

        DeclRefExpr declRef = ClavaNodeFactory.declRefExpr(functionName, ValueKind.L_VALUE, returnType,
                ClavaNodeInfo.undefinedInfo());

        List<Type> argTypes = Arrays.stream(args)
                .map(arg -> ((Typable) arg.getNode()).getType())
                .collect(Collectors.toList());

        FunctionTypeData fData = new FunctionTypeData(Types.isVoid(returnType), false, false, null, CallingConv.C);
        FunctionProtoType type = ClavaNodeFactory.functionProtoType(new FunctionProtoTypeData(), fData,
                new TypeData(""), ClavaNodeInfo.undefinedInfo(), returnType, argTypes);

        List<Expr> exprArgs = Arrays.stream(args)
                .map(arg -> (Expr) arg.getNode())
                .collect(Collectors.toList());

        CallExpr call = ClavaNodeFactory.callExpr(new ExprData(type, ValueKind.R_VALUE), ClavaNodeInfo.undefinedInfo(),
                declRef, exprArgs);

        return CxxJoinpoints.create(call, null, ACall.class);
    }

    /**
     * Creates a joinpoint representing an empty translation unit.
     *
     * @param varName
     * @param joinpoint
     * @return
     */
    public static AFile file(String filename, String path) {
        // Test if path is absolute
        if (new File(path).isAbsolute()) {
            ClavaLog.warning(
                    "Cannot use absolute paths for new 'file' join points, replacing '" + path + "' with empty String");
            path = "";
        }

        return CxxJoinpoints.create(ClavaNodeFactory.translationUnit(filename, path, Collections.emptyList()),
                null, AFile.class);
    }

    public static AJoinPoint externC(AJoinPoint jpDecl) {
        // Allowed classes for now: CxxFunction
        // TODO: This might be expanded in the future
        boolean isFunction = jpDecl instanceof CxxFunction;
        if (!isFunction) {
            ClavaLog.warning(
                    "Constructor 'externC' does not support joinpoint of type '" + jpDecl.getJoinpointType() + "'");
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

        LinkageSpecDecl linkage = ClavaNodeFactory.linkageSpecialDecl(LanguageId.C, DeclData.empty(),
                ClavaNodeInfo.undefinedInfo(), Arrays.asList(decl));

        return CxxJoinpoints.create(linkage, null);
    }

    // public static AJoinPoint whileLoop() {
    // ClavaNodeFactory.whileStmt(info, condition, thenStmt)
    // }

    // public static ACxxWeaverJoinPoint constArrayType(String typeCode, Standard standard, List<Integer> dims) {
    public static ACxxWeaverJoinPoint constArrayType(String typeCode, String standard, List<Integer> dims) {
        return constArrayType(ClavaNodeFactory.literalType(typeCode), standard, dims);
    }

    // public static ACxxWeaverJoinPoint constArrayType(Type outType, Standard standard, List<Integer> dims) {
    public static ACxxWeaverJoinPoint constArrayType(Type outType, String standardString, List<Integer> dims) {

        Standard standard = Standard.getEnumHelper().valueOf(standardString);

        Preconditions.checkNotNull(dims);
        Preconditions.checkArgument(dims.size() > 0);

        Type inType = null;

        ListIterator<Integer> li = dims.listIterator(dims.size());
        while (li.hasPrevious()) {

            ArrayTypeData arrayTypeData = new ArrayTypeData(ArraySizeType.NORMAL, Collections.emptyList(), standard);
            TypeData typeData = new TypeData(outType.getCode());
            ClavaNodeInfo info = ClavaNodeInfo.undefinedInfo();

            inType = outType;
            outType = ClavaNodeFactory.constantArrayType(li.previous(), arrayTypeData, typeData, info, inType);
        }

        return CxxJoinpoints.create(outType, null);
    }

    public static AJoinPoint omp(String directiveName) {
        // Get directive
        OmpDirectiveKind kind = OmpDirectiveKind.getHelper().valueOf(directiveName);
        return CxxJoinpoints.create(OmpParser.newOmpPragma(kind), null);

        // ClavaNodeFactory.wrapperStmt(ClavaNodeInfo.undefinedInfo(),
    }

    public static AStatement caseStmt(AExpression value, AStatement subStmt) {
        CaseStmt caseStmt = ClavaNodeFactory.caseStmt(ClavaNodeInfo.undefinedInfo(), (Expr) value.getNode(),
                (Stmt) subStmt.getNode());

        return CxxJoinpoints.create(caseStmt, null, AStatement.class);
    }

    /**
     * 
     * @param value
     * @param expr
     * @return a list with a case statement and a break statement
     */
    public static List<AStatement> caseFromExpr(AExpression value, AExpression expr) {
        // Create compound stmt
        ExprStmt exprStmt = ClavaNodesLegacy.exprStmt((Expr) expr.getNode());
        BreakStmt breakStmt = ClavaNodeFactory.breakStmt(ClavaNodeInfo.undefinedInfo());

        CompoundStmt compoundStmt = ClavaNodeFactory.compoundStmt(ClavaNodeInfo.undefinedInfo(),
                Arrays.asList(exprStmt));
        compoundStmt.setNaked(true);
        // InlineComment comment = ClavaNodeFactory.inlineComment("Case " + value.getCode(), false,
        // ClavaNodeInfo.undefinedInfo());
        // Stmt commentStmt = ClavaNodeFactory.wrapperStmt(ClavaNodeInfo.undefinedInfo(), comment);

        AStatement caseStmt = caseStmt(value, CxxJoinpoints.create(compoundStmt, null, AStatement.class));

        return Arrays.asList(caseStmt,
                CxxJoinpoints.create(breakStmt, null, AStatement.class));

    }

    public static AStatement switchStmt(AExpression condition, AStatement body) {
        Stmt switchStmt = ClavaNodeFactory.switchStmt(ClavaNodeInfo.undefinedInfo(), (Expr) condition.getNode(),
                (Stmt) body.getNode());

        return CxxJoinpoints.create(switchStmt, null, AStatement.class);
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

        CompoundStmt body = ClavaNodeFactory.compoundStmt(ClavaNodeInfo.undefinedInfo(), statements);
        Stmt switchStmt = ClavaNodeFactory.switchStmt(ClavaNodeInfo.undefinedInfo(), (Expr) condition.getNode(), body);

        return CxxJoinpoints.create(switchStmt, null, AStatement.class);
    }

    ////// Methods that only use ClavaFactory

    public static ACxxWeaverJoinPoint builtinType(String typeCode) {
        BuiltinKind kind = BuiltinKind.getHelper().valueOf(typeCode);

        BuiltinType type = CxxWeaver.getFactory().builtinType(kind);

        return CxxJoinpoints.create(type, null);
    }

    public static AExpression doubleLiteral(double floating) {
        FloatingLiteral floatingLiteral = CxxWeaver.getFactory()
                .floatingLiteral(FloatKind.DOUBLE, floating);
        // Type type = CxxWeaver.getFactory().builtinType(BuiltinKind.DOUBLE);
        // FloatingLiteral intLiteral = ClavaNodeFactory.floatingLiteral(FloatKind.DOUBLE, Double.toString(floating),
        // new ExprData(type), ClavaNodeInfo.undefinedInfo());

        return CxxJoinpoints.create(floatingLiteral, null, AExpression.class);
    }

    public static ACxxWeaverJoinPoint longType() {
        BuiltinType type = CxxWeaver.getFactory().builtinType(BuiltinKind.LONG);
        return CxxJoinpoints.create(type, null);
    }

    public static AExpression integerLiteral(int integer) {
        IntegerLiteral intLiteral = CxxWeaver.getFactory().integerLiteral(integer);
        // Type intType = ClavaNodeFactory.builtinType(BuiltinKind.INT);
        // IntegerLiteral intLiteral = ClavaNodeFactory.integerLiteral(Integer.toString(integer), new ExprData(intType),
        // ClavaNodeInfo.undefinedInfo());

        return CxxJoinpoints.create(intLiteral, null, AExpression.class);
    }
}
