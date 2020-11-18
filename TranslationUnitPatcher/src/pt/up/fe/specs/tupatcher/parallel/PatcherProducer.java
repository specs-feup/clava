/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.tupatcher.parallel;

import java.io.File;
import java.util.List;

import pt.up.fe.specs.util.collections.concurrentchannel.ChannelProducer;

public class PatcherProducer {

    private final List<File> sourceFiles;
    private final ChannelProducer<PatcherResult> producer;

    public PatcherProducer(List<File> sourceFiles, ChannelProducer<PatcherResult> producer) {
        this.sourceFiles = sourceFiles;
        this.producer = producer;
    }

    public boolean execute() {
        for (File sourceFile : sourceFiles) {
            System.out.println("Producer: " + sourceFile);
            producer.put(new PatcherResult(sourceFile.toString()));
        }

        // Finish by adding poison
        System.out.println("Producer: poison");
        producer.put(PatcherResult.getPoison());

        return true;
    }

}
