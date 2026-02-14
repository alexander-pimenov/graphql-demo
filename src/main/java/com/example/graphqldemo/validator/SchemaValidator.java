package com.example.graphqldemo.validator;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

//@Component
public class SchemaValidator {

    @EventListener(ApplicationReadyEvent.class)
    public void validateSchema() {
        // Spring GraphQL автоматически валидирует схему при старте
        // это в качестве примера, чтобы в лог записывалось,
        // что схема валидна ,но это можно не делать.
        System.out.println("Schema is valid!");
    }
}