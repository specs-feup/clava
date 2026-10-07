typedef struct { int value; } something;
typedef const something const_something;
something ordinary;
const_something qualified;
int update(void) { ordinary.value = 1; return qualified.value; }
