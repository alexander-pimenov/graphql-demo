`schema.graphqls` - это сердце GraphQL приложения. 
Давай разберем её подробно, от основ до продвинутых возможностей.

## Что такое schema.graphqls?

Это файл, который определяет **контракт** твоего API - все возможные запросы, типы данных и их взаимосвязи. GraphQL использует **Schema Definition Language (SDL)** для описания API.

## Полный разбор schema.graphqls

```graphql
# schema.graphqls

# ⚠️ ВАЖНО: Порядок определения типов не имеет значения!
# GraphQL собирает все определения в единую схему

# ---------- ОСНОВНЫЕ ТИПЫ ДАННЫХ ----------

type Author {
    id: ID!                    # ID! - обязательное поле (non-null)
    name: String!              # String! - обязательное поле
    email: String!              # Email тоже обязательный
    books: [Book!]!            # Список книг: сам список обязателен (!)
                               # и каждый элемент списка тоже обязателен (!)
}

type Book {
    id: ID!                    # ID - специальный тип для идентификаторов
    title: String!             # Обычные строки
    isbn: String               # Nullable поле (может быть null)
    publishedYear: Int         # Int может быть null
    author: Author!            # Связь с автором (обязательна)
}

# ---------- ТОЧКИ ВХОДА В API ----------

# Query - все операции чтения (аналог GET в REST)
type Query {
    # Book queries
    bookById(id: ID!): Book     # Может вернуть Book или null
    booksByTitle(title: String!): [Book!]!  # Всегда возвращает список (может быть пустым)
    allBooks: [Book!]!          # Все книги
    
    # Author queries
    authorById(id: ID!): Author
    allAuthors: [Author!]!
    authorByEmail(email: String!): Author
}

# Mutation - все операции изменения (аналог POST/PUT/DELETE)
type Mutation {
    # Book mutations
    createBook(
        title: String!,         # Обязательный параметр
        isbn: String,           # Необязательный параметр
        publishedYear: Int,      # Необязательный параметр
        authorId: ID!           # Обязательный ID автора
    ): Book!                    # Всегда возвращает созданную книгу
    
    updateBook(
        id: ID!,
        title: String,
        isbn: String,
        publishedYear: Int
    ): Book!                     # Всегда возвращает обновленную книгу
    
    deleteBook(id: ID!): Boolean!  # Возвращает true/false
    
    # Author mutations
    createAuthor(name: String!, email: String!): Author!
    updateAuthor(id: ID!, name: String, email: String): Author!
    deleteAuthor(id: ID!): Boolean!
}

# ---------- ДОПОЛНИТЕЛЬНЫЕ ВОЗМОЖНОСТИ ----------

# Input Types - для сложных входных параметров
input BookInput {
    title: String!
    isbn: String
    publishedYear: Int
    authorId: ID!
}

input AuthorInput {
    name: String!
    email: String!
}

# Enum - перечисления
enum BookGenre {
    FICTION
    NON_FICTION
    SCIENCE
    HISTORY
    BIOGRAPHY
}

# Interface - общие поля для разных типов
interface SearchResult {
    id: ID!
    title: String!
}

type Movie implements SearchResult {
    id: ID!
    title: String!
    director: String!
    duration: Int
}

# Union - объединение разных типов
union Media = Book | Movie

# Query с использованием новых типов
extend type Query {
    search(term: String!): [SearchResult!]!
    mediaById(id: ID!): Media
    booksByGenre(genre: BookGenre!): [Book!]!
}

# Subscription - для real-time обновлений (websocket)
type Subscription {
    bookAdded: Book!
    bookUpdated: Book!
}
```

## Ключевые концепции синтаксиса

### 1. **Типы полей и nullability**

```graphql
type Product {
    id: ID!                    # Поле обязательно и не может быть null
    name: String!              # Поле обязательно
    description: String        # Поле может быть null
    price: Float!              # Обязательное число с плавающей точкой
    tags: [String!]!           # Обязательный список, каждый элемент обязателен
    metadata: [String]         # Список может быть null, элементы могут быть null
    relatedProducts: [Product] # Список может быть null, элементы могут быть null
}
```

**Правила:**
- `!` после типа - поле **всегда** должно иметь значение
- `[Type]!` - сам список обязателен, но элементы могут быть null
- `[Type!]` - список может быть null, но элементы в нем обязательны
- `[Type!]!` - и список, и все элементы обязательны

### 2. **Скалярные типы (базовые)**

```graphql
# Встроенные скаляры
type Example {
    id: ID!           # Уникальный идентификатор (сериализуется как String)
    name: String!     # Строка UTF-8
    age: Int          # Целое число (32 бита)
    price: Float      # Число с плавающей точкой
    isActive: Boolean # true/false
    createdAt: AWSDateTime  # Кастомный скаляр (требует реализации)
}

# Кастомные скаляры (нужно реализовать самим)
scalar Date
scalar Email
scalar PhoneNumber
```

### 3. **Аргументы полей**

```graphql
type Query {
    # Простые аргументы
    booksByTitle(title: String!): [Book!]!
    
    # Несколько аргументов
    booksByFilters(
        title: String,
        authorId: ID,
        yearFrom: Int,
        yearTo: Int,
        limit: Int = 10,        # Значение по умолчанию
        offset: Int = 0
    ): [Book!]!
    
    # Аргументы со сложным типом
    booksByInput(input: BookFiltersInput!): [Book!]!
}

input BookFiltersInput {
    title: String
    authorId: ID
    yearFrom: Int
    yearTo: Int
    limit: Int = 10
    offset: Int = 0
}
```

## Как схема "оживает" в коде

### 1. **Сопоставление типов с Java классами**

```graphql
type Author {
    id: ID!
    name: String!
    email: String!
    books: [Book!]!
}
```

```java
// Java класс должен иметь поля с соответствующими типами
@Entity
public class Author {
    private Long id;           // ID! -> Long/Integer
    private String name;       // String! -> String
    private String email;      // String! -> String
    private List<Book> books;  // [Book!]! -> List<Book>
    
    // Геттеры/сеттеры...
}
```

### 2. **Сопоставление Query полей с методами**

```graphql
type Query {
    bookById(id: ID!): Book
}
```

```java
@Controller
public class BookController {
    @QueryMapping
    public Book bookById(@Argument Long id) {  // ID! -> Long
        return bookService.getBookById(id);     // Book -> Book
    }
}
```

### 3. **Сопоставление Mutation полей**

```graphql
type Mutation {
    createBook(title: String!, isbn: String, publishedYear: Int, authorId: ID!): Book!
}
```

```java
@Controller
public class BookController {
    @MutationMapping
    public Book createBook(
            @Argument String title,      // String!
            @Argument String isbn,        // String (nullable)
            @Argument Integer publishedYear, // Int (nullable)
            @Argument Long authorId       // ID!
    ) {
        return bookService.createBook(title, isbn, publishedYear, authorId);
    }
}
```

## Продвинутые возможности

### 1. **Интерфейсы**

```graphql
interface Node {
    id: ID!
    createdAt: String!
}

interface Commentable {
    comments: [Comment!]!
}

type Post implements Node & Commentable {
    id: ID!
    createdAt: String!
    title: String!
    content: String!
    comments: [Comment!]!
}

type Comment implements Node {
    id: ID!
    createdAt: String!
    text: String!
    author: Author!
}
```

### 2. **Union типы**

```graphql
union SearchResult = Book | Author | Post

type Query {
    search(term: String!): [SearchResult!]!
}
```

```java
// В Java нужно использовать специальную аннотацию
@GraphQLUnion
public class SearchResult {
    // Может содержать Book, Author или Post
}
```

### 3. **Директивы**

```graphql
# Встроенные директивы
type User {
    id: ID!
    name: String!
    email: String! @deprecated(reason: "Use 'contact.email' instead")
    password: String! @include(if: $includePassword)  # Условное включение
    secretData: String! @skip(if: $skipSecret)        # Условное исключение
}

# Кастомные директивы
directive @auth(role: String!) on FIELD_DEFINITION
directive @formatDate(format: String = "YYYY-MM-DD") on FIELD_DEFINITION

type Query {
    adminData: String! @auth(role: "ADMIN")
    userBirthday: String! @formatDate(format: "DD.MM.YYYY")
}
```

## На что обращать внимание при проектировании

### 1. **Принципы дизайна**

```graphql
# ❌ ПЛОХО: Избыточность, негибкость
type Query {
    getBookById(id: ID!): Book
    getBookByTitle(title: String!): Book
    getBookByIsbn(isbn: String!): Book
    getBookByAuthor(authorId: ID!): [Book!]!
}

# ✅ ХОРОШО: Единый гибкий интерфейс
type Query {
    books(filter: BookFilter, sort: BookSort, page: PageInput): BookConnection!
}

input BookFilter {
    id: ID
    title: String
    isbn: String
    authorId: ID
    publishedYear: IntRange
}

input IntRange {
    from: Int
    to: Int
}
```

### 2. **Пагинация (Connection pattern)**

```graphql
# Стандарт Relay пагинации
type BookConnection {
    edges: [BookEdge!]!
    pageInfo: PageInfo!
    totalCount: Int!
}

type BookEdge {
    node: Book!
    cursor: String!
}

type PageInfo {
    hasNextPage: Boolean!
    hasPreviousPage: Boolean!
    startCursor: String
    endCursor: String
}

type Query {
    books(first: Int, after: String, last: Int, before: String): BookConnection!
}
```

### 3. **Версионирование (не нужно!)**

```graphql
# GraphQL не требует версионирования - просто добавляем поля
type User {
    id: ID!
    name: String!      # Старое поле
    fullName: String   # Новое поле (nullable)
    email: String!
    
    # Можно помечать устаревшие поля
    oldField: String @deprecated(reason: "Use newField instead")
    newField: String!
}
```

### 4. **Документирование**

```graphql
"""
Represents a book in the library system
"""
type Book {
    """
    The unique identifier of the book
    """
    id: ID!
    
    """
    The title of the book (must be unique within author)
    """
    title: String!
    
    """
    International Standard Book Number
    Format: 978-5-389-00001-2
    """
    isbn: String
}
```

## Проверка схемы

### 1. **Валидация через код**

```java
@Component
public class SchemaValidator {
    
    @EventListener(ApplicationReadyEvent.class)
    public void validateSchema() {
        // Spring GraphQL автоматически валидирует схему при старте
        System.out.println("Schema is valid!");
    }
}
```

### 2. **Тестирование схемы**

```java
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
```

## Интроспекция (самодокументирование)

Одно из главных преимуществ GraphQL - автоматическая документация. Ты можешь узнать всё о схеме через специальные запросы:

```graphql
# Узнать все типы
{
  __schema {
    types {
      name
      kind
      description
    }
  }
}

# Узнать поля конкретного типа
{
  __type(name: "Book") {
    name
    fields {
      name
      type {
        name
        kind
      }
      isDeprecated
      deprecationReason
    }
  }
}
```

## Практические советы

### 1. **Организация файлов для больших проектов**

```
src/main/resources/graphql/
├── schema.graphqls           # Главный файл (импортирует остальные)
├── types/
│   ├── book.graphqls
│   ├── author.graphqls
│   └── common.graphqls
├── queries/
│   ├── book-queries.graphqls
│   └── author-queries.graphqls
├── mutations/
│   ├── book-mutations.graphqls
│   └── author-mutations.graphqls
└── inputs/
    └── filters.graphqls
```

### 2. **Именование**

```graphql
# Типы - существительные в единственном числе
type Book {}
type Author {}

# Поля - camelCase
type Book {
    publishedYear: Int!
}

# Query поля - глаголы или существительные
type Query {
    books(): [Book!]!           # Множественное число для списков
    book(id: ID!): Book         # Единственное число для одного объекта
}

# Input типы - с суффиксом Input
input BookInput {}
input AuthorFilterInput {}
```

### 3. **Безопасность**

```graphql
# Ограничение глубины запросов
type Query {
    # Потенциально опасный запрос (может быть очень глубоким)
    user: User!
}

# Лучше ограничивать
type User {
    friends(limit: Int = 10): [User!]!
}
```

Схема GraphQL - это живой документ, который растет вместе с приложением. Главное помнить: обратная совместимость обеспечивается добавлением новых полей, а не удалением старых (можно помечать `@deprecated`).

---

