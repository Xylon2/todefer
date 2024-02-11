ALTER TABLE habit
ALTER COLUMN highlight TYPE date USING null;

--;;

ALTER TABLE task
ALTER COLUMN highlight TYPE date USING null;
