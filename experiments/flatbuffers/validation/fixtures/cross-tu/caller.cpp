#include "api.h"

int caller(int value) {
    return service(value) + 1;
}

int main() {
    return caller(4);
}
