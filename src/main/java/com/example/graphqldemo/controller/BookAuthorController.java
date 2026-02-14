package com.example.graphqldemo.controller;


import com.example.graphqldemo.entity.Author;
import com.example.graphqldemo.entity.Book;
import com.example.graphqldemo.service.AuthorService;
import com.example.graphqldemo.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GraphQL Контроллер - ключевой класс в GraphQL приложении, который связывает HTTP запросы с бизнес-логикой.
 * <p>
 * Назначение класса
 * <p>
 * BookController в GraphQL терминологии называется Data Fetcher или Resolver.
 * Его основная задача - обрабатывать входящие GraphQL запросы и возвращать данные согласно схеме,
 * определенной в schema.graphqls.
 * <p>
 * В отличие от REST контроллеров, где каждый метод соответствует конкретному URL эндпоинту, здесь
 * методы соответствуют полям в GraphQL схеме.
 * <p>
 * Аннотация @Controller<br>
 * Стандартная Spring аннотация из модуля Spring MVC<br>
 * - Указывает, что класс является Spring компонентом (bean)<br>
 * - Позволяет Spring автоматически обнаружить класс при сканировании компонентов<br>
 * - В контексте GraphQL: регистрирует все методы с @QueryMapping, @MutationMapping, @SchemaMapping как резолверы
 * <p>
 * Аннотация @RequiredArgsConstructor<br>
 * - Lombok аннотация для генерации конструктора<br>
 * - Создает конструктор для всех final полей<br>
 * - Позволяет использовать внедрение зависимостей через конструктор (рекомендуемый способ)
 * <p>
 * Аннотации методов:
 * <p>
 * Аннотации @QueryMapping - для чтения данных:<br>
 * - Соответствует Query полям в GraphQL схеме<br>
 * - Аналог GET запросов в REST<br>
 * - Не изменяет состояние данных<br>
 * - В схеме schema.graphqls определено как:
 * <pre>{@code
 *  type Query {
 *  bookById(id: ID!): Book
 *  }
 *  }</pre>
 * <p>
 * Как работает:
 * <p>
 * 1. Spring GraphQL анализирует входящий запрос
 * <p>
 * 2. Находит метод в контроллере с именем, совпадающим с полем Query (или можно указать явно через name параметр)
 * <p>
 * 3. Вызывает метод в контроллере, передавая аргументы из запроса
 * <p>
 * Возвращает результат
 * <p>
 * Аннотация @MutationMapping - для изменения данных:<br>
 * - Соответствует Mutation полям в схеме<br>
 * - Аналог POST/PUT/DELETE в REST<br>
 * - Изменяет состояние данных<br>
 * - В схеме определено как:
 * <pre>{@code
 * type Mutation {
 *    createBook(title: String!, isbn: String, publishedYear: Int, authorId: ID!): Book!
 * }
 * }</pre>
 * <p>
 * Аннотация @SchemaMapping - для связей между типами
 * <pre>{@code
 *   @SchemaMapping
 *   public List<Book> books(Author author) {
 *   return author.getBooks();
 *   }
 * }</pre>
 * - Связывает поля сложных типов<br>
 * - Определяет, как получить данные для поля, которое не хранится напрямую<br>
 * - В схеме определено как поле в типе:
 * <pre>{@code
 * type Author {
 *   books: [Book!]!  # Это поле нужно заполнить через резолвер/fetcher/контроллер
 * }
 * }</pre>
 * <p>
 * Особенности:
 * <p>
 * - Первый параметр - родительский объект, в примере, это Author<br>
 * - Имя метода в контроллере должно совпадать с именем поля в схеме: books ==> books<br>
 * - Можно явно указать typeName и field если имена отличаются:
 * <pre>{@code
 * @SchemaMapping(typeName="Author", field="books")
 * public List<Book> getAuthorBooks(Author author) {
 *     return author.getBooks();
 * }
 * }</pre>
 * <p>
 * Аннотация @Argument - для получения параметров:
 * <pre>{@code
 * public Book bookById(@Argument Long id) {...}
 * }></pre>
 * <br>
 * - Извлекает аргументы из GraphQL запроса<br>
 * - Имя параметра метода должно совпадать с именем аргумента в схеме: id ==> id<br>
 * - Можно указать явно: {@code @Argument(name = "bookId") Long id}
 * <p>
 * Почему это эффективно?<br>
 * - {@code N+1 проблема} - Spring GraphQL может оптимизировать запросы, группируя вызовы резолверов<br>
 * - {@code Ленивая загрузка} - данные загружаются только если они запрошены клиентом<br>
 * - {@code Гранулярный контроль} - каждый резолвер отвечает только за свое поле
 * <p>
 * <p>
 * Сравнение с REST контроллером
 * <pre>{@code
 * // REST подход - множество эндпоинтов
 * @RestController
 * public class BookRestController {
 *     @GetMapping("/books/{id}")
 *     public Book getBook(@PathVariable Long id) { ... }
 *
 *     @GetMapping("/books")
 *     public List<Book> getAllBooks() { ... }
 *
 *     @GetMapping("/books/search")
 *     public List<Book> searchBooks(@RequestParam String title) { ... }
 *
 *     @PostMapping("/books")
 *     public Book createBook(@RequestBody BookRequest request) { ... }
 * }
 *
 * // GraphQL подход - один контроллер с множеством резолверов
 * @Controller
 * public class BookController {
 *     @QueryMapping public Book bookById(@Argument Long id) { ... }
 *     @QueryMapping public List<Book> allBooks() { ... }
 *     @QueryMapping public List<Book> booksByTitle(@Argument String title) { ... }
 *     @MutationMapping public Book createBook(@Argument String title, ...) { ... }
 * }
 *
 * }</pre>
 * <p>
 * Best Practices:<br>
 * - Разделяй резолверы по типу - можно создать отдельные классы для BookResolver, AuthorResolver<br>
 * - Используй @BatchMapping для сложных связей (улучшает производительность)<br>
 * - Не клади бизнес-логику в контроллер - делегируй сервисам<br>
 * - Обрабатывай ошибки - используй @GraphQlExceptionHandler<br>
 * <p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class BookAuthorController {
    private final BookService bookService;
    private final AuthorService authorService;

    // Queries for Books
    @QueryMapping
    public Book bookById(@Argument Long id) {
        log.info("bookById: id={}", id);
        return bookService.getBookById(id);
    }

    @QueryMapping
    public List<Book> booksByTitle(@Argument String title) {
        log.info("booksByTitle: title={}", title);
        return bookService.getBooksByTitle(title);
    }

    @QueryMapping
    public List<Book> allBooks() {
        log.info("allBooks");
        return bookService.getAllBooks();
    }

    // Queries for Authors
    @QueryMapping
    public Author authorById(@Argument Long id) {
        log.info("authorById: id={}", id);
        return authorService.getAuthorById(id);
    }

    @QueryMapping
    public List<Author> allAuthors() {
        log.info("allAuthors");
        return authorService.getAllAuthors();
    }

    @QueryMapping
    public Author authorByEmail(@Argument String email) {
        log.info("authorByEmail: email={}", email);
        return authorService.getAuthorByEmail(email);
    }

    // Mutations for Books
    @MutationMapping
    public Book createBook(
            @Argument String title,
            @Argument String isbn,
            @Argument Integer publishedYear,
            @Argument Long authorId) {
        log.info("createBook: title={}, isbn={}, publishedYear={}, authorId={}", title, isbn, publishedYear, authorId);
        return bookService.createBook(title, isbn, publishedYear, authorId);
    }

    @MutationMapping
    public Book updateBook(
            @Argument Long id,
            @Argument String title,
            @Argument String isbn,
            @Argument Integer publishedYear) {
        log.info("updateBook: id={}, title={}, isbn={}, publishedYear={}", id, title, isbn, publishedYear);
        return bookService.updateBook(id, title, isbn, publishedYear);
    }

    @MutationMapping
    public Boolean deleteBook(@Argument Long id) {
        log.info("deleteBook: id={}", id);
        return bookService.deleteBook(id);
    }

    // Mutations for Authors
    @MutationMapping
    public Author createAuthor(@Argument String name, @Argument String email) {
        log.info("createAuthor: name={}, email={}", name, email);
        return authorService.createAuthor(name, email);
    }

    @MutationMapping
    public Author updateAuthor(
            @Argument Long id,
            @Argument String name,
            @Argument String email) {
        log.info("updateAuthor: id={}, name={}, email={}", id, name, email);
        return authorService.updateAuthor(id, name, email);
    }

    @MutationMapping
    public Boolean deleteAuthor(@Argument Long id) {
        log.info("deleteAuthor: id={}", id);
        return authorService.deleteAuthor(id);
    }

    // ============== РЕЗОЛВЕРЫ СВЯЗЕЙ ==============
    // Resolvers for relationships

    /**
     * Резолвер для поля books у Author.
     * Простая реализация - загружает книги для одного автора.
     * Подходит для случаев, когда запрашивается один автор.
     */
    @SchemaMapping(typeName = "Author", field = "books")
    public List<Book> books(Author author) {
        log.info("Loading books for author: id={}, name={}", author.getId(), author.getName());

        // Вариант 1: Если книги уже загружены (например, через JOIN в запросе)
        if (author.getBooks() != null && !author.getBooks().isEmpty()) {
            log.info("Books already loaded for author {}", author.getId());
            return author.getBooks();
        }

        // Вариант 2: Явно загружаем книги через сервис
        log.info("Explicitly loading books for author {}", author.getId());
        return bookService.getBooksByAuthorId(author.getId());
        // This is a simple implementation - in real app you might want to use a repository directly
        // or implement batch loading with @BatchMapping
        //return author.getBooks();
    }

    // Это для примера, с явным указанием (typeName="Author", field="books"),
    // если поля в схеме не совпадают с именем метода в контроллере
    //
    //@SchemaMapping(typeName="Author", field="books")
    //public List<Book> getAuthorBooks(Author author) {
    //    return author.getBooks();
    //}

    // Resolvers for relationships

    /**
     * Резолвер для поля author у Book.
     * Этот метод будет вызван для ОДНОЙ книги.
     * Простая реализация - загружает автора для одной книги.
     */
    @SchemaMapping(field = "author")
    public Author author(Book book) {
        log.info("Loading author for book: id={}, title={}", book.getId(), book.getTitle());

        // Вариант 1: Если автор уже загружен (например, через JOIN в запросе)
        if (book.getAuthor() != null) {
            log.info("Author already loaded for book {}", book.getId());
            return book.getAuthor();
        }

        // Вариант 2: Явно загружаем автора через сервис
        log.info("Explicitly loading author for book {}", book.getId());
        return authorService.getAuthorById(book.getAuthor().getId());
        // This is a simple implementation - in real app you might want to use a repository directly
        // or implement batch loading with @BatchMapping
//        return book.getAuthor();
    }

    // ============== БАТЧЕВЫЙ РЕЗОЛВЕР (РЕКОМЕНДУЕМЫЙ) ==============

    /**
     * Батчевый резолвер для загрузки авторов для множества книг одним запросом.
     * Решает проблему N+1 запроса!
     * <p>
     * Как это работает:
     * 1. GraphQL собирает ВСЕ книги, для которых нужно загрузить авторов
     * 2. Вызывает этот метод один раз со списком книг
     * 3. Мы загружаем всех нужных авторов одним запросом к БД
     * 4. Возвращаем {@code Map<Book, Author>} - для каждой книги её автора
     * <p>
     * Этот метод будет вызван для МНОЖЕСТВА книг
     * Spring определяет по наличию List<Book> в параметрах.
     */
    //@BatchMapping(field = "author") //TODO с этим стоит еще попрактиковаться и разобраться.
    @BatchMapping
    public Map<Book, Author> getAuthorBatch(List<Book> books) {
        log.info("Batch loading authors for {} books", books.size());

        if (books.isEmpty()) {
            return Map.of();
        }

        // Логируем IDs книг для отладки
        Set<Long> bookIds = books.stream()
                .map(Book::getId)
                .collect(Collectors.toSet());
        log.info("Book IDs: {}", bookIds);

        // Делегируем загрузку сервису
        // + Мониторинг производительности
        long start = System.currentTimeMillis();
        try {
            return bookService.getAuthorsForBooks(books);
        } finally {
            long duration = System.currentTimeMillis() - start;
            if (duration > 100) { // Если запрос выполняется дольше 100ms
                log.warn("Slow batch loading for {} books: {}ms", books.size(), duration);
            }
        }
    }

    //Еще для примера: Обработка null значений
    //@BatchMapping
    //public Map<Book, Author> author(List<Book> books) {
    //    return books.stream()
    //        .filter(book -> book.getAuthor() != null) // Игнорируем книги без авторов
    //        .collect(Collectors.toMap(
    //            Function.identity(),
    //            book -> authorRepository.findById(book.getAuthor().getId()).orElse(null)
    //        ));
    //}
}
