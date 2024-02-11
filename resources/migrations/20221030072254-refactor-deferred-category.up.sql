alter table deferredCategory
rename to defCatNamed

--;;

alter table defCatNamed
drop column def_date

--;;

create table defCatDated (
    cat_id serial not null primary key,
    def_date date,
    order_key serial
)

--;;

alter table task
rename column defcat_ref to defcat_named

--;;

alter table task
add column defcat_dated integer references defCatDated (cat_id)
