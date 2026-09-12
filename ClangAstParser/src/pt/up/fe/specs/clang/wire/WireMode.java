package pt.up.fe.specs.clang.wire;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** Experimental selector shared by parsing, cache compression and file lifetime. */
public final class WireMode {
    public static String mode() {
        String mode=System.getProperty("clava.astWire","text");
        if(!mode.equals("text")&&!mode.equals("flat-eager")&&!mode.equals("flat-lazy"))
            throw new IllegalArgumentException("Unknown clava.astWire mode: "+mode);
        return mode;
    }
    public static boolean enabled() {return !mode().equals("text");}
    public static boolean lazy() {return mode().equals("flat-lazy");}
    /** Mapped properties may outlive their original AST; retain immutable files until JVM exit. */
    public static void retainUntilExit(File folder) {
        try(var files=Files.walk(folder.toPath())) {
            // deleteOnExit runs in reverse registration order, children before directories.
            files.sorted().forEach(path->path.toFile().deleteOnExit());
        } catch(IOException e) {throw new java.io.UncheckedIOException(e);}
    }
}
