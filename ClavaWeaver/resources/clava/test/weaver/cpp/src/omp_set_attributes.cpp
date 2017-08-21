double foo() {

    double a = 0;
    
    #pragma omp parallel num_threads(4)
    {
        #pragma omp for shared(i) private(a)
        for(int i=0; i<10; i++) {
            a += i;
        }
        
    }

    #pragma omp parallel for schedule(monotonic:static,1) proc_bind(master)
    for(int i=0; i<1000; i++) {
        a += a * i;
    }

    return a;
}

int main() {
       
    foo();
}
