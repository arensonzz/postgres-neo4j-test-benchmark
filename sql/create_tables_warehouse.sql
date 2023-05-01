CREATE TABLE IF NOT EXISTS customer (
    id bigint NOT NULL,
    name varchar(50) NOT NULL CHECK (name <> ''),
    address varchar(150) NOT NULL CHECK (address <> ''),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS item (
    id bigint NOT NULL,
    name varchar(100) NOT NULL CHECK (name <> ''),
    balance int NOT NULL,
    unit varchar(10) NOT NULL CHECK (unit <> ''),
    purchaseprice decimal(65, 10) NOT NULL,
    vat decimal(65, 2) NOT NULL,
    removed smallint NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS worktype (
    id bigint NOT NULL,
    name varchar(20) NOT NULL CHECK (name <> ''),
    price bigint NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS invoice (
    id bigint NOT NULL,
    customerId bigint NOT NULL,
    state int NOT NULL,
    duedate date DEFAULT NULL,
    previousinvoice bigint NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT customer_ibfk_1 FOREIGN KEY (customerId) REFERENCES customer (id)
);

CREATE TABLE IF NOT EXISTS target (
    id bigint NOT NULL,
    name varchar(100) NOT NULL CHECK (name <> ''),
    address varchar(100) NOT NULL CHECK (address <> ''),
    customerid bigint NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT target_ibfk_1 FOREIGN KEY (customerid) REFERENCES customer (id)
);

CREATE TABLE IF NOT EXISTS WORK (
    id bigint NOT NULL,
    name varchar(100) NOT NULL CHECK (name <> ''),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS workinvoice (
    workId bigint NOT NULL,
    invoiceId bigint NOT NULL,
    PRIMARY KEY (workId, invoiceId),
    CONSTRAINT workinvoice_ibfk_1 FOREIGN KEY (workId) REFERENCES WORK (id),
    CONSTRAINT workinvoice_ibfk_2 FOREIGN KEY (invoiceId) REFERENCES invoice (id)
);

CREATE TABLE IF NOT EXISTS worktarget (
    workId bigint NOT NULL,
    targetId bigint NOT NULL,
    PRIMARY KEY (workId, targetId),
    CONSTRAINT worktarget_ibfk_1 FOREIGN KEY (workId) REFERENCES WORK (id),
    CONSTRAINT worktarget_ibfk_2 FOREIGN KEY (targetId) REFERENCES target (id)
);

CREATE TABLE IF NOT EXISTS useditem (
    amount int DEFAULT NULL CHECK (amount > 0),
    discount decimal(65, 2) DEFAULT NULL,
    workId bigint NOT NULL,
    itemId bigint NOT NULL,
    PRIMARY KEY (workId, itemId),
    CONSTRAINT useditem_ibfk_1 FOREIGN KEY (workId) REFERENCES WORK (id),
    CONSTRAINT useditem_ibfk_2 FOREIGN KEY (itemId) REFERENCES item (id)
);

CREATE TABLE IF NOT EXISTS workhours (
    worktypeId bigint NOT NULL,
    hours int NOT NULL,
    discount decimal(65, 2) DEFAULT NULL,
    workId bigint NOT NULL,
    PRIMARY KEY (workId, worktypeId),
    CONSTRAINT workhours_ibfk_1 FOREIGN KEY (workId) REFERENCES WORK (id),
    CONSTRAINT workhours_ibfk_2 FOREIGN KEY (worktypeId) REFERENCES worktype (id)
);

