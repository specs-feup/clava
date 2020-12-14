#define tick long long

static __inline__ tick gettick (void) {
    unsigned a, d;
    __asm__ __volatile__("rdtsc": "=a" (a), "=d" (d) );
	
	 __asm__ ( "movl $10, %eax;"
                "movl $20, %ebx;"
                "addl %ebx, %eax;");
	
	int a___d0, a___d1;
    __asm__ __volatile__("cld; rep; stosq"
        :"=c" (a___d0), "=D" (a___d1)
        :"a" (0), "0" (sizeof(long long) / sizeof(int)), "1" (10)
        :"memory");
	
    return (((tick)a) | (((tick)d) << 32));
}