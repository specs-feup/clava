laraImport("clava.MathExtra");

console.log("Original expression: a - (b - c)");
console.log("Simplified  expression: " + MathExtra.simplify("a - (b - c)"));
console.log("Original expression: a - (b - c)^2");
console.log("Simplified  expression: " + MathExtra.simplify("a - (b - c)^2"));

const constants = {};
constants["a"] = 2;
console.log("Original expression: a - (b - c), with a=2");
console.log(
    "Simplified  expression: " + MathExtra.simplify("a - (b - c)", constants)
);
