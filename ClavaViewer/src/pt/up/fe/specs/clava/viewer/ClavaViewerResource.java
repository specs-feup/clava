package pt.up.fe.specs.clava.viewer;

import pt.up.fe.specs.util.providers.ResourceProvider;

public enum ClavaViewerResource implements ResourceProvider {
    TEST("test.c");

    private static final String BASEPATH = "code/";

    private final String resource;

    private ClavaViewerResource(String resource) {
        this.resource = ClavaViewerResource.BASEPATH + resource;
    }

    @Override
    public String getResource() {
        return resource;
    }
}
