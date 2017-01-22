# --- !Ups

CREATE TABLE pages (
    page_id TEXT NOT NULL,
    title TEXT,
    content TEXT,
    PRIMARY KEY (page_id)
);
INSERT INTO pages(page_id, title, content)
  VALUES ('home', 'Home Page', 'Test content');
INSERT INTO pages(page_id, title, content)
  VALUES ('navigation', '', '- [Home](page:home)');

# --- !Downs

DROP TABLE pages;
