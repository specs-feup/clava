#include <stdio.h>
#include <malloc.h>

void printArray(int*** array, int height, int rows, int cols) {
  printf("Printing 3D Array:\n");
  for (int h = 0; h < height; h++) {
    printf("Height %d\n", h);
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        printf("%d ", array[h][r][c]);
      }
      printf("\n");
    }
    printf("\n");
  }	
}

int main(int argc, char* argv[])
{
  int rows = 2;
  int cols = 3;
  int height = 4;
  int ***array;
  int r, c, h;

  printf ("3D Array has %d rows\n", rows);
  printf ("3D Array has %d columns\n", cols);
  printf ("3D Array has %d height\n", height);
  
  array = (int ***) malloc (sizeof(int ***)*height);
  for (h = 0; h < height; h++) {
    array[h] = (int **) malloc(sizeof(int*)*rows);
    for (r = 0; r < rows; r++) {
      array[h][r] = (int *) malloc(sizeof(int)*cols);
    }
  }

  int counter = 1;
  for (h = 0; h < height; h++) {
    for (r = 0; r < rows; r++) {
      for (c = 0; c < cols; c++) {
        array[h][r][c] = counter;
        counter ++;
      }
    }
  }

  printArray(array, height, rows, cols);

  return 0;
}