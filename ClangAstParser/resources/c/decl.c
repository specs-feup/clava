typedef char base;

int foo() {	
	return 2;
}

float* fooPointer(float* aPointer) {
	return aPointer;
}

typedef double aType;

aType fooTypedef() {
	return 2.0;
}

void * polybench_alloc_data(unsigned long long n, int elt_size);

void vlatypes(int ni, int a[3][ni]) {}

typedef void(*function_prt_with_vla) (int ni,int nj,double tmp[ni + 0][nj + 0]);

void exact(int i, int j, int k, double u000ijk[]);

int CompareSizesByValue(void const * sz0, void const * sz1) {
	// Test multi declaration where both declarations are pointers
   long long * size0 = (long long *) sz0, *size1 = (long long *) sz1;
   int res = 0;
   if(*size0 - *size1 > 0) res = 1;
   else if(*size0 - *size1 < 0) res = -1;
   
   return res;
}

int main() {
	float* aFloatPointer;
	
	int fooResult = foo();
	float * fooPointerResult = fooPointer(aFloatPointer);
	aType fooTypedefResult = fooTypedef();

	int n = 10;
	base (*seq)[n + 0];
	seq = (base(*)[n + 0])polybench_alloc_data (n + 0, sizeof(base));
	
	int ** matrix1, **matrix2, *simplePointer, noPointer;
	int a, b;
}
