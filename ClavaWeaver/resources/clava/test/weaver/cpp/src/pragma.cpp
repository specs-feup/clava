#pragma functionPragma
int main() {
	
	int a;
	
	#pragma statementPragma
	a = 0;
	
	#pragma scopePragma
	{
		a = a + a;
	}

	#pragma lara marker foo
    {
  	  #pragma lara marker bar
  	  {
  		int a = 0;
  	  }
    }
	
}