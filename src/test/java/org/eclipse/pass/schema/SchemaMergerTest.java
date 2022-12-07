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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SchemaMergerTest {

    @Test
    void simpleIgnorePreamble() throws JsonMappingException, JsonProcessingException {
        String schema1 = "{\r\n" + "            \"$schema\": \"http://example.org/schema\",\r\n"
                + "            \"$id\": \"http://example.org/foo\",\r\n" + "            \"title\": \"foo\",\r\n"
                + "            \"description\": \"foo schema\",\r\n" + "            \"$comment\": \"one\",\r\n"
                + "            \"a\": \"1\"\r\n" + "        }";
        String schema2 = "{\r\n" + "            \"$schema\": \"http://example.org/schema\",\r\n"
                + "            \"$id\": \"http://example.org/bar\",\r\n" + "            \"title\": \"bar\",\r\n"
                + "            \"description\": \"bar schema\",\r\n" + "            \"$comment\": \"two\",\r\n"
                + "            \"b\": \"2\"\r\n" + "        }";
        String expected_json = "{\r\n" + "            \"a\": \"1\",\r\n" + "            \"b\": \"2\"\r\n" + "        }";
        ObjectMapper map = new ObjectMapper();
        JsonNode schema_one = map.readTree(schema1);
        JsonNode schema_two = map.readTree(schema2);
        JsonNode expected = map.readTree(expected_json);

        List<JsonNode> toMerge = new ArrayList<JsonNode>(Arrays.asList(schema_one, schema_two));
        SchemaMerger merger = new SchemaMerger(toMerge);
        JsonNode result = merger.mergeSchemas();
        assertEquals(result, expected);
    }

    @Test
    void ignorableConflicts() throws JsonMappingException, JsonProcessingException {
        String schema1 = "{\r\n" + "            \"a\": {\r\n" + "                \"title\": \"A\",\r\n"
                + "                \"description\": \"a letter\",\r\n"
                + "                \"$comment\": \"displays good\",\r\n" + "                \"type\": \"letter\"\r\n"
                + "            }\r\n" + "        }";
        String schema2 = "{\r\n" + "            \"a\": {\r\n" + "                \"title\": \"a\",\r\n"
                + "                \"description\": \"an awesome letter\",\r\n"
                + "                \"$comment\": \"displays nicely\",\r\n" + "                \"type\": \"letter\"\r\n"
                + "            }\r\n" + "        }";
        String expected_json = "{\r\n" + "            \"a\": {\r\n" + "                \"title\": \"a\",\r\n"
                + "                \"$comment\": \"displays nicely\",\r\n"
                + "                \"description\": \"an awesome letter\",\r\n"
                + "                \"type\": \"letter\"\r\n" + "            }\r\n" + "        }";
        ObjectMapper map = new ObjectMapper();
        JsonNode schema_one = map.readTree(schema1);
        JsonNode schema_two = map.readTree(schema2);
        JsonNode expected = map.readTree(expected_json);

        List<JsonNode> toMerge = new ArrayList<JsonNode>(Arrays.asList(schema_one, schema_two));
        SchemaMerger merger = new SchemaMerger(toMerge);
        JsonNode result = merger.mergeSchemas();
        assertEquals(expected, result);
    }

    @Test
    void simpleArrayDeduplication() throws JsonMappingException, JsonProcessingException {
        String schema1 = "{\r\n" + "            \"array\": [\"a\", \"b\", \"c\"]\r\n" + "        }";
        String schema2 = "{\r\n" + "            \"array\": [\"b\", \"c\", \"d\"]\r\n" + "        }";
        String schema3 = "{\r\n" + "            \"array\": [\"c\", \"d\", \"e\"]\r\n" + "        }";
        String expected_json = "{\r\n" + "            \"array\": [\"a\", \"b\", \"c\", \"d\", \"e\"]\r\n" + "        }";
        ObjectMapper map = new ObjectMapper();
        JsonNode schema_one = map.readTree(schema1);
        JsonNode schema_two = map.readTree(schema2);
        JsonNode schema_three = map.readTree(schema3);
        JsonNode expected = map.readTree(expected_json);

        List<JsonNode> toMerge = new ArrayList<JsonNode>(Arrays.asList(schema_one, schema_two, schema_three));
        SchemaMerger merger = new SchemaMerger(toMerge);
        JsonNode result = merger.mergeSchemas();
        assertEquals(expected, result);
    }

    @Test
    void complexArrayDeduplication() throws JsonMappingException, JsonProcessingException {
        String schema1 = "{\r\n" + "            \"array\": [{\"a\": [\"b\", {\"c\": \"d\"}]}, \"e\"]\r\n" + "        }";
        String schema2 = "{\r\n" + "            \"array\": [{\"a\": [\"b\", {\"c\": \"d\"}]}, \"f\"]\r\n" + "        }";
        String schema3 = "{\r\n" + "            \"array\": [\"e\", \"f\", {\"g\": \"h\"}]\r\n" + "        }";
        String expected_json = "{\r\n"
                + "            \"array\": [{\"a\": [\"b\", {\"c\": \"d\"}]}, \"e\", \"f\", {\"g\": \"h\"}]\r\n"
                + "        }";
        ObjectMapper map = new ObjectMapper();
        JsonNode schema_one = map.readTree(schema1);
        JsonNode schema_two = map.readTree(schema2);
        JsonNode schema_three = map.readTree(schema3);
        JsonNode expected = map.readTree(expected_json);

        List<JsonNode> toMerge = new ArrayList<JsonNode>(Arrays.asList(schema_one, schema_two, schema_three));
        SchemaMerger merger = new SchemaMerger(toMerge);
        JsonNode result = merger.mergeSchemas();
        assertEquals(expected, result);
    }

    @Test
    void objectMerge() throws JsonMappingException, JsonProcessingException {
        String schema1 = "{\r\n" + "            \"a\": \"b\",\r\n" + "            \"c\": [\"d\", \"e\"]\r\n"
                + "        }";
        String schema2 = "{\r\n" + "            \"a\": \"b\",\r\n" + "            \"c\": [\"e\", \"f\", \"g\"]\r\n"
                + "        }";
        String schema3 = "{\r\n" + "            \"h\": {\r\n" + "                \"i\": \"j\",\r\n"
                + "                \"k\": [\"l\", \"m\"],\r\n" + "                \"n\": {\r\n"
                + "                    \"o\": \"p\"\r\n" + "                }\r\n" + "            }\r\n" + "        }";
        String schema4 = "{\r\n" + "            \"h\": {\r\n" + "                \"k\": [\"l\", \"m\", \"m'\"],\r\n"
                + "                \"n\": {\r\n" + "                    \"q\": \"r\"\r\n" + "                }\r\n"
                + "            }\r\n" + "        }";
        String expected_json = "{\r\n" + "            \"a\": \"b\",\r\n"
                + "            \"c\": [\"d\", \"e\", \"f\", \"g\"],\r\n" + "            \"h\": {\r\n"
                + "                \"i\": \"j\",\r\n" + "                \"k\": [\"l\", \"m\", \"m'\"],\r\n"
                + "                \"n\": {\r\n" + "                    \"o\": \"p\",\r\n"
                + "                    \"q\": \"r\"\r\n" + "                }\r\n" + "            }\r\n" + "        }";
        ObjectMapper map = new ObjectMapper();
        JsonNode schema_one = map.readTree(schema1);
        JsonNode schema_two = map.readTree(schema2);
        JsonNode schema_three = map.readTree(schema3);
        JsonNode schema_four = map.readTree(schema4);
        JsonNode expected = map.readTree(expected_json);

        List<JsonNode> toMerge = new ArrayList<JsonNode>(
                Arrays.asList(schema_one, schema_two, schema_three, schema_four));
        SchemaMerger merger = new SchemaMerger(toMerge);
        JsonNode result = merger.mergeSchemas();
        assertEquals(expected, result);
    }

    @Test
    void testMergerFull() throws IOException {
        InputStream schema1 = SchemaMergerTest.class.getResourceAsStream("/example/schemas/schema1.json");
        InputStream schema2 = SchemaMergerTest.class.getResourceAsStream("/example/schemas/schema2.json");
        InputStream schema3 = SchemaMergerTest.class.getResourceAsStream("/example/schemas/schema3.json");
        InputStream schema4 = SchemaMergerTest.class.getResourceAsStream("/example/schemas/schema4.json");
        InputStream expected_json = SchemaMergerTest.class
                .getResourceAsStream("/example/schemas/example_merged_basic.json");

        ObjectMapper map = new ObjectMapper();
        JsonNode schema_one = map.readTree(schema1);
        JsonNode schema_two = map.readTree(schema2);
        JsonNode schema_three = map.readTree(schema3);
        JsonNode schema_four = map.readTree(schema4);
        JsonNode expected = map.readTree(expected_json);

        List<JsonNode> toMerge = new ArrayList<JsonNode>(
                Arrays.asList(schema_one, schema_two, schema_three, schema_four));
        SchemaMerger merger = new SchemaMerger(toMerge);
        JsonNode result = merger.mergeSchemas();
        assertEquals(expected, result);
    }

}
