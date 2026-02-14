package com.example.graphqldemo.service;


import com.example.graphqldemo.entity.Author;
import com.example.graphqldemo.repository.AuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthorService {
    private final AuthorRepository authorRepository;

    public Author getAuthorById(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Author not found with id: " + id));
    }

    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    public Author getAuthorByEmail(String email) {
        return authorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Author not found with email: " + email));
    }

    @Transactional
    public Author createAuthor(String name, String email) {
        if (authorRepository.existsByEmail(email)) {
            throw new RuntimeException("Author with email " + email + " already exists");
        }

        Author author = new Author();
        author.setName(name);
        author.setEmail(email);

        return authorRepository.save(author);
    }

    @Transactional
    public Author updateAuthor(Long id, String name, String email) {
        Author author = getAuthorById(id);

        if (name != null) author.setName(name);
        if (email != null) {
            if (!email.equals(author.getEmail()) && authorRepository.existsByEmail(email)) {
                throw new RuntimeException("Author with email " + email + " already exists");
            }
            author.setEmail(email);
        }

        return authorRepository.save(author);
    }

    @Transactional
    public boolean deleteAuthor(Long id) {
        if (authorRepository.existsById(id)) {
            authorRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
