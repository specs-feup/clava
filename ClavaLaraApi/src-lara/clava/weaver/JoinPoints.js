laraImport("weaver.JoinPointsBase");
laraImport("lara.Check");
laraImport("clava.Clava");
laraImport("clava._ClavaJavaTypes");

JoinPoints.prototype.toJoinPoint = function (node) {
  const cxxJps = _ClavaJavaTypes.getCxxJoinPoints();
  return cxxJps.createFromLara(node);
};

/**
 *
 * @return {$jp[]} the children of the given node
 */
JoinPoints.prototype._all_children = function ($jp) {
  return $jp.children;
};

/**
 *
 * @return {$jp[]} the descendants of the given node
 */
JoinPoints.prototype._all_descendants = function ($jp) {
  return $jp.descendants;
};

/**
 *
 * @return {$jp[]} all the nodes that are inside the scope of a given node
 */
JoinPoints.prototype._all_scope_nodes = function ($jp) {
  return $jp.scopeNodes;
};

// JoinPoints.prototype.children = function ($jp, jpType) {
//   if ($jp === undefined) {
//     return [];
//   }

//   Check.isJoinPoint($jp);

//   const children = $jp.astChildren;
//   if (jpType === undefined) {
//     return children;
//   }

//   return this._filterNodes(children, jpType);
// };

// JoinPoints.prototype.descendants = function ($jp, jpType) {
//   if ($jp === undefined) {
//     return [];
//   }

//   const descendants = $jp.descendants;

//   if (jpType === undefined) {
//     return descendants;
//   }

//   Check.isJoinPoint($jp);

//   return this._filterNodes(descendants, jpType);
// };

// JoinPoints.prototype._filterNodes = function ($jps, jpType) {
//   return [...$jps].filter(($jp) => $jp.instanceOf(jpType));
// };
