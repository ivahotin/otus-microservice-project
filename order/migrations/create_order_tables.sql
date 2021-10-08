create extension if not exists "uuid-ossp";
create table if not exists orders (
    id uuid primary key,
    consumer_id uuid not null,
    price integer not null,
    version integer not null,
    status varchar(20) not null,
    delivery_id text,
    reservation_id integer,
    payment_id integer
);

create unique index concurrently if not exists owner_id_version_idx on orders (consumer_id, version);

create table if not exists consumer_order_outbox_event (
    order_id uuid primary key,
    payload  jsonb not null
);