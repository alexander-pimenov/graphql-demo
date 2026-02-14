-- Убедимся, что расширение для UUID доступно (опционально)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--CREATE TABLE IF NOT EXISTS authors (
--    id BIGSERIAL PRIMARY KEY,
--    name VARCHAR(255) NOT NULL,
--    email VARCHAR(255) UNIQUE
--);

--CREATE TABLE IF NOT EXISTS books (
--    id BIGSERIAL PRIMARY KEY,
--    title VARCHAR(255) NOT NULL,
--    isbn VARCHAR(20) UNIQUE,
--    published_year INTEGER,
--    author_id BIGINT REFERENCES authors(id) ON DELETE CASCADE
--);

-- Создаем таблицы с явным указанием схемы
CREATE TABLE IF NOT EXISTS authors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS books (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    isbn VARCHAR(20) UNIQUE,
    published_year INTEGER,
    author_id BIGINT REFERENCES authors(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для лучшей производительности
CREATE INDEX IF NOT EXISTS idx_books_author_id ON books(author_id);
CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);
CREATE INDEX IF NOT EXISTS idx_authors_email ON authors(email);

-- Insert sample data
INSERT INTO authors (name, email) VALUES
    ('Джоан Роулинг', 'jk.rowling@email.com'),
    ('Джордж Оруэлл', 'george.orwell@email.com'),
    ('Лев Толстой', 'leo.tolstoy@email.com'),
    ('Александр Пушкин', 'alex.pushkin@email.com'),
    ('Михаил Лермонтов', 'mikhail.lermontov@email.com'),
    ('Федор Достоевский', 'fedor.dostoevsky@email.com'),
    ('Александр Грибоедов', 'alex.grig@email.com')
    ON CONFLICT (email) DO NOTHING;

INSERT INTO books (title, isbn, published_year, author_id) VALUES
    ('Гарри Поттер и философский камень', '978-5-389-00001-2', 1997, 1),
    ('1984', '978-5-389-00002-9', 1949, 2),
    ('Скотный двор', '978-5-389-00003-6', 1945, 2),
    ('Анна Каренина', '978-5-389-00004-3', 1877, 3),
    ('Евгений Онегин', '978-5-389-00005-0', 1833, 3),
    ('Капитанская дочка', '978-5-389-00006-7', 1836, 3),
    ('Руслан и Людмила', '978-5-389-00007-4', 1820, 4),
    ('Бородино', '978-5-389-00008-1', 1832, 5),
    ('Братья Карамазовы', '978-5-389-00009-8', 1880, 6),
    ('Герой нашего времени', '978-5-389-00010-4', 1840, 7),
    ('Идиот', '978-5-389-00011-1', 1869, 7);