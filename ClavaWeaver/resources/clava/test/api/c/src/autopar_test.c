double bar(int a);

double bar(int a) {
    return a + 10;
}

double foo() {
    double a = 0;

    for(int i=0; i<1000; i++) {
        //a += i;
        a += bar(i);
    }
    
    return a;
}

int main() {
    
    foo();
 
}