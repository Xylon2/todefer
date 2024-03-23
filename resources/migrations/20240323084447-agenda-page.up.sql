ALTER TABLE task
ADD COLUMN todo date DEFAULT null;

--;;

ALTER TABLE habit
ADD COLUMN todo date DEFAULT null;

--;;

CREATE TABLE pageagenda (
  agenda_id INT REFERENCES apppage(page_id) ON DELETE CASCADE,
  page_id INT REFERENCES apppage(page_id) ON DELETE CASCADE,
  PRIMARY KEY (agenda_id, page_id)  
);

--;;

ALTER TYPE pagetype ADD VALUE 'agenda';
