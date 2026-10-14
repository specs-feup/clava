# Clang AST parser resources

## Bundled include archives

An include archive's `entrypoints.txt` lists search-root directories relative to the extracted archive, in search order. A root that contains an immediate `*.framework/Headers` directory is treated as a framework search root and passed to Clang with `-iframework`.

Framework bundles must expose headers at that canonical `Headers/` path, even when their underlying files live in a versioned framework directory.

Other bundled roots continue to use `-isystem`. User-provided `SYSTEM_INCLUDES` also use `-isystem`. Regular include flags from Clava, such as `-I`, retain their normal handling.

The libc `AUTO` probe checks system libc without bundled include roots. If it selects bundled libc, the normal parse invocation applies the framework-root rules above.
