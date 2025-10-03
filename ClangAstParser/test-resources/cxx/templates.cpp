#include "templates.h"

#include <iostream>
#include <list>
#include <vector>


struct MAtom {
	int a;
};

std::list<MAtom>::iterator it_atom1;

template <typename t = float>
inline t Distance2(const t a[3], const t b[3])
{
	return (a[0] - b[0]) * (a[0] - b[0]) +
				 (a[1] - b[1]) * (a[1] - b[1]) +
				 (a[2] - b[2]) * (a[2] - b[2]);
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

// TODO: TypeParser not supporting some nodes
/*
template <template <typename> class m>
struct Monad {
    template <typename a>
    static m<a> mreturn(const a&);

    template <typename a, typename b>
    static m<b> mbind(const m<a>&, m<b>(*)(const a&));
};
*/

// TODO: Add support for code in the page http://en.cppreference.com/w/cpp/language/function_template
/*
template<typename T>
void f(T s)
{
    std::cout << s << '\n';
}
 
template void f<double>(double); // instantiates f<double>(double)
template void f<>(char); // instantiates f<char>(char), template argument deduced
template void f(int); // instantiates f<int>(int), template argument deduced
*/

int main() {
    
	// calling template function creates specialized versions that need to be removed
    std::vector<double> m1,m2,m3;
    matrix_mult_tiling(m1, m2, m3, 2, 4, 7, 2, 2);
	
	return 0;
}