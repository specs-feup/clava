#define MACRO (1 + 2)
int x = 3 + MACRO + 5;
int y = MACRO;

#define A 2
#define BAC (A + A)

int a = A + BAC;

int main() {
	return a;
}