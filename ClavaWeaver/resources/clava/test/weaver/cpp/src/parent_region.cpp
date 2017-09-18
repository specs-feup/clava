int z;

double bar() {
    return 1.0;
}

double foo(int fooParam) {
    double a = 0;
    
    for(int i=0; i<1000; i++) {
        a += bar();
        
        if(a) {
			a = 2;
        } else {
			a = 3;
        }
        
        {
            int insideScope;
        }
    }
    
    return a;
}

int main() {
    
    foo(10);

}
