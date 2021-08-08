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

package pt.up.fe.specs.clava.weaver.importable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.util.SpecsLogs;

public class LowLevelApi {

    public static List<String> getFields(Object object) {
        List<String> fieldNames = new ArrayList<>();
        try {
            for (Field field : object.getClass().getDeclaredFields()) {
                field.setAccessible(true); // You might want to set modifier to public first.
                fieldNames.add(field.getName());
            }
        } catch (IllegalArgumentException e) {
            SpecsLogs.warn("Error message:\n", e);
        }

        return fieldNames;
    }

    public static Object getValue(Object object, String fieldName) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true); // You might want to set modifier to public first.
            Object value = field.get(object);
            return value;
        } catch (
                IllegalArgumentException | NoSuchFieldException | SecurityException | IllegalAccessException e) {
            SpecsLogs.warn("Error message:\n", e);
        }

        return null;
    }

    public static Class<?> getFieldClass(Object object, String fieldName) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true); // You might want to set modifier to public first.
            return field.getType();
        } catch (
                IllegalArgumentException | NoSuchFieldException | SecurityException e) {
            SpecsLogs.warn("Error message:\n", e);
        }

        return null;
    }

    public static ClavaNode getNode(ACxxWeaverJoinPoint joinpoint) {
        return joinpoint.getNode();
    }

}
