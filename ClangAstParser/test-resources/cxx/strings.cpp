int main() {
		
	// Issue #102
	char bom[] = "\xFF\xFE";

	const wchar_t* s4 = L"ABC" L"DEF\xff猫"; // ok, same as
	const wchar_t* s5 = L"ABCDEF\xff猫";
		
	return 0;
}