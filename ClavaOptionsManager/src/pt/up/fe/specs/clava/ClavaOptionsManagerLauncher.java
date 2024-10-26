package pt.up.fe.specs.clava;

import pt.up.fe.specs.WeaverOptionsManagerLauncher;
import pt.up.fe.specs.clava.weaver.CxxWeaver;

public class ClavaOptionsManagerLauncher extends WeaverOptionsManagerLauncher {
    public static void main(String[] args) {
        launchGui(new CxxWeaver());
    }
}
