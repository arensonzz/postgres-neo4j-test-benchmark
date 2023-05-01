CREATE TABLE IF NOT EXISTS firstnames (
    id serial,
    firstname varchar(100) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS surnames (
    id serial,
    surname varchar(100) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS addresses (
    id serial,
    street varchar(200) NOT NULL,
    city varchar(100) NOT NULL,
    district varchar(100) NOT NULL,
    region varchar(50) NOT NULL,
    postcode varchar(50) NOT NULL,
    PRIMARY KEY (id)
);

