
#include <iostream>

int single_return(double n)
{
    return n > 0.0 ? 1 : 0;
}

int multiple_return(double n)
{
    if (n < 0.01 && n > -0.01)
    {
        return 0;
    }
    if (n > 0.0)
    {
        return 1;
    }
    return -1;
}

int single_return_oob(double n)
{
    if (n > 0.0)
        return 1;
}

void no_return(double n)
{
    if (n / 16.0 > 1.0)
    {
        std::cout << "Greater than 16" << std::endl;
    }
}

void earlyReturn(double n)
{
    if (n / 16.0 > 1.0)
    {
        std::cout << "Greater than 16" << std::endl;
        return;
    }
    std::cout << "Not greater than 16" << std::endl;
}
