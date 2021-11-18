create extension if not exists "uuid-ossp";
create table if not exists users(
    id uuid primary key default public.uuid_generate_v4(),
    username varchar(256) unique not null,
    password varchar(256) not null
);