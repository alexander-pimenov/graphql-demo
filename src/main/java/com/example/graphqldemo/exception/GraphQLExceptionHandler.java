package com.example.graphqldemo.exception;


import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Обработка ошибок с @GraphQlExceptionHandler - это элегантный способ централизованной обработки исключений в GraphQL.
 * <p>
 * Преимущества @GraphQlExceptionHandler<br>
 * - Централизация - вся обработка ошибок в одном месте<br>
 * - Типизация - разные обработчики для разных исключений<br>
 * - Контекст - доступ к DataFetchingEnvironment<br>
 * - Расширяемость - можно добавлять кастомные поля в extensions<br>
 * - Чистота кода - сервисы не засоряются обработкой ошибок<br>
 * <p>
 * Такой подход делает GraphQL API более профессиональным и удобным для клиентов, так как они получают
 * структурированную информацию об ошибках, а не просто текстовое сообщение.
 */
@ControllerAdvice
public class GraphQLExceptionHandler {

    // Обработка кастомного исключения "ResourceNotFoundException"
    @GraphQlExceptionHandler(ResourceNotFoundException.class)
    public GraphQLError handleResourceNotFound(ResourceNotFoundException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError()
                .message(ex.getMessage())
                .errorType(ErrorType.NOT_FOUND)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .extensions(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "resourceId", ex.getResourceId(),
                        "resourceType", ex.getResourceType()
                ))
                .build();
    }

    // Обработка ошибок валидации
    @GraphQlExceptionHandler(ValidationException.class)
    public GraphQLError handleValidationException(ValidationException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError()
                .message("Validation failed: " + ex.getMessage())
                .errorType(ErrorType.BAD_REQUEST)
                .path(env.getExecutionStepInfo().getPath())
                .extensions(Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "errors", ex.getErrors(),
                        "invalidFields", ex.getInvalidFields()
                ))
                .build();
    }

    // Обработка ошибок доступа
//    @GraphQlExceptionHandler
//    public GraphQLError handleAccessDeniedException(AccessDeniedException ex, DataFetchingEnvironment env) {
//        return GraphqlErrorBuilder.newError()
//                .message("Access denied: " + ex.getMessage())
//                .errorType(ErrorType.FORBIDDEN)
//                .path(env.getExecutionStepInfo().getPath())
//                .extensions(Map.of(
//                        "timestamp", LocalDateTime.now().toString(),
//                        "requiredRole", ex.getRequiredRole()
//                ))
//                .build();
//    }

    // Обработка всех остальных исключений
    @GraphQlExceptionHandler
    public GraphQLError handleGenericException(Exception ex, DataFetchingEnvironment env) {
        Map<String, Object> extensions = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "exceptionType", ex.getClass().getSimpleName()
        );
        return GraphqlErrorBuilder.newError()
                .message("Internal server error: " + ex.getMessage())
                .errorType(ErrorType.INTERNAL_ERROR)
                .path(env.getExecutionStepInfo().getPath())
                .extensions(extensions)
                .build();
    }

    // Обработка с дополнительной логикой
    @GraphQlExceptionHandler
    public GraphQLError handleCustomException(CustomBusinessException ex, DataFetchingEnvironment env) {
        Map<String, Object> extensions = Map.of(
                "errorCode", ex.getErrorCode(),
                "retryable", ex.isRetryable() ? "true" : "false",
                "timestamp", LocalDateTime.now().toString()
        );

        return GraphqlErrorBuilder.newError()
                .message(ex.getMessage())
                .errorType(ErrorType.INTERNAL_ERROR)
                .path(env.getExecutionStepInfo().getPath())
                .extensions(extensions)
                .build();
    }

    // Обработка нескольких типов исключений
    @GraphQlExceptionHandler({ConstraintViolationException.class})
    public GraphQLError handleValidation(Exception ex, DataFetchingEnvironment env) {
        return createError("Validation error: " + ex.getMessage(), ErrorType.BAD_REQUEST, env);
    }

    private GraphQLError createError(String message, ErrorType type, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError()
                .message(message)
                .errorType(type)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation())
                .extensions(Map.of("timestamp", LocalDateTime.now().toString()))
                .build();
    }
}