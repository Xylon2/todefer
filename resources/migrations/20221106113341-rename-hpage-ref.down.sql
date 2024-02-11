ALTER TABLE habit
RENAME COLUMN page_ref TO hpage_ref

--;;

ALTER TABLE habit
RENAME COLUMN freq_unit TO unit

--;;

ALTER TABLE habit
RENAME COLUMN freq_value TO frequency

--;;

ALTER TABLE habit
ALTER COLUMN date_scheduled
DROP DEFAULT
