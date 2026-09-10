#include "header space/piece.h"
#define PLUS_ONE(v) ((v) + 1)
namespace names { int value = 3; }
template<class T> T identity(T v) { return v; }
const char *identity_text = "n123 0x123abc_1";
unsigned long long large = 18446744073709551615ULL;
int source_continuation = 12\
34;
int check(int value) {
    int *pointer = nullptr;
    int result = PLUS_ONE(value) + names::value;
    result += identity<int>(value);
    result += static_cast<int>(2.5);
    result += (int)3.5;
    result++;
    --result;
    return detail::increment(result) + (pointer == nullptr);
}
