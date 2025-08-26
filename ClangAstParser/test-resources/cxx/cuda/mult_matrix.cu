#include <stdio.h>
#define TILE_DIM 16

__global__ void multiMatrix (int *a, int *b, int *c, int N) {
	int idx = threadIdx.x + blockDim.x * blockIdx.x;                
	int idy = threadIdx.y + blockDim.y * blockIdx.y;                
    int pos = idx + idy * N;                                        
	int temp_result = 0;  
	int posa, posb;                                          

	__shared__ int s_a[TILE_DIM][TILE_DIM];
	__shared__ int s_b[TILE_DIM][TILE_DIM];

	for (int tile_idx = 0; tile_idx < gridDim.x; tile_idx++) 
	{
        posa = idy * N + (tile_idx * TILE_DIM + threadIdx.x);
        posb = (tile_idx * TILE_DIM + threadIdx.y) * N + idx;
		
		if (posa < N*N) {
			s_a[threadIdx.y][threadIdx.x] = a[posa];
        } else {
            s_a[threadIdx.y][threadIdx.x] = 0;
        }
        if (posb < N*N) {
            s_b[threadIdx.y][threadIdx.x] = b[posb];
        } else {
            s_b[threadIdx.y][threadIdx.x] = 0;
        }
                                                                    
        __syncthreads();
		if (idx < N && idy < N) {
			for (int i=0; i < TILE_DIM; i++) {
                temp_result += s_a[threadIdx.y][i] * 
					s_b[i][threadIdx.x]; 
			}
		}
            
        __syncthreads();  
    }
    __syncthreads();   
    
    if(idx < N && idy < N)  {
		c[pos] = temp_result;
	} 
} 

int main (int argc, char* argv[]){
	int N = 4; 
	size_t size = N*N*sizeof(int);
	int num_thread, num_block;

	int *h_a, *h_b, *h_c;
	h_a = (int*)malloc(size);
	h_b = (int*)malloc(size);
	h_c = (int*)malloc(size);

	int *d_a, *d_b, *d_c;
	cudaMalloc(&d_a, size);
	cudaMalloc(&d_b, size);
	cudaMalloc(&d_c, size);

    int i = 0, j = 0;
	for (i = 0; i < N*N; i++){
		h_a[i] = h_b[i] = i;
	}
	
    cudaMemcpy(d_a,h_a,size,cudaMemcpyHostToDevice);
	cudaMemcpy(d_b,h_b,size,cudaMemcpyHostToDevice);
	cudaMemset(d_c,0,size);

	num_block = ceil((float)N/TILE_DIM);
    num_thread = N < TILE_DIM ? N : TILE_DIM;

	printf("Blocks: %d    Threads: %d  \n", num_block, num_thread);

	dim3 gridsize(num_block,num_block,1);
	dim3 blocksize(num_thread,num_thread,1);
  
    multiMatrix<<<gridsize,blocksize>>>(d_a, d_b, d_c, N);

    cudaMemcpy(h_c, d_c, size, cudaMemcpyDeviceToHost);


	cudaFree(d_a);
	cudaFree(d_b);
    cudaFree(d_c);
    free(h_a);
    free(h_b);
    free(h_c);
}