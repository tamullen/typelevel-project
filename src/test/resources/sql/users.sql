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
     'tamullen',
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
     'amanley',
     'Amber',
     'Manley',
     'tamullen',
     'RECRUITER'
);
