#define SCALE(x) ((x) * 2)
/* The text n123 and 0x123abc_1 must remain source text. */
const char *text = "n123 0x123abc_1";
int f(int x) {
    int a[] = { 1, 2, 3 };
    int *p = a;
    int v = SCALE(x);
    v += (int)3.5;
    ++v;
    return v + p[1];
}
