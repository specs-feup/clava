#include <stdio.h>

#define NSTREAM 4
#define BDIM 128

void printArray(float *a, int size){
    for (int i = 0; i < size; i++){
        if(i % 128 == 0)
            printf("\n");
        printf("%.0f ", a[i]);
    }
    printf("\n\n");
}


__global__ void sumArrays(float *A, float *B, float *C, const int N)
{
    int idx = blockIdx.x * blockDim.x + threadIdx.x;

    if (idx < N)
    {
        if( idx == N-1)
            C[idx] = A[idx+2] + B[idx+2];
        else
            C[idx] = A[idx] + B[idx];
    }
}

int main(int argc, char **argv)
{
    printf("> %s Starting...\n", argv[0]);

    // set up data size of vectors
    int nElem = 1 << 9;
    printf("> vector size = %d\n", nElem);
    size_t nBytes = nElem * sizeof(float);

    // malloc pinned host memory for async memcpy
    float *h_A, *h_B, *gpuRef;
    cudaHostAlloc((void**)&h_A, nBytes, cudaHostAllocDefault);
    cudaHostAlloc((void**)&h_B, nBytes, cudaHostAllocDefault);
    cudaHostAlloc((void**)&gpuRef, nBytes, cudaHostAllocDefault);

    // initialize data at host side
    for (int i = 0; i < nElem; i++)
    {
        h_A[i] = h_B[i] = i;
    }
    memset(gpuRef,  0, nBytes);

    // malloc device global memory
    float *d_A, *d_B, *d_C;
    cudaMalloc((float**)&d_A, nBytes);
    cudaMalloc((float**)&d_B, nBytes);
    cudaMalloc((float**)&d_C, nBytes);

    // invoke kernel at host side
    dim3 block (BDIM);
    dim3 grid  ((nElem + block.x - 1) / block.x);
    printf("> grid (%d, %d) block (%d, %d)\n", grid.x, grid.y, block.x,
            block.y);

    // sequential operation
    cudaMemcpy(d_A, h_A, nBytes, cudaMemcpyHostToDevice);
    cudaMemcpy(d_B, h_B, nBytes, cudaMemcpyHostToDevice);

    sumArrays<<<grid, block>>>(d_A, d_B, d_C, nElem);

    cudaMemcpy(gpuRef, d_C, nBytes, cudaMemcpyDeviceToHost);

    printf("\n");
    printArray(gpuRef, nElem);

    // grid parallel operation
    int iElem = nElem / NSTREAM;
    size_t iBytes = iElem * sizeof(float);
    grid.x = (iElem + block.x - 1) / block.x;

    cudaStream_t stream[NSTREAM];

    for (int i = 0; i < NSTREAM; ++i)
    {
        cudaStreamCreate(&stream[i]);
    }


    // initiate all work on the device asynchronously in depth-first order
    for (int i = 0; i < NSTREAM; ++i)
    {
        int ioffset = i * iElem;
        cudaMemcpyAsync(&d_A[ioffset], &h_A[ioffset], iBytes,
                              cudaMemcpyHostToDevice, stream[i]);
        cudaMemcpyAsync(&d_B[ioffset], &h_B[ioffset], iBytes,
                              cudaMemcpyHostToDevice, stream[i]);
        sumArrays<<<grid, block, 0, stream[i]>>>(&d_A[ioffset], &d_B[ioffset],
                &d_C[ioffset], iElem);
        cudaMemcpyAsync(&gpuRef[ioffset], &d_C[ioffset], iBytes,
                              cudaMemcpyDeviceToHost, stream[i]);
    }

    // check kernel error
    cudaGetLastError();

    // free device global memory
    cudaFree(d_A);
    cudaFree(d_B);
    cudaFree(d_C);

    // free host memory
    cudaFreeHost(h_A);
    cudaFreeHost(h_B);
    cudaFreeHost(gpuRef);


    // destroy streams
    for (int i = 0; i < NSTREAM; ++i)
    {
        cudaStreamDestroy(stream[i]);
    }

    cudaDeviceReset();
    return(0);
}