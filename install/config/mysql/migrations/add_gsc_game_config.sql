-- GSC+ Game Service Center configuration
-- Loaded by ThirdPartyLoad.getGscConfig() via game_config WHERE name = 'gsc_game'
INSERT INTO vinplay.game_config (name, value, platform)
VALUES ('gsc_game', '{"operator_code":"G7A1","operatorUrl":"https://staging.gsimw.com","secret_key":"abYVbCrLT2VwpASotZGmCT","username":"SUNKR","password":"Qwer1234","currency":"IDR","exchangeRate":1.0,"tax":0,"operatorLobbyUrl":"https://staging-play.sunkr.bet"}', 'all')
ON DUPLICATE KEY UPDATE value = VALUES(value);
