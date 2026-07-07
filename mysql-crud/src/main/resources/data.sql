INSERT IGNORE INTO users (id, name, email) VALUES
    (1, 'Alice', 'alice@example.com'),
    (2, 'Bob',   'bob@example.com'),
    (3, 'Carol', 'carol@example.com');

INSERT IGNORE INTO orders (id, user_id, amount, status) VALUES
    (1, 1, 99.50,  'PAID'),
    (2, 1, 15.00,  'PENDING'),
    (3, 2, 250.00, 'PAID');
