import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;

/** Sequential framing scan with bounded mappings; node accessors keep their backing mapping. */
final class MappedRecords implements AutoCloseable {
    private static final long WINDOW = 64L * 1024 * 1024;
    private static final int MAX_RECORD = 16 * 1024 * 1024;
    private final FileChannel channel;
    private final long size;
    private long position, mappedStart = -1;
    private ByteBuffer mapped;
    record Frame(ByteBuffer buffer, int rootOffset, int bytes) {}

    MappedRecords(Path path) throws IOException {
        channel = FileChannel.open(path, StandardOpenOption.READ);
        size = channel.size();
    }

    Frame next() throws IOException {
        if (position == size) return null;
        if (size - position < 4) throw new EOFException("Truncated record length");
        if (mapped == null || position < mappedStart || position + 4 > mappedStart + mapped.limit()) remap();
        int offset = Math.toIntExact(position - mappedStart);
        int length = mapped.getInt(offset);
        if (length < 8 || length > MAX_RECORD || length > size - position - 4)
            throw new IOException("Invalid record length " + length + " at " + position);
        if (offset + 4L + length > mapped.limit()) {
            remap();
            offset = Math.toIntExact(position - mappedStart);
        }
        int root = mapped.getInt(offset + 4);
        if (root < 4 || root >= length) throw new IOException("Invalid root offset");
        var frame = new Frame(mapped, offset + 4 + root, length);
        position += 4L + length;
        return frame;
    }

    private void remap() throws IOException {
        mappedStart = position / WINDOW * WINDOW;
        long bytes = Math.min(size - mappedStart, WINDOW + MAX_RECORD + 4L);
        mapped = channel.map(FileChannel.MapMode.READ_ONLY, mappedStart, bytes).order(ByteOrder.LITTLE_ENDIAN);
    }

    public void close() throws IOException { channel.close(); }
}
