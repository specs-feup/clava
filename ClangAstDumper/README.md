# Building

```sh
mkdir build
cd build
cmake ..
make
```

## Dependencies

**Node.js is required to build this project**

```sh
# Required for all targets
sudo apt install clang-16 libclang-16-dev llvm-16-dev

# Required for building the stand-alone tool
sudo apt install libpolly-16-dev libedit-dev libzstd-dev
```