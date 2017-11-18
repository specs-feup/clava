double foo() {

    double a = 0;
    
    #pragma omp parallel num_threads(4 + 2) , proc_bind(master)
    {
        #pragma omp for shared(i) private(a) collapse(1) ordered(2 + 1) ordered reduction(+: a)
        for(int i=0; i<10; i++) {
            a += i;
        }
        
    }

}

int main() {
       
    foo();

}
