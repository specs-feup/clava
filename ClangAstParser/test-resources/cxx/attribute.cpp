#include <stdlib.h>

struct aligned_struct { short f[3]; } __attribute__ ((aligned (8)));
struct S { short f[3]; } __attribute__ ((aligned));

__attribute__((always_inline))
void myk(float* A) 
{
    int i = 1;
}

// Format
extern void print_like_1(int*, int, const char *text, ...) __attribute__ ((__format__ (__printf__, 3, 4))) __attribute__ ((__nonnull__ (4)));
extern void print_like_2(int*, const char *text, ...) __attribute__ ((__format__ (printf, 2, 3)));

// NonNull
extern void * my_memcpy1 (void *dest, const void *src, size_t len)
        __attribute__((nonnull (1, 2)));
extern void * my_memcpy2 (void *dest, const void *src, size_t len)
        __attribute__((nonnull));
		