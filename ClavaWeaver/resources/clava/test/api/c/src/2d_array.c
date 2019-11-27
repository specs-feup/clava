#include <stdio.h>
#include <malloc.h>

void printArray(int** array, int rows, int cols) {
  printf ("Printing 2D Array:\n");
  for (int r = 0; r < rows; r++) {
    for (int c = 0; c < cols; c++) {
        printf("%d ", array[r][c]);
    }
    printf("\n");

  }	
}


int main(int argc, char* argv[])
{
  int rows = 3;
  int cols = 4;

  int **array;
  int r, c;
  printf ("2D Array has %d rows\n", rows);
  printf ("2D Array has %d columns\n", cols);

  array = (int **) malloc (sizeof(int **)*rows);
  for (r = 0; r < rows; r++) {
        array[r] = (int *) malloc(sizeof(int)*cols);

  }

  int counter = 1;
  for (r = 0; r < rows; r++) {
    for (c = 0; c < cols; c++) {
		array[r][c] = counter;
		counter++;
    }
  }
  
  printArray(array, rows, cols);


  return 0;
}