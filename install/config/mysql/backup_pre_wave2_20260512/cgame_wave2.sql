
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
DROP TABLE IF EXISTS `card`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `card` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `amount` int DEFAULT NULL,
  `pin` varchar(125) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `serial` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint(1) DEFAULT '0' COMMENT '0:normal, 1:used',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` varchar(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` int DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `updated_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `pin` (`pin`,`serial`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `card` WRITE;
/*!40000 ALTER TABLE `card` DISABLE KEYS */;
/*!40000 ALTER TABLE `card` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `daily`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily` (
  `date` date NOT NULL,
  `total_user_active` int DEFAULT '0',
  `total_user_active_android` int DEFAULT '0',
  `total_user_active_ios` int DEFAULT '0',
  `total_user_new` int DEFAULT '0' COMMENT 'new user reg',
  `total_device_new` int DEFAULT '0',
  `total_device_new_android` int DEFAULT NULL,
  `total_device_new_ios` int DEFAULT '0',
  `total_paying` int DEFAULT '0',
  `total_paying_new` int DEFAULT '0',
  `total_paying_new_android` int DEFAULT '0',
  `total_paying_new_ios` int DEFAULT '0',
  `total_cash_in` int DEFAULT '0',
  `total_cash_in_android` int DEFAULT '0',
  `total_cash_in_ios` int DEFAULT '0',
  `total_cash_in_card` int DEFAULT '0',
  `total_cash_in_iap` int DEFAULT '0',
  `total_cash_out` int DEFAULT '0',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `daily` WRITE;
/*!40000 ALTER TABLE `daily` DISABLE KEYS */;
/*!40000 ALTER TABLE `daily` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `daily_cp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_cp` (
  `cp` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date` date NOT NULL,
  `total_user_active` int DEFAULT '0',
  `total_user_new` int DEFAULT '0' COMMENT 'new user reg',
  `total_device_new` int DEFAULT '0',
  `total_paying` int DEFAULT '0',
  `total_paying_new` int DEFAULT '0',
  `total_cash_in` int DEFAULT '0',
  `total_cash_in_android` int DEFAULT '0',
  `total_cash_in_ios` int DEFAULT '0',
  `total_cash_in_card` int DEFAULT '0',
  `total_cash_in_iap` int DEFAULT '0',
  `total_cash_out` int DEFAULT '0',
  UNIQUE KEY `date` (`date`,`cp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `daily_cp` WRITE;
/*!40000 ALTER TABLE `daily_cp` DISABLE KEYS */;
/*!40000 ALTER TABLE `daily_cp` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `daily_game`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `daily_game` (
  `date` date DEFAULT NULL,
  `fish_cash_in` int DEFAULT '0',
  `fish_cash_out` int DEFAULT '0',
  `solo_cash_in` int DEFAULT '0',
  `solo_cash_out` int DEFAULT '0',
  `slot_cash_in` int DEFAULT '0',
  `slot_cash_out` int DEFAULT '0',
  `taixiu_cash_in` int DEFAULT '0',
  `taixiu_cash_out` int DEFAULT '0',
  `updated_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `date` (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `daily_game` WRITE;
/*!40000 ALTER TABLE `daily_game` DISABLE KEYS */;
/*!40000 ALTER TABLE `daily_game` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `minigame`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `minigame` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `device_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `os_version` varchar(25) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `platform` enum('android','ios') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `minigame` WRITE;
/*!40000 ALTER TABLE `minigame` DISABLE KEYS */;
/*!40000 ALTER TABLE `minigame` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

