#include <stdio.h>

__global__ void sample (int *d_a, int N) {
	int idx = threadIdx.x + blockDim.x * blockIdx.x;                
	int idy = threadIdx.y + blockDim.y * blockIdx.y;                
    int pos = idx + idy * N;                                                                              

    if(pos % 2 == 0)
        d_a[pos] = 1;
} 

int main (int argc, char* argv[]) {
	int N = 8; 
	size_t size = N*N*sizeof(int);

	int *h_a;
	h_a = (int*)malloc(size);

	int *d_a;
	cudaMalloc(&d_a, size);

    int i = 0;
	for (i = 0; i < N*N; i++)
		h_a[i] = 0;
	
    cudaMemcpy(d_a,h_a,size,cudaMemcpyHostToDevice);
	sample<<<4,4>>>(d_a, N);
    cudaMemcpy(h_a, d_a, size, cudaMemcpyDeviceToHost);

	for(i = 0; i < N*N; i++)
		printf("%d ", h_a[i]);

	cudaFree(d_a);
    free(h_a);
}