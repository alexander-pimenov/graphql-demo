package com.example.graphqldemo;


import com.example.graphqldemo.entity.Author;
import com.example.graphqldemo.entity.Book;
import com.example.graphqldemo.repository.AuthorRepository;
import com.example.graphqldemo.repository.BookRepository;
import com.example.graphqldemo.service.BookService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тестирование {@link BookService} без поднятия контекста Spring, только моки.
 * Пример проверки методов BookService.
 */
@Slf4j
public class BookServiceTest {

    private BookService bookService;

    private final BookRepository bookRepository = mock(BookRepository.class); // мок для репозитория
    private final AuthorRepository authorRepository = mock(AuthorRepository.class); // мок для репозитория

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository, authorRepository);
    }


    @Test
    public void demonstrateBatchLoading() {
        // Получаем список книг (например, первые 10)
        List<Book> books = new ArrayList<>();
        List<Author> authors = new ArrayList<>();

        //мокируем возврат 10 книг и 10 авторов
        for (int i = 0; i < 10; i++) {
            Author author = new Author(1L + i, "Author " + i, "email@" + i + ".co", List.of());
            authors.add(author);
            books.add(new Book(1L + i, "Title " + i, "ISBN-" + i, (i + 11 * 3), author));

        }

        when(bookRepository.findAll()).thenReturn(books);
        when(authorRepository.findAllById(any())).thenReturn(authors);

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
