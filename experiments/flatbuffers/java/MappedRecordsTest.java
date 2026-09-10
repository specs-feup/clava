import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;

/** Checks framing across mapping windows and the 2 GiB file boundary without allocating a large payload. */
public class MappedRecordsTest {
    public static void main(String[] args) throws Exception {
        Path path=Files.createTempFile("flat-mapping-", ".sparse");
        int count=140,length=16*1024*1024;
        try {
            try(var out=FileChannel.open(path,StandardOpenOption.WRITE)) {
                for(int i=0;i<count;i++) {
                    out.position((long)i*(length+4L));
                    var prefix=ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putInt(length).putInt(4).putInt(i).flip();
                    while(prefix.hasRemaining())out.write(prefix);
                }
                out.position(count*(length+4L)-1);out.write(ByteBuffer.wrap(new byte[]{0}));
            }
            try(var frames=new MappedRecords(path)) {
                for(int i=0;i<count;i++) {
                    var frame=frames.next();
                    if(frame==null||frame.buffer().getInt(frame.rootOffset())!=i)throw new AssertionError("Frame "+i);
                }
                if(frames.next()!=null)throw new AssertionError("Unexpected extra frame");
            }
            System.out.println("mapping_windows_and_large_file_passed bytes="+Files.size(path));
        } finally {Files.deleteIfExists(path);}
    }
}
