INSERT INTO profiles (id, description, type) VALUES
(1, 'admin' , 'ADMIN'),
(2, 'user', 'USER');

INSERT INTO users (id, profileId, username, email, password) VALUES
(1, 1, 'juan', 'juaniago2001@gmail.com', '$2a$10$AP1AOeCCOrUoNQyMeTwZ9uACC6H6Y5Z4sl76vxtwn4HNzK3F9x30.'),
(2, 2, 'juanUser', 'juaniago2002@gmail.com', '$2a$10$AP1AOeCCOrUoNQyMeTwZ9uACC6H6Y5Z4sl76vxtwn4HNzK3F9x30.');


-- Insertar productos
INSERT INTO products (id, name, description, imagePath, price, stock, reference, userModifiedId) VALUES
(1, 'Producto A', 'Descripción del Producto A', '/images/productA.jpg', 10.99, 100, 'REF001', 1),
(2, 'Producto B', 'Descripción del Producto B', '/images/productB.jpg', 15.99, 200, 'REF002', 1);


-- Insertar órdenes
INSERT INTO orders (id, totalPrice, status, paymentMethod, userId, createdAt, updatedAt) VALUES
(1, 26.98, 'COMPLETED', 'CREDIT_CARD', 2, '2025-04-16T00:00:00', '2025-04-16T00:00');


-- Insertar detalles de órdenes
INSERT INTO orderDetails (idOrder, idProduct, amount) VALUES
(1, 1, 1),
(1, 2, 1);