package pt.up.fe.specs.clava.weaver.memoi;
/**
 * Copyright 2019 SPeCS.
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

public class MemoiEntry {

    private String key;
    private String output;
    private int counter;

    public MemoiEntry(MemoiEntry otherEntry) {

        this.key = otherEntry.key;
        this.output = otherEntry.output;
        this.counter = otherEntry.counter;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public void inc(int newValue) {

        this.counter += newValue;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder("{");
        builder.append(key);
        builder.append(", ");
        builder.append(output);
        builder.append(", ");
        builder.append(counter);
        builder.append("}");

        return builder.toString();
    }
}
