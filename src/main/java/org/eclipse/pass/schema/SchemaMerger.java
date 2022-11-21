/*
 * Copyright 2022 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.pass.schema;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The SchemaMerger class merges a list of SchemaInstance objects into a single
 * SchemaInstance object
 */
public class SchemaMerger {

    private ObjectNode merged;
    private String[] ignorable = { "title", "description", "$id", "$schema", "$comment" };

    public SchemaMerger() {
        merged = new ObjectMapper().createObjectNode();
    }

    /**
     * Merges a list of SchemaInstance object into a single one Field:value pairs
     * should only be added if not already present
     *
     * @param schemas list of schemas to be merged
     * @return SchemaInstance of merged schemas
     */
    JsonNode mergeSchemas(List<JsonNode> schemas) {
        for (JsonNode schema : schemas) {
            Iterator<String> fieldnames = schema.fieldNames();
            fieldnames.forEachRemaining(f -> {
                if (!Arrays.asList(ignorable).contains(f))
                    mergeIn(f, schema.get(f), merged);
            });
        }
        return merged;
    }

    /**
     * Merges a field:value pair into the schema If there is already a value in this
     * field, it should be the same type. Else error. In case
     */
    private void mergeIn(String fieldName, JsonNode value, ObjectNode schema) {
        if (schema.get(fieldName) == null) { // if this value is not already in the merged schema, add it
            schema.set(fieldName, value);
            return;
        }
        // the field already exists, so check type
        if (value.isValueNode()) {
            if (!schema.get(fieldName).isValueNode()) {
                // throw error: type mismatch
                System.out.println("TYPE MISMATCH");
                return;
            }
            schema.set(fieldName, value);
            return;
        }
        if (value.isArray()) {
            if (!schema.get(fieldName).isArray()) {
                // throw error: type mismatch
                System.out.println("TYPE MISMATCH");
                return;
            }
            for (JsonNode element : value) {
                Iterator<JsonNode> existing_elements = ((ArrayNode) schema.get(fieldName)).elements();
                boolean hasElement = false;
                while (existing_elements.hasNext()) {
                    if (existing_elements.next().equals(element)) {
                        hasElement = true;
                    }
                }
                if (!hasElement) { // add element if not there
                    ((ArrayNode) (schema.get(fieldName))).add(element);
                }
            }
            return;
        }
        if (value.isObject()) {
            if (!schema.get(fieldName).isObject()) {
                // throw error: type mismatch
                System.out.println("TYPE MISMATCH");
                return;
            }
            Iterator<String> fieldnames = value.fieldNames();
            fieldnames.forEachRemaining(f -> mergeIn(f, value.get(f), (ObjectNode) schema.get(fieldName)));
            return;
        }
    }
}
