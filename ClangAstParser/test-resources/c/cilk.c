double bar(int a) {
    return a + 10;
}


void cilk() {
    double a1 = cilk_spawn bar(1); // func () returns a value
    a1 = cilk_spawn bar(2); // func () returns a value
    cilk_spawn bar(3); // func () may return void 

    cilk_sync;

	cilk_for(int i = 0; i <= 10000; i++)
		bar(i);
}
