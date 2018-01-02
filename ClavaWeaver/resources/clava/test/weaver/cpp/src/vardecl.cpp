// Parameters should be captured as vardecl
int functionWithParam(int a, int b) {
	return a + b;
}

int functionWithDeclaration();

int functionWithDeclaration() {
	int a = 0;
	return a + 1;
}

int functionWithoutDeclaration() {
	return 2;
}

int functionWithoutDefinition();

int functionWithDefinitionAfterCall();

int main() {
	functionWithDeclaration();
	functionWithoutDeclaration();
	functionWithoutDefinition();
	functionWithDefinitionAfterCall();
}

int functionWithDefinitionAfterCall() {
	return 3;
}
