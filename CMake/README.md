# Clava CMake Package

CMake Package to call Clava from CMake files.

## Installing

Copy the contents of this folder to a folder called `Clava` in a place where CMake can find it (e.g., `/usr/local/lib/Clava`)

If you do not want to install the Clava CMake plugin, you can manually define the path in the CMakeLists.txt file with the variable Clava_DIR:

```
set(Clava_DIR <PATH_TO_CMAKE_CLAVA_PLUGIN>)
```

If you have Clava installed locally, you can specify what command should be used to run it by using the variable LOCAL_CLAVA:

```
set(LOCAL_CLAVA "npx clava classic")	
```

## Example

To apply a LARA file to the current code, write the following in the `CMakeLists.txt` file:

```
find_package(Clava REQUIRED)
clava_weave(<TARGET_NAME> <CLAVA_SCRIPT>)  
```

Where `<TARGET_NAME>` is the name of a CMake target (e.g., created with `add_library` or `add_executable`) and `<LARA_FILE>` is a .lara file.

To pass arguments to the LARA scripts, use the argument `ARGS` followed by a single string:

```
clava_weave(foo Bar.js ARGS "inputFile:'data.json', execute:true, iterations:10")  
```

You can pass flags as you would when calling Clava from the command-line with the argument `FLAGS` followed by the flags:

```
clava_weave(foo Bar.js FLAGS --parsing-threads 8)  
```

Additionally, you can pass flags directly to the JVM that launches Clava using the argument `JAVA_FLAGS`:

```
clava_weave(foo Bar.js JAVA_FLAGS -Xmx1024m)  
```
