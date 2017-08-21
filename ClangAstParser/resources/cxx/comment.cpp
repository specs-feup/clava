
int foo // Comment between function name and arguments
(int a) {
	return a;
}

// Comment before main function

int main() {
	
	#pragma before scope
	{
		int innerA = 0;
	}
	
	//! Inline Comment
	//! Inline Comment with comma -> "
	// Simple inline
	/* Multiline in a sigle line */
	/* Multiline single line with closing in next line
	*/
	/**
	 * Multiline style with
	 * several lines
	 */
	 #pragma generic pragma
	 #pragma generic multi \
	 line \
	 pragma 
	 
	int a;
	
	// Comment originally above while   
    while(1) { // Comment in front of while open bracket
		// Comment inside empty body
    } // Comment in front of while close bracket
	
	if(1)
	{ // Comment in front of if
	}
	
	try{
	}catch(...) { // Comment in front of catch
	}
	
	return 0;
	
	// Comment at the end of function
}

// Comment after main function