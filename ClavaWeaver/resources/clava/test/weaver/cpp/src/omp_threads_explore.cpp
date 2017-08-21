int main()  {
    int a = 0;
    
    #pragma lara marker loop1
    {
        
		#pragma omp parallel for reduction( min : a, i )
		for(int i=0; i<10; i++) {
			a += i;    
		}
    
    }
}
