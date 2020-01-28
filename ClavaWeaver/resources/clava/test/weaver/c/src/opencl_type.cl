struct foo {
    int hello;
    global float* world;
};

__kernel __attribute__((reqd_work_group_size(128, 1, 1)))
__attribute__((vec_type_hint(int)))
__attribute__((work_group_size_hint(128, 1, 1)))
void vectoradd(global int* A, global int* B ) {
    int i = get_global_id(0);
    struct foo f;
    f.hello = 1;
    if (i < 100)
    A[i] = B[i] * 2;
}

