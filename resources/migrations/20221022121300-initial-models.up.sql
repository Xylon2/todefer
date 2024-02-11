create type pageType as enum ('task', 'habit');

--;;

create type timeUnit as enum ('days', 'weeks', 'months', 'years');

--;;

create table appPage (
    page_id serial not null primary key,
    page_name text not null,
    order_key serial,
    page_type pageType not null
)

--;;

create table deferredCategory (
    cat_id serial not null primary key,
    cat_name text,
    def_date timestamp,
    order_key serial
)

--;;

create table task (
    task_id serial not null primary key,
    task_name text not null,
    page_ref integer references appPage (page_id),
    defcat_ref integer references deferredCategory (cat_id)
)

--;;

create table habit (
    habit_id serial not null primary key,
    habit_name text not null,
    hpage_ref integer references appPage (page_id),
    unit timeUnit,
    frequency integer,
    date_scheduled timestamp,
    last_done timestamp
)
