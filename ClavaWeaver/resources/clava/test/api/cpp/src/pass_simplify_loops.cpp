#include <cstddef>
#include <iostream>

int main()
{
    int matrix[25];

    for (size_t i = 0; i < 25; i++)
    {
        matrix[i] = 2;
    }

    for (size_t row = 0; row < 5; row++)
    {
        if (row == 4)
        {
            break;
        }
        for (size_t col = 0; col < 5; col++)
        {
            if (col == row)
            {
                continue;
            }
            matrix[row * 5 + col] = 0;
        }
    }

    int i = 0;
    do
    {
        if (i == 2)
            continue;
        std::cout << i << std::endl;
        if (i < 0)
            break;
    } while (i < 7);

    return 0;
}