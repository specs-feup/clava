import weaver.Query;
import weaver.Weaver;

import lara.Strings;

aspectdef SerializeNode

	var rootXml = Weaver.serialize(Query.root());

	//println("ROOT: " + rootXml);

	var $jp = Weaver.deserialize(rootXml);

	println(Query.searchFrom($jp, "function", "foo").first().code);

end
