INSERT INTO profiles (id, description, type) VALUES
(1, 'admin' , 'ADMIN'),
(2, 'user', 'USER');

INSERT INTO users (id, profileId, username, email, password, phone, address) VALUES
(1, 1, 'juan', 'juaniago2001@gmail.com', '$2a$10$AP1AOeCCOrUoNQyMeTwZ9uACC6H6Y5Z4sl76vxtwn4HNzK3F9x30.', 697902399, 'A Pedreira 37, A Devesa, Ribadeo'),
(2, 2, 'juanUser', 'juaniago2002@gmail.com', '$2a$10$AP1AOeCCOrUoNQyMeTwZ9uACC6H6Y5Z4sl76vxtwn4HNzK3F9x30.', NULL, 'Calle Falsa 123, Ciudad Inventada'),
(3, 2, 'maria', 'maria@example.com', '$2a$10$AP1AOeCCOrUoNQyMeTwZ9uACC6H6Y5Z4sl76vxtwn4HNzK3F9x30.', 600123456, NULL),
(4, 2, 'pedro', 'pedro@example.com', '$2a$10$AP1AOeCCOrUoNQyMeTwZ9uACC6H6Y5Z4sl76vxtwn4HNzK3F9x30.', NULL, NULL),
(5, 2, 'ana', 'ana@example.com', '$2a$10$AP1AOeCCOrUoNQyMeTwZ9uACC6H6Y5Z4sl76vxtwn4HNzK3F9x30.', 600987654, 'Avenida Siempreviva 742, Springfield');


-- Insertar productos
INSERT INTO products (id, name, description, imagePath, price, stock, reference, userModifiedId) VALUES
(1, 'Producto A', 'Descripción del Producto A', '/images/productA.jpg', 10.99, 100, 'REF001', 1),
(2, 'Producto B', 'Descripción del Producto B', '/images/productB.jpg', 15.99, 200, 'REF002', 1),
(3, 'Producto C', 'Descripción del Producto C', '/images/productC.jpg', 20.50, 150, 'REF003', 1),
(4, 'Producto D', 'Descripción del Producto D', '/images/productD.jpg', 8.99, 300, 'REF004', 1),
(5, 'Producto E', 'Descripción del Producto E', '/images/productE.jpg', 12.00, 50, 'REF005', 1);


-- Insertar órdenes
INSERT INTO orders (id, totalPrice, status, paymentMethod, userId, createdAt, updatedAt) VALUES
(1, 26.98, 'COMPLETED', 'CREDIT_CARD', 2, '2025-04-16T00:00:00', '2025-04-16T00:00:00'),
(2, 32.50, 'PENDING', 'PAYPAL', 3, '2025-04-17T00:00:00', '2025-04-17T00:00:00'),
(3, 50.95, 'SHIPPED', 'CREDIT_CARD', 4, '2025-04-18T00:00:00', '2025-04-18T00:00:00'),
(4, 24.00, 'COMPLETED', 'TRANSFER', 5, '2025-04-19T00:00:00', '2025-04-19T00:00:00');


-- Insertar detalles de órdenes
INSERT INTO orderDetails (idOrder, idProduct, amount) VALUES
(1, 1, 1),  -- Producto A: 10.99 * 1
(1, 2, 1),  -- Producto B: 15.99 * 1  => Total: 26.98
(2, 3, 1),  -- Producto C: 20.50 * 1
(2, 5, 1),  -- Producto E: 12.00 * 1  => Total: 32.50
(3, 1, 3),  -- Producto A: 10.99 * 3 = 32.97
(3, 4, 2),  -- Producto D: 8.99 * 2 = 17.98  => Total: 50.95
(4, 5, 2);  -- Producto E: 12.00 * 2  => Total: 24.00

INSERT INTO reviews (id, title, content, scoring, productId, userId) VALUES
 (1, 'Buen producto', 'Descripción de prueba', 10, 1, 2),
 (2, 'Excelente', 'Muy satisfecho', 9, 2, 3),
 (3, 'Regular', 'Podría ser mejor', 6, 3, 4),
 (4, 'Bueno', 'Cumple su función', 8, 1, 5),
 (5, 'No está mal', 'Aceptable', 7, 4, 2),
 (6, 'Satisfecho', 'Buena compra', 8, 5, 3);