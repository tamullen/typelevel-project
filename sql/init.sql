CREATE DATABASE board;
\c board;

CREATE TABLE jobs (
            id uuid DEFAULT gen_random_uuid(),
            date bigint NOT NULL,
            ownerEmail text NOT NULL,
            company text NOT NULL,
            title text Not NULL,
            description text NOT NULL,
            externalUrl text NOT NULL,
            remote BOOLEAN NOT NULL DEFAULT false,
            location text,
            salaryLo integer,
            salaryHi integer,
            currency text,
            country text,
            tags text[],
            image text,
            seniority text,
            other text,
            active BOOLEAN NOT NULL DEFAULT false
);

ALTER TABLE jobs
ADD CONSTRAINT pk_jobs PRIMARY KEY (id);

CREATE TABLE users (
             email text NOT NULL,
             hashedPassword text NOT NULL,
             firstName text,
             lastName text,
             company text,
             role text NOT NULL
        );

ALTER TABLE users
ADD CONSTRAINT pk_users PRIMARY KEY (email);

CREATE TABLE recoveryTokens (
    email text NOT NULL,
    token text NOT NULL,
    expiration bigint NOT NULL
);

ALTER TABLE recoveryTokens
ADD CONSTRAINT pk_recoverytokens PRIMARY KEY (email);