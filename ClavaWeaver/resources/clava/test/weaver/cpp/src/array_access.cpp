#include "array_access.h"

int main() {

	int a[2];
	int b[3];
	
	a[b[a[0]]] = a[0];

	int a2[2][3];
	int b2[4][5];
	
	a2[b2[1][1]][a2[1][2]] = b2[0][0];

    
    int a3[2][3][4];
    int b3[2][3][4];
    
    a3[b3[3][4][5]][2][3] = 0;

	double (*a4)[10][10];
	int i, *j;
	(*a4)[i][*j] = 0;
	

	A aArray[10];
	aArray[0].b[0].value = 0;

	B bInstance;
	aArray[0].b[1] = bInstance;

	A aInstance;
	aInstance.b[2].value = 1;
	aInstance.b[3] = bInstance;
	aInstance.value[0] = 2;
}