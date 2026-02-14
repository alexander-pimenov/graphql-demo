package com.example.graphqldemo;


import com.example.graphqldemo.entity.Author;
import com.example.graphqldemo.entity.Book;
import com.example.graphqldemo.repository.BookRepository;
import com.example.graphqldemo.service.BookService;
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
        List<Book> books = bookRepository.findAll().subList(0, (int) Math.min(10, bookRepository.count()));

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
