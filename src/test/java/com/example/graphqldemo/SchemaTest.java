package com.example.graphqldemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Тестируем схему.
 * Важно перед запуском тестов запустить БД.
 */
@SpringBootTest
@AutoConfigureGraphQlTester
public class SchemaTest {

    @Test
    void schemaShouldBeValid() {
        // Просто проверяем что схема загружается
    }

    @Test
    void testBookType() {
        // Проверяем структуру через introspection
        String query = """
            {
                __type(name: "Book") {
                    name
                    fields {
                        name
                        type {
                            name
                            kind
                        }
                    }
                }
            }
            """;

        // Выполняем запрос...
    }
}
