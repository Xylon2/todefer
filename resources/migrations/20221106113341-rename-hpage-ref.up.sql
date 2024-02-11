ALTER TABLE habit
RENAME COLUMN hpage_ref TO page_ref

--;;

ALTER TABLE habit
RENAME COLUMN unit TO freq_unit

--;;

ALTER TABLE habit
RENAME COLUMN frequency TO freq_value

--;;

ALTER TABLE habit
ALTER COLUMN date_scheduled
SET DEFAULT CURRENT_DATE
