package com.example.graphqldemo.service;


import com.example.graphqldemo.entity.Author;
import com.example.graphqldemo.entity.Book;
import com.example.graphqldemo.exception.ResourceNotFoundException;
import com.example.graphqldemo.exception.ValidationException;
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
    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Book",
                        String.valueOf(id)
                ));
    }

    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public List<Book> getBooksByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }

    @Transactional
    public Book createBook(String title, String isbn, Integer publishedYear, Long authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Author not found with id: " + authorId));

        // Валидация
        if (title == null || title.trim().isEmpty()) {
            throw new ValidationException(
                    List.of("Title is required"),
                    Map.of("title", "must not be empty")
            );
        }

        // TODO - доработать валидацию ISBN
        // готовые библиотеки, например:
        //Apache Commons Validator (ISBNValidator)
        //ISBN Tools (Maven: com.github.jonathanlink:isbn)
//        if (isbn != null && !isbn.matches("\\d{3}-\\d-\\d{5}-\\d{3}-\\d")) {
//            throw new ValidationException(
//                    List.of("Invalid ISBN format"),
//                    Map.of("isbn", "must match pattern: 978-5-389-00001-2")
//            );
//        }

        // Бизнес-логика
        Book book = new Book();
        book.setTitle(title);
        book.setIsbn(isbn);
        book.setPublishedYear(publishedYear);
        book.setAuthor(author);

        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(Long id, String title, String isbn, Integer publishedYear) {
        Book book = getBookById(id);

        if (title != null) book.setTitle(title);
        if (isbn != null) book.setIsbn(isbn);
        if (publishedYear != null) book.setPublishedYear(publishedYear);

        return bookRepository.save(book);
    }

    @Transactional
    public boolean deleteBook(Long id) {
        if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id);
            return true;
        }
        return false;
    }

    /**
     * Получить книги по ID автора
     */
    @Transactional(readOnly = true)
    public List<Book> getBooksByAuthorId(Long authorId) {
        log.info("Finding books by author id: {}", authorId);
        return bookRepository.findByAuthorId(authorId);
    }

    // ============== БАТЧЕВАЯ ЗАГРУЗКА ==============

    /**
     * Эффективно загружает авторов для множества книг одним запросом.
     * Это основной метод для @BatchMapping!
     * <p>
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

        log.info("Unique author IDs to load: {}", authorIds);

        // Шаг 2: Загружаем всех авторов ОДНИМ запросом к БД
        List<Author> authors = authorRepository.findAllById(authorIds);
        log.info("Loaded {} authors from database", authors.size());

        // Шаг 3: Создаем Map для быстрого доступа authorId -> Author
        Map<Long, Author> authorById = authors.stream()
                .collect(Collectors.toMap(
                        Author::getId,        // ключ - ID автора
                        Function.identity(),  // значение - сам автор
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
            // Важно: кэшируем автора в книге для будущих вызовов
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
