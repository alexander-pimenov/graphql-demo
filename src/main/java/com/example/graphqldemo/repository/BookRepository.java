package com.example.graphqldemo.repository;


import com.example.graphqldemo.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByTitleContainingIgnoreCase(String title);

    List<Book> findByAuthorId(Long authorId);

    /**
     * Метод для батчевой загрузки с JOIN FETCH
     */
    @Query("SELECT b FROM Book b JOIN FETCH b.author a WHERE b.id IN :bookIds")
    List<Book> findAllWithAuthorsByIds(@Param("bookIds") Set<Long> bookIds);

    /**
     * Альтернативный метод, если нужно загрузить книги для множества авторов
     */
    @Query("SELECT b FROM Book b WHERE b.author.id IN :authorIds")
    List<Book> findAllByAuthorIds(@Param("authorIds") Set<Long> authorIds);
}