/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ast.decl.FriendDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

class FriendDeclStructuredFidelityTest {
    @TempDir
    Path temporary;

    @Test
    void factoryInitializesOwnerAndFriendReferenceLinks() {
        var factory = new ClavaContext().getFactory();
        CXXRecordDecl owner = factory.cxxRecordDecl("Owner", List.of());
        FunctionDecl function = factory.functionDecl("target", factory.functionProtoType(factory.builtinType("int")));
        FriendDecl friend = factory.friendDecl(owner, function);

        assertSame(owner, friend.get(FriendDecl.OWNER_RECORD));
        assertSame(function, friend.get(FriendDecl.FRIEND_DECL));
        assertTrue(friend.get(FriendDecl.FRIEND_TYPE) instanceof pt.up.fe.specs.clava.utils.NullNode);
        assertEquals("friend int target();", friend.getCode());
    }

    @Test
    void rejectsDependentFriendFormsDuringParsingWithNodeContext() {
        SpecsSystem.programStandardInit();
        File source = SpecsIo.resourceCopy("cxx/wire_friend_member_templates.cpp",
                temporary.toFile(), false, true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> parse(source.toPath(), "dependent-parse"));
        String messages = exceptionMessages(exception);
        assertTrue(messages.contains("dependent friend declarations"), messages);
        assertTrue(messages.contains("FriendDecl"), messages);
        assertTrue(messages.contains(" at "), messages);
    }

    @Test
    void printsCurrentAstAfterFriendDeclarationEditAndReparsesGeneratedSource() throws Exception {
        SpecsSystem.programStandardInit();
        Path source = Files.writeString(temporary.resolve("friend_transform.cpp"),
                "struct Owner { friend int original_friend(int value); };\n");

        App first = parse(source, "first-parse");
        FriendDecl friend = first.getDescendants(FriendDecl.class).stream()
                .filter(candidate -> candidate.get(FriendDecl.FRIEND_DECL) instanceof FunctionDecl)
                .findFirst().orElseThrow();
        FunctionDecl declaration = (FunctionDecl) friend.get(FriendDecl.FRIEND_DECL);
        declaration.set(NamedDecl.DECL_NAME, "edited_friend");

        File generated = first.write(Files.createDirectories(temporary.resolve("first-output")).toFile()).stream()
                .filter(file -> file.getName().equals(source.getFileName().toString()))
                .findFirst().orElseThrow();
        String generatedCode = Files.readString(generated.toPath());
        assertTrue(generatedCode.contains("friend int edited_friend(int value);"), generatedCode);

        App second = parse(generated.toPath(), "second-parse");
        FriendDecl reparsedFriend = second.getDescendants(FriendDecl.class).stream()
                .filter(candidate -> candidate.get(FriendDecl.FRIEND_DECL) instanceof FunctionDecl)
                .findFirst().orElseThrow();
        assertEquals("edited_friend", ((FunctionDecl) reparsedFriend.get(FriendDecl.FRIEND_DECL)).getDeclName());
    }

    @Test
    void printsCurrentDependentFriendTypeAfterTypeChildReplacementAndReparses() throws Exception {
        SpecsSystem.programStandardInit();
        Path source = Files.writeString(temporary.resolve("friend_type_transform.cpp"),
                "struct Replacement {};\n"
                        + "template <typename T> struct Owner { friend T; };\n");

        App first = parse(source, "friend-type-first-parse");
        FriendDecl friend = first.getDescendants(FriendDecl.class).stream().findFirst().orElseThrow();
        CXXRecordDecl replacement = first.getDescendants(CXXRecordDecl.class).stream()
                .filter(record -> record.getDeclName().equals("Replacement"))
                .findFirst().orElseThrow();
        friend.setChild(0, first.getFactory().recordType(replacement));

        File generated = first.write(Files.createDirectories(temporary.resolve("friend-type-output")).toFile()).stream()
                .filter(file -> file.getName().equals(source.getFileName().toString()))
                .findFirst().orElseThrow();
        String generatedCode = Files.readString(generated.toPath());
        assertTrue(generatedCode.contains("friend Replacement;"), generatedCode);

        App second = parse(generated.toPath(), "friend-type-second-parse");
        FriendDecl reparsedFriend = second.getDescendants(FriendDecl.class).stream().findFirst().orElseThrow();
        assertEquals("Replacement", reparsedFriend.get(FriendDecl.FRIEND_TYPE).getCode());
    }

    private App parse(Path source, String name) throws Exception {
        CodeParser parser = CodeParser.newInstance();
        parser.set(CodeParser.GENERATED_PARSE_ROOT, Files.createDirectories(temporary.resolve(name)).toFile());
        parser.set(CodeParser.AST_DUMP_CACHE, false);
        parser.set(CodeParser.SHOW_EXEC_INFO, false);
        parser.set(ParallelCodeParser.PARALLEL_PARSING, false);
        return parser.parse(List.of(source.toFile()), List.of("-std=c++17"));
    }

    private static String exceptionMessages(Throwable exception) {
        StringBuilder messages = new StringBuilder();
        for (Throwable current = exception; current != null; current = current.getCause()) {
            messages.append(current.getClass().getSimpleName()).append(": ")
                    .append(current.getMessage()).append('\n');
        }
        return messages.toString();
    }

}
