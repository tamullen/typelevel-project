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

INSERT INTO users (
             email,
             hashedPassword,
             firstName,
             lastName,
             company,
             role
) VALUES (
     'travis@test.com',
     '$2a$10$vQdL/KO4Nf5m26EPOyM9jOeVJwbc3n97QQ7sRmeA/BHe7wJeAwP3W',
     'Travis',
     'Mullen',
     'tamullen',
     'ADMIN'
);

INSERT INTO users (
             email,
             hashedPassword,
             firstName,
             lastName,
             company,
             role
) VALUES (
     'amber@test.com',
     '$2a$10$7wVKg4nQ/ZsWUR4XmBy1VOY2rAX0dojNpony6SkerCpeHe/nXkTQq',
     'Amber',
     'Manley',
     'tamullen',
     'RECRUITER'
);
