ALTER TABLE habit
ALTER COLUMN highlight TYPE text USING null;

--;;

ALTER TABLE task
ALTER COLUMN highlight TYPE text USING null;
