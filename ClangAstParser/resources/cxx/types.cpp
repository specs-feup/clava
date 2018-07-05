/*
// Not working yet
// Dependent sized array type
template<typename T, int Size>
class array {
  T data[Size];
};
*/

int foo();

int main() {
	
	// Constant array type
	int array1[4 + 4*100];
	
	// Variable array type
	int array3[10+foo()]; 


}
