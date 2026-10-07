#define EMPTY_BODY {}

__attribute__((malloc))
void *allocate(void) {
    return 0;
}

__attribute__((nothrow))
__attribute__((const))
float custom_sqrt(float x);

void update_target_data(int x) {
#pragma omp target enter data map(to: x)
#pragma omp target exit data map(from: x)
#pragma omp target update to(x)
}

void preserve_structured_bodies(int *x) {
#pragma omp parallel
    {
    }
#pragma omp parallel
    {
        ++*x;
    }
}

void preserve_macro_body(int *x) {
#pragma omp parallel
    EMPTY_BODY
    *x = 7;
}
