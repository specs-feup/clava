void foo() {
   int a = 10;

   int switch1, result1;
   int defaultOnly;
   switch(a) { 
        default:
            result1 = 20;
            int switch1DefaultInst1;
            int switch1DefaultInst2;
            break;
   }
   switch1 = 0;
   int afterDefaultOnly;
   int afterSwitch1;


   int switch2, result2;
   int noDefault;
   switch (a) {
        case 1:
            result2 = 1;
            int switch2Case1Inst1;
            int switch2Case1Inst2;
            break;

        case 3:
            result2 = 3;
            int switch2Case3Inst1;
            int switch2Case3Inst2;
            break;
      
        case 4 ... 8:
            result2 = 48;
            int switch2RangeCaseInst1;
            int switch2RangeCaseInst2;
            break;
   }
   switch2 = 0;
   int afterNoDefault;
   int afterSwitch2;


   int switch3, result3;
   int withDefault;
   switch (a) {
        case 1:
            result3 = 1;
            int switch3Case1Inst1;
            int switch3Case1Inst2;
            break;

        case 3:
            result3 = 3;
            int switch3Case3Inst1;
            int switch3Case3Inst2;
            break;
      
        case 4 ... 8:
            result3 = 48;
            int switch3RangeCaseInst1;
            int switch3RangeCaseInst2;
            break;
         
        default:
            result3 = 20;
            int switch3DefaultInst1;
            int switch3DefaultInst2;
            break;

   }
   switch3 = 0;
   int afterWithDefault;
   int afterSwitch3;


   int switch4, result4;
   int intermediateDefault;
   switch (a) {
        case 1:
            result4 = 1;
            int switch4Case1Inst1;
            int switch4Case1Inst2;
            break;

        default:
            result4 = 20;
            int switch4DefaultInst1;
            int switch4DefaultInst2;
            break;

        case 3:
            result4 = 3;
            int switch4Case3Inst1;
            int switch4Case3Inst2;
            break;
   }
   switch4 = 0;
   int afterIntermediateDefault;
   int afterSwitch4;
   

   int switch5, result5;
   int noBreak;
   switch (a) {
        case 1:
            result5 = 1;
            int switch5Case1Inst1;
            int switch5Case1Inst2;
            if (1) {
            int ifBody;
            break;
            }

        case 2:
            result5 = 2;
            int switch5Case2Inst1;
            int switch5Case2Inst2;

        default:
            result5 = 20;
            int switch5DefaultInst1;
            int switch5DefaultInst2;

        case 3:
            result5 = 3;
            int switch5Case3Inst1;
            int switch5Case3Inst2;
   }
   switch5 = 0;
   int afterNoBreak;
   int afterSwitch5;

   int switch6, result6;
   int lastSwitch;
   switch (a) {
        case 1:
            result6 = 1;
            int switch6Case1Inst1;
            int switch6Case1Inst2;
            break;

        case 3:
            result6 = 3;
            int switch6Case3Inst1;
            int switch6Case3Inst2;
            break;
   }
}