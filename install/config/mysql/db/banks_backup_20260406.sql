USE vinplay;
-- MySQL dump 10.13  Distrib 8.0.45, for Linux (x86_64)
--
-- Host: localhost    Database: vinplay
-- ------------------------------------------------------
-- Server version	8.0.45

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
-- Table structure for table `banks`
--

DROP TABLE IF EXISTS `banks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `banks` (
  `id` int NOT NULL AUTO_INCREMENT,
  `bank_name` varchar(150) DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `create_date` varchar(120) DEFAULT NULL,
  `update_date` varchar(120) DEFAULT NULL,
  `code` varchar(50) DEFAULT NULL,
  `logo` varchar(255) DEFAULT NULL,
  `add_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `banks`
--

LOCK TABLES `banks` WRITE;
/*!40000 ALTER TABLE `banks` DISABLE KEYS */;
INSERT INTO `banks` VALUES (1,'KB Kookmin Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','KB','','system'),(2,'Shinhan Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','SHINHAN','','system'),(3,'Woori Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','WOORI','','system'),(4,'KEB Hana Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','HANA','','system'),(5,'NH NongHyup Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','NH','','system'),(6,'IBK Industrial Bank of Korea',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','IBK','','system'),(7,'KakaoBank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','KAKAO','','system'),(8,'K Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','KBANK','','system'),(9,'Toss Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','TOSS','','system'),(10,'SC First Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','SC','','system'),(11,'Citibank Korea',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','CITI','','system'),(12,'Busan Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','BUSAN','','system'),(13,'Daegu Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','DAEGU','','system'),(14,'Gwangju Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','GWANGJU','','system'),(15,'Kyongnam Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','KYONGNAM','','system'),(16,'Jeonbuk Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','JEONBUK','','system'),(17,'Jeju Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','JEJU','','system'),(18,'Suhyup Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','SUHYUP','','system'),(19,'MG Community Credit',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','MG','','system'),(20,'Korean Federation of Community Credit Cooperatives',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','SHINHYUP','','system'),(21,'Post Office Bank',1,'2026-04-05 10:13:32','2026-04-05 10:13:32','POST','','system');
/*!40000 ALTER TABLE `banks` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-06  8:41:13
