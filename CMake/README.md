# CMake Plugin

Plugin to call Clava from CMake files.

## Installing

Copy the contents of this folder to a folder called `Clava` in a place where CMake can find it (e.g., `/usr/local/lib/Clava`)

## Example

To apply a LARA file to the current code, write the following in the `CMakeLists.txt` file:

```
find_package(Clava REQUIRED)
clava_weave(<TARGET_NAME> <LARA_FILE>)  
```

Where `<TARGET_NAME>` is the name of a CMake target (e.g., created with `add_library` or `add_executable`) and `<LARA_FILE>` is a .lara file.
