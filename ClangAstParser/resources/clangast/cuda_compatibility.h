#pragma once

#include <cuda_runtime.h>

extern "C" __host__ cudaError_t cudaConfigureCall(
    dim3 gridDim,
    dim3 blockDim,
    size_t sharedMem = 0,
    cudaStream_t stream = 0);
