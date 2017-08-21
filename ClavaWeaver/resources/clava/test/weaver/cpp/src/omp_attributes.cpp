double foo() {

    double a = 0;
    
    #pragma omp parallel num_threads(4) , proc_bind(master)
    {
        #pragma omp for shared(i) private(a)
        for(int i=0; i<10; i++) {
            a += i;
        }
        
    }

}

int main() {
       
    foo();

}
