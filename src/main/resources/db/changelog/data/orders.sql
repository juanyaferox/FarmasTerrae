INSERT INTO orders (
  id, totalPrice, status, paymentMethod, userId,
  createdAt, updatedAt, name, address
) VALUES
-- Pedidos de Pedro García
(1, 45.85, 'COMPLETED', 'CREDIT_CARD', 5, '2025-05-15T10:30:00', '2025-05-15T14:20:00', 'Pedro García', 'Calle Covadonga 23, Avilés, Asturias'),
(2, 28.90, 'COMPLETED', 'PAYPAL', 5, '2025-05-18T16:45:00', '2025-05-18T18:30:00', 'Pedro García', 'Calle Covadonga 23, Avilés, Asturias'),

-- Pedidos de Laura Martínez
(3, 67.40, 'SHIPPED', 'CREDIT_CARD', 6, '2025-05-20T09:15:00', '2025-05-21T11:00:00', 'Laura Martínez', 'Avenida de Galicia 67, Oviedo, Asturias'),
(4, 19.95, 'PENDING', 'TRANSFER', 6, '2025-05-25T14:30:00', '2025-05-25T14:30:00', 'Laura Martínez', 'Avenida de Galicia 67, Oviedo, Asturias'),

-- Pedidos de Miguel López
(5, 89.75, 'COMPLETED', 'CREDIT_CARD', 7, '2025-05-12T11:20:00', '2025-05-13T16:45:00', 'Miguel López', 'Calle San Francisco 89, Gijón, Asturias'),
(6, 156.30, 'SHIPPED', 'PAYPAL', 7, '2025-05-22T08:00:00', '2025-05-23T10:30:00', 'Miguel López', 'Calle San Francisco 89, Gijón, Asturias'),

-- Pedidos de Sofia Ruiz
(7, 34.90, 'COMPLETED', 'CREDIT_CARD', 8, '2025-05-14T13:45:00', '2025-05-14T17:20:00', 'Sofia Ruiz', 'Plaza de España 34, Mieres, Asturias'),
(8, 72.85, 'COMPLETED', 'TRANSFER', 8, '2025-05-19T10:15:00', '2025-05-20T12:30:00', 'Sofia Ruiz', 'Plaza de España 34, Mieres, Asturias'),

-- Pedidos de David Fernández
(9, 41.25, 'PENDING', 'PAYPAL', 9, '2025-05-26T15:30:00', '2025-05-26T15:30:00', 'David Fernández', 'Calle Corrida 56, Gijón, Asturias'),
(10, 23.80, 'COMPLETED', 'CREDIT_CARD', 9, '2025-05-17T12:00:00', '2025-05-17T18:45:00', 'David Fernández', 'Calle Corrida 56, Gijón, Asturias'),

-- Pedidos de Carmen González
(11, 95.60, 'SHIPPED', 'CREDIT_CARD', 10, '2025-05-21T09:30:00', '2025-05-22T14:15:00', 'Carmen González', 'Avenida de los Monumentos 78, Cangas de Onís, Asturias'),
(12, 58.45, 'COMPLETED', 'PAYPAL', 10, '2025-05-16T16:20:00', '2025-05-16T19:30:00', 'Carmen González', 'Avenida de los Monumentos 78, Cangas de Onís, Asturias'),

-- Pedidos de Roberto Sánchez
(13, 127.90, 'COMPLETED', 'TRANSFER', 11, '2025-05-13T14:45:00', '2025-05-14T10:20:00', 'Roberto Sánchez', 'Calle Real 90, Llanes, Asturias'),
(14, 36.75, 'PENDING', 'CREDIT_CARD', 11, '2025-05-24T11:00:00', '2025-05-24T11:00:00', 'Roberto Sánchez', 'Calle Real 90, Llanes, Asturias'),

-- Pedidos de Elena Jiménez
(15, 82.15, 'COMPLETED', 'PAYPAL', 12, '2025-05-11T08:30:00', '2025-05-12T15:45:00', 'Elena Jiménez', 'Plaza de Armas 12, Pravia, Asturias'),
(16, 29.95, 'SHIPPED', 'CREDIT_CARD', 12, '2025-05-23T13:15:00', '2025-05-24T09:30:00', 'Elena Jiménez', 'Plaza de Armas 12, Pravia, Asturias'),

-- Pedidos de Javier Moreno
(17, 114.50, 'COMPLETED', 'CREDIT_CARD', 13, '2025-05-10T10:45:00', '2025-05-11T16:20:00', 'Javier Moreno', 'Calle Libertad 45, Langreo, Asturias'),
(18, 63.25, 'PENDING', 'TRANSFER', 13, '2025-05-27T14:00:00', '2025-05-27T14:00:00', 'Javier Moreno', 'Calle Libertad 45, Langreo, Asturias'),

-- Pedidos de Raquel Torres
(19, 48.90, 'COMPLETED', 'PAYPAL', 14, '2025-05-09T12:30:00', '2025-05-09T18:15:00', 'Raquel Torres', 'Avenida de Castilla 67, Oviedo, Asturias'),
(20, 175.80, 'SHIPPED', 'CREDIT_CARD', 14, '2025-05-28T09:00:00', '2025-05-29T11:30:00', 'Raquel Torres', 'Avenida de Castilla 67, Oviedo, Asturias'),

-- Pedidos de Antonio Vargas
(21, 91.35, 'COMPLETED', 'CREDIT_CARD', 15, '2025-05-08T15:20:00', '2025-05-09T12:45:00', 'Antonio Vargas', 'Calle de la Paz 23, Villaviciosa, Asturias'),
(22, 37.60, 'COMPLETED', 'PAYPAL', 15, '2025-05-30T11:45:00', '2025-05-30T16:30:00', 'Antonio Vargas', 'Calle de la Paz 23, Villaviciosa, Asturias');