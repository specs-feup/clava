#include <string>

void foo() // Inline comment, before compound (unsupported)
{ // Inline, after compound
}

int main() { // Inline in function main, after compound
	int test = 0;
	
	test = 0
		+ 1; // Inline after statement that ends in a different line that started
		
	test = 0
	  + 1 // Inline in the middle of a statement 
	  + 2; 

	test = 0
	  + 2 // Several comments (1/2) 
	  + 3; // In the same statement (2/2)
	  
	test = 0
	  + 2 // Inline comments (1/2) 
	      // With a hole (2/2) (unsupported)
	  + 3;
	  
	// Simple if
	if(0) {
		test = 10;
	}
	
	// Simple empty if
	if(0) {
		
	}
	
	// If with else
	if(0) {
		test = 100;
	} else {
		test = 200;
	}
	
	// Declaration inside if
	if(int a = 0) {
		test = 20;
	}
	
	// Declaration inside empty if
	if(int a = 0) {

	}

	// Declaration inside empty if
	if(int a = 0) {
		test = 1000;
	} else {
		test = 2000;
	}
	
	// Chained if
	if(0) { // Inline after a compound stmt
		// Inline inside compound stmt
	} else if(1) {
		
	} else {
		
	}
	
	int currentDay = -1;
	std::string currentDayString = "Monday";

	if (currentDayString == "Monday") currentDay = 0; // Monday
	else if (currentDayString == "Tuesday") currentDay = 1; // Tuesday
	else if (currentDayString == "Wednesday") currentDay = 2; // Wednesday
	else if (currentDayString == "Thursday") currentDay = 3;
	else if (currentDayString == "Friday") currentDay = 4;
	else if (currentDayString == "Saturday") currentDay = 5;
	else if (currentDayString == "Sunday") currentDay = 6;
	
	int conditional = 0 ? 1 : 10;
	
	return 0;
}

int a; // Inline comment at the end of the file with code behind