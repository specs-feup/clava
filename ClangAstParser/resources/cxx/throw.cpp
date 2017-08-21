#include <stddef.h>

inline int func() noexcept {
    return 1;
}

int func2() noexcept(true) {
    return 1;
}

int func3() throw() {
    return 1;
}

int func4() throw(...) {
    return 1;
}

void func5() noexcept try {

} catch (...) {
    
}

