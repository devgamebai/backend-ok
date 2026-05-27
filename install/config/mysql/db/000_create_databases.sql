CREATE DATABASE IF NOT EXISTS vinplay CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS vinplay_minigame CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS vinplay_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS vinplay_gamebai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cgame CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON vinplay.* TO 'sunwinkr_user'@'%';
GRANT ALL PRIVILEGES ON vinplay_minigame.* TO 'sunwinkr_user'@'%';
GRANT ALL PRIVILEGES ON vinplay_admin.* TO 'sunwinkr_user'@'%';
GRANT ALL PRIVILEGES ON vinplay_gamebai.* TO 'sunwinkr_user'@'%';
GRANT ALL PRIVILEGES ON cgame.* TO 'sunwinkr_user'@'%';
FLUSH PRIVILEGES;
