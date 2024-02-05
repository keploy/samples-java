delete from author;
delete from book;

insert into author (first_name,last_name)
values
    ('Sophia', 'Anderson'),
    ('Liam', 'Williams'),
    ('Emma', 'Jones'),
    ('Jackson', 'Taylor'),
    ('Olivia', 'Brown'),
    ('Noah', 'Miller'),
    ('Ava', 'Davis'),
    ('Ethan', 'Moore'),
    ('Isabella', 'Clark'),
    ('Mason', 'Lee'),
    ('Amelia', 'Smith'),
    ('Logan', 'Johnson'),
    ('Mia', 'Martin'),
    ('Caden', 'White'),
    ('Harper', 'Thompson'),
    ('Elijah', 'Harris'),
    ('Aria', 'Wilson'),
    ('Caleb', 'Anderson'),
    ('Abigail', 'Thomas'),
    ('Sebastian', 'Hall');

insert into book ("name", "page_count", "author_id")
values
    ('The Secret of the Moon', 300, 12),
    ('Shadows in the Twilight', 450, 18),
    ('The Art of Dreaming', 200, 5),
    ('Endless Journey', 550, 16),
    ('Whispers of the Soul', 400, 9),
    ('The Last Chapter', 350, 3),
    ('Traces In Time', 150, 20),
    ('The Destiny of Winds', 500, 2),
    ('Lights of the Morning', 250, 8),
    ('Reflections in the Darkness', 600, 14),
    ('The Deception of Reality', 320, 15),
    ('Beyond the Mists', 480, 7),
    ('The Secret of the Woods', 180, 10),
    ('The Melody of the Heart', 420, 11),
    ('The Last Refuge', 260, 16),
    ('Spring Awakening', 510, 4),
    ('The Enigma of the Labyrinth', 390, 12),
    ('Light Among Shadows', 290, 18),
    ('Fragments of Eternity', 440, 8),
    ('The Silence of Winter', 170, 1);