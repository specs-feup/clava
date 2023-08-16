int foo (int c, int unused_arg) {
    int switch_condition = 1;
    int result;

    switch(switch_condition) {
        case 1:
            result = 10;
            break;
        case 2:
            if (c > 10)
                result = 20;
            else 
                result = 2;
            break;
        default:
            result = 30;
    }
    
    int a = 0;
    int b;
    do {
        b = a + 1;
        c = c + b; 
        a = b * 2;
    } while (a < 9);

    return c;
}