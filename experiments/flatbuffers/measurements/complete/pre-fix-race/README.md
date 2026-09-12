# Invalid timing observation retained for diagnosis

The second lazy warm run returned 154 passed, 8 failed and 2 pending. It omitted
several ASTs after `MemoizedDataStore` rejected keys missing from a partially
initialized shared StoreDefinition map. Its 30.55 seconds is not a speedup.

The matrix was stopped. These files preserve the failed run; final comparisons
must use a fresh complete matrix after the shared-map fix and runtime rebuild.
