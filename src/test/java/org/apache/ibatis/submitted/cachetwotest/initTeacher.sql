drop table teachers if exists;

create table teachers
(
    id   int,
    name varchar(20)
);

insert into teachers (id, name)
values (1, '邓俊辉');