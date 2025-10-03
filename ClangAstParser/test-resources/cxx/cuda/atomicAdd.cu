#include <stdio.h>

__global__ void add(int *a){
    int id = blockDim.x * blockIdx.x + threadIdx.x;
    int idx = id % 10;
    //a[idx] += 10;
    atomicAdd(&a[idx], 10);
}

int main(){
    int *a;
    size_t size = 10 * sizeof(int);

    cudaMallocManaged(&a, size);

    add<<<10,1024>>>(a);
    cudaDeviceSynchronize();

    for (int i = 0; i < 10; i++)
        printf("%d ", a[i]);

}