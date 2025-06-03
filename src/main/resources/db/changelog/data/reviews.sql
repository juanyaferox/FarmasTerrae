INSERT INTO reviews (id, title, content, scoring, productId, userId) VALUES
-- Reseñas de Pedro García (userId: 5)
(1, 'Excelente vitamina C', 'La tomo cada mañana y he notado menos resfriados. Buen sabor a naranja y fácil de masticar.', 9, 1, 5),
(2, 'Analgésico eficaz', 'El paracetamol de siempre, funciona bien para dolores de cabeza y fiebre. Precio correcto.', 8, 31, 5),
(3, 'Perfecta para el gimnasio', 'La creatina se disuelve bien y he notado mejor rendimiento en mis entrenamientos.', 9, 23, 5),

-- Reseñas de Laura Martínez (userId: 6)
(4, 'Crema antiarrugas muy buena', 'Llevo usándola 2 meses y mi piel se ve más hidratada y suave. Recomendable.', 8, 64, 6),
(5, 'Protección solar ideal', 'No deja residuo blanco y protege muy bien del sol. La uso diariamente en la cara.', 10, 59, 6),
(6, 'Contorno de ojos efectivo', 'He notado reducción en las ojeras después de varias semanas de uso constante.', 7, 74, 6),

-- Reseñas de Miguel López (userId: 7)
(7, 'Medicamento necesario', 'Para el colesterol funciona bien, sin efectos secundarios molestos hasta ahora.', 8, 38, 7),
(8, 'Coenzima Q10 de calidad', 'Suplemento premium, he notado más energía desde que lo tomo. Vale la pena.', 9, 9, 7),
(9, 'Omega 3 excelente', 'Cápsulas de buen tamaño, sin regusto a pescado. Muy recomendable para la salud cardiovascular.', 10, 4, 7),

-- Reseñas de Sofia Ruiz (userId: 8)
(10, 'Resveratrol potente', 'Antioxidante natural de alta calidad. Precio elevado pero merece la pena.', 8, 30, 8),
(11, 'Champú anticaspa efectivo', 'Por fin encontré un champú que controla mi caspa sin resecar el cabello.', 9, 56, 8),
(12, 'Exfoliante corporal fantástico', 'Deja la piel súper suave, el aroma es agradable y la textura perfecta.', 9, 72, 8),

-- Reseñas de David Fernández (userId: 9)
(13, 'Omeprazol funciona bien', 'Para la acidez estomacal es muy efectivo. Lo tomo en ayunas como indicado.', 8, 34, 9),
(14, 'Antihistamínico confiable', 'Para las alergias primaverales me va muy bien, sin somnolencia.', 8, 35, 9),

-- Reseñas de Carmen González (userId: 10)
(15, 'Multivitamínico completo', 'Específico para seniors, me siento con más vitalidad desde que lo tomo.', 9, 10, 10),
(16, 'Calcio necesario', 'Para mis huesos, mi médico me lo recomendó. Fácil de tomar.', 8, 7, 10),
(17, 'Colágeno hidrolizado bueno', 'Se disuelve bien en agua, sin sabor extraño. Para articulaciones y piel.', 8, 14, 10),

-- Reseñas de Roberto Sánchez (userId: 11)
(18, 'Glucosamina muy efectiva', 'Para las articulaciones ha sido un cambio notable. Menos dolor en rodillas.', 9, 15, 11),
(19, 'Gel antiinflamatorio útil', 'Para dolores musculares localizados funciona muy bien. No mancha la ropa.', 8, 37, 11),
(20, 'Botiquín completo', 'Viene con todo lo básico para emergencias. Perfecto para el coche.', 9, 86, 11),

-- Reseñas de Elena Jiménez (userId: 12)
(21, 'Spirulina de gran calidad', 'Alga rica en nutrientes, he notado más energía y mejor digestión.', 8, 17, 12),
(22, 'Adaptógeno excelente', 'La rhodiola me ayuda con el estrés del trabajo. Muy recomendable.', 9, 20, 12),
(23, 'Probióticos efectivos', 'Mejor digestión y menos hinchazón abdominal desde que los tomo.', 9, 13, 12),

-- Reseñas de Javier Moreno (userId: 13)
(24, 'Metformina esencial', 'Para mi diabetes tipo 2, funciona perfectamente sin efectos secundarios graves.', 9, 40, 13),
(25, 'Enalapril confiable', 'Mantiene mi presión arterial controlada. Medicamento de confianza.', 8, 39, 13),

-- Reseñas de Raquel Torres (userId: 14)
(26, 'Base de maquillaje perfecta', 'Cobertura natural, no se ve artificial y dura todo el día. Mi favorita.', 10, 91, 14),
(27, 'Máscara de pestañas increíble', 'Volumen espectacular sin grumos, resistente al agua como promete.', 9, 93, 14),
(28, 'Paleta de sombras versátil', 'Tonos neutros perfectos para cualquier ocasión. Pigmentación excelente.', 9, 95, 14),

-- Reseñas de Antonio Vargas (userId: 15)
(29, 'Saw Palmetto efectivo', 'Para problemas de próstata, he notado mejora significativa. Muy recomendable.', 9, 28, 15),
(30, 'Zinc de buena absorción', 'Suplemento básico pero de calidad. No causa molestias estomacales.', 8, 8, 15),
(31, 'L-Carnitina para deporte', 'Me ayuda con la energía durante los entrenamientos. Buen suplemento.', 8, 24, 15),

-- Reseñas adicionales variadas
(32, 'Vitamina D3 indispensable', 'Especialmente en invierno, es fundamental para el sistema inmune.', 9, 2, 6),
(33, 'Hierro bien tolerado', 'No me causa estreñimiento como otros hierros que he probado.', 8, 6, 10),
(34, 'Melatonina para dormir', 'Me ayuda a conciliar el sueño de forma natural. Sublingual muy práctica.', 9, 16, 12),
(35, 'Ibuprofeno de confianza', 'Para dolores musculares y antiinflamatorio, siempre efectivo.', 8, 32, 9),
(36, 'Sérum vitamina C fantástico', 'Ilumina el rostro y unifica el tono. Se absorbe rápidamente.', 10, 65, 14),
(37, 'Agua micelar suave', 'Desmaquilla perfectamente sin irritar los ojos. Muy suave para pieles sensibles.', 9, 71, 14),
(38, 'Cúrcuma antiinflamatoria', 'Con pimienta negra para mejor absorción. Noto menos inflamación articular.', 8, 21, 11),
(39, 'Biotina para el cabello', 'Desde que la tomo tengo el cabello más fuerte y brillante.', 9, 22, 6),
(40, 'Ashwagandha relajante', 'Adaptógeno que me ayuda con el estrés y la ansiedad. Muy recomendable.', 9, 18, 13);