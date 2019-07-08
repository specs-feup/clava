# CMake Plugin

Plugin to call Clava from CMake files.

## Installing

Copy the contents of this folder to a folder called `Clava` in a place where CMake can find it (e.g., `/usr/local/lib/Clava`)

Alternatively, if you are on Linux, you can download the installation script [clava-update](https://raw.githubusercontent.com/specs-feup/clava/master/install/linux/clava-update), which installs Clava on the folder where you run the script. If you run the script with `sudo`, it will install the CMake plugin in `/usr/local/lib/Clava`. 


If you do not want to install the Clava CMake plugin, you can manually define the path in the CMakeLists.txt file with the variable Clava_DIR:

```
set(Clava_DIR <PATH_TO_CMAKE_CLAVA_PLUGIN>)
```

## Example

To apply a LARA file to the current code, write the following in the `CMakeLists.txt` file:

```
find_package(Clava REQUIRED)
clava_weave(<TARGET_NAME> <LARA_FILE>)  
```

Where `<TARGET_NAME>` is the name of a CMake target (e.g., created with `add_library` or `add_executable`) and `<LARA_FILE>` is a .lara file.
