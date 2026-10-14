int asmGoto(int value) {
    asm goto volatile inline("test %0, %0\n\tjne %l[target]" : : "r"(value) : : target);
    return value;
target:
    return 0;
}
