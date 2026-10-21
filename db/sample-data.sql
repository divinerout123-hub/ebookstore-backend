-- Sample data for the bookstore database (PostgreSQL).
-- Prerequisite: start the app once so Hibernate creates the tables (spring.jpa.hibernate.ddl-auto=update).
-- Run:  psql -h localhost -d bookstore -f db/sample-data.sql
-- Safe to re-run: existing rows (same category name / ISBN / email) are skipped.
-- Prices are in INR. Ratings, stock and descriptions are illustrative sample values.

BEGIN;

-- ---------------------------------------------------------------- categories
INSERT INTO categories (name, description, created_at, updated_at) VALUES
    ('Romance',   'Love stories and relationship-driven fiction',      now(), now()),
    ('Mystery',   'Crime, detective and suspense novels',              now(), now()),
    ('SciFi',     'Science fiction and speculative futures',           now(), now()),
    ('Self-Help', 'Personal growth, habits and productivity',          now(), now()),
    ('Fantasy',   'Magic, myth and imagined worlds',                   now(), now())
ON CONFLICT (name) DO NOTHING;

-- ------------------------------------------------------------------ products
-- category_id is looked up by name so the script does not depend on generated ids.
INSERT INTO products (isbn, title, author, publisher, description, price, stock_quantity,
                      category_id, language, rating, cover_image_url, created_at, updated_at)
SELECT v.isbn, v.title, v.author, v.publisher, v.description, v.price, v.stock_quantity,
       c.id, 'English', v.rating, NULL, now(), now()
FROM (VALUES
    -- Romance
    ('9780141439518', 'Pride and Prejudice',                     'Jane Austen',       'Penguin Classics',
     'Elizabeth Bennet and Mr Darcy in Regency England.',                            299.00, 40, 4.5, 'Romance'),
    ('9780446605236', 'The Notebook',                            'Nicholas Sparks',   'Grand Central Publishing',
     'A love story that spans decades.',                                             399.00, 35, 4.1, 'Romance'),
    -- Mystery
    ('9780062693662', 'Murder on the Orient Express',            'Agatha Christie',   'HarperCollins',
     'Hercule Poirot investigates a murder aboard a snowbound train.',               349.00, 50, 4.2, 'Mystery'),
    ('9780307588371', 'Gone Girl',                               'Gillian Flynn',     'Broadway Books',
     'A wife disappears and her husband becomes the prime suspect.',                 449.00, 30, 4.1, 'Mystery'),
    -- SciFi
    ('9780441013593', 'Dune',                                    'Frank Herbert',     'Ace Books',
     'Politics, religion and ecology on the desert planet Arrakis.',                 599.00, 25, 4.3, 'SciFi'),
    ('9780812550702', 'Ender''s Game',                           'Orson Scott Card',  'Tor Books',
     'A gifted child is trained to command Earth''s fleet.',                         499.00, 28, 4.3, 'SciFi'),
    -- Self-Help
    ('9781847941831', 'Atomic Habits',                           'James Clear',       'Random House Business',
     'Tiny changes that compound into remarkable results.',                          549.00, 60, 4.4, 'Self-Help'),
    ('9780062457714', 'The Subtle Art of Not Giving a F*ck',     'Mark Manson',       'HarperOne',
     'A counterintuitive approach to living a good life.',                           399.00, 45, 3.9, 'Self-Help'),
    -- Fantasy
    ('9780547928227', 'The Hobbit',                              'J.R.R. Tolkien',    'Houghton Mifflin Harcourt',
     'Bilbo Baggins joins a quest to reclaim a dwarf kingdom from a dragon.',        499.00, 38, 4.3, 'Fantasy'),
    ('9781408855652', 'Harry Potter and the Philosopher''s Stone', 'J.K. Rowling',    'Bloomsbury',
     'A boy discovers he is a wizard and starts at Hogwarts.',                       599.00, 55, 4.5, 'Fantasy')
) AS v(isbn, title, author, publisher, description, price, stock_quantity, rating, category_name)
JOIN categories c ON c.name = v.category_name
ON CONFLICT (isbn) DO NOTHING;

-- --------------------------------------------------------------------- user
-- Login: email aarav.sharma@example.com, demo password from the README ("Sample data" section).
-- No hash is stored in this file: it is generated at load time with pgcrypto's bcrypt ($2a$, cost 10),
-- which Spring Security's BCryptPasswordEncoder can verify. Pass a different password with:
--   psql -v demo_pw='your-password' -f db/sample-data.sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
\if :{?demo_pw}
\else
  \set demo_pw 'Password@123'
\endif
INSERT INTO users (first_name, last_name, email, password, phone, role, created_at, updated_at)
VALUES ('Aarav', 'Sharma', 'aarav.sharma@example.com',
        crypt(:'demo_pw', gen_salt('bf', 10)),
        '+919876543210', 'CUSTOMER', now(), now())
ON CONFLICT (email) DO NOTHING;

COMMIT;
