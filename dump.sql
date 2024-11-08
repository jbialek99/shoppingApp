-- MySQL dump 10.13  Distrib 8.0.37, for Linux (x86_64)
--
-- Host: localhost    Database: app_database
-- ------------------------------------------------------
-- Server version	8.0.37-0ubuntu0.23.10.2

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint DEFAULT NULL,
  `product_id` bigint DEFAULT NULL,
  `quantity` int NOT NULL,
  `price` decimal(38,2) NOT NULL,
  `total_item_price` decimal(38,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `order_id` (`order_id`),
  KEY `product_id` (`product_id`),
  CONSTRAINT `order_items_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `order_items_ibfk_2` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=160 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
INSERT INTO `order_items` VALUES (113,104,5,1,30000.00,30000.00),(116,107,4,1,45000.00,45000.00),(117,109,3,1,15000.00,15000.00),(119,108,1,1,35000.00,35000.00),(120,110,2,1,35000.00,35000.00),(121,110,3,1,15000.00,15000.00),(122,111,6,1,20000.00,20000.00),(123,113,7,1,35000.00,35000.00),(124,114,8,1,15000.00,15000.00),(125,112,9,1,45000.00,45000.00),(126,115,10,1,30000.00,30000.00),(127,116,1,1,35000.00,35000.00),(128,116,2,1,35000.00,35000.00),(129,117,1,2,35000.00,70000.00),(130,118,2,1,35000.00,35000.00),(131,118,3,1,15000.00,15000.00),(132,119,1,1,35000.00,35000.00),(133,120,3,1,15000.00,15000.00),(134,121,3,1,15000.00,15000.00),(135,122,3,1,15000.00,15000.00),(137,125,3,1,15000.00,15000.00),(138,126,3,1,15000.00,15000.00),(139,127,1,1,35000.00,35000.00),(140,128,1,1,35000.00,35000.00),(141,129,3,1,15000.00,15000.00),(142,124,1,1,35000.00,35000.00),(143,130,3,1,15000.00,15000.00),(144,131,3,1,15000.00,15000.00),(146,133,4,1,45000.00,45000.00),(151,134,3,1,15000.00,15000.00),(152,135,1,1,35000.00,35000.00),(153,136,2,1,35000.00,35000.00),(154,137,1,1,35000.00,35000.00),(155,138,1,1,35000.00,35000.00),(156,139,1,1,35000.00,35000.00),(157,140,1,1,35000.00,35000.00),(158,141,1,1,35000.00,35000.00),(159,142,1,1,35000.00,35000.00);
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `order_date` datetime(6) DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb3_unicode_ci NOT NULL,
  `total_price` decimal(38,2) NOT NULL,
  `contact_name` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `contact_phone` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `contact_address` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=143 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES (104,30,'2024-11-01 09:37:56.979982','CONFIRMED',30000.00,' ','',''),(107,NULL,'2024-11-01 10:08:56.438235','CONFIRMED',45000.00,'xcv4 43','54ft','re'),(108,30,'2024-11-01 10:15:23.855507','CONFIRMED',35000.00,'Jakub Białek','508633766','Kwiatowa 7'),(109,34,'2024-11-01 10:17:20.569972','CONFIRMED',15000.00,'Andrzej Miłosz','123456789','Celebrytów 15'),(110,30,'2024-11-01 11:32:21.136898','CONFIRMED',50000.00,'Jakub Białek','508633766','Kwiatowa 7'),(111,30,'2024-11-01 11:38:21.033374','CONFIRMED',20000.00,'Jakub Białek','508633766','Kwiatowa 7'),(112,30,'2024-11-01 11:38:45.141947','CONFIRMED',45000.00,'Jakub Białek','508633766','Kwiatowa 7'),(113,NULL,'2024-11-01 11:38:54.761422','CONFIRMED',35000.00,'J H','508633766','H'),(114,NULL,'2024-11-01 11:39:10.787544','CONFIRMED',15000.00,'3 6','6','6'),(115,NULL,'2024-11-01 11:46:55.258804','CONFIRMED',30000.00,'Jakub Białek','508633766','Kwiatowa'),(116,30,'2024-11-01 11:48:58.548403','CONFIRMED',70000.00,'Jakub Białek','508633766','Kwiatowa 7'),(117,30,'2024-11-01 11:49:29.125372','CONFIRMED',70000.00,'Jakub Białek','508633766','Kwiatowa 7'),(118,NULL,'2024-11-01 11:53:03.817497','CONFIRMED',50000.00,'Jakub3 Białek3','5086337366','Kwiatowa'),(119,NULL,'2024-11-01 11:56:50.828559','CONFIRMED',35000.00,'A A','508633766','A 2'),(120,NULL,'2024-11-01 12:00:35.902361','CONFIRMED',15000.00,'Jakub Białek','508633766','Kwiatowa 7'),(121,NULL,'2024-11-01 12:02:55.211993','CONFIRMED',15000.00,'Jakub Białek','508633766','Kwiatowa5'),(122,30,'2024-11-01 12:03:27.579196','CONFIRMED',15000.00,'Jakub Białek','508633766','Kwiatowa 7'),(124,30,'2024-11-01 12:11:28.367766','CONFIRMED',35000.00,'Jakub Białek','508633766','Kwiatowa 7'),(125,34,'2024-11-01 12:21:38.840219','CONFIRMED',15000.00,'Andrzej Miłosz','123456789','Celebrytów 15'),(126,NULL,'2024-11-01 12:22:15.188079','CONFIRMED',15000.00,'Jakub Białek','508633766','Kwiatowa 7'),(127,NULL,'2024-11-01 12:22:31.313214','CONFIRMED',35000.00,'Jakub Białek','508633766','2Kwiatowa'),(128,NULL,'2024-11-01 12:27:03.544981','CONFIRMED',35000.00,'Jakub Białek','508633766','Kwiatowa 7'),(129,NULL,'2024-11-01 12:27:20.061693','CONFIRMED',15000.00,'Jakub Białek','508633766','Kwiatowa2 '),(130,30,'2024-11-01 12:28:24.513366','CONFIRMED',15000.00,'Jakub Białek','508633766','Kwiatowa 7'),(131,NULL,'2024-11-01 12:29:09.218172','CONFIRMED',15000.00,'Jakub Białek','508633766','Kwiatowa 7'),(133,30,'2024-11-01 12:45:46.169178','CONFIRMED',45000.00,'Jakub Białek','508633766','Kwiatowa 7'),(134,30,'2024-11-01 12:50:02.064921','CONFIRMED',15000.00,'Jakub Białek','508633766','Kwiatowa 7'),(135,30,'2024-11-07 18:23:00.177265','CONFIRMED',35000.00,'Jakub Białek','508633766','Kwiatowa 7'),(136,NULL,'2024-11-07 18:28:40.648828','CONFIRMED',35000.00,'w w','123123123','s 2'),(137,30,'2024-11-07 18:30:42.869541','CONFIRMED',35000.00,'Jakub Białek','508633766','alsa 2'),(138,NULL,'2024-11-07 18:31:22.964776','CONFIRMED',35000.00,'a a','111111111','a 2'),(139,NULL,'2024-11-07 18:31:41.221828','CONFIRMED',35000.00,'ds e','111111111','s 2'),(140,NULL,'2024-11-07 18:33:24.355107','CONFIRMED',35000.00,'a a','000000000','a 1'),(141,30,'2024-11-07 21:12:19.521030','CONFIRMED',35000.00,'Jakub Białek','508633766','Kwiatowa 7'),(142,30,'2024-11-07 21:12:50.733312','CONFIRMED',35000.00,'Jakub Białek','508633766','Kwiatowa 7');
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb3_unicode_ci NOT NULL,
  `description` varchar(500) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `price` decimal(38,2) NOT NULL,
  `stock` int NOT NULL,
  `image_url` varchar(1000) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (1,'Nissan GT-R','A comfortable and fuel-efficient sedan.',35000.00,0,'/images/nissan.jpg'),(2,'Mazda MX-5','A luxury SUV with modern features.',35000.00,0,'/images/mazda.jpg'),(3,'Honda Accord','An affordable hatchback with great mileage.',15000.00,0,'/images/honda.jpg'),(4,'BMW M5','A sporty coupe with high performance.',45000.00,0,'/images/bmw.jpg'),(5,'Mercedes V5','A family-friendly minivan with ample space.',30000.00,0,'/images/mercedes.jpg'),(6,'Volvo S60','A comfortable and fuel-efficient sedan.',20000.00,2,'/images/volvo.jpg'),(7,'Audi A7','A luxury SUV with modern features.',35000.00,2,'/images/audi.jpg'),(8,'Chevrolet Camaro','An affordable hatchback with great mileage.',15000.00,1,'/images/chevrolet.jpg'),(9,'Peugeot 508','A sporty coupe with high performance.',45000.00,2,'/images/peugeot.jpg'),(10,'Ford Focus','A family-friendly minivan with ample space.',30000.00,1,'/images/ford.jpg');
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) COLLATE utf8mb3_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb3_unicode_ci NOT NULL,
  `email` varchar(255) COLLATE utf8mb3_unicode_ci NOT NULL,
  `role` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT 'USER',
  `first_name` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `last_name` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  `phone` varchar(255) COLLATE utf8mb3_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (30,'Please','$argon2id$v=19$m=4096,t=3,p=1$Lu+gLQFkjz4U4zUX8TmY5g$jc9S/78baRagwh2Ta2UzM1GCo256iNkjyI03t7mMHuc','please@gmail.com','USER','Jakub','Białek','Kwiatowa 7','508633766'),(34,'admin','$argon2id$v=19$m=4096,t=3,p=1$Vi1WSXDYoU5Z4lPj/5Z+6A$dWbmHXsZaZfg8gvKAp604PNPRIOo9xylLhdchhuVO2M','admin@admin','USER','Andrzej','Miłosz','Celebrytów 15','123456789'),(35,'root','$argon2id$v=19$m=4096,t=3,p=1$YLgDCkqieIjg1Q3csXOe8Q$0Ktx4FE2lyxJltieud3i9UtYvLEaZk3pRjVTyJCOjVA','root@root','USER',NULL,NULL,NULL,NULL),(36,'sdf','$argon2id$v=19$m=4096,t=3,p=1$Abpo7wBSjXwfePCC//fixg$bfRV8pB1Lrh9lyOK0cUohhpTu/l8lo+SbCNhYNrD2Ik','3d@e','USER',NULL,NULL,NULL,NULL),(37,'r','$argon2id$v=19$m=4096,t=3,p=1$GEk8/k4sqy7HVUUCXNRn1A$l0NVKP8Od0yS9IEdk2aVlGvS0KRSirRH3sJOx8hJVKk','r@r','USER',NULL,NULL,NULL,NULL),(38,'e','$argon2id$v=19$m=4096,t=3,p=1$c1Rf0V7KLSwJA8zTexv1qA$8K1OXQhXj6/t6U2POfvlRtba/U7VTus6INAw2VBpJGU','e@e','USER',NULL,NULL,NULL,NULL),(39,'Sfd','$argon2id$v=19$m=4096,t=3,p=1$GEIx/+xkKImxGJeQaioCPA$d+VVHtBREB6qRq8l0iC7jSxp03wyW6fq2UXnzqy32Fk','s@s','USER',NULL,NULL,NULL,NULL),(40,'please1','$argon2id$v=19$m=4096,t=3,p=1$SQHvr+J+m5JDYqIkobNU8g$IxBXOymo0yUBU0qZxuVFGhlTTG5n+f1ZZK81Y3IKWQc','please@ww','USER','Jakub','Białek','Kwiatowa 7','508633766'),(41,'sad','$argon2id$v=19$m=4096,t=3,p=1$XYiglfKEDDm3XyFHQqnTUw$/XkOFUUaZhWJaDoMgeaqsiZVmc5NANTZ70odVKxryaw','sa@ds','USER',NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-11-07 23:05:58
