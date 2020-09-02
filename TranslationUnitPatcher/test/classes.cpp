TYPE_PATCH_00 test_classes(){
	A a = /*r*/A(2,3);
	a.a1();
	B b = a.a2->a3();
	C c;
	c.c1 = A::a4();
	/*the following example doesn't work:
	D d();
	d.d1(); //BASE_OF_MEMBER_REFERENCE_IS_A_FUNCTION
	*/
	return c.c2();
}
