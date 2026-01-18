import Timer from "@specs-feup/clava/api/lara/code/Timer.js";
import Clava from "@specs-feup/clava/api/clava/Clava.js";
import Logger from "@specs-feup/clava/api/lara/code/Logger.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

// Disable SpecsLogger, in order to be able to compile woven code without the project
Clava.useSpecsLogger = false;

// Instrument call to 'Calculate'
const timer = new Timer();

for (const $call of Query.search("call")) {
    if ($call.name !== "bar" && $call.name !== "foo") {
        continue;
    }

    timer.time($call, "Time:");
}

// Disable printing result
timer.setPrint(false);

let $call = Query.search("call", "bar2").first();
const bar2TimeVar = timer.time($call);
let logger = new Logger();
logger
    .text("I want to print the value of the elapsed time (")
    .double(bar2TimeVar)
    .text(
        "), which is in the unit '" +
            timer.getUnit().getUnitsString() +
            "' and put other stuff after it"
    )
    .ln()
    .log(timer.getAfterJp());

// Enable printing again
timer.setPrint(true);

$call = Query.search("call", "bar3").first();
timer.time($call);
logger = new Logger();
logger
    .text("This should appear after the timer print")
    .ln()
    .log(timer.getAfterJp());

for (const $file of Query.search("file")) {
    console.log($file.code);
}
