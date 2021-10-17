create extension if not exists "uuid-ossp";
create table if not exists items (
    id          serial primary key,
    title       text not null,
    description text not null,
    quantity    integer not null,
    price       integer not null
);

create index concurrently if not exists title_idx on items (title);

create table if not exists reservations (
    id                  serial primary key,
    consumer_id         uuid not null,
    idempotency_key     uuid not null,
    items               jsonb not null,
    subtotal            integer not null
);

create unique index concurrently if not exists idempotency_key_idx on reservations (idempotency_key);
create index concurrently if not exists consumer_id_idx on reservations (consumer_id);