alter table task
drop column defcat_dated

--;;

alter table task
rename column defcat_named to defcat_ref 

--;;

drop table defCatDated

--;;

alter table defCatNamed
add column def_date date

--;;

alter table defCatNamed
rename to deferredCategory
