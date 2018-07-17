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

void ifClause(int test, int parallel, int parallel2) {
	
	#pragma omp parallel for if( parallel : test) private(i)
    for(int i=0; i<1000; i++) {
    }
	
	#pragma omp parallel if(parallel:test)
    for(int i=0; i<1000; i++) {
    }
	
	#pragma omp parallel if(parallel)
    for(int i=0; i<1000; i++) {
    }	

	#pragma omp parallel if(parallel+2)
    for(int i=0; i<1000; i++) {
    }		

	#pragma omp parallel if(parallel2)
    for(int i=0; i<1000; i++) {
    }			
}


void complicated_omp_pragma() {
	#pragma omp parallel for  default(shared)  num_threads((abs(1 - nz2)<1000)?1:omp_get_max_threads()) private(k, j, i, m, add) firstprivate(nz2, ny2, nx2) reduction (+:rms[:5])
	for(int i=0; i<1000; i++) {
	}
}

int main() {
       
    foo();

}
