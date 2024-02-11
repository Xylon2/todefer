alter table task
drop constraint task_defcat_ref_fkey

--;;

alter table task
drop constraint task_defcat_dated_fkey

--;;

alter table task
add constraint task_defcat_ref_fkey
foreign key (defcat_named)
references defcatnamed(cat_id)
on delete set default

--;;

alter table task
add constraint task_defcat_dated_fkey
foreign key (defcat_dated)
references defcatdated(cat_id)
on delete set default
