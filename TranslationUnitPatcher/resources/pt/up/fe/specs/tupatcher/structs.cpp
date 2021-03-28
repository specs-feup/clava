TYPE_PATCH_00 test_structs(){
	//line comment
	A a = {1,2,3,4};
	/*block comment*/
	B b(1,2,3,4);
	C c = b.b2;
	D d = /* another comment*/ {1,2,3,4};
	b.b1 = 0;
	E e;
	e.e1.e2 = 0;
	//the following example doesn't work:
	//F f {{},1};
	return c;
}
