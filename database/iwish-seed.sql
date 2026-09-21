-- I-Wish demo seed data
USE iwish;

-- Demo password for all sample accounts is: demo
INSERT INTO users (name, email, password_hash) VALUES
('Demo User', 'demo@iwish.local', 'demo'),
('Mona Ali', 'mona@iwish.local', 'demo'),
('Omar Hassan', 'omar@iwish.local', 'demo');

INSERT INTO friendships (requester_id, receiver_id, status) VALUES
(1, 2, 'ACCEPTED'),
(1, 3, 'ACCEPTED');

INSERT INTO available_items (name, category, description, default_price) VALUES
('Noise Cancelling Headphones', 'Tech', 'For daily focus time', 129.99),
('Weekend Backpack', 'Travel', 'A compact adventure bag', 79.50),
('Classic Watch', 'Style', 'A timeless everyday watch', 150.00),
('Kindle Reader', 'Books', 'A lightweight e-reader', 119.00);

INSERT INTO wish_items (owner_id, title, category, description, price, contributed, purchased) VALUES
(2, 'Noise Cancelling Headphones', 'Tech', 'For my daily focus time', 129.99, 0.00, FALSE),
(2, 'Weekend Backpack', 'Travel', 'A compact adventure bag', 79.50, 0.00, FALSE),
(3, 'Classic Watch', 'Style', 'A timeless everyday watch', 150.00, 0.00, FALSE);

-- Optional example notification
INSERT INTO notifications (user_id, message, is_read)
VALUES (1, 'Welcome to I-Wish — make someone happy today!', FALSE);
