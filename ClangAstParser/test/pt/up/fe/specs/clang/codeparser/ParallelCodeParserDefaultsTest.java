package pt.up.fe.specs.clang.codeparser;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

class ParallelCodeParserDefaultsTest {
    @Test
    void workersReadInitializedDefaultsWithoutLosingExplicitOptions() throws Exception {
        try (var workers = Executors.newFixedThreadPool(8)) {
            for (int round = 0; round < 50; round++) {
                var parser = new ParallelCodeParser();
                parser.set(CodeParser.SHOW_CODE, true);
                parser.initializeParallelDefaults();
                assertFalse(parser.hasValue(CodeParser.GENERATED_PARSE_ROOT));
                var keys = parser.getStoreDefinitionTry().orElseThrow().getKeys();
                var tasks = new ArrayList<Future<?>>();
                for (int worker = 0; worker < 8; worker++) {
                    tasks.add(workers.submit(() -> {
                        for (var key : keys) {
                            if (key != CodeParser.GENERATED_PARSE_ROOT && key.hasDefaultValue()) {
                                assertTrue(parser.hasValue(key), key.getName());
                                parser.get(key);
                            }
                        }
                    }));
                }
                for (var task : tasks) task.get();
                assertTrue(parser.get(CodeParser.SHOW_CODE));
                assertFalse(parser.get(CodeParser.SHOW_CLANG_DUMP));
            }
        }
    }
}
