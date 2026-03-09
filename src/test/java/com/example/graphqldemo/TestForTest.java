package com.example.graphqldemo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import java.util.List;

public class TestForTest {

    @DisplayName("Проверка на пустые строки")
    @Test
    void checkNotBlank() {
        //Вариант 1
        List<String> oldValues = List.of("1", "", "3");
        ParamUpdateJ p1 = new ParamUpdateJ("1", oldValues, List.of("1", "2", "3"));

        //вернет true - если в списке хотя бы один элемент не пустой (хотя бы одна не пустая строка)
        boolean b = p1.oldValues().stream().anyMatch(StringUtils::isNotBlank);
        System.out.println(b); //true


        //Вариант 2
        List<String> oldValues2 = List.of(" ", "", "   ");
        ParamUpdateJ p2 = new ParamUpdateJ("2", oldValues2, List.of("1", "2", "3"));

        //вернет true - если в списке хотя бы один элемент не пустой (хотя бы одна не пустая строка)
        boolean b2 = p2.oldValues().stream().anyMatch(StringUtils::isNotBlank);
        System.out.println(b2); //false

        //Вариант 3
        List<String> oldValues3 = List.of();
        ParamUpdateJ p3 = new ParamUpdateJ("3", oldValues3, List.of("1", "2", "3"));

        //вернет true - если в списке хотя бы один элемент не пустой (хотя бы одна не пустая строка)
        boolean b3 = p3.oldValues().stream().anyMatch(StringUtils::isNotBlank);
        System.out.println(b3); //false

    }

    record ParamUpdateJ(String id, List<String> oldValues, List<String> newValues) {
    }
}
