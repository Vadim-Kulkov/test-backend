create table author
(
    id               serial primary key,
    name             text      not null,
    creationDatetime timestamp not null
);