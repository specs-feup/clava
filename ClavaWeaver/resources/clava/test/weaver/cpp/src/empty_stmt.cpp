int main() {
	// An empty statement
	;
	
	// Empty statements that should not be converted to emptyStmt
	for(;;);
	
	// Else is an empty statement that should not be converted to emptyStmt
	if(true) {
	}
	
	while(true);
	
	if(false);
	
	{
		// Another empty statement
		;
	}
}