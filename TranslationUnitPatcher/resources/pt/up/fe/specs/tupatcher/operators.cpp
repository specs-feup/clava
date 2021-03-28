TYPE_PATCH_00 test_operators(){
	A a0 = A();
	a0.k();
	a0 = a1;
	if (a0 == a1) {
		B b;
		a0 += b;
		C c = C();
		c -= a0;
		c << "sss";
		c >> "sss";
		c <<= "sss";
		c *= "sss";
		return a + c * b - b ^ d;
	}
	else if (a0 * a1 != c) {
		return a1 & c;
	}
}
/*
TYPE_PATCH_05 f5(){
    Array a5 = {1,2,3};
    return a5[0]; //Error: type 'Array' does not provide a subscript operator
}
*/