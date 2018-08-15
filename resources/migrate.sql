-- :name create-migrations-table :!
create table if not exists "migrations" (
  "id" integer primary key autoincrement not null,
  "name" char(128) not null
);

-- :name count-saved-migrations :1
select count(1) as count from migrations;

-- :name record-migration :!
insert into migrations (name) values (:name);