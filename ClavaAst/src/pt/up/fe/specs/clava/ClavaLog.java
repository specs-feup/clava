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

import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.logging.SpecsLogger;

public class ClavaLog extends SpecsLogger {

    private static final String CLAVA_WEAVER_TAG = buildLoggerName(ClavaLog.class);
    private static final Lazy<ClavaLog> LOGGER = buildLazy(ClavaLog::new);

    public static ClavaLog logger() {
        return LOGGER.get();
    }

    private ClavaLog() {
        super(CLAVA_WEAVER_TAG);
    }

    public static void deprecated(String message) {
        logger().msgInfo("[DEPRECATED] " + message);
    }

    public static void info(String message) {
        logger().msgInfo(message);
    }

    public static void info(Supplier<String> message) {
        info(message.get());
    }

    public static void warning(ClavaNode node, String message) {
        warning(message + " (" + node.getLocation() + ")");
        // LoggingUtils.msgInfo("[Warning] " + message + " (" + pragma.getLocation() + ")");
    }

    public static void warning(String message) {
        logger().msgWarn(message);
        // SpecsLogs.msgInfo("[Warning] " + message);
    }

    /**
     * Info-level message prefixed with [ClavaMetrics].
     * 
     * @param takeTime
     */
    public static void metrics(String message) {
        info("[ClavaMetrics] " + message);
    }
}
