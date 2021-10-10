create extension if not exists "uuid-ossp";
create table if not exists deliveries (
    id serial primary key,
    idempotency_key uuid not null,
    type varchar(20) not null,
    city varchar(60) not null,
    delivery_datetime timestamp not null
);

create unique index concurrently if not exists idempotency_key_idx on deliveries using btree (idempotency_key);