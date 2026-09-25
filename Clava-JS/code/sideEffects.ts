import JavaTypes from "@specs-feup/lara/api/lara/util/JavaTypes.ts";
import Weaver from "@specs-feup/lara/api/weaver/Weaver.ts";

const CxxWeaverOptions = JavaTypes.getType(
  "pt.up.fe.specs.clava.weaver.options.CxxWeaverOption"
);

const datastore = Weaver.getWeaverEngine().getData().get();

datastore.set(CxxWeaverOptions.DISABLE_CLAVA_INFO, true);

// The cache folder is the Java default shared by every frontend
// (ClangResources.getDefaultCacheFolder()). No override is set here.
