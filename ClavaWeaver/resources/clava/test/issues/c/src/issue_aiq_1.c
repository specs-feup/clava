#include <stdint.h>
#include <stddef.h>
#include <stdio.h>

int main(void) {
    int32_t result = 0;
    int8_t vector[] = {10, 4, 8, 2, 6, 12};
    int8_t matrix_col[] = {12, 6, 2, 8, 4, 10};

    size_t len = 6;

    for (size_t i = 0; i < len; i++) {
        result += vector[i] * matrix_col[i];
    }

    printf("%d\n", result);
    return 0;
}