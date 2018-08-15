-- :name m0-wal-pragma :*
pragma journal_mode = WAL;

-- :name m1-foreign-key-pragma :!
pragma foreign_keys = on;

-- :name m3-create-users-table :!
create table if not exists "users" (
  "id" integer primary key autoincrement not null,
  "created_at" integer not null,
  "username" char(128) not null,
  "access_token" char(128) not null
);

-- :name m4-create-unique-username-index :!
create unique index if not exists user_usernames on "users" ("username");

-- :name m5-create-claims-table :!
create table if not exists "claims" (
  "id" integer primary key autoincrement not null,
  "created_at" integer not null,
  "author_id" integer not null,
  "text" char(128) not null,
  "final_up" integer default null,
  "final_down" integer default null,
  foreign key (author_id) references "users" (id)
);

-- :name m6-create-votes-table :!
create table if not exists "votes" (
  "id" integer primary key autoincrement not null,
  "created_at" integer not null,
  "claim_id" integer not null,
  "user_id" integer not null,
  "direction" char(128) not null,
  foreign key (user_id) references "users" (id) on delete cascade
);

-- :name m7-create-vote-claim-user-index :!
create unique index if not exists vote_claim_user on "votes" ("claim_id" asc, "user_id" asc);

-- :name m8-create-scores-table :!
create table if not exists "scores" (
  "id" integer primary key autoincrement not null,
  "user_id" integer not null,
  "latest_scored_claim_id" integer not null default(0),
  "karma" integer not null default(0),
  "streak" integer not null default(0),
  foreign key (user_id) references "users" (id)
);

-- :name m9-create-scores-user-index :!
create unique index if not exists scores_user on "scores" ("user_id");