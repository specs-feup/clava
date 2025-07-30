#include <iostream>
#include <stdlib.h>
#include <assert.h>

using namespace std;

#define N 1000
typedef double FLOAT;

#define MIN(x, y) (((x)<(y))? (x):(y))

static void NaiveMatrixMultiply(FLOAT* A, FLOAT* B, FLOAT* C, const int n)
{
    for (int i=0; i<n; i++)
        for (int j=0; j<n; j++)
            for (int k=0; k<n; k++)
                C[i*n+j] += A[i*n+k]*B[k*n+j];
}

static void BlockedMatrixMultiply(FLOAT* A, FLOAT* B, FLOAT* C, const int n)
{
#pragma isat tuning name(tune_mm) scope(M1_begin, M1_end) measure(M2_begin, M2_end) variable(blk_k, range($L1_CACHE_LINESIZE, 16*$L1_CACHE_LINESIZE, $L1_CACHE_LINESIZE/2)) variable(blk_j, range($L1_CACHE_LINESIZE, 16*$L1_CACHE_LINESIZE, $L1_CACHE_LINESIZE/2)) variable(blk_i, range($L1_CACHE_LINESIZE, 16*$L1_CACHE_LINESIZE, $L1_CACHE_LINESIZE/2)) // add "search(dependent)" at the end of this pragma if you want to search the variables dependently

#pragma isat marker M1_begin
    
    const int blk_i= 64;
    const int blk_j= 64;
    const int blk_k= 64;
    
    for (int i=0; i<n; i += blk_i)
        for (int j=0; j<n; j += blk_j)
            for (int k=0; k<n; k += blk_k)
                for (int ii=i; ii<MIN(i+blk_i, n); ii++)
                    for (int jj=j; jj<MIN(j+blk_j, n); jj++)
                        for (int kk=k; kk<MIN(k+blk_k, n); kk++)
                            C[ii*n+jj] = C[ii*n + jj] + A[ii*n+kk]*B[kk*n+jj];
#pragma isat marker M1_end
}

static void Initialize(FLOAT* X, const int n, const FLOAT v)
{
    for (int i=0; i<n; i++)
        for (int j=0; j<n; j++)
            X[i*n + j] = v;
}

static FLOAT Sum(FLOAT* X, const int n)
{
    FLOAT s = 0;
    
    for (int i=0; i<n; i++)
        for (int j=0; j<n; j++)
            s += X[i*n + j];

    return s;
}

int main(int argc, char** argv)
{
    FLOAT* A = new FLOAT[N * N];

    FLOAT* B = new FLOAT[N * N];

    //FLOAT* C_naive = new FLOAT[N * N];
	//assert(C_naive);

    FLOAT* C_blocked = new FLOAT[N * N];
    
    Initialize(A, N, 1);
    Initialize(B, N, 2);
    //Initialize(C_naive, N, 0);
    Initialize(C_blocked, N, 0);

#pragma isat marker M2_begin    
    BlockedMatrixMultiply(A, B, C_blocked, N);
#pragma isat marker M2_end
    
    const FLOAT s_blocked = Sum(C_blocked, N);
    
    //NaiveMatrixMultiply(A, B, C_naive, N);
    //const FLOAT s_naive = Sum(C_naive, N);

    //cout << "Sum-naive = " << s_naive << endl;
    cout << "Sum-blocked = " << s_blocked << endl;
}