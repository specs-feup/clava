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

package pt.up.fe.specs.clava;

import java.util.function.Supplier;

import pt.up.fe.specs.util.logging.EnumLogger;
import pt.up.fe.specs.util.logging.TagLogger;
import pt.up.fe.specs.util.logging.TagLoggerUser;

// public class ClavaLog extends SpecsLogger {
public interface ClavaLog extends TagLoggerUser<ClavaLoggerTag> {

    // private static final String CLAVA_WEAVER_TAG = buildLoggerName(ClavaLog.class);
    // private static final Lazy<ClavaLog> LOGGER = buildLazy(ClavaLog::new);

    static final ThreadLocal<EnumLogger<ClavaLoggerTag>> CLAVA_LOGGER = ThreadLocal
            .withInitial(() -> EnumLogger.newInstance(ClavaLoggerTag.class).addToIgnoreList(ClavaLog.class));

    public static EnumLogger<ClavaLoggerTag> getLogger() {
        return CLAVA_LOGGER.get();
    }

    @Override
    default TagLogger<ClavaLoggerTag> logger() {
        return CLAVA_LOGGER.get();
    }

    // private static ClavaLog logger() {
    // return LOGGER.get();
    // }

    // private ClavaLog() {
    // super(CLAVA_WEAVER_TAG);
    // }

    public static void deprecated(String message) {
        CLAVA_LOGGER.get().deprecated(message);
    }

    public static void debug(String message) {
        CLAVA_LOGGER.get().debug(message);
    }

    public static void debug(Supplier<String> message) {
        CLAVA_LOGGER.get().debug(message);
    }

    public static void info(String message) {
        CLAVA_LOGGER.get().info(message);
    }

    public static void info(Supplier<String> message) {
        info(message.get());
    }

    public static void warning(ClavaNode node, String message) {
        warning(message + " (" + node.getLocation() + ")");
        // LoggingUtils.msgInfo("[Warning] " + message + " (" + pragma.getLocation() + ")");
    }

    public static void warning(String message) {
        // TODO: Implemented as info for now
        CLAVA_LOGGER.get().warn(message);
        // throw new RuntimeException("Not implemented yet");
        // CLAVA_LOGGER.warn(message);
        // SpecsLogs.msgInfo("[Warning] " + message);
    }

    /**
     * Info-level message prefixed with [ClavaMetrics].
     * 
     * @param takeTime
     */
    public static void metrics(String message) {
        CLAVA_LOGGER.get().info(ClavaLoggerTag.METRICS, message);
    }
}
