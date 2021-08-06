/**
 * Copyright 2017 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.tester;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.lazy.ThreadSafeLazy;
import pt.up.fe.specs.util.providers.FileResourceProvider.ResourceWriteData;

/**
 * 
 * @author JoaoBispo
 *
 */
public class ClavaLauncher {

    private static final String CLAVA_CLASSNAME = "pt.up.fe.specs.cxxweaver.CxxWeaverLauncher";

    private static final Lazy<ClavaLauncher> CLAVA_LAUNCHER = new ThreadSafeLazy<>(() -> ClavaLauncher.newInstance());

    public static ClavaLauncher getInstance() {
        return CLAVA_LAUNCHER.get();
    }

    private static ClavaLauncher newInstance() {
        // Destination folder for Clava jar
        File clavaFolder = SpecsIo.mkdir(SpecsIo.getJarPath(ClavaLauncher.class).orElse(SpecsIo.getWorkingDir()),
                "clava_jar");

        // Prepare resource
        ResourceWriteData clavaJar = ClavaTesterWebResource.CLAVA_JAR.writeVersioned(clavaFolder, ClavaLauncher.class);

        loadLibrary(clavaJar.getFile());

        try {
            Class<?> clavaClass = Class.forName(CLAVA_CLASSNAME);
            Method method = clavaClass.getMethod("execute", List.class);
            return new ClavaLauncher(method);
        } catch (NoSuchMethodException | SecurityException | ClassNotFoundException | IllegalArgumentException e) {
            throw new RuntimeException("Could not create dynamic method for invoking Clava", e);
        }

    }

    private final Method method;

    private ClavaLauncher(Method method) {
        this.method = method;
    }

    public boolean execute(List<String> args) {

        try {
            Object result = method.invoke(null, args);
            Preconditions.checkArgument(result instanceof Boolean);
            return (Boolean) result;
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException("Problems while calling Clava", e);
        }

    }

    /**
     * Adds the supplied Java Archive library to java.class.path. This is benign if the library is already loaded.
     * 
     * <p>
     * Based on this answer: https://stackoverflow.com/questions/27187566/load-jar-dynamically-at-runtime
     */
    private static synchronized void loadLibrary(File jar) {
        try {
            /*We are using reflection here to circumvent encapsulation; addURL is not public*/
            java.net.URLClassLoader loader = (java.net.URLClassLoader) ClassLoader.getSystemClassLoader();
            java.net.URL url = jar.toURI().toURL();
            /*Disallow if already loaded*/
            for (java.net.URL it : java.util.Arrays.asList(loader.getURLs())) {
                if (it.equals(url)) {
                    return;
                }
            }
            java.lang.reflect.Method method = java.net.URLClassLoader.class.getDeclaredMethod("addURL",
                    new Class[] { java.net.URL.class });
            method.setAccessible(true); /*promote the method to public access*/
            method.invoke(loader, new Object[] { url });
        } catch (final java.lang.NoSuchMethodException | java.lang.IllegalAccessException
                | java.net.MalformedURLException | java.lang.reflect.InvocationTargetException e) {
            throw new RuntimeException("Could not dynamically load jar '" + jar + "'", e);
        }
    }
}
