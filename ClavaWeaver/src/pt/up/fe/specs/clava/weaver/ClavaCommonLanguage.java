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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.EnumConstantDecl;
import pt.up.fe.specs.clava.ast.decl.FieldDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.CXXMemberCallExpr;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.MemberExpr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.classmap.FunctionClassMap;


public class ClavaCommonLanguage {

	private static final FunctionClassMap<ClavaNode, String> JOINPOINT_MAPPER;
	static {
		JOINPOINT_MAPPER = new FunctionClassMap<>();
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

		switch (node.getTagKind()) {
		case CLASS:
			return "ClassJp";
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
		if (node.getMemberDecl() instanceof VarDecl)
			return "JoinPoint";

		if (node.getMemberDecl() instanceof CXXMethodDecl)
			return "JoinPoint";

		if (node.getMemberDecl() instanceof EnumConstantDecl)
			return "JoinPoint";
		*/

		return "JoinPoint";

	}
}
