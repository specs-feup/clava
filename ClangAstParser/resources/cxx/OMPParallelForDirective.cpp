/* USE -fopenmp */
/* ALSO CapturedDecl */

int main() {
    #pragma omp parallel for
    for (int i = 0 ; i < 0; ++i);
}

