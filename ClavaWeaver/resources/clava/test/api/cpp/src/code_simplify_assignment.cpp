#include <cstdlib>

struct obj
{
    int **matrix;
};

int main(void)
{
    int i = 1;
    i *= 2;
    i /= 2;
    i %= 3;
    i += i;
    i -= 4;
    i <<= 2;
    i >>= 2;
    i &= 15;
    i ^= i;
    i |= 2;
    int *row = (int *)malloc(4);
    int **col = (int **)malloc(8);
    col[0] = row;
    struct obj o;
    o.matrix = col;
    struct obj *ref = &o;
    ref->matrix[0][0] += 1;
    o.matrix += 1;
    return ref->matrix[-1][0];
}
