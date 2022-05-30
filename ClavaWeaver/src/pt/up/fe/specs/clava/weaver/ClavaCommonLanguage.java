/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.clava.weaver;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXConstructorDecl;
import pt.up.fe.specs.clava.ast.decl.CXXDestructorDecl;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FieldDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CXXConstructExpr;
import pt.up.fe.specs.clava.ast.expr.CXXMemberCallExpr;
import pt.up.fe.specs.clava.ast.expr.CXXThrowExpr;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.ConditionalOperator;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.MemberExpr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.stmt.BreakStmt;
import pt.up.fe.specs.clava.ast.stmt.CXXCatchStmt;
import pt.up.fe.specs.clava.ast.stmt.CXXTryStmt;
import pt.up.fe.specs.clava.ast.stmt.CaseStmt;
import pt.up.fe.specs.clava.ast.stmt.ContinueStmt;
import pt.up.fe.specs.clava.ast.stmt.DefaultStmt;
import pt.up.fe.specs.clava.ast.stmt.GotoStmt;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.SwitchStmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.classmap.FunctionClassMap;


public class ClavaCommonLanguage {

	private static final FunctionClassMap<ClavaNode, String> JOINPOINT_MAPPER;
	static {
		JOINPOINT_MAPPER = new FunctionClassMap<>();
		JOINPOINT_MAPPER.put(GotoStmt.class, node -> "GotoJp");
		JOINPOINT_MAPPER.put(DefaultStmt.class, node -> "DefaultJp");
		JOINPOINT_MAPPER.put(ContinueStmt.class, node -> "ContinueJp");
		JOINPOINT_MAPPER.put(BreakStmt.class, node -> "BreakJp");
		JOINPOINT_MAPPER.put(CXXThrowExpr.class, node -> "ThrowJp");
		JOINPOINT_MAPPER.put(CXXCatchStmt.class, node -> "CatchJp");
		JOINPOINT_MAPPER.put(CXXTryStmt.class, node -> "TryJp");
		// JOINPOINT_MAPPER.put(ForStmt.class, node -> "ForJp");
		// JOINPOINT_MAPPER.put(DoStmt.class, node -> "DoJp");
		// JOINPOINT_MAPPER.put(WhileStmt.class, node -> "WhileJp");
		JOINPOINT_MAPPER.put(BinaryOperator.class, node -> "BinaryJp");
		JOINPOINT_MAPPER.put(ConditionalOperator.class, node -> "TernaryJp");
		JOINPOINT_MAPPER.put(LoopStmt.class, node -> "LoopJp");
		JOINPOINT_MAPPER.put(CaseStmt.class, node -> "CaseJp");
		JOINPOINT_MAPPER.put(SwitchStmt.class, node -> "SwitchJp");
		JOINPOINT_MAPPER.put(IfStmt.class, node -> "IfJp");
		JOINPOINT_MAPPER.put(Stmt.class, ClavaCommonLanguage::stmt);
		JOINPOINT_MAPPER.put(CXXConstructExpr.class, node -> "ConstructorCallJp");
		JOINPOINT_MAPPER.put(CXXConstructorDecl.class, node -> "ConstructorJp");
		JOINPOINT_MAPPER.put(DeclRefExpr.class, node -> "VarRefJp");
		JOINPOINT_MAPPER.put(VarDecl.class, node -> "VarDeclJp");
		JOINPOINT_MAPPER.put(ParmVarDecl.class, node -> "ParamJp");
		JOINPOINT_MAPPER.put(Type.class, node -> "TypeJp");
		JOINPOINT_MAPPER.put(MemberExpr.class, ClavaCommonLanguage::memberExpr);
		JOINPOINT_MAPPER.put(FieldDecl.class, node -> "FieldJp");
		JOINPOINT_MAPPER.put(CXXMemberCallExpr.class, node -> "MemberCallJp");
		JOINPOINT_MAPPER.put(CallExpr.class, node -> "CallJp");
		JOINPOINT_MAPPER.put(Expr.class, node -> "ExprJp");
		JOINPOINT_MAPPER.put(FunctionDecl.class, node -> "FunctionJp");
		JOINPOINT_MAPPER.put(CXXMethodDecl.class, node -> "MethodJp");
		JOINPOINT_MAPPER.put(CXXRecordDecl.class, ClavaCommonLanguage::cxxRecordDecl);
		JOINPOINT_MAPPER.put(Decl.class, node -> "DeclJp");
		JOINPOINT_MAPPER.put(TranslationUnit.class, node -> "FileJp");
		JOINPOINT_MAPPER.put(App.class, node -> "ProgramJp");
		JOINPOINT_MAPPER.put(ClavaNode.class, node -> "JoinPoint");
	}

	public static String getJoinPointName(ClavaNode node) {
		return JOINPOINT_MAPPER.apply(node);
	}

	private static String cxxRecordDecl(CXXRecordDecl node) {
	    
	    // if has definition, use it for correct mapping
	    Optional<CXXRecordDecl> definitionOpt = node.getDefinition();
	    if (definitionOpt.isPresent() && !node.equals(node.getDefinition().get())) {
	        node = definitionOpt.get();
	    }
        
	    // add implementation of methods as child
        addMethodImplementations(node);
        
        // do same for bases
        for (CXXRecordDecl nodeBase : node.getAllBases()) {
            addMethodImplementations(nodeBase);
        }
	    
        // check if has definition, if not return null;
        if (!definitionOpt.isPresent()) return "JoinPoint";
	    
	    
	    if (node.isInterface()) return "InterfaceJp";

        switch (node.getTagKind()) {
        case CLASS:
            return "ClassJp";
        case INTERFACE:
            return "InterfaceJp";
		/*
		 * case STRUCT: return "StructJp";
		 */
		default:
			return "JoinPoint";
		}
	}

	private static String memberExpr(MemberExpr node) {

		if (node.getMemberDecl() instanceof FieldDecl)
			return "FieldRefJp";
		/*
		 * if (node.getMemberDecl() instanceof VarDecl) return "JoinPoint";
		 * 
		 * if (node.getMemberDecl() instanceof CXXMethodDecl) return "JoinPoint";
		 * 
		 * if (node.getMemberDecl() instanceof EnumConstantDecl) return "JoinPoint";
		 */

		return "JoinPoint";

	}
	
	private static String stmt(Stmt node) {

		if (node.getParent() instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) node.getParent();
			
			if(ifStmt.getThen().isPresent() && ifStmt.getThen().get() == node)
				return "ThenJp";
			
			if(ifStmt.getElse().isPresent() && ifStmt.getElse().get() == node)
				return "ElseJp";
			
			
		}
			return "StmtJp";

	}
	
	private static void addMethodImplementations(CXXRecordDecl node) {

        for (CXXMethodDecl method : node.getMethods()) {
            
            // if has body, do nothing
            if (method.hasBody()) continue;
            
            // if implementation is equal, do nothing
            Optional<FunctionDecl> implementationOptional = method.getImplementation();
            if (!implementationOptional.isPresent() || method.equals(implementationOptional.get())) continue; 
            
            FunctionDecl implementation = implementationOptional.get();

            boolean isAlreadyInMethodsList = node.getMethods().stream()
                    // filter its own
                    .filter(m -> !m.equals(method))
                    // map to implementation
                    .map(m -> m.getImplementation())
                    // filter those which do not have implementation
                    .filter(impl -> impl.isPresent())
                    // map to code
                    .map(impl -> impl.get().getCode())
                    .anyMatch(code -> code.equals(implementation.getCode()));
            
            if (isAlreadyInMethodsList) continue;

            // node.addMethod((CXXMethodDecl) implementation);
            node.addChild((CXXMethodDecl) implementation);

            // System.out.println("adding impl for => " + node.getDeclName() + "." + method.getDeclName());
        }
	}
}
