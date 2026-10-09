/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.wire;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clava.context.ClavaContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ProtoAstReaderTest {

    private static final Envelope END = Envelope.newBuilder().setEnd(End.getDefaultInstance()).build();

    @Test
    void acceptsACompleteHeaderAndEndStream() throws IOException {
        var header = Envelope.newBuilder().setHeader(header()).build();
        var bytes = stream(header, END);

        var data = ProtoAstReader.read(new ByteArrayInputStream(bytes), new ClavaContext(), null, "unit");
        assertEquals(0, data.get(ClangAstData.NODE_DATA).size());
    }

    @Test
    void rejectsAnIncompatibleHeaderBeforeApplyingRecords() {
        var badHeader = header().toBuilder().setProtocolMajor(2).build();
        var bytes = stream(Envelope.newBuilder().setHeader(badHeader).build());
        IOException error = assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(bytes), new ClavaContext(), null, "unit"));
        assertEquals(true, error.getMessage().contains("version"));
    }

    @Test
    void rejectsMissingMagic() {
        var bytes = stream(Envelope.newBuilder().setHeader(header()).build());
        bytes[0] = 'X';
        assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(bytes), new ClavaContext(), null, "unit"));
    }

    @Test
    void rejectsMissingRequiredHeaderPresence() {
        var incomplete = header().toBuilder().clearSchemaSha256().build();
        var bytes = stream(Envelope.newBuilder().setHeader(incomplete).build());
        IOException error = assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(bytes), new ClavaContext(), null, "unit"));
        assertEquals(true, error.getMessage().contains("required field"));
    }

    @Test
    void rejectsAnEmptyChunk() {
        var header = Envelope.newBuilder().setHeader(header()).build();
        var empty = Envelope.newBuilder().setChunk(Chunk.getDefaultInstance()).build();
        IOException error = assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(stream(header, empty)),
                        new ClavaContext(), null, "unit"));
        assertEquals(true, error.getMessage().contains("at least one Record"));
    }

    @Test
    void rejectsAReferenceToAnUnknownTopLevelNode() {
        var header = Envelope.newBuilder().setHeader(header()).build();
        var topLevel = chunk(Record.newBuilder().setTopLevel(TopLevel.newBuilder()
                .setKind(TopLevelKind.TOPLEVELKIND_DECL).setNode(1)));
        IOException error = assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(stream(header, topLevel, END)),
                        new ClavaContext(), null, "unit"));
        assertEquals(true, error.getMessage().contains("Top-level reference"));
    }

    @Test
    void rejectsAnUnresolvableRequiredNodeReference() {
        var header = Envelope.newBuilder().setHeader(header()).build();
        var node = chunk(Record.newBuilder().setNode(Node.newBuilder()
                .setId(1)
                .setClassName("PointerType")
                .setPointerTypeData(PointerTypeData.newBuilder()
                        .setBase(TypeData.newBuilder()
                                .setTypeAsString("int *")
                                .setTypeDependency(TypeDependency.TYPEDEPENDENCY_NONE)
                                .setIsVariablyModified(false)
                                .setContainsUnexpandedParameterPack(false)
                                .setIsFromAst(false)
                                .setUnqualifiedDesugaredType(-1))
                        .setPointeeType(-1))));
        var nodeClass = chunk(Record.newBuilder().setNodeClass(NodeClass.newBuilder()
                .setNode(1).setClassName("PointerType")));
        var children = chunk(Record.newBuilder().setChildren(Children.newBuilder()
                .setNode(1)));

        IOException error = assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(stream(header, node, nodeClass, children, END)),
                        new ClavaContext(), null, "unit"));
        assertEquals(true, error.getMessage().contains("resolve protobuf AST references"));
    }

    @Test
    void acceptsRepeatedTopLevelRecordsWithSetSemantics() throws IOException {
        var header = Envelope.newBuilder().setHeader(header()).build();
        var node = chunk(Record.newBuilder().setNode(Node.newBuilder()
                .setId(1)
                .setClassName("BuiltinType")
                .setBuiltinTypeData(BuiltinTypeData.newBuilder()
                        .setBase(TypeData.newBuilder()
                                .setTypeAsString("int")
                                .setTypeDependency(TypeDependency.TYPEDEPENDENCY_NONE)
                                .setIsVariablyModified(false)
                                .setContainsUnexpandedParameterPack(false)
                                .setIsFromAst(false)
                                .setUnqualifiedDesugaredType(-1))
                        .setKind(BuiltinKind.BUILTINKIND_INT)
                        .setKindLiteral("int"))));
        var nodeClass = chunk(Record.newBuilder().setNodeClass(NodeClass.newBuilder()
                .setNode(1).setClassName("BuiltinType")));
        var topLevel = chunk(Record.newBuilder().setTopLevel(TopLevel.newBuilder()
                .setKind(TopLevelKind.TOPLEVELKIND_TYPE).setNode(1)));
        var children = chunk(Record.newBuilder().setChildren(Children.newBuilder()
                .setNode(1)));

        var data = ProtoAstReader.read(
                new ByteArrayInputStream(stream(header, node, nodeClass, topLevel, topLevel, children, END)),
                new ClavaContext(), null, "unit");

        assertEquals(1, data.get(ClangAstData.TOP_LEVEL_TYPE_IDS).size());
    }

    @Test
    void rejectsMismatchedNodePayload() {
        var header = Envelope.newBuilder().setHeader(header()).build();
        var node = chunk(Record.newBuilder().setNode(Node.newBuilder()
                .setId(1)
                .setClassName("BuiltinType")
                .setDeclData(DeclData.newBuilder())));

        IOException error = assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(stream(header, node)),
                        new ClavaContext(), null, "unit"));
        assertEquals(true, error.getMessage().contains("payload"));
    }

    @Test
    void acceptsAlignedAttrSpecializedPayloadInsteadOfGenericAttributePayload() {
        var completeAttribute = AttributeData.newBuilder()
                .setBase(nodeData())
                .setKind(AttributeKind.ATTRIBUTEKIND_ALIGNED)
                .setIsImplicit(false)
                .setIsInherited(false)
                .setIsLateParsed(false)
                .setIsPackExpansion(false)
                .build();
        var payload = AlignedAttrData.newBuilder()
                .setBase(completeAttribute)
                .setSpelling("aligned")
                .setIsExpression(true)
                .setAlignment(8)
                .build();

        assertEquals(Node.NodeCase.ALIGNED_ATTR_DATA,
                ProtoGeneratedBindings.expectedPayload("AlignedAttr"));
        assertEquals(Node.NodeCase.ATTRIBUTE_DATA,
                ProtoGeneratedBindings.expectedPayload("AssumeAlignedAttr"));
        assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.expectedPayload("Expr"));
        assertDoesNotThrow(() -> ProtoGeneratedBindings.validatePayload(
                "AlignedAttr", Node.NodeCase.ALIGNED_ATTR_DATA));
        assertThrows(IllegalArgumentException.class, () -> ProtoGeneratedBindings.validatePayload(
                "AlignedAttr", Node.NodeCase.ATTRIBUTE_DATA));
        assertDoesNotThrow(() -> ProtoGeneratedBindings.validate(payload, "AlignedAttr"));
    }

    @Test
    void rejectsAlignedAttrPayloadWhenInheritedAttributeDataIsIncomplete() {
        var incompleteAttribute = AttributeData.newBuilder()
                .setBase(nodeData())
                .setKind(AttributeKind.ATTRIBUTEKIND_ALIGNED)
                .setIsImplicit(false)
                .setIsInherited(false)
                .setIsLateParsed(false)
                .build();
        var payload = AlignedAttrData.newBuilder()
                .setBase(incompleteAttribute)
                .setSpelling("aligned")
                .setIsExpression(true)
                .setAlignment(8)
                .build();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ProtoGeneratedBindings.validate(payload, "AlignedAttr"));
        assertEquals(true, error.getMessage().contains("AttributeData.is_pack_expansion"));
    }

    private static NodeData nodeData() {
        return NodeData.newBuilder().setSource(SourceInfo.newBuilder()
                .setIsMacro(false).setSystemHeader(false)).build();
    }

    @Test
    void rejectsUnsupportedUnreachableNodeWithClassIdAndSourceLocation() {
        var header = Envelope.newBuilder().setHeader(header()).build();
        var source = SourceInfo.newBuilder().setExpansion(Range.newBuilder()
                .setFile(1).setLine(12).setColumn(5));
        var file = Record.newBuilder().setFile(File.newBuilder().setId(1).setPath("/tmp/unsupported.cpp"));
        var node = Record.newBuilder().setNode(Node.newBuilder()
                .setId(1)
                .setClassName("LifetimeExtendedTemporaryDecl")
                .setDeclData(DeclData.newBuilder()
                        .setBase(NodeData.newBuilder().setSource(source))));
        var records = chunk(file, node);

        IOException error = assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(stream(header, records)),
                        new ClavaContext(), null, "unit"));
        assertEquals(true, error.getMessage().contains("LifetimeExtendedTemporaryDecl"));
        assertEquals(true, error.getMessage().contains("id 1"));
        assertEquals(true, error.getMessage().contains("/tmp/unsupported.cpp:12:5"));
    }

    @Test
    void rejectsIncompleteLanguageRecord() {
        var header = Envelope.newBuilder().setHeader(header()).build();
        var language = chunk(Record.newBuilder().setLanguage(Language.newBuilder()
                .setFile("unit.cpp")));

        IOException error = assertThrows(IOException.class,
                () -> ProtoAstReader.read(new ByteArrayInputStream(stream(header, language, END)),
                        new ClavaContext(), null, "unit"));
        assertEquals(true, error.getMessage().contains("Language is missing required fields"));
    }

    private static Envelope chunk(Record.Builder... records) {
        return Envelope.newBuilder().setChunk(Chunk.newBuilder().addAllRecords(
                java.util.Arrays.stream(records).map(Record.Builder::build).toList())).build();
    }

    private static Header header() {
        return Header.newBuilder()
                .setProtocolMajor(1)
                .setProtocolMinor(0)
                .setSchemaId("clava-ast-wire")
                .setProducerVersion(ProtoAstReader.PRODUCER_VERSION)
                .setLlvmMajor(18)
                .setSchemaSha256(com.google.protobuf.ByteString.copyFromUtf8(ProtoSchemaHash.VALUE))
                .build();
    }

    private static byte[] stream(Envelope... envelopes) {
        var bytes = new ByteArrayOutputStream();
        bytes.writeBytes(new byte[] { 'C', 'L', 'A', 'V', 'A', 'P', 'B', '1' });
        for (Envelope envelope : envelopes) {
            byte[] payload = envelope.toByteArray();
            int length = payload.length;
            while ((length & ~0x7f) != 0) {
                bytes.write((length & 0x7f) | 0x80);
                length >>>= 7;
            }
            bytes.write(length);
            bytes.writeBytes(payload);
        }
        return bytes.toByteArray();
    }
}
