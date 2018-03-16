#include <array>
#include <vector>
#include <cstdlib>
#include <algorithm>

class B {
    
};

class A
{
 public:
 static constexpr int number_of_sizes = 3;
 static constexpr std::array<int, number_of_sizes> SPACE{{512, 256, 128}};
 static constexpr B SPACE2{};

 int a;
 
 void xpto();
};

constexpr std::array<int, A::number_of_sizes> A::SPACE;
constexpr B A::SPACE2;

void A::xpto() {
    
    return;
}


template< typename T >
void matrix_mult_tiling(const std::vector<T>& A , const std::vector<T>& B, std::vector<T>& C, const int N, const int M, const int K, const int BS1, const int BS2) {

   for(int i=0; i<N; i++) {
       for(int j=0; j<K; j++) {
           C[K*i + j] = 0;
       }
    }

    for(int l2=0; l2<M; l2 += BS1) {
        for(int j2=0; j2<K; j2 += BS2) {
            for(int i=0; i<N; i++) {
                for(int l=l2; l< std::min(M, l2+BS1); l++) {
                    for(int j=j2; j< std::min(K, j2+BS2); j++) {
                        C[K*i + j] += A[M*i+l]*B[K*l+j];
                    }
                }
            }
        }
    }
}

int main() {
    
    // declare the matrices
    std::vector<double> m1,m2,m3;

    // execute the kernel
    matrix_mult_tiling(m1, m2, m3, 2, 4, 7, 2, 2);
    
    return 0;
}
