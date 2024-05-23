CREATE TABLE recoveryTokens (
    email text NOT NULL,
    token text NOT NULL,
    expiration bigint NOT NULL
);

ALTER TABLE recoveryTokens
ADD CONSTRAINT pk_recoverytokens PRIMARY KEY (email);