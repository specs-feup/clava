import JavaTypes from "@specs-feup/lara/api/lara/util/JavaTypes.js";
import { Weaver } from "@specs-feup/lara/code/Weaver.js";

const CxxWeaverOptions = JavaTypes.getType(
  "pt.up.fe.specs.clava.weaver.options.CxxWeaverOption"
);

const datastore = Weaver.getDatastore();

datastore.set(CxxWeaverOptions.PARSE_INCLUDES, true);
datastore.set(CxxWeaverOptions.DISABLE_CLAVA_INFO, true);
