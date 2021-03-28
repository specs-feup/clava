TYPE_PATCH_00 test_nested(){
	A::static_function(arg0, arg1);
	A::A1 a1 = A::A1();
	/* the following examples don't work
	
	//this one doesn't work because the fields inside the class are not ordered:
	A::A1 a3 = A::a2();

	//the following 2 don't work because the ErrorPatcher can't
	//find nested type or function inside another nested type
	A::A1::static_function(arg0, arg1);
	
	B::B1::B2 b();
	b.bbb();
	
	*/
	if (a1.a2() == B::c){
		return true;
	}
	else {
		// the following example doesn't work
		//the variable c should be constant
		//see function caseValueIsNotConst in the class ErrorPatcher
		/*switch (a1.a3){
			case B::c:
				return true;
		}*/
		return false;
	}

}
