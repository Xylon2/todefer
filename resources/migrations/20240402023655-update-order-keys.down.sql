alter table task
rename column order_key_task to sort_id;

--;;

alter table habit
rename column order_key_habit to sort_id;

--;;

alter table task
drop column order_key_todo;

--;;

alter table habit
drop column order_key_todo;
