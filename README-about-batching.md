Давай правильно реализуем эти методы с учетом производительности и лучших практик. 
Особенно важно правильно реализовать `@BatchMapping`, чтобы избежать проблемы N+1 запроса.

## Полная реализация с объяснениями

### 1. **Обновленный BookController**

```java
package com.example.graphqldemo.controller;

import com.example.graphqldemo.model.Author;
import com.example.graphqldemo.model.Book;
import com.example.graphqldemo.service.AuthorService;
import com.example.graphqldemo.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final AuthorService authorService;

    // ... существующие QueryMapping и MutationMapping методы ...

    // ============== РЕЗОЛВЕРЫ СВЯЗЕЙ ==============

    /**
     * Резолвер для поля books у Author.
     * Простая реализация - загружает книги для одного автора.
     * Подходит для случаев, когда запрашивается один автор.
     */
    @SchemaMapping(typeName = "Author", field = "books")
    public List<Book> getAuthorBooks(Author author) {
        log.debug("Loading books for author: id={}, name={}", author.getId(), author.getName());
        
        // Вариант 1: Если книги уже загружены (например, через JOIN в запросе)
        if (author.getBooks() != null && !author.getBooks().isEmpty()) {
            log.debug("Books already loaded for author {}", author.getId());
            return author.getBooks();
        }
        
        // Вариант 2: Явно загружаем книги через сервис
        log.debug("Explicitly loading books for author {}", author.getId());
        return bookService.getBooksByAuthorId(author.getId());
    }

    /**
     * Резолвер для поля author у Book.
     * Простая реализация - загружает автора для одной книги.
     */
    @SchemaMapping
    public Author getAuthor(Book book) {
        log.debug("Loading author for book: id={}, title={}", book.getId(), book.getTitle());
        
        // Вариант 1: Если автор уже загружен
        if (book.getAuthor() != null) {
            log.debug("Author already loaded for book {}", book.getId());
            return book.getAuthor();
        }
        
        // Вариант 2: Явно загружаем автора через сервис
        log.debug("Explicitly loading author for book {}", book.getId());
        return authorService.getAuthorById(book.getAuthor().getId());
    }

    // ============== БАТЧЕВЫЙ РЕЗОЛВЕР (РЕКОМЕНДУЕМЫЙ) ==============

    /**
     * Батчевый резолвер для загрузки авторов для множества книг одним запросом.
     * Решает проблему N+1 запроса!
     * 
     * Как это работает:
     * 1. GraphQL собирает ВСЕ книги, для которых нужно загрузить авторов
     * 2. Вызывает этот метод один раз со списком книг
     * 3. Мы загружаем всех нужных авторов одним запросом к БД
     * 4. Возвращаем Map<Book, Author> - для каждой книги её автора
     */
    @BatchMapping
    public Map<Book, Author> author(List<Book> books) {
        log.info("Batch loading authors for {} books", books.size());
        
        if (books.isEmpty()) {
            return Map.of();
        }
        
        // Логируем IDs книг для отладки
        Set<Long> bookIds = books.stream()
                .map(Book::getId)
                .collect(Collectors.toSet());
        log.debug("Book IDs: {}", bookIds);
        
        // Делегируем загрузку сервису
        return bookService.getAuthorsForBooks(books);
    }
}
```

### 2. **Сервис с реализацией батчевой загрузки**

```java
package com.example.graphqldemo.service;

import com.example.graphqldemo.model.Author;
import com.example.graphqldemo.model.Book;
import com.example.graphqldemo.repository.AuthorRepository;
import com.example.graphqldemo.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    // ... существующие методы ...

    /**
     * Получить книги по ID автора
     */
    @Transactional(readOnly = true)
    public List<Book> getBooksByAuthorId(Long authorId) {
        log.debug("Finding books by author id: {}", authorId);
        return bookRepository.findByAuthorId(authorId);
    }

    // ============== БАТЧЕВАЯ ЗАГРУЗКА ==============

    /**
     * Эффективно загружает авторов для множества книг одним запросом.
     * Это核心 метода для @BatchMapping!
     * 
     * Алгоритм:
     * 1. Собираем все ID авторов из книг
     * 2. Загружаем всех авторов одним запросом (WHERE id IN (...))
     * 3. Создаем Map для быстрого доступа authorId -> Author
     * 4. Для каждой книги находим её автора и возвращаем в Map
     */
    @Transactional(readOnly = true)
    public Map<Book, Author> getAuthorsForBooks(List<Book> books) {
        log.info("Batch loading authors for {} books", books.size());
        
        if (books.isEmpty()) {
            return Collections.emptyMap();
        }

        // Шаг 1: Собираем все уникальные ID авторов из книг
        Set<Long> authorIds = books.stream()
                .map(book -> book.getAuthor().getId())  // Получаем ID автора из книги
                .collect(Collectors.toSet());
        
        log.debug("Unique author IDs to load: {}", authorIds);

        // Шаг 2: Загружаем всех авторов ОДНИМ запросом к БД
        List<Author> authors = authorRepository.findAllById(authorIds);
        log.debug("Loaded {} authors from database", authors.size());

        // Шаг 3: Создаем Map для быстрого доступа authorId -> Author
        Map<Long, Author> authorById = authors.stream()
                .collect(Collectors.toMap(
                    Author::getId,      // ключ - ID автора
                    Function.identity(), // значение - сам автор
                    (existing, replacement) -> existing // на случай дубликатов
                ));

        // Шаг 4: Для каждой книги находим её автора
        Map<Book, Author> result = new HashMap<>(books.size());
        for (Book book : books) {
            Long authorId = book.getAuthor().getId();
            Author author = authorById.get(authorId);
            
            if (author == null) {
                log.warn("Author with id {} not found for book: {}", authorId, book.getId());
                // Можно выбросить исключение или пропустить
                continue;
            }
            
            // Важно: устанавливаем связь и в обратную сторону для кэширования
            book.setAuthor(author);
            result.put(book, author);
        }

        log.info("Successfully mapped {} books to authors", result.size());
        return result;
    }

    /**
     * Альтернативная реализация с использованием JOIN FETCH
     * Еще более эффективный способ, если тебе нужны только авторы
     */
    @Transactional(readOnly = true)
    public Map<Book, Author> getAuthorsForBooksOptimized(List<Book> books) {
        if (books.isEmpty()) {
            return Collections.emptyMap();
        }

        // Собираем ID книг
        Set<Long> bookIds = books.stream()
                .map(Book::getId)
                .collect(Collectors.toSet());

        // Загружаем книги сразу с авторами через JOIN FETCH
        List<Book> booksWithAuthors = bookRepository.findAllWithAuthorsByIds(bookIds);
        
        // Создаем результат
        return booksWithAuthors.stream()
                .collect(Collectors.toMap(
                    Function.identity(),
                    Book::getAuthor,
                    (b1, b2) -> b1
                ));
    }
}
```

### 3. **Добавляем нужные методы в репозиторий**

```java
package com.example.graphqldemo.repository;

import com.example.graphqldemo.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    // Существующий метод
    List<Book> findByAuthorId(Long authorId);
    
    // Новый метод для батчевой загрузки с JOIN FETCH
    @Query("SELECT b FROM Book b JOIN FETCH b.author a WHERE b.id IN :bookIds")
    List<Book> findAllWithAuthorsByIds(@Param("bookIds") Set<Long> bookIds);
    
    // Альтернативный метод, если нужно загрузить книги для множества авторов
    @Query("SELECT b FROM Book b WHERE b.author.id IN :authorIds")
    List<Book> findAllByAuthorIds(@Param("authorIds") Set<Long> authorIds);
}
```

### 4. **Добавляем метод в AuthorRepository**

```java
package com.example.graphqldemo.repository;

import com.example.graphqldemo.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    
    Optional<Author> findByEmail(String email);
    boolean existsByEmail(String email);
    
    // Для загрузки авторов с их книгами (если нужно)
    @Query("SELECT a FROM Author a LEFT JOIN FETCH a.books WHERE a.id IN :authorIds")
    List<Author> findAllWithBooksByIds(@Param("authorIds") Set<Long> authorIds);
}
```

### 5. **Тестовый пример для демонстрации**

```java
package com.example.graphqldemo.service;

import com.example.graphqldemo.model.Author;
import com.example.graphqldemo.model.Book;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

@Slf4j
@SpringBootTest
public class BatchLoadingTest {

    @Autowired
    private BookService bookService;
    
    @Autowired
    private BookRepository bookRepository;

    @Test
    public void demonstrateBatchLoading() {
        // Получаем список книг (например, первые 10)
        List<Book> books = bookRepository.findAll().subList(0, Math.min(10, bookRepository.count()));
        
        log.info("Loading authors for {} books", books.size());
        
        // Без батчевой загрузки было бы 10+1 запросов к БД!
        // С батчевой загрузкой - всего 2 запроса:
        // 1. Запрос на получение книг
        // 2. Один запрос на получение всех авторов
        Map<Book, Author> authorMap = bookService.getAuthorsForBooks(books);
        
        log.info("Successfully loaded {} authors", authorMap.size());
        
        // Проверяем результат
        authorMap.forEach((book, author) -> {
            log.info("Book: {} -> Author: {}", book.getTitle(), author.getName());
        });
    }
}
```

### 6. **GraphQL запрос для тестирования батчевой загрузки**

```graphql
# Этот запрос продемонстрирует работу @BatchMapping
# Без батчевой загрузки было бы:
# - 1 запрос для получения книг
# - ДЛЯ КАЖДОЙ книги отдельный запрос для автора (N запросов)
# С @BatchMapping:
# - 1 запрос для книг
# - 1 запрос для всех авторов

query TestBatchLoading {
  allBooks {
    id
    title
    author {
      id
      name
      email
    }
  }
}
```

## Почему это важно?

### Проблема N+1 запроса:
```java
// ❌ ПЛОХО: N+1 запросов
for (Book book : books) {
    Author author = authorRepository.findById(book.getAuthor().getId()); // N дополнительных запросов!
}

// ✅ ХОРОШО: 1 запрос
Set<Long> authorIds = books.stream().map(b -> b.getAuthor().getId()).collect(Collectors.toSet());
List<Author> authors = authorRepository.findAllById(authorIds); // Один запрос!
```

### Сравнение производительности:

| Количество книг | Без @BatchMapping | С @BatchMapping | Улучшение |
|----------------|-------------------|-----------------|-----------|
| 10 | 11 запросов | 2 запроса | 5.5x |
| 100 | 101 запрос | 2 запроса | 50.5x |
| 1000 | 1001 запрос | 2 запроса | 500x |

## Дополнительные советы

### 1. **Обработка null значений**
```java
@BatchMapping
public Map<Book, Author> author(List<Book> books) {
    return books.stream()
        .filter(book -> book.getAuthor() != null) // Игнорируем книги без авторов
        .collect(Collectors.toMap(
            Function.identity(),
            book -> authorRepository.findById(book.getAuthor().getId()).orElse(null)
        ));
}
```

### 2. **Кэширование результатов**
```java
@Cacheable(value = "authors", key = "#books.size()")
public Map<Book, Author> getAuthorsForBooks(List<Book> books) {
    // реализация
}
```

### 3. **Мониторинг производительности**
```java
@BatchMapping
public Map<Book, Author> author(List<Book> books) {
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
```

Такая реализация обеспечивает оптимальную производительность и масштабируемость твоего GraphQL API!

---

Эта ошибка возникает потому, что у тебя в контроллере есть **конфликтующие методы** - Spring не может определить, какой метод использовать для резолвинга поля `author`. Давай разберем проблему и ее решение.

## Причина ошибки

У тебя в контроллере есть два метода, которые пытаются обрабатывать одно и то же поле `author`:

```java
@Controller
public class BookAuthorController {
    
    // Метод 1: Обычный резолвер для поля author
    @SchemaMapping
    public Author author(Book book) {
        return book.getAuthor();
    }
    
    // Метод 2: Батчевый резолвер для того же поля author
    @BatchMapping
    public Map<Book, Author> loadAuthors(List<Book> books) {  // Конфликт!
        return bookService.getAuthorsForBooks(books);
    }
}
```

Spring видит, что оба метода претендуют на обработку поля `author` у типа `Book`, и не может выбрать, какой использовать.

## Решения проблемы

### **Решение 1: Удалить обычный резолвер (правильный подход)**

Если ты используешь `@BatchMapping`, обычный резолвер не нужен:

```java
@Slf4j
@Controller
@RequiredArgsConstructor
public class BookAuthorController {
    
    private final BookService bookService;
    
    // Только батчевый метод
    @BatchMapping
    public Map<Book, Author> author(List<Book> books) {
        log.info("Batch loading authors for {} books", books.size());
        return bookService.getAuthorsForBooks(books);
    }
    
    // Другие методы контроллера...
    @QueryMapping
    public List<Book> allBooks() {
        return bookService.getAllBooks();
    }
}
```

### **Решение 2: Разные имена методов (если очень нужно оставить оба)**

```java
@Controller
@RequiredArgsConstructor
public class BookAuthorController {
    
    private final BookService bookService;
    
    // Для одиночной загрузки (будет использоваться, если @BatchMapping не подходит)
    @SchemaMapping(field = "author")
    public Author getAuthorSingle(Book book) {
        log.info("Single author load for book {}", book.getId());
        return book.getAuthor();
    }
    
    // Для батчевой загрузки
    @BatchMapping(field = "author")  // Явно указываем, что это для поля author
    public Map<Book, Author> getAuthorBatch(List<Book> books) {
        log.info("Batch loading authors for {} books", books.size());
        return bookService.getAuthorsForBooks(books);
    }
}
```

### **Решение 3: Использовать разные типы параметров (не рекомендуется)**

```java
@Controller
@RequiredArgsConstructor
public class BookAuthorController {
    
    // Этот будет вызван для одной книги
    @SchemaMapping
    public Author author(Book book) {
        return book.getAuthor();
    }
    
    // Этот будет вызван для списка книг
    // Проблема: Spring все равно видит конфликт!
    @BatchMapping
    public Map<Book, Author> author(List<Book> books) {
        return bookService.getAuthorsForBooks(books);
    }
}
```

## Полный правильный пример

Вот как должен выглядеть твой контроллер:

```java
package com.example.graphqldemo.controller;

import com.example.graphqldemo.entity.Author;
import com.example.graphqldemo.entity.Book;
import com.example.graphqldemo.service.BookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BookAuthorController {
    
    private final BookService bookService;
    
    // ============== QUERY METHODS ==============
    
    @QueryMapping
    public List<Book> allBooks() {
        return bookService.getAllBooks();
    }
    
    @QueryMapping
    public Book bookById(@Argument Long id) {
        return bookService.getBookById(id);
    }
    
    // ============== MUTATION METHODS ==============
    
    @MutationMapping
    public Book createBook(
            @Argument String title,
            @Argument String isbn,
            @Argument Integer publishedYear,
            @Argument Long authorId) {
        return bookService.createBook(title, isbn, publishedYear, authorId);
    }
    
    // ============== FIELD RESOLVERS ==============
    
    /**
     * Batch resolver for author field on Book type.
     * This will be called once for all books that need their authors.
     * No need for a separate single resolver!
     */
    @BatchMapping
    public Map<Book, Author> author(List<Book> books) {
        log.info("Batch loading authors for {} books", books.size());
        
        if (books.isEmpty()) {
            return Map.of();
        }
        
        // Логируем, какие книги обрабатываем
        books.forEach(book -> 
            log.debug("Processing book ID: {}, Author ID: {}", 
                book.getId(), 
                book.getAuthor() != null ? book.getAuthor().getId() : "null")
        );
        
        // Делегируем сервису
        return bookService.getAuthorsForBooks(books);
    }
    
    /**
     * Optional: Если нужно поле books у Author
     */
    @BatchMapping
    public Map<Author, List<Book>> books(List<Author> authors) {
        log.info("Batch loading books for {} authors", authors.size());
        return bookService.getBooksForAuthors(authors);
    }
}
```

## Соответствующий сервис

```java
package com.example.graphqldemo.service;

import com.example.graphqldemo.entity.Author;
import com.example.graphqldemo.entity.Book;
import com.example.graphqldemo.repository.AuthorRepository;
import com.example.graphqldemo.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {
    
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    
    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Book not found: " + id));
    }
    
    @Transactional
    public Book createBook(String title, String isbn, Integer publishedYear, Long authorId) {
        Author author = authorRepository.findById(authorId)
            .orElseThrow(() -> new RuntimeException("Author not found: " + authorId));
        
        Book book = new Book();
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setPublishedYear(publishedYear);
        book.setAuthor(author);
        
        return bookRepository.save(book);
    }
    
    /**
     * Батчевая загрузка авторов для книг
     */
    @Transactional(readOnly = true)
    public Map<Book, Author> getAuthorsForBooks(List<Book> books) {
        if (books.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // Собираем уникальные ID авторов
        Set<Long> authorIds = books.stream()
            .map(book -> book.getAuthor().getId())
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        log.debug("Loading authors with IDs: {}", authorIds);
        
        // Загружаем всех авторов одним запросом
        List<Author> authors = authorRepository.findAllById(authorIds);
        
        // Создаем Map authorId -> Author
        Map<Long, Author> authorById = authors.stream()
            .collect(Collectors.toMap(Author::getId, Function.identity()));
        
        // Создаем результат
        Map<Book, Author> result = new HashMap<>(books.size());
        for (Book book : books) {
            Author author = authorById.get(book.getAuthor().getId());
            if (author != null) {
                result.put(book, author);
                // Кэшируем в книге
                book.setAuthor(author);
            } else {
                log.warn("Author not found for book {} with authorId {}", 
                    book.getId(), book.getAuthor().getId());
            }
        }
        
        log.debug("Successfully loaded {} authors for {} books", 
            result.size(), books.size());
        
        return result;
    }
    
    /**
     * Батчевая загрузка книг для авторов
     */
    @Transactional(readOnly = true)
    public Map<Author, List<Book>> getBooksForAuthors(List<Author> authors) {
        if (authors.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Set<Long> authorIds = authors.stream()
            .map(Author::getId)
            .collect(Collectors.toSet());
        
        // Загружаем все книги для этих авторов
        List<Book> books = bookRepository.findAllByAuthorIds(authorIds);
        
        // Группируем книги по автору
        Map<Author, List<Book>> result = new HashMap<>(authors.size());
        
        // Создаем Map для быстрого доступа к автору
        Map<Long, Author> authorById = authors.stream()
            .collect(Collectors.toMap(Author::getId, Function.identity()));
        
        // Группируем книги
        Map<Long, List<Book>> booksByAuthorId = books.stream()
            .collect(Collectors.groupingBy(book -> book.getAuthor().getId()));
        
        // Собираем результат
        for (Author author : authors) {
            List<Book> authorBooks = booksByAuthorId.getOrDefault(author.getId(), new ArrayList<>());
            result.put(author, authorBooks);
            // Кэшируем в авторе
            author.setBooks(authorBooks);
        }
        
        return result;
    }
}
```

## Проверка, что конфликта нет

После исправления, при запуске приложения ты должен увидеть в логах что-то такое:

```
INFO  o.s.g.d.s.RuntimeWiringGraphQlSource - GraphQL schema built with:
  Query: allBooks, bookById
  Mutation: createBook
  Batch mappings: 
    Book.author -> java.util.Map<Book, Author> (BookAuthorController.author)
    Author.books -> java.util.Map<Author, List<Book>> (BookAuthorController.books)
```

## Дополнительная проверка через GraphiQL

Выполни этот запрос, чтобы убедиться, что батчевая загрузка работает:

```graphql
{
  allBooks {
    id
    title
    author {
      id
      name
    }
  }
}
```

В логах ты должен увидеть ОДИН batched-запрос, а не множество отдельных:

```
INFO  c.e.g.controller.BookAuthorController - Batch loading authors for 5 books
DEBUG c.e.g.service.BookService - Loading authors with IDs: [1, 2, 3]
DEBUG c.e.g.service.BookService - Successfully loaded 3 authors for 5 books
```

## Ключевые моменты

1. **Нельзя иметь два метода, обрабатывающих одно поле** - Spring выберет один, и это вызовет ошибку
2. **`@BatchMapping` достаточно одного** - он заменяет обычный `@SchemaMapping`
3. **Имя метода не важно**, важно поле, которое он обрабатывает
4. **Возвращаемый тип** должен быть `Map<Родитель, Дочерний>` или `Flux<Дочерний>` для реактивного стека

После удаления дублирующего метода приложение запустится без ошибок!

---

Абсолютно верно! Ты правильно понял - если методы имеют **разные названия**, но обрабатывают **одно и то же поле** (field = "author"), Spring всё равно видит конфликт. Давай я подробно объясню почему и покажу правильное решение.

## Почему возникает конфликт

Spring GraphQL строит **реестр резолверов** по принципу: **тип + поле**. Неважно, как называются методы - важно, для какого поля они предназначены:

```java
// Оба метода претендуют на одно и то же место в реестре:
// Тип: Book, Поле: author

@SchemaMapping(field = "author")  // Регистрируется как (Book, author)
public Author getAuthorSingle(Book book) {}

@BatchMapping(field = "author")    // Тоже пытается зарегистрироваться как (Book, author)
public Map<Book, Author> getAuthorBatch(List<Book> books) {}
```

## Правильное решение

### **Вариант 1: Использовать только `@BatchMapping` (рекомендуется)**

```java
@Slf4j
@Controller
@RequiredArgsConstructor
public class BookController {
    
    private final BookService bookService;
    
    // ============== QUERIES ==============
    
    @QueryMapping
    public List<Book> allBooks() {
        return bookService.getAllBooks();
    }
    
    @QueryMapping
    public Book bookById(@Argument Long id) {
        return bookService.getBookById(id);
    }
    
    // ============== MUTATIONS ==============
    
    @MutationMapping
    public Book createBook(
            @Argument String title,
            @Argument String isbn,
            @Argument Integer publishedYear,
            @Argument Long authorId) {
        return bookService.createBook(title, isbn, publishedYear, authorId);
    }
    
    // ============== BATCH RESOLVERS ==============
    
    /**
     * ЕДИНСТВЕННЫЙ резолвер для поля author у Book.
     * Spring автоматически вызовет этот метод, когда нужно загрузить авторов
     * для нескольких книг сразу.
     */
    @BatchMapping   // По умолчанию ищет поле с именем метода = "author"
    public Map<Book, Author> author(List<Book> books) {
        log.info("Batch loading authors for {} books", books.size());
        
        if (books.isEmpty()) {
            return Map.of();
        }
        
        // Собираем ID авторов
        Set<Long> authorIds = books.stream()
            .map(book -> book.getAuthor().getId())
            .collect(Collectors.toSet());
        
        log.debug("Loading authors with IDs: {}", authorIds);
        
        // Загружаем всех авторов одним запросом
        List<Author> authors = authorService.findAllById(authorIds);
        
        // Создаем Map для быстрого доступа
        Map<Long, Author> authorById = authors.stream()
            .collect(Collectors.toMap(Author::getId, Function.identity()));
        
        // Формируем результат для батчера
        Map<Book, Author> result = new HashMap<>();
        for (Book book : books) {
            Author author = authorById.get(book.getAuthor().getId());
            if (author != null) {
                result.put(book, author);
                // Кэшируем в книге для будущих обращений
                book.setAuthor(author);
            }
        }
        
        log.debug("Successfully mapped {} authors", result.size());
        return result;
    }
    
    /**
     * Если нужно поле books у Author - тоже используем @BatchMapping
     */
    @BatchMapping
    public Map<Author, List<Book>> books(List<Author> authors) {
        log.info("Batch loading books for {} authors", authors.size());
        return bookService.getBooksForAuthors(authors);
    }
}
```

### **Вариант 2: Разделить на разные классы (альтернативный подход)**

Можно разделить резолверы по разным классам - тогда конфликта не будет:

```java
// ============== ОСНОВНОЙ КОНТРОЛЛЕР ==============
@Slf4j
@Controller
@RequiredArgsConstructor
public class BookController {
    
    private final BookService bookService;
    
    @QueryMapping
    public List<Book> allBooks() {
        return bookService.getAllBooks();
    }
    
    @QueryMapping
    public Book bookById(@Argument Long id) {
        return bookService.getBookById(id);
    }
    
    @MutationMapping
    public Book createBook(@Argument String title, @Argument Long authorId) {
        return bookService.createBook(title, authorId);
    }
}

// ============== РЕЗОЛВЕР ДЛЯ AUTHOR ==============
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthorResolver {
    
    private final BookService bookService;
    
    /**
     * Этот класс отвечает ТОЛЬКО за резолвинг поля author у Book
     */
    @BatchMapping
    public Map<Book, Author> author(List<Book> books) {
        log.info("Resolver: loading authors for {} books", books.size());
        return bookService.getAuthorsForBooks(books);
    }
}

// ============== РЕЗОЛВЕР ДЛЯ BOOKS ==============
@Slf4j
@Controller
@RequiredArgsConstructor
public class BooksResolver {
    
    private final BookService bookService;
    
    /**
     * Этот класс отвечает ТОЛЬКО за резолвинг поля books у Author
     */
    @BatchMapping
    public Map<Author, List<Book>> books(List<Author> authors) {
        log.info("Resolver: loading books for {} authors", authors.size());
        return bookService.getBooksForAuthors(authors);
    }
}
```

### **Вариант 3: Явное указание типа и поля (для сложных случаев)**

Если имена методов не совпадают с полями:

```java
@Controller
@RequiredArgsConstructor
public class CustomResolvers {
    
    private final BookService bookService;
    
    /**
     * Явно указываем, что этот метод обрабатывает поле author у типа Book
     */
    @BatchMapping(typeName = "Book", field = "author")
    public Map<Book, Author> resolveAuthorsForBooks(List<Book> books) {
        return bookService.getAuthorsForBooks(books);
    }
    
    /**
     * Явно указываем для поля books у типа Author
     */
    @BatchMapping(typeName = "Author", field = "books")
    public Map<Author, List<Book>> resolveBooksForAuthors(List<Author> authors) {
        return bookService.getBooksForAuthors(authors);
    }
}
```

## Демонстрация работы

Чтобы увидеть, что батчевая загрузка действительно работает, добавь логирование:

```java
@BatchMapping
public Map<Book, Author> author(List<Book> books) {
    log.info("========== BATCH RESOLVER CALLED ==========");
    log.info("Books count: {}", books.size());
    books.forEach(book -> 
        log.info("  Book id: {}, author id: {}", 
            book.getId(), 
            book.getAuthor() != null ? book.getAuthor().getId() : "null")
    );
    
    long startTime = System.currentTimeMillis();
    
    Map<Book, Author> result = bookService.getAuthorsForBooks(books);
    
    log.info("Batch resolved {} authors in {} ms", result.size(), 
        System.currentTimeMillis() - startTime);
    log.info("==========================================");
    
    return result;
}
```

## Тестовый запрос для проверки

Выполни этот запрос в GraphiQL:

```graphql
{
  # Запрос всех книг - проверит батчевую загрузку авторов
  allBooks {
    id
    title
    author {
      id
      name
      email
    }
  }
  
  # Запрос одного автора - проверит, что резолвер работает и для одного объекта
  authorById(id: 1) {
    id
    name
    books {
      id
      title
    }
  }
}
```

В логах ты увидишь:
```
INFO  c.e.g.controller.AuthorResolver - ========== BATCH RESOLVER CALLED ==========
INFO  c.e.g.controller.AuthorResolver - Books count: 5
INFO  c.e.g.controller.AuthorResolver -   Book id: 1, author id: 1
INFO  c.e.g.controller.AuthorResolver -   Book id: 2, author id: 2
INFO  c.e.g.controller.AuthorResolver -   Book id: 3, author id: 1
INFO  c.e.g.controller.AuthorResolver -   Book id: 4, author id: 3
INFO  c.e.g.controller.AuthorResolver -   Book id: 5, author id: 2
INFO  c.e.g.service.BookService - Loading authors with IDs: [1, 2, 3]
INFO  c.e.g.service.BookService - Found authors: 3
INFO  c.e.g.controller.AuthorResolver - Batch resolved 5 authors in 15 ms
INFO  c.e.g.controller.AuthorResolver - ==========================================
```

## Что важно запомнить

1. **Один метод на одно поле** - нельзя иметь два метода для одного поля
2. **`@BatchMapping` достаточно одного** - он заменяет обычный резолвер
3. **Имя метода не важно**, важно только поле, которое он обрабатывает
4. **Разделение по классам** помогает организовать код, но не влияет на уникальность поля

Ты всё делаешь правильно, удалив лишний метод - это единственный способ избежать конфликта!

---
