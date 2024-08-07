# Building

```sh
mkdir build
cd build
cmake ..
make
```

The CMakeLists.txt has two targets, `plugin` and `tool`, use `<MAKE_CMD> <target>` to build a specific target.

The target `tool` has been successfully built in Ubuntu and Windows (MinGW), make sure the executable clang-<VERSION> is in the path, as well as the path `/usr/lib/llvm-<VERSION>` (`C:/usr/...` in Windows).

## Dependencies

**Node.js is required to build this project**

```sh
# Required for all targets
sudo apt install clang-16 libclang-16-dev llvm-16-dev

# Required for building the stand-alone tool
sudo apt install libpolly-16-dev libedit-dev libzstd-dev
```