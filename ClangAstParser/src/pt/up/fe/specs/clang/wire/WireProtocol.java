package pt.up.fe.specs.clang.wire;

/** Production AST wire format. */
public final class WireProtocol {
    private WireProtocol() {}

    public static final String FORMAT = "flatbuffers-v2";
    public static final int SCHEMA_VERSION = 2;
    public static final String FLATBUFFERS_VERSION = "25.12.19";
    public static final String FLATBUFFERS_COMMIT = "7e163021e59cca4f8e1e35a7c828b5c6b7915953";
}
