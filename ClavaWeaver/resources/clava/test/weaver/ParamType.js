aspectdef ParamType

   select function{'error_norm'}.body.loop.varref end
   apply
       println('varref_name : ' + $varref.name + ' #' + $varref.line);
       println('\t\tisArray = ' + $varref.vardecl.type.isArray);
       println('\t\tisArray = ' + $varref.type.isArray);
       println($varref.ast);
   end
   condition $varref.name!= 'm' end

end