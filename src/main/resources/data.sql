INSERT INTO profiles (id, description, type) VALUES
(1, 'admin' , 'ADMIN'),
(2, 'user', 'USER');

INSERT INTO users (id, profileId, username, email, password, phone, address) VALUES
(1, 1, 'juan', 'juaniago2001@gmail.com', '$2a$10$AP1AOeCCOrUoNQyMeTwZ9uACC6H6Y5Z4sl76vxtwn4HNzK3F9x30.', 697902399, 'A Pedreira 37, A Devesa, Ribadeo'),
(2, 2, 'juan2', 'juaniago2002@gmail.com', '$2a$10$AP1AOeCCOrUoNQyMeTwZ9uACC6H6Y5Z4sl76vxtwn4HNzK3F9x30.', NULL, 'Calle Falsa 123, Ciudad Inventada'),
(3, 2, 'maria', 'maria@example.com', '$2a$10$AP1AOeCCOrUoNQyMeTwZ9uACC6H6Y5Z4sl76vxtwn4HNzK3F9x30.', 600123456, NULL),
(4, 2, 'pedro', 'pedro@example.com', '$2a$10$AP1AOeCCOrUoNQyMeTwZ9uACC6H6Y5Z4sl76vxtwn4HNzK3F9x30.', NULL, NULL),
(5, 2, 'ana', 'ana@example.com', '$2a$10$AP1AOeCCOrUoNQyMeTwZ9uACC6H6Y5Z4sl76vxtwn4HNzK3F9x30.', 600987654, 'Avenida Siempreviva 742, Springfield');


INSERT INTO products (
  id, name, description, imagePath, price, stock, reference, userModifiedId, category
) VALUES
-- 50 VITAMIN
(  1, 'Producto VITAMIN 1',  'Desc VITAMIN 1',  '/images/vitamin1.jpg',  1234, 100, 'REFVIT001', 1, 'VITAMIN'),
(  2, 'Producto VITAMIN 2',  'Desc VITAMIN 2',  '/images/vitamin2.jpg',  1347, 110, 'REFVIT002', 1, 'VITAMIN'),
(  3, 'Producto VITAMIN 3',  'Desc VITAMIN 3',  '/images/vitamin3.jpg',  1456, 120, 'REFVIT003', 1, 'VITAMIN'),
(  4, 'Producto VITAMIN 4',  'Desc VITAMIN 4',  '/images/vitamin4.jpg',  1567, 130, 'REFVIT004', 1, 'VITAMIN'),
(  5, 'Producto VITAMIN 5',  'Desc VITAMIN 5',  '/images/vitamin5.jpg',  1678, 140, 'REFVIT005', 1, 'VITAMIN'),
(  6, 'Producto VITAMIN 6',  'Desc VITAMIN 6',  '/images/vitamin6.jpg',  1789, 150, 'REFVIT006', 1, 'VITAMIN'),
(  7, 'Producto VITAMIN 7',  'Desc VITAMIN 7',  '/images/vitamin7.jpg',  1890, 160, 'REFVIT007', 1, 'VITAMIN'),
(  8, 'Producto VITAMIN 8',  'Desc VITAMIN 8',  '/images/vitamin8.jpg',  1901, 170, 'REFVIT008', 1, 'VITAMIN'),
(  9, 'Producto VITAMIN 9',  'Desc VITAMIN 9',  '/images/vitamin9.jpg',  2012, 180, 'REFVIT009', 1, 'VITAMIN'),
( 10, 'Producto VITAMIN 10', 'Desc VITAMIN 10', '/images/vitamin10.jpg', 2123, 190, 'REFVIT010', 1, 'VITAMIN'),
( 11, 'Producto VITAMIN 11', 'Desc VITAMIN 11', '/images/vitamin11.jpg', 2234, 200, 'REFVIT011', 1, 'VITAMIN'),
( 12, 'Producto VITAMIN 12', 'Desc VITAMIN 12', '/images/vitamin12.jpg', 2345, 210, 'REFVIT012', 1, 'VITAMIN'),
( 13, 'Producto VITAMIN 13', 'Desc VITAMIN 13', '/images/vitamin13.jpg', 2456, 220, 'REFVIT013', 1, 'VITAMIN'),
( 14, 'Producto VITAMIN 14', 'Desc VITAMIN 14', '/images/vitamin14.jpg', 2567, 230, 'REFVIT014', 1, 'VITAMIN'),
( 15, 'Producto VITAMIN 15', 'Desc VITAMIN 15', '/images/vitamin15.jpg', 2678, 240, 'REFVIT015', 1, 'VITAMIN'),
( 16, 'Producto VITAMIN 16', 'Desc VITAMIN 16', '/images/vitamin16.jpg', 2789, 250, 'REFVIT016', 1, 'VITAMIN'),
( 17, 'Producto VITAMIN 17', 'Desc VITAMIN 17', '/images/vitamin17.jpg', 2890, 260, 'REFVIT017', 1, 'VITAMIN'),
( 18, 'Producto VITAMIN 18', 'Desc VITAMIN 18', '/images/vitamin18.jpg', 2901, 270, 'REFVIT018', 1, 'VITAMIN'),
( 19, 'Producto VITAMIN 19', 'Desc VITAMIN 19', '/images/vitamin19.jpg', 3012, 280, 'REFVIT019', 1, 'VITAMIN'),
( 20, 'Producto VITAMIN 20', 'Desc VITAMIN 20', '/images/vitamin20.jpg', 3123, 290, 'REFVIT020', 1, 'VITAMIN'),
( 21, 'Producto VITAMIN 21', 'Desc VITAMIN 21', '/images/vitamin21.jpg', 3234, 300, 'REFVIT021', 1, 'VITAMIN'),
( 22, 'Producto VITAMIN 22', 'Desc VITAMIN 22', '/images/vitamin22.jpg', 3345, 310, 'REFVIT022', 1, 'VITAMIN'),
( 23, 'Producto VITAMIN 23', 'Desc VITAMIN 23', '/images/vitamin23.jpg', 3456, 320, 'REFVIT023', 1, 'VITAMIN'),
( 24, 'Producto VITAMIN 24', 'Desc VITAMIN 24', '/images/vitamin24.jpg', 3567, 330, 'REFVIT024', 1, 'VITAMIN'),
( 25, 'Producto VITAMIN 25', 'Desc VITAMIN 25', '/images/vitamin25.jpg', 3678, 340, 'REFVIT025', 1, 'VITAMIN'),
( 26, 'Producto VITAMIN 26', 'Desc VITAMIN 26', '/images/vitamin26.jpg', 3789, 350, 'REFVIT026', 1, 'VITAMIN'),
( 27, 'Producto VITAMIN 27', 'Desc VITAMIN 27', '/images/vitamin27.jpg', 3890, 360, 'REFVIT027', 1, 'VITAMIN'),
( 28, 'Producto VITAMIN 28', 'Desc VITAMIN 28', '/images/vitamin28.jpg', 3901, 370, 'REFVIT028', 1, 'VITAMIN'),
( 29, 'Producto VITAMIN 29', 'Desc VITAMIN 29', '/images/vitamin29.jpg', 4012, 380, 'REFVIT029', 1, 'VITAMIN'),
( 30, 'Producto VITAMIN 30', 'Desc VITAMIN 30', '/images/vitamin30.jpg', 4123, 390, 'REFVIT030', 1, 'VITAMIN'),
( 31, 'Producto VITAMIN 31', 'Desc VITAMIN 31', '/images/vitamin31.jpg', 4234, 400, 'REFVIT031', 1, 'VITAMIN'),
( 32, 'Producto VITAMIN 32', 'Desc VITAMIN 32', '/images/vitamin32.jpg', 4345, 410, 'REFVIT032', 1, 'VITAMIN'),
( 33, 'Producto VITAMIN 33', 'Desc VITAMIN 33', '/images/vitamin33.jpg', 4456, 420, 'REFVIT033', 1, 'VITAMIN'),
( 34, 'Producto VITAMIN 34', 'Desc VITAMIN 34', '/images/vitamin34.jpg', 4567, 430, 'REFVIT034', 1, 'VITAMIN'),
( 35, 'Producto VITAMIN 35', 'Desc VITAMIN 35', '/images/vitamin35.jpg', 4678, 440, 'REFVIT035', 1, 'VITAMIN'),
( 36, 'Producto VITAMIN 36', 'Desc VITAMIN 36', '/images/vitamin36.jpg', 4789, 450, 'REFVIT036', 1, 'VITAMIN'),
( 37, 'Producto VITAMIN 37', 'Desc VITAMIN 37', '/images/vitamin37.jpg', 4890, 460, 'REFVIT037', 1, 'VITAMIN'),
( 38, 'Producto VITAMIN 38', 'Desc VITAMIN 38', '/images/vitamin38.jpg', 4901, 470, 'REFVIT038', 1, 'VITAMIN'),
( 39, 'Producto VITAMIN 39', 'Desc VITAMIN 39', '/images/vitamin39.jpg', 5012, 480, 'REFVIT039', 1, 'VITAMIN'),
( 40, 'Producto VITAMIN 40', 'Desc VITAMIN 40', '/images/vitamin40.jpg', 5123, 490, 'REFVIT040', 1, 'VITAMIN'),
( 41, 'Producto VITAMIN 41', 'Desc VITAMIN 41', '/images/vitamin41.jpg', 5234, 500, 'REFVIT041', 1, 'VITAMIN'),
( 42, 'Producto VITAMIN 42', 'Desc VITAMIN 42', '/images/vitamin42.jpg', 5345, 510, 'REFVIT042', 1, 'VITAMIN'),
( 43, 'Producto VITAMIN 43', 'Desc VITAMIN 43', '/images/vitamin43.jpg', 5456, 520, 'REFVIT043', 1, 'VITAMIN'),
( 44, 'Producto VITAMIN 44', 'Desc VITAMIN 44', '/images/vitamin44.jpg', 5567, 530, 'REFVIT044', 1, 'VITAMIN'),
( 45, 'Producto VITAMIN 45', 'Desc VITAMIN 45', '/images/vitamin45.jpg', 5678, 540, 'REFVIT045', 1, 'VITAMIN'),
( 46, 'Producto VITAMIN 46', 'Desc VITAMIN 46', '/images/vitamin46.jpg', 5789, 550, 'REFVIT046', 1, 'VITAMIN'),
( 47, 'Producto VITAMIN 47', 'Desc VITAMIN 47', '/images/vitamin47.jpg', 5890, 560, 'REFVIT047', 1, 'VITAMIN'),
( 48, 'Producto VITAMIN 48', 'Desc VITAMIN 48', '/images/vitamin48.jpg', 5901, 570, 'REFVIT048', 1, 'VITAMIN'),
( 49, 'Producto VITAMIN 49', 'Desc VITAMIN 49', '/images/vitamin49.jpg', 6012, 580, 'REFVIT049', 1, 'VITAMIN'),
( 50, 'Producto VITAMIN 50', 'Desc VITAMIN 50', '/images/vitamin50.jpg', 6123, 590, 'REFVIT050', 1, 'VITAMIN'),

-- 20 MEDICINE
( 51, 'Producto MEDICINE 1',  'Desc MEDICINE 1',  '/images/medicine1.jpg',  1111, 100, 'REFMED001', 1, 'MEDICINE'),
( 52, 'Producto MEDICINE 2',  'Desc MEDICINE 2',  '/images/medicine2.jpg',  1222, 110, 'REFMED002', 1, 'MEDICINE'),
( 53, 'Producto MEDICINE 3',  'Desc MEDICINE 3',  '/images/medicine3.jpg',  1333, 120, 'REFMED003', 1, 'MEDICINE'),
( 54, 'Producto MEDICINE 4',  'Desc MEDICINE 4',  '/images/medicine4.jpg',  1444, 130, 'REFMED004', 1, 'MEDICINE'),
( 55, 'Producto MEDICINE 5',  'Desc MEDICINE 5',  '/images/medicine5.jpg',  1555, 140, 'REFMED005', 1, 'MEDICINE'),
( 56, 'Producto MEDICINE 6',  'Desc MEDICINE 6',  '/images/medicine6.jpg',  1666, 150, 'REFMED006', 1, 'MEDICINE'),
( 57, 'Producto MEDICINE 7',  'Desc MEDICINE 7',  '/images/medicine7.jpg',  1777, 160, 'REFMED007', 1, 'MEDICINE'),
( 58, 'Producto MEDICINE 8',  'Desc MEDICINE 8',  '/images/medicine8.jpg',  1888, 170, 'REFMED008', 1, 'MEDICINE'),
( 59, 'Producto MEDICINE 9',  'Desc MEDICINE 9',  '/images/medicine9.jpg',  1999, 180, 'REFMED009', 1, 'MEDICINE'),
( 60, 'Producto MEDICINE 10', 'Desc MEDICINE 10', '/images/medicine10.jpg', 2010, 190, 'REFMED010', 1, 'MEDICINE'),
( 61, 'Producto MEDICINE 11', 'Desc MEDICINE 11', '/images/medicine11.jpg', 2121, 200, 'REFMED011', 1, 'MEDICINE'),
( 62, 'Producto MEDICINE 12', 'Desc MEDICINE 12', '/images/medicine12.jpg', 2232, 210, 'REFMED012', 1, 'MEDICINE'),
( 63, 'Producto MEDICINE 13', 'Desc MEDICINE 13', '/images/medicine13.jpg', 2343, 220, 'REFMED013', 1, 'MEDICINE'),
( 64, 'Producto MEDICINE 14', 'Desc MEDICINE 14', '/images/medicine14.jpg', 2454, 230, 'REFMED014', 1, 'MEDICINE'),
( 65, 'Producto MEDICINE 15', 'Desc MEDICINE 15', '/images/medicine15.jpg', 2565, 240, 'REFMED015', 1, 'MEDICINE'),
( 66, 'Producto MEDICINE 16', 'Desc MEDICINE 16', '/images/medicine16.jpg', 2676, 250, 'REFMED016', 1, 'MEDICINE'),
( 67, 'Producto MEDICINE 17', 'Desc MEDICINE 17', '/images/medicine17.jpg', 2787, 260, 'REFMED017', 1, 'MEDICINE'),
( 68, 'Producto MEDICINE 18', 'Desc MEDICINE 18', '/images/medicine18.jpg', 2898, 270, 'REFMED018', 1, 'MEDICINE'),
( 69, 'Producto MEDICINE 19', 'Desc MEDICINE 19', '/images/medicine19.jpg', 2909, 280, 'REFMED019', 1, 'MEDICINE'),
( 70, 'Producto MEDICINE 20', 'Desc MEDICINE 20', '/images/medicine20.jpg', 3020, 290, 'REFMED020', 1, 'MEDICINE'),

-- 10 CARE
( 71, 'Producto CARE 1',  'Desc CARE 1',  '/images/care1.jpg',  999, 50, 'REFCAR001', 1, 'CARE'),
( 72, 'Producto CARE 2',  'Desc CARE 2',  '/images/care2.jpg', 1011, 55, 'REFCAR002', 1, 'CARE'),
( 73, 'Producto CARE 3',  'Desc CARE 3',  '/images/care3.jpg', 1122, 60, 'REFCAR003', 1, 'CARE'),
( 74, 'Producto CARE 4',  'Desc CARE 4',  '/images/care4.jpg', 1233, 65, 'REFCAR004', 1, 'CARE'),
( 75, 'Producto CARE 5',  'Desc CARE 5',  '/images/care5.jpg', 1344, 70, 'REFCAR005', 1, 'CARE'),
( 76, 'Producto CARE 6',  'Desc CARE 6',  '/images/care6.jpg', 1455, 75, 'REFCAR006', 1, 'CARE'),
( 77, 'Producto CARE 7',  'Desc CARE 7',  '/images/care7.jpg', 1566, 80, 'REFCAR007', 1, 'CARE'),
( 78, 'Producto CARE 8',  'Desc CARE 8',  '/images/care8.jpg', 1677, 85, 'REFCAR008', 1, 'CARE'),
( 79, 'Producto CARE 9',  'Desc CARE 9',  '/images/care9.jpg', 1788, 90, 'REFCAR009', 1, 'CARE'),
( 80, 'Producto CARE 10', 'Desc CARE 10', '/images/care10.jpg',1899, 95, 'REFCAR010', 1, 'CARE'),

-- 10 FIRST_AID
( 81, 'Producto FIRST_AID 1',  'Desc FIRST_AID 1',  '/images/firstaid1.jpg',  888, 40, 'REFFA001', 1, 'FIRST_AID'),
( 82, 'Producto FIRST_AID 2',  'Desc FIRST_AID 2',  '/images/firstaid2.jpg',  999, 45, 'REFFA002', 1, 'FIRST_AID'),
( 83, 'Producto FIRST_AID 3',  'Desc FIRST_AID 3',  '/images/firstaid3.jpg', 1010, 50, 'REFFA003', 1, 'FIRST_AID'),
( 84, 'Producto FIRST_AID 4',  'Desc FIRST_AID 4',  '/images/firstaid4.jpg', 1111, 55, 'REFFA004', 1, 'FIRST_AID'),
( 85, 'Producto FIRST_AID 5',  'Desc FIRST_AID 5',  '/images/firstaid5.jpg', 1212, 60, 'REFFA005', 1, 'FIRST_AID'),
( 86, 'Producto FIRST_AID 6',  'Desc FIRST_AID 6',  '/images/firstaid6.jpg', 1313, 65, 'REFFA006', 1, 'FIRST_AID'),
( 87, 'Producto FIRST_AID 7',  'Desc FIRST_AID 7',  '/images/firstaid7.jpg', 1414, 70, 'REFFA007', 1, 'FIRST_AID'),
( 88, 'Producto FIRST_AID 8',  'Desc FIRST_AID 8',  '/images/firstaid8.jpg', 1515, 75, 'REFFA008', 1, 'FIRST_AID'),
( 89, 'Producto FIRST_AID 9',  'Desc FIRST_AID 9',  '/images/firstaid9.jpg', 1616, 80, 'REFFA009', 1, 'FIRST_AID'),
( 90, 'Producto FIRST_AID 10', 'Desc FIRST_AID 10', '/images/firstaid10.jpg',1717,85, 'REFFA010', 1, 'FIRST_AID'),

-- 10 BEAUTY
( 91, 'Producto BEAUTY 1',  'Desc BEAUTY 1',  '/images/beauty1.jpg',  1414,30, 'REFBEA001', 1, 'BEAUTY'),
( 92, 'Producto BEAUTY 2',  'Desc BEAUTY 2',  '/images/beauty2.jpg',  1515,35, 'REFBEA002', 1, 'BEAUTY'),
( 93, 'Producto BEAUTY 3',  'Desc BEAUTY 3',  '/images/beauty3.jpg',  1616,40, 'REFBEA003', 1, 'BEAUTY'),
( 94, 'Producto BEAUTY 4',  'Desc BEAUTY 4',  '/images/beauty4.jpg',  1717,45, 'REFBEA004', 1, 'BEAUTY'),
( 95, 'Producto BEAUTY 5',  'Desc BEAUTY 5',  '/images/beauty5.jpg',  1818,50, 'REFBEA005', 1, 'BEAUTY'),
( 96, 'Producto BEAUTY 6',  'Desc BEAUTY 6',  '/images/beauty6.jpg',  1919,55, 'REFBEA006', 1, 'BEAUTY'),
( 97, 'Producto BEAUTY 7',  'Desc BEAUTY 7',  '/images/beauty7.jpg',  2020,60, 'REFBEA007', 1, 'BEAUTY'),
( 98, 'Producto BEAUTY 8',  'Desc BEAUTY 8',  '/images/beauty8.jpg',  2121,65, 'REFBEA008', 1, 'BEAUTY'),
( 99, 'Producto BEAUTY 9',  'Desc BEAUTY 9',  '/images/beauty9.jpg',  2222,70, 'REFBEA009', 1, 'BEAUTY'),
(100, 'Producto BEAUTY 10', 'Desc BEAUTY 10','/images/beauty10.jpg',2323,75, 'REFBEA010', 1, 'BEAUTY')
;

-- Insertar órdenes
INSERT INTO orders (id, totalPrice, status, paymentMethod, userId, createdAt, updatedAt) VALUES
(1, 26.98, 'COMPLETED', 'CREDIT_CARD', 2, '2025-04-16T00:00:00', '2025-04-16T00:00:00'),
(2, 32.50, 'PENDING', 'PAYPAL', 3, '2025-04-17T00:00:00', '2025-04-17T00:00:00'),
(3, 50.95, 'SHIPPED', 'CREDIT_CARD', 4, '2025-04-18T00:00:00', '2025-04-18T00:00:00'),
(4, 24.00, 'COMPLETED', 'TRANSFER', 5, '2025-04-19T00:00:00', '2025-04-19T00:00:00');


-- Insertar detalles de órdenes
INSERT INTO orderDetails (idOrder, idProduct, amount) VALUES
(1, 1, 1),
(1, 2, 1),
(2, 3, 1),
(2, 5, 1),
(3, 1, 3),
(3, 4, 2),
(4, 5, 2);

INSERT INTO reviews (id, title, content, scoring, productId, userId) VALUES
 (1, 'Buen producto', 'Descripción de prueba', 10, 1, 2),
 (2, 'Excelente', 'Muy satisfecho', 9, 2, 2),
 (3, 'Muy útil', 'Lo necesitaba', 7, 3, 3),
 (4, 'Cumple su función', 'Buena calidad', 8, 5, 3),
 (5, 'Buena relación calidad/precio', 'Me gustó', 8, 1, 4),
 (6, 'Perfecto', 'Ideal para mi uso', 9, 4, 4),
 (7, 'Todo bien', 'Buena compra', 8, 5, 5);