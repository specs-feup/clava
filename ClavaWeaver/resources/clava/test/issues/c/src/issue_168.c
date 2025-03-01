#include <stdio.h>

float buzz(int *A, double *B, char C[])
{
    for (int i = 0; i < 100; i++)
    {
        C[i] = 0;
    }
    int *X;
    for (int i = 0; i < 100; i++)
    {
        C[i] = C[i] + 1;
        for (int j = 0; j < 100; j++)
        {
            if (C[i] > 0)
                return 0.3;
            C[j] = C[j] + 2;
            for (int k = 0; k < 100; k++)
            {
                C[k] = C[k] + 3;
                if (C[k] > 0)
                {

                    return 0.4;
                }
                for (int l = 0; l < 100; l++)
                {
                    C[l] = C[l] + 4;
                }
            }
        }
    }
    return 0.6;
}