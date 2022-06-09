laraImport("clava.code.RemoveShadowing");
laraImport("clava.pass.SingleReturnFunction");

function PrepareForInlining($function) {
  new SingleReturnFunction().apply($function);
  RemoveShadowing($function);
}
