int foo() {
   int i;
   int k;
   int h;
   int m;
   int b;
	{
		int scope;
		int scopeStmt2;
	}
	
	if(0 == 0) {
      
		int ifWithoutElse;
		int ifWithoutElseStmt2;		
      
	}
	int abc;
    abc = 0;
	if(0 != 0) {
		{
         
			{
				int ifWithElseThen;
				int ifWithElseThenStmt2;		
			}
         int j;
         if(1 == 1) {
            int if1equals1;
         }
         else {
            int elseif;
            if(2==2) {
               int ifihefi;
            }
            int ijfij;
         }

		}
      int s;
	} else {
		int ifWithElseElse;
		int ifWithElseElseStmt2;
	}
	
	int afterIfElse;
	
	for(int i=0; i<10; i++) {
      
		int loopBody;
		int loopBodyStmt2;
      for(int l = 0; l < 5; l++) {
         int innerFor;
         for(int k = 0; k < 4; k++) {
            int innerInnerFor;
            {
               int jijrg;
            }
            {
               int qqq;
            }
            for(int m = 0; m<4;m++) {
               int innerIfFor;
               int ineneuhg;
              if(3==3) {
                 
                 int jgji;
                 int jijgij;
                
              }
              int ijfiji;
              
            }

         }
      }
     
	}

   int betweenFor;

   for(int k = 0; k < 4; k++) {
      int forLoop;
      int forLoop2;
      for(int j = 0; j < 4; j++) {
         int hkngkg;
         int argrghr;
      }
   }
   
   if(1) {
	   return 10;
	   // After return
   }	

   betweenFor = 0;   
   
   return 20;
}

// 'break' and 'continue' statements
int breakAndContinueExample() {
   for(int k = 0; k < 4; k++) {
      int forLoop1;

      for(int j = 0; j < 4; j++) {
         int forLoop2;

         int ifStmt;
         if (j > k) {
            int thenBody;
            break;
         }
         else if (j == k) {
            int elseBody;
            continue;
         }
         int afterIfStmt;
      }
      int afterForLoop2;
   }
   int afterForLoop1;
   
   int whileLoop;
   int a = 10;
   while(a > 0) {
      int whileBody;

      int ifStmt2;
      if (1) {
         a--;
         continue;
      }
      int afterIfStmt2;
      a--;
   }
   int afterWhile;

   int doWhileLoop;
   int b = 10;
   do {
      int whileBody;

      int ifStmt3;
      if (1) {
         b--;
         continue;
      }
      int afterIfStmt3;
      b--;
   } while(b > 0);
   int afterDoWhile;
}

// 'switch' statements
int switchCaseExample() {
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
         int switch2RangeCaseInst1;
         int switch2RangeCaseInst2;
         break;
         
      default:
         result3 = 20;
         int switch3DefaultInst1;
         int switch3DefaultInst2;
         break;

   }
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
   int afterNoBreak;
   int afterSwitch5;

   return 0;
}

// 'goto' and 'label' statements
int gotoAndLabelExample() {
   int x = 1, y, z, result;

   if (x == 1) {
      goto label1;
   } else {
      goto label2;
   }

   label1:
      y = x * 2;
      z = y + 5;
      goto end;

   label2:
      y = x + 3;
      z = y - 2;

   end:
   result = y + z;
   return result;
}
