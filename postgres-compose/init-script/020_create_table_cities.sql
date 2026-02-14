create table if not exists cities
(
    id         bigserial primary key,
    country_id bigint,
    name       varchar(255)
);