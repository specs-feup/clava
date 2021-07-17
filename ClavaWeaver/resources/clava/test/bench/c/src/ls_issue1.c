void matrix_mult_Kernel(double const *A, double const *B, double *C, int const N, int const M, int const K) {
   for(int ii = 0; ii < N; ii++) {
      for(int jj = 0; jj < K; jj++) {
         C[K * ii + jj] = 0;
      }
   }
   // main loop nest
   for(int i = 0; i < N; i++) {
      for(int l = 0; l < M; l++) {
         for(int j = 0; j < K; j++) {
            C[K * i + j] += A[M * i + l] * B[K * l + j];
         }
      }
   }
}