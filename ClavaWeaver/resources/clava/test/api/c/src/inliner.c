#include <stdio.h>

void funA(int x, int *X, int *Y) {
    for (int i = 0; i < 100; i++) {
        X[i] = Y[i] * x;
    }
}

int funD(int x) {
    return x * x * x;
}

int funB(int *X, int *Y) {
    for (int i = 0; i < 100; i++) {
        X[i] = X[i] * Y[i];
    }

    for (int i = 0; i < 100; i++) {
        Y[i] = X[i] + Y[i];
        Y[i] = funD(Y[i]);
    }

    for (int i = 0; i < 100; i++) {
        Y[i] = X[i] * Y[i];
        Y[i] = funD(Y[i]);
    }
    return X[0] + Y[0];
}

int funC(int x, int y, int *X) {
    funB(X, X);
    return funD(x) + funD(y) + X[6];
}

int main() {
    int A[100] = {1};
    int B[100] = {2};
    int C[100] = {3};

    funA(500, A, B);
    int x = funB(B, C);
	
    
    for (int ii = 0; ii < 100;ii++) {
       
    }

    for (int i = 0; i < 100; i++) {
        for (int j = 0; j < 100; j++) {
            int y = x + A[0];
            funC(x, y, C);
            funC(y, x, C);
        }
    }

    int sumA = 0;
    int sumB = 0;
    int sumC = 0;
    for (int i = 0; i < 100; i++) {
        sumA += A[i];
        sumB += B[i];
        sumC += C[i];
    }


    return A[2];
}

int repeatedCall(int i) {
	if(i == 0) {
		return 1;
	}
	
	return 2;
}

int inlineTest2() {
	int a = 0;

	int b = repeatedCall(a);
	a = b;
	b = repeatedCall(a);
	a = b;

	return a;
}



int arrayParamL3(int key[32]) {
	return key[0];
}

int arrayParamL2(int key[32]) {
	return arrayParamL3(key);
}

int arrayParamL1(int key[32]) {
	return arrayParamL2(key);
}


void arrayParam() {
    int a[32];
    int b;
    
    b = arrayParamL1(a);
    
}


int functionThatUsesGlobal();

int functionThatCallsFunctionThatUsesGlobal() {
	return functionThatUsesGlobal();
}

const int globalVar[2] = {1, 2};

int functionThatUsesGlobal() {
	return globalVar[0];
}


int functionThatReturns(int a) {
	if(a) {
		return 1;
	}
	
	return 0;
}
  
void functionThatCallsFunctionWithReturnButsDoesNotUseResult() {
	functionThatReturns(10);
}

int functionWithComments() {
	// This is a comment
	return 1;
}

int functionWithNakedIf(a) {
	int b = 0;
	if(a) b = functionWithComments();
	return b;
}

// Forward declaration
double functionWithCall(int a);

double functionWhichCallIsNotDeclared() {
	return functionWithCall(10);
}

double functionCalledByOtherFunction(double a) {
	return a + 10;
}

double functionWithCall(int a) {
	return functionCalledByOtherFunction(a) + functionCalledByOtherFunction(a);
}


typedef struct {
   double a;
   double b;
} a_struct;

double functionWithVarInStruct(a_struct a) {
	double m;
	m = 10;
	a = (a_struct){m, 20};
	
	return a.a;
}

double functionThatCallsOtherWithVarInStruct() {
	a_struct b;
	
	return functionWithVarInStruct(b);
}

void functioWithStruct(int n, int xd1, void *ox, a_struct exponent[n])
{
    a_struct (*x)[xd1] = (a_struct (*)[xd1])ox;
}

void functionThatCallsFunctionWithStruct(void* ox2) {
	int n2 = 3;
	int xd2 = 2;
	a_struct exponent2[3];
	functioWithStruct(n2, xd2, ox2, exponent2);
}


void functionWith2DimPointer(void * os, int n, int m)
{
    double (*r)[n][m] = (double (*)[n][m]) os;
}

void functionThatCallFunctionWith2DimPointer(void * os) {
	functionWith2DimPointer(os, 10, 20);
}