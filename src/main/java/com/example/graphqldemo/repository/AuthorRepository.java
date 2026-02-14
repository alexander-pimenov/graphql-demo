package com.example.graphqldemo.repository;


import com.example.graphqldemo.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Для загрузки авторов с их книгами (если нужно)
     */
    @Query("SELECT a FROM Author a LEFT JOIN FETCH a.books WHERE a.id IN :authorIds")
    List<Author> findAllWithBooksByIds(@Param("authorIds") Set<Long> authorIds);
}
