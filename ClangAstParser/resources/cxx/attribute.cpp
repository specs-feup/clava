struct aligned_struct { short f[3]; } __attribute__ ((aligned (8)));
struct S { short f[3]; } __attribute__ ((aligned));

__attribute__((always_inline))
void myk(float* A) 
{
    int i = 1;
}