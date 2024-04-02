alter table task
rename column sort_id to order_key_task;

--;;

alter table habit
rename column sort_id to order_key_habit;

--;;

alter table task
add order_key_todo integer;

--;;

alter table habit
add order_key_todo integer;
