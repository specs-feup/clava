TYPE_PATCH_00 f(char *c){
    Type var1 = c;
    var1 += var3 - var4;
    return var1;
}

TYPE_PATCH_02 f2(){
    return a2[0];
}

TYPE_PATCH_03 f3(){
    A a = A();
    B *b = a.a1()->a2;
    C &c = var5->a1()->a3();
    return c;
}

/*the following examples don't work
TYPE_PATCH_04 f4(){
    return a4[0][0];
}

TYPE_PATCH_05 f5(Array a5){
    return a5[0];
}

void f6(){
    D d[2] = function();
    E **e = function2();
}
*/
