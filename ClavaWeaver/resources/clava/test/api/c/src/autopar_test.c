#include <stdio.h>

double bar(int a) {
    return a + 10;
}

double foo() {
    double a = 0;

    int numIter = 1000;
    for(int i=0; i<numIter; i++) {
        a+= i;
    }

    for(int i=0; i<1000; i++) {
        a += bar(i);
    }
    
    for(int i=0; i<1000; i++) {
        printf("Function with possible side-effects\n");
    }
    
    #pragma clava data safeFunctions:['printf']
    for(int i=0; i<1000; i++) {
        printf("Ignore this function, consider it safe");
    }
    

    
    return a;
}

int main() {
    
    foo();
 
}
