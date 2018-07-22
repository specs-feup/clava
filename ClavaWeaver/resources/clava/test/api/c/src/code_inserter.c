#include <stdio.h>
#define N 1000

double bar(int a) {
    return a + 10;
}

double foo() {
    double a = 0;

    for(int i=0; i<N; i++) {
        a+= i;
    }

    for(int i=0; i<N; i++) {
        a += bar(i);
    }
    
    for(int i=0; i<N; i++) {
        printf("Function with possible side-effects\n");
    }
    
    #pragma clava data safeFunctions:['printf']
    for(int i=0; i<N; i++) {
        printf("Ignore this function, consider it safe");
    }
    

    
    return a;
}

int main() {
    
    foo();
 
}
