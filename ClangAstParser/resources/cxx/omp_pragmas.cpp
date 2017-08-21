double bar() {
    return 1.0;
}

double foo() {
    double a = 0;
    
    #pragma omp parallel num_threads(4) 
    {
        #pragma omp for shared(i)
        for(int i=0; i<10; i++) {
            a += i;
        }
        
    }

    #pragma omp parallel proc_bind(close) 
    {
        #pragma omp for shared(i)
        for(int i=0; i<10; i++) {
            a += i;
        }
        
    }

    #pragma omp parallel for schedule(monotonic:static,1)
    for(int i=0; i<1000; i++) {
        a += bar();
    }
    
    return a;
}

int main() {
       
    foo();

}
