INSERT INTO orders (
  id, totalPrice, status, paymentMethod, userId,
  createdAt, updatedAt, name, address
) VALUES
(1, 26.98, 'COMPLETED', 'CREDIT_CARD', 2, '2025-04-16T00:00:00', '2025-04-16T00:00:00', 'Juan', 'Calle Cualquiera'),
(2, 32.50, 'PENDING',   'PAYPAL',      3, '2025-04-17T00:00:00', '2025-04-17T00:00:00', 'Juan', 'Calle Cualquiera'),
(3, 50.95, 'SHIPPED',   'CREDIT_CARD', 4, '2025-04-18T00:00:00', '2025-04-18T00:00:00', 'Juan', 'Calle Cualquiera'),
(4, 24.00, 'COMPLETED', 'TRANSFER',    5, '2025-04-19T00:00:00', '2025-04-19T00:00:00', 'Juan', 'Calle Cualquiera');
