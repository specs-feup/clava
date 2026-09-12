struct Outer { union { struct { int x; }; int y; }; int z; };
struct Outer value = {.x = 3, .z = 4};
int main(void) { return value.x + value.z; }
