create extension if not exists "uuid-ossp";
create table if not exists billing_accounts (
    consumer_id uuid primary key,
    amount integer not null default 0
);

create table if not exists transactions (
    id serial primary key,
    idempotency_key uuid not null,
    created_at timestamp without time zone not null,
    amount integer not null,
    is_cancelled boolean not null default false
);

create unique index concurrently if not exists idempotency_key_idx on transactions using btree (idempotency_key);