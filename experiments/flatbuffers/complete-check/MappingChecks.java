package pt.up.fe.specs.clang.wire;

import astwire.v2.Block;
import astwire.v2.Counter;
import astwire.v2.Record;
import astwire.v2.RecordPayload;
import astwire.v2.TemplateExpansion;
import astwire.v2.TemplateName;
import astwire.v2.TemplateNameValue;
import astwire.v2.DirectTemplateName;

import com.google.flatbuffers.FlatBufferBuilder;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Focused checks for v2 framing, bounded mappings, and generated presence validation. */
public final class MappingChecks {
    private static final int MAGIC = 0x32564c43; // little-endian bytes: CLV2
    private static final int ONE_MIB = 1024 * 1024;
    private static final int GIANT_RECORD = 64 * ONE_MIB + 4096;

    private MappingChecks() {
    }

    public static void main(String[] args) throws Exception {
        validSizePrefixedBlock();
        sparseBoundaryAndLargeRecord();
        malformedRecords();
        generatedPresenceValidation();
        System.out.println("mapping_checks_passed=true");
    }

    private static void validSizePrefixedBlock() throws Exception {
        Path path = Files.createTempFile("wire-v2-valid-", ".bin");
        try {
            Files.write(path, validBlock());
            try (MappedRecords records = new MappedRecords(path)) {
                MappedRecords.Frame frame = records.next();
                require(frame != null, "valid block did not produce a frame");
                Block block = new Block().__assign(frame.rootOffset(), frame.buffer());
                require(block.recordsLength() == 1, "valid block record count");
                Record record = block.records(0);
                require(record.payloadType() == RecordPayload.Counter, "valid block payload type");
                Counter counter = (Counter) record.payload(new Counter());
                require(counter.value() == 17, "valid block counter value");
                require(records.next() == null, "valid block produced an extra frame");
            }
            System.out.println("valid_block=true");
        } finally {
            Files.deleteIfExists(path);
        }
    }

    /**
     * Uses a sparse file so the scan crosses 2 GiB and then maps a record larger than the
     * 64 MiB window without allocating either the file body or a giant Java byte array.
     */
    private static void sparseBoundaryAndLargeRecord() throws Exception {
        final int smallRecord = ONE_MIB;
        final int smallCount = 2048;
        final long stride = Integer.BYTES + (long) smallRecord;
        final long giantPosition = smallCount * stride;
        final long fileSize = giantPosition + Integer.BYTES + GIANT_RECORD;
        require(giantPosition > 2L * 1024 * 1024 * 1024 - 1024 * 1024,
                "stress record did not cross 2 GiB");

        Path path = Files.createTempFile("wire-v2-sparse-", ".bin");
        try {
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE)) {
                for (int i = 0; i < smallCount; i++) {
                    writeFrameHeader(channel, i * stride, smallRecord, i);
                }
                writeFrameHeader(channel, giantPosition, GIANT_RECORD, 0x6a1a17);
                channel.position(fileSize - 1);
                channel.write(ByteBuffer.wrap(new byte[] {0}));
            }

            try (MappedRecords records = new MappedRecords(path)) {
                for (int i = 0; i < smallCount; i++) {
                    MappedRecords.Frame frame = records.next();
                    require(frame != null, "missing sparse frame " + i);
                    require(frame.bytes() == smallRecord, "small frame size " + i);
                    require(frame.buffer().getInt(frame.rootOffset()) == MAGIC, "small frame identifier " + i);
                    require(frame.buffer().getInt(frame.rootOffset() + Integer.BYTES) == i,
                            "small frame marker " + i);
                }

                MappedRecords.Frame giant = records.next();
                require(giant != null, "missing >64 MiB frame");
                require(giant.bytes() == GIANT_RECORD, "large frame size");
                require(giant.buffer().getInt(giant.rootOffset()) == MAGIC, "large frame identifier");
                require(giant.buffer().getInt(giant.rootOffset() + Integer.BYTES) == 0x6a1a17,
                        "large frame marker");
                require(records.next() == null, "sparse file produced an extra frame");
            }

            require(Files.size(path) > 2L * 1024 * 1024 * 1024, "sparse file did not exceed 2 GiB");
            System.out.println("sparse_boundary=true,large_record_bytes=" + GIANT_RECORD
                    + ",file_bytes=" + Files.size(path));
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private static void malformedRecords() throws Exception {
        expectFailure("truncated_length", new byte[] {1, 2, 3}, EOFException.class);
        expectFailure("short_length", prefixOnly(4), IOException.class);
        expectFailure("truncated_body", prefixOnly(12), IOException.class);
        expectFailure("bad_identifier", frameBytes(12, 4, 0), IOException.class);
        expectFailure("small_root", frameBytes(12, 0, MAGIC), IOException.class);
        expectFailure("large_root", frameBytes(12, 12, MAGIC), IOException.class);
        System.out.println("malformed_records=true");
    }

    private static void generatedPresenceValidation() {
        FlatBufferBuilder builder = new FlatBufferBuilder(64);
        int absentCounter = Counter.createCounter(builder, 0);
        int absentRecord = Record.createRecord(builder, RecordPayload.Counter, absentCounter);
        builder.finish(absentRecord);
        Record parsedAbsentRecord = Record.getRootAsRecord(builder.dataBuffer());
        Counter counter = (Counter) parsedAbsentRecord.payload(new Counter());
        require(!counter.hasValue(), "FlatBuffers default scalar unexpectedly present");
        expectValidationFailure("mandatory_scalar", () -> GeneratedNodes.validate(parsedAbsentRecord));

        builder = new FlatBufferBuilder(64);
        int presentCounter = Counter.createCounter(builder, 17);
        int presentRecord = Record.createRecord(builder, RecordPayload.Counter, presentCounter);
        builder.finish(presentRecord);
        GeneratedNodes.validate(Record.getRootAsRecord(builder.dataBuffer()));

        builder = new FlatBufferBuilder(128);
        int direct = DirectTemplateName.createDirectTemplateName(builder, 0);
        int missingReference = TemplateName.createTemplateName(builder, TemplateNameValue.DirectTemplateName, direct);
        builder.finish(missingReference);
        final FlatBufferBuilder missingReferenceBuilder = builder;
        expectValidationFailure("mandatory_reference", () -> GeneratedNodes.validate(
                TemplateName.getRootAsTemplateName(missingReferenceBuilder.dataBuffer())));

        builder = new FlatBufferBuilder(128);
        direct = DirectTemplateName.createDirectTemplateName(builder, 17);
        int presentName = TemplateName.createTemplateName(builder, TemplateNameValue.DirectTemplateName, direct);
        int optionalAbsent = TemplateExpansion.createTemplateExpansion(builder, 0, presentName);
        builder.finish(optionalAbsent);
        TemplateExpansion expansion = TemplateExpansion.getRootAsTemplateExpansion(builder.dataBuffer());
        require(!expansion.hasNumExpansions(), "wire_optional field was serialized unexpectedly");
        GeneratedNodes.validate(expansion);
        System.out.println("generated_presence=true");
    }

    private static byte[] validBlock() {
        FlatBufferBuilder builder = new FlatBufferBuilder(256);
        int counter = Counter.createCounter(builder, 17);
        int record = Record.createRecord(builder, RecordPayload.Counter, counter);
        int records = Block.createRecordsVector(builder, new int[] {record});
        int block = Block.createBlock(builder, records);
        Block.finishSizePrefixedBlockBuffer(builder, block);
        return builder.sizedByteArray();
    }

    private static byte[] prefixOnly(int length) {
        ByteBuffer bytes = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putInt(length);
        return bytes.array();
    }

    private static byte[] frameBytes(int length, int root, int identifier) {
        ByteBuffer bytes = ByteBuffer.allocate(Integer.BYTES + length).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putInt(length).putInt(root).putInt(identifier);
        return bytes.array();
    }

    private static void writeFrameHeader(FileChannel channel, long position, int length, int marker)
            throws IOException {
        ByteBuffer bytes = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putInt(length).putInt(4).putInt(MAGIC).putInt(marker).flip();
        channel.position(position);
        while (bytes.hasRemaining()) {
            channel.write(bytes);
        }
    }

    private static void expectFailure(String name, byte[] bytes, Class<? extends Throwable> expected)
            throws Exception {
        Path path = Files.createTempFile("wire-v2-malformed-", ".bin");
        try {
            Files.write(path, bytes);
            try (MappedRecords records = new MappedRecords(path)) {
                try {
                    records.next();
                    throw new AssertionError(name + " was accepted");
                } catch (Throwable failure) {
                    if (!expected.isInstance(failure)) {
                        throw new AssertionError(name + " threw " + failure, failure);
                    }
                }
            }
        } finally {
            Files.deleteIfExists(path);
        }
    }

    private static void expectValidationFailure(String name, Runnable action) {
        try {
            action.run();
            throw new AssertionError(name + " was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected: GeneratedNodes distinguishes an absent mandatory field from its default value.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
