Минимальное приложение на GraphQL. Постое приложение для работы с книгами и авторами.

## Структура проекта

```
graphql-demo/
├── docker-compose.yml
├── pom.xml
├── src/
│   └── main/
│       ├── java/com/example/graphqldemo/
│       │   ├── GraphqlDemoApplication.java
│       │   ├── controller/
│       │   │   └── BookController.java
│       │   ├── model/
│       │   │   ├── Author.java
│       │   │   └── Book.java
│       │   ├── repository/
│       │   │   ├── AuthorRepository.java
│       │   │   └── BookRepository.java
│       │   └── service/
│       │       ├── BookService.java
│       │       └── AuthorService.java
│       └── resources/
│           ├── application.yml
│           ├── schema.graphqls
│           └── db/migration/
│               └── V1__init.sql
```

## 1. docker-compose.yml для запуска БД может быть таким:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16
    container_name: graphql-demo-db
    environment:
      POSTGRES_DB: graphqldb
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: unless-stopped

volumes:
  postgres_data:
```

## 2. GraphQL схема (src/main/resources/graphql/schema.graphqls)

```graphql
type Author {
    id: ID!
    name: String!
    email: String!
    books: [Book!]!
}

type Book {
    id: ID!
    title: String!
    isbn: String
    publishedYear: Int
    author: Author!
}

type Query {
    # Book queries
    bookById(id: ID!): Book
    booksByTitle(title: String!): [Book!]!
    allBooks: [Book!]!
    
    # Author queries
    authorById(id: ID!): Author
    allAuthors: [Author!]!
    authorByEmail(email: String!): Author
}

type Mutation {
    # Book mutations
    createBook(title: String!, isbn: String, publishedYear: Int, authorId: ID!): Book!
    updateBook(id: ID!, title: String, isbn: String, publishedYear: Int): Book!
    deleteBook(id: ID!): Boolean!
    
    # Author mutations
    createAuthor(name: String!, email: String!): Author!
    updateAuthor(id: ID!, name: String, email: String): Author!
    deleteAuthor(id: ID!): Boolean!
}
```

## Запуск и тестирование

### 1. Запуск PostgreSQL:
```bash
docker-compose up -d
```

### 2. Запуск приложения:
```bash
./mvnw spring-boot:run
```

### 3. Доступ к GraphiQL:
Открой браузер и перейди по адресу: http://localhost:8181/graphiql

### Примеры запросов:

**Получить все книги:**
```graphql
query {
  allBooks {
    id
    title
    isbn
    publishedYear
    author {
      id
      name
      email
    }
  }
}
```

**Создать новую книгу:**
```graphql
mutation {
  createBook(
    title: "Война и мир"
    isbn: "978-5-389-00005-0"
    publishedYear: 1869
    authorId: 3
  ) {
    id
    title
    author {
      name
    }
  }
}
```

**Найти автора по email:**
```graphql
query {
  authorByEmail(email: "jk.rowling@email.com") {
    id
    name
    books {
      title
      publishedYear
    }
  }
}
```

## Основные особенности этого решения:

1. **GraphQL Schema First подход** - определяем схему в файле `.graphqls`
2. **Автоматическая документация** через GraphiQL интерфейс
3. **Полноценные CRUD операции** для обеих сущностей
4. **Связи между объектами** через резолверы
5. **Миграции БД** через Flyway
6. **Docker** для PostgreSQL

Теперь можно экспериментировать с запросами в GraphiQL интерфейсе.

---

GraphiQL - это интерактивная среда для выполнения GraphQL запросов прямо в браузере. 
Покажу, как ей пользоваться!

## Интерфейс GraphiQL

Когда ты открываешь `http://localhost:8181/graphiql`, ты видишь три основные панели:

1. **Левая панель** - здесь ты пишешь запросы
2. **Средняя панель** - здесь отображается результат
3. **Правая панель** (можно открыть по кнопке "Docs") - документация по API

## Основные возможности

### 1. **Автодополнение (IntelliSense)**
Начни печатать, и GraphiQL будет предлагать варианты. Например, набери `{` и нажми `Ctrl+Space` (или `Cmd+Space` на Mac).

### 2. **Документация**
Нажми на кнопку "Docs" в правом верхнем углу - откроется интерактивная документация по всем доступным запросам и типам.

## Примеры запросов для тестирования

### **Базовые запросы (Queries)**

#### Получить все книги:
```graphql
{
  allBooks {
    id
    title
    isbn
    publishedYear
    author {
      id
      name
    }
  }
}
```

#### Получить конкретную книгу по ID:
```graphql
{
  bookById(id: 1) {
    title
    publishedYear
    author {
      name
      email
    }
  }
}
```

#### Получить всех авторов и их книги:
```graphql
{
  allAuthors {
    id
    name
    email
    books {
      title
      publishedYear
    }
  }
}
```

#### Поиск книг по названию:
```graphql
{
  booksByTitle(title: "Гарри") {
    title
    author {
      name
    }
  }
}
```

### **Мутации (Изменение данных)**

#### Создать нового автора:
```graphql
mutation {
  createAuthor(
    name: "Фёдор Достоевский"
    email: "f.dostoevsky@email.com"
  ) {
    id
    name
    email
  }
}
```

#### Создать новую книгу:
```graphql
mutation {
  createBook(
    title: "Преступление и наказание"
    isbn: "978-5-389-00006-7"
    publishedYear: 1866
    authorId: 4  # ID созданного выше автора
  ) {
    id
    title
    author {
      name
    }
  }
}
```

#### Обновить книгу:
```graphql
mutation {
  updateBook(
    id: 1
    title: "Гарри Поттер и философский камень (обновленное издание)"
  ) {
    id
    title
  }
}
```

#### Удалить книгу:
```graphql
mutation {
  deleteBook(id: 1)
}
```

### **Сложные запросы с фрагментами**

Можно переиспользовать части запросов:

```graphql
{
  author1: authorById(id: 1) {
    ...authorInfo
  }
  
  author2: authorById(id: 2) {
    ...authorInfo
  }
}

fragment authorInfo on Author {
  id
  name
  email
  books {
    title
    publishedYear
  }
}
```

### **Запросы с переменными**

В левой панели пишем запрос:
```graphql
query GetBook($bookId: ID!) {
  bookById(id: $bookId) {
    title
    author {
      name
    }
  }
}
```

В нижней панели "Query Variables" добавляем переменные:
```json
{
  "bookId": "2"
}
```

## Полезные клавиши

- `Ctrl+Enter` (или `Cmd+Enter` на Mac) - выполнить запрос
- `Ctrl+Space` - показать подсказки
- `Shift+Ctrl+P` - форматировать запрос (сделать красиво)
- `Ctrl+F` - поиск по тексту запроса

## Пошаговое знакомство

Давай попробуем выполнить несколько запросов, чтобы понять работу GraphQL:

### **Шаг 1: Посмотрим, что у нас есть в базе**
```graphql
{
  allAuthors {
    id
    name
  }
}
```

### **Шаг 2: Выберем только нужные поля**
```graphql
{
  allBooks {
    title
    author {
      name
    }
  }
}
```

### **Шаг 3: Создадим новые данные**
```graphql
mutation {
  createAuthor(
    name: "Михаил Булгаков"
    email: "bulgakov@email.com"
  ) {
    id
    name
  }
}
```

### **Шаг 4: Сложный запрос с несколькими сущностями**
```graphql
{
  # Информация о первом авторе
  author1: authorById(id: 1) {
    name
    books {
      title
    }
  }
  
  # Информация о второй книге
  book2: bookById(id: 2) {
    title
    author {
      name
      email
    }
  }
  
  # Все книги с фильтрацией по году
  oldBooks: booksByTitle(title: "1984") {
    title
    publishedYear
  }
}
```

## Особенности GraphQL, которые ты заметишь:

1. **Только те поля, которые нужны** - в ответе приходят только запрошенные поля
2. **Иерархическая структура** - запрос повторяет структуру ответа
3. **Сильная типизация** - каждый тип данных строго определён
4. **Один эндпоинт** - все запросы идут на один URL

## Советы для новичков:

1. **Изучай документацию** - кнопка "Docs" покажет все доступные запросы и типы
2. **Начинай с простых запросов** - сначала получи все книги, потом добавляй поля
3. **Используй автодополнение** - оно подскажет, какие поля доступны
4. **Не бойся экспериментировать** - это не production, можно пробовать любые запросы

GraphiQL - отличный инструмент для изучения GraphQL, так как он даёт полную информацию о доступных данных прямо в интерфейсе. Чем больше будешь экспериментировать, тем быстрее освоишь синтаксис и возможности GraphQL!

---

Отлично! Давай подробно разберем `BookController` - это ключевой класс в GraphQL приложении, который связывает HTTP запросы с бизнес-логикой.

## Полный листинг класса BookController

```java
package com.example.graphqldemo.controller;

import com.example.graphqldemo.model.Author;
import com.example.graphqldemo.model.Book;
import com.example.graphqldemo.service.AuthorService;
import com.example.graphqldemo.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final AuthorService authorService;

    // Queries for Books
    @QueryMapping
    public Book bookById(@Argument Long id) {
        return bookService.getBookById(id);
    }

    @QueryMapping
    public List<Book> booksByTitle(@Argument String title) {
        return bookService.getBooksByTitle(title);
    }

    @QueryMapping
    public List<Book> allBooks() {
        return bookService.getAllBooks();
    }

    // Queries for Authors
    @QueryMapping
    public Author authorById(@Argument Long id) {
        return authorService.getAuthorById(id);
    }

    @QueryMapping
    public List<Author> allAuthors() {
        return authorService.getAllAuthors();
    }

    @QueryMapping
    public Author authorByEmail(@Argument String email) {
        return authorService.getAuthorByEmail(email);
    }

    // Mutations for Books
    @MutationMapping
    public Book createBook(
            @Argument String title,
            @Argument String isbn,
            @Argument Integer publishedYear,
            @Argument Long authorId) {
        return bookService.createBook(title, isbn, publishedYear, authorId);
    }

    @MutationMapping
    public Book updateBook(
            @Argument Long id,
            @Argument String title,
            @Argument String isbn,
            @Argument Integer publishedYear) {
        return bookService.updateBook(id, title, isbn, publishedYear);
    }

    @MutationMapping
    public Boolean deleteBook(@Argument Long id) {
        return bookService.deleteBook(id);
    }

    // Mutations for Authors
    @MutationMapping
    public Author createAuthor(@Argument String name, @Argument String email) {
        return authorService.createAuthor(name, email);
    }

    @MutationMapping
    public Author updateAuthor(
            @Argument Long id,
            @Argument String name,
            @Argument String email) {
        return authorService.updateAuthor(id, name, email);
    }

    @MutationMapping
    public Boolean deleteAuthor(@Argument Long id) {
        return authorService.deleteAuthor(id);
    }

    // Field resolvers for relationships
    @SchemaMapping
    public List<Book> books(Author author) {
        return author.getBooks();
    }

    @SchemaMapping
    public Author author(Book book) {
        return book.getAuthor();
    }
}
```

## Назначение класса

`BookController` в GraphQL терминологии называется **Data Fetcher** или **Resolver**. Его основная задача - обрабатывать входящие GraphQL запросы и возвращать данные согласно схеме, определенной в `schema.graphqls`.

В отличие от REST контроллеров, где каждый метод соответствует конкретному URL эндпоинту, здесь методы соответствуют полям в GraphQL схеме.

## Аннотации класса и их значение

### 1. **`@Controller`**
```java
@Controller
public class BookController {
```
- **Стандартная Spring аннотация** из модуля Spring MVC
- Указывает, что класс является Spring компонентом (bean)
- Позволяет Spring автоматически обнаружить класс при сканировании компонентов
- В контексте GraphQL: регистрирует все методы с `@QueryMapping`, `@MutationMapping`, `@SchemaMapping` как резолверы

### 2. **`@RequiredArgsConstructor`**
```java
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final AuthorService authorService;
```
- **Lombok аннотация** для генерации конструктора
- Создает конструктор для всех `final` полей
- Позволяет использовать внедрение зависимостей через конструктор (рекомендуемый способ)

## Аннотации методов

### **`@QueryMapping`** - для чтения данных

```java
@QueryMapping
public Book bookById(@Argument Long id) {
    return bookService.getBookById(id);
}
```

- Соответствует **Query** полям в GraphQL схеме
- Аналог GET запросов в REST
- Не изменяет состояние данных
- В схеме определено как:
```graphql
type Query {
    bookById(id: ID!): Book
}
```

**Как работает:**
1. Spring GraphQL анализирует входящий запрос
2. Находит метод с именем, совпадающим с полем Query (или можно указать явно через `name` параметр)
3. Вызывает метод, передавая аргументы из запроса
4. Возвращает результат

### **`@MutationMapping`** - для изменения данных

```java
@MutationMapping
public Book createBook(
        @Argument String title,
        @Argument String isbn,
        @Argument Integer publishedYear,
        @Argument Long authorId) {
    return bookService.createBook(title, isbn, publishedYear, authorId);
}
```

- Соответствует **Mutation** полям в схеме
- Аналог POST/PUT/DELETE в REST
- Изменяет состояние данных
- В схеме определено как:
```graphql
type Mutation {
    createBook(title: String!, isbn: String, publishedYear: Int, authorId: ID!): Book!
}
```

### **`@SchemaMapping`** - для связей между типами

```java
@SchemaMapping
public List<Book> books(Author author) {
    return author.getBooks();
}
```

- Связывает поля сложных типов
- Определяет, как получить данные для поля, которое не хранится напрямую
- В схеме определено как поле в типе:
```graphql
type Author {
    books: [Book!]!  # Это поле нужно заполнить через резолвер
}
```

**Особенности:**
- Первый параметр - родительский объект
- Имя метода должно совпадать с именем поля
- Можно явно указать `typeName` и `field` если имена отличаются:
```java
@SchemaMapping(typeName="Author", field="books")
public List<Book> getAuthorBooks(Author author) {
    return author.getBooks();
}
```

### **`@Argument`** - для получения параметров

```java
public Book bookById(@Argument Long id) {
```

- Извлекает аргументы из GraphQL запроса
- Имя параметра должно совпадать с именем аргумента в схеме
- Можно указать явно: `@Argument(name = "bookId") Long id`

## Как всё это работает вместе

### Процесс обработки запроса:

1. **Клиент отправляет GraphQL запрос:**
```graphql
query {
  bookById(id: 1) {
    title
    author {
      name
      books {
        title
      }
    }
  }
}
```

2. **Spring GraphQL парсит запрос** и определяет структуру

3. **Вызывается метод `bookById`:**
```java
@QueryMapping
public Book bookById(@Argument Long id)  // id = 1
```

4. **Полученный объект Book содержит author, но author.books еще не загружен**

5. **Spring видит, что нужно поле `books` у Author** и вызывает соответствующий резолвер:
```java
@SchemaMapping
public List<Book> books(Author author)  // author - объект из шага 4
```

6. **Результаты собираются в единый ответ**

### Почему это эффективно?

1. **N+1 проблема** - Spring GraphQL может оптимизировать запросы, группируя вызовы резолверов
2. **Ленивая загрузка** - данные загружаются только если они запрошены клиентом
3. **Гранулярный контроль** - каждый резолвер отвечает только за свое поле

## Сравнение с REST контроллером

```java
// REST подход - множество эндпоинтов
@RestController
public class BookRestController {
    @GetMapping("/books/{id}")
    public Book getBook(@PathVariable Long id) { ... }
    
    @GetMapping("/books")
    public List<Book> getAllBooks() { ... }
    
    @GetMapping("/books/search")
    public List<Book> searchBooks(@RequestParam String title) { ... }
    
    @PostMapping("/books")
    public Book createBook(@RequestBody BookRequest request) { ... }
}

// GraphQL подход - один контроллер с множеством резолверов
@Controller
public class BookController {
    @QueryMapping public Book bookById(@Argument Long id) { ... }
    @QueryMapping public List<Book> allBooks() { ... }
    @QueryMapping public List<Book> booksByTitle(@Argument String title) { ... }
    @MutationMapping public Book createBook(@Argument String title, ...) { ... }
}
```

## Best Practices

1. **Разделяй резолверы по типу** - можно создать отдельные классы для BookResolver, AuthorResolver
2. **Используй `@BatchMapping`** для сложных связей (улучшает производительность)
3. **Не клади бизнес-логику в контроллер** - делегируй сервисам
4. **Обрабатывай ошибки** - используй `@GraphQlExceptionHandler`

Вот пример более продвинутой организации:

```java
@Controller
@RequiredArgsConstructor
public class BookQueryResolver {
    private final BookService bookService;
    
    @QueryMapping
    public Book bookById(@Argument Long id) {
        return bookService.getBookById(id);
    }
}

@Controller
@RequiredArgsConstructor
public class BookMutationResolver {
    private final BookService bookService;
    
    @MutationMapping
    public Book createBook(@Argument CreateBookInput input) {
        return bookService.createBook(input);
    }
}

@Controller
@RequiredArgsConstructor
public class BookFieldResolver {
    
    @BatchMapping
    public Map<Book, Author> author(List<Book> books) {
        // Эффективно загружает авторов для множества книг одним запросом
        return bookService.getAuthorsForBooks(books);
    }
}
```

Таким образом, `BookController` (или резолверы) является мостом между GraphQL миром запросов и твоим Java приложением с бизнес-логикой и базой данных.

---

Отличные вопросы! Давай разберу каждый подробно.

## 1. Как GraphQL находит методы в контроллерах?

Spring GraphQL использует несколько стратегий для поиска методов-резолверов:

### **Стратегия 1: Сканирование всех `@Controller` бинов**

Spring сканирует все классы, помеченные `@Controller` (или `@Component`), и собирает все методы с аннотациями `@QueryMapping`, `@MutationMapping`, `@SchemaMapping` в единый реестр.

```java
@Controller
public class BookController {
    @QueryMapping  // Этот метод будет найден
    public Book bookById(@Argument Long id) { ... }
}

@Controller
public class AuthorController {
    @QueryMapping  // И этот метод тоже будет найден
    public Author authorById(@Argument Long id) { ... }
}
```

### **Стратегия 2: Сопоставление по имени метода**

По умолчанию имя метода должно совпадать с именем поля в схеме:

```java
// schema.graphqls
type Query {
    bookById(id: ID!): Book  // поле называется bookById
}

@Controller
public class BookController {
    @QueryMapping  // Ищет поле с именем "bookById"
    public Book bookById(@Argument Long id) { ... }  // имя совпадает
}
```

### **Стратегия 3: Явное указание имени**

Если имя метода отличается от поля в схеме, можно указать явно:

```java
// schema.graphqls
type Query {
    getBook(id: ID!): Book  // поле называется getBook
}

@Controller
public class BookController {
    @QueryMapping("getBook")  // Явно указываем имя поля
    public Book findBookById(@Argument Long id) { ... }  // имя метода другое
}
```

### **Стратегия 4: Разделение по типам с `@SchemaMapping`**

Для полей сложных типов Spring ищет резолверы по комбинации `typeName` и `field`:

```java
@Controller
public class BookController {
    // Для поля author у типа Book
    @SchemaMapping(typeName = "Book", field = "author")
    public Author resolveAuthor(Book book) { ... }
    
    // Упрощенная запись - Spring определит по параметрам
    @SchemaMapping
    public Author author(Book book) { ... }  // typeName=Book, field=author
}
```

### **Как это работает внутри:**

```java
// Упрощенная схема работы реестра резолверов
public class ResolverRegistry {
    private Map<String, DataFetcher> queryResolvers = new HashMap<>();
    private Map<String, DataFetcher> mutationResolvers = new HashMap<>();
    private Map<TypeFieldKey, DataFetcher> fieldResolvers = new HashMap<>();
    
    public void registerQuery(String fieldName, DataFetcher fetcher) {
        queryResolvers.put(fieldName, fetcher);
    }
    
    public DataFetcher getQueryResolver(String fieldName) {
        return queryResolvers.get(fieldName);
    }
}
```

## 2. Обработка ошибок с `@GraphQlExceptionHandler`

Это элегантный способ централизованной обработки исключений в GraphQL. Давай создадим полноценный пример.

### **Базовый обработчик ошибок**

```java
package com.example.graphqldemo.exception;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class GraphQLExceptionHandler {

    // Обработка кастомного исключения "ResourceNotFoundException"
    @GraphQlExceptionHandler
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
    @GraphQlExceptionHandler
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
    @GraphQlExceptionHandler
    public GraphQLError handleAccessDeniedException(AccessDeniedException ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError()
                .message("Access denied: " + ex.getMessage())
                .errorType(ErrorType.FORBIDDEN)
                .path(env.getExecutionStepInfo().getPath())
                .extensions(Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "requiredRole", ex.getRequiredRole()
                ))
                .build();
    }

    // Обработка всех остальных исключений
    @GraphQlExceptionHandler
    public GraphQLError handleGenericException(Exception ex, DataFetchingEnvironment env) {
        return GraphqlErrorBuilder.newError()
                .message("Internal server error: " + ex.getMessage())
                .errorType(ErrorType.INTERNAL_ERROR)
                .path(env.getExecutionStepInfo().getPath())
                .extensions(Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "exceptionType", ex.getClass().getSimpleName()
                ))
                .build();
    }
}
```

### **Кастомные исключения**

```java
package com.example.graphqldemo.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String resourceId;
    private final String resourceType;
    
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s not found with id: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public ResourceNotFoundException(String message, String resourceType, String resourceId) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
}
```

```java
package com.example.graphqldemo.exception;

import lombok.Getter;
import java.util.List;
import java.util.Map;

@Getter
public class ValidationException extends RuntimeException {
    private final List<String> errors;
    private final Map<String, String> invalidFields;
    
    public ValidationException(List<String> errors, Map<String, String> invalidFields) {
        super("Validation failed");
        this.errors = errors;
        this.invalidFields = invalidFields;
    }
    
    public ValidationException(String message, List<String> errors, Map<String, String> invalidFields) {
        super(message);
        this.errors = errors;
        this.invalidFields = invalidFields;
    }
}
```

### **Использование в сервисах**

```java
package com.example.graphqldemo.service;

import com.example.graphqldemo.exception.ResourceNotFoundException;
import com.example.graphqldemo.exception.ValidationException;
import com.example.graphqldemo.model.Book;
import com.example.graphqldemo.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Book", 
                    String.valueOf(id)
                ));
    }
    
    public Book createBook(String title, String isbn, Integer publishedYear, Long authorId) {
        // Валидация
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException(
                List.of("Title is required"),
                Map.of("title", "must not be empty")
            );
        }
        
        if (isbn != null && !isbn.matches("\\d{3}-\\d-\\d{5}-\\d{3}-\\d")) {
            throw new ValidationException(
                List.of("Invalid ISBN format"),
                Map.of("isbn", "must match pattern: 978-5-389-00001-2")
            );
        }
        
        // Бизнес-логика
        Book book = new Book();
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setPublishedYear(publishedYear);
        
        return bookRepository.save(book);
    }
}
```

### **Более продвинутый обработчик с интерфейсом**

Можно создать отдельные классы для каждого типа ошибок:

```java
package com.example.graphqldemo.exception.handler;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.ErrorType;

import java.time.LocalDateTime;
import java.util.Map;

public interface GraphQLErrorHandler<T extends Exception> {
    
    GraphQLError handle(T exception, DataFetchingEnvironment env);
    
    default GraphQLError buildError(String message, ErrorType errorType, 
                                    DataFetchingEnvironment env, Map<String, Object> extensions) {
        var builder = GraphqlErrorBuilder.newError()
                .message(message)
                .errorType(errorType)
                .path(env.getExecutionStepInfo().getPath())
                .location(env.getField().getSourceLocation());
        
        if (extensions != null && !extensions.isEmpty()) {
            builder.extensions(extensions);
        }
        
        return builder.build();
    }
    
    default Map<String, Object> defaultExtensions() {
        return Map.of("timestamp", LocalDateTime.now().toString());
    }
}
```

```java
package com.example.graphqldemo.exception.handler;

import com.example.graphqldemo.exception.ResourceNotFoundException;
import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ResourceNotFoundHandler implements GraphQLErrorHandler<ResourceNotFoundException> {
    
    @Override
    public GraphQLError handle(ResourceNotFoundException ex, DataFetchingEnvironment env) {
        Map<String, Object> extensions = Map.of(
            "timestamp", LocalDateTime.now().toString(),
            "resourceId", ex.getResourceId(),
            "resourceType", ex.getResourceType(),
            "errorCode", "RESOURCE_NOT_FOUND"
        );
        
        return buildError(
            ex.getMessage(),
            ErrorType.NOT_FOUND,
            env,
            extensions
        );
    }
}
```

### **Примеры ответов с ошибками**

**Запрос с несуществующим ID:**
```graphql
{
  bookById(id: 999) {
    title
  }
}
```

**Ответ:**
```json
{
  "errors": [
    {
      "message": "Book not found with id: 999",
      "locations": [
        {
          "line": 2,
          "column": 3
        }
      ],
      "path": ["bookById"],
      "extensions": {
        "errorType": "NOT_FOUND",
        "timestamp": "2024-01-15T10:30:45.123",
        "resourceId": "999",
        "resourceType": "Book"
      }
    }
  ],
  "data": {
    "bookById": null
  }
}
```

**Запрос с невалидными данными:**
```graphql
mutation {
  createBook(
    title: ""
    isbn: "invalid-isbn"
    authorId: 1
  ) {
    id
    title
  }
}
```

**Ответ:**
```json
{
  "errors": [
    {
      "message": "Validation failed",
      "locations": [
        {
          "line": 2,
          "column": 3
        }
      ],
      "path": ["createBook"],
      "extensions": {
        "errorType": "BAD_REQUEST",
        "timestamp": "2024-01-15T10:35:22.456",
        "errors": [
          "Title is required",
          "Invalid ISBN format"
        ],
        "invalidFields": {
          "title": "must not be empty",
          "isbn": "must match pattern: 978-5-389-00001-2"
        }
      }
    }
  ],
  "data": {
    "createBook": null
  }
}
```

### **Типы ошибок в GraphQL**

Spring GraphQL предоставляет стандартные типы ошибок:

```java
public enum ErrorType {
    BAD_REQUEST,           // 400 - Неверный запрос
    UNAUTHORIZED,          // 401 - Не авторизован
    FORBIDDEN,            // 403 - Доступ запрещен
    NOT_FOUND,            // 404 - Не найдено
    INTERNAL_ERROR,       // 500 - Внутренняя ошибка
    BAD_RESPONSE          // Ошибка при формировании ответа
}
```

### **Глобальная обработка с фильтрацией**

Можно создавать разные обработчики для разных типов исключений:

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    // Обработка конкретного исключения
    @GraphQlExceptionHandler(ResourceNotFoundException.class)
    public GraphQLError handleNotFound(ResourceNotFoundException ex, DataFetchingEnvironment env) {
        return createError(ex.getMessage(), ErrorType.NOT_FOUND, env);
    }
    
    // Обработка нескольких типов исключений
    @GraphQlExceptionHandler({ConstraintViolationException.class, ValidationException.class})
    public GraphQLError handleValidation(Exception ex, DataFetchingEnvironment env) {
        return createError("Validation error: " + ex.getMessage(), ErrorType.BAD_REQUEST, env);
    }
    
    // Обработка с дополнительной логикой
    @GraphQlExceptionHandler
    public GraphQLError handleCustomException(CustomBusinessException ex, DataFetchingEnvironment env) {
        var extensions = Map.of(
            "errorCode", ex.getErrorCode(),
            "retryable", ex.isRetryable(),
            "timestamp", LocalDateTime.now().toString()
        );
        
        return GraphqlErrorBuilder.newError()
                .message(ex.getMessage())
                .errorType(ErrorType.INTERNAL_ERROR)
                .path(env.getExecutionStepInfo().getPath())
                .extensions(extensions)
                .build();
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
```

### **Преимущества `@GraphQlExceptionHandler`**

1. **Централизация** - вся обработка ошибок в одном месте
2. **Типизация** - разные обработчики для разных исключений
3. **Контекст** - доступ к DataFetchingEnvironment
4. **Расширяемость** - можно добавлять кастомные поля в extensions
5. **Чистота кода** - сервисы не засоряются обработкой ошибок

Такой подход делает GraphQL API более профессиональным и удобным для клиентов, так как они получают структурированную информацию об ошибках, а не просто текстовое сообщение.

---


