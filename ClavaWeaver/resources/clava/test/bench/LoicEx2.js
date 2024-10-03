import lara.Io;
import clava.Clava;
import clava.ClavaJoinPoints;

aspectdef Launcher
   select file.function end
   apply
	  replaceTypesTemplatesInCall($function);
   end
   
end
 
function replaceTypesTemplatesInCall($function) {
    var cpt = 0;
    var vcalls = $function.calls;
    for (var aCall of vcalls) { 
      var $vargTemplate=undefined;
      try {  
       var $fc = aCall.children[0];
       println(" === The first child of the call is : " + $fc.code);
       var targs = $fc.getValue("templateArguments");
       for (var x of targs) {
        var $vargTemplate = undefined;
        try { 
           $vargTemplate = x.getValue("expr");
           println( " *** expression detected = " + x);
           } catch(e) {  } 
        
        if ( $vargTemplate === undefined) 
         try { 
           $vargTemplate = x.getValue("type");
           println( " *** type  detected  = " + x);
           try { 
            x.setValue("type", ClavaJoinPoints.typeLiteral("newType" + cpt));
            cpt++;
            println(" === AFTER MY TRANSFO, The first child of the call is : " + $fc.code);
           }
           catch(e) { println(e);  }
         }
         catch(e) {   }
        
         
       }
      } catch(e) {  }
     }
}