-- Replace master bank list (vinplay.banks) with Korean banks.
-- Source: 3 places reading this table — /bankaccount CMS dropdown (c=8820),
-- c=3011 CreateDeposit company_bank response, and c=3003 SubmitUserBank picker.
-- Target: KRW market, all 3 surfaces show the same Korean bank list.

DELETE FROM vinplay.banks;
ALTER TABLE vinplay.banks AUTO_INCREMENT = 1;

INSERT INTO vinplay.banks (bank_name, code, logo, status, add_by, create_date, update_date) VALUES
  ('KB Kookmin Bank',                                        'KB',       '', 1, 'system', NOW(), NOW()),
  ('Shinhan Bank',                                           'SHINHAN',  '', 1, 'system', NOW(), NOW()),
  ('Woori Bank',                                             'WOORI',    '', 1, 'system', NOW(), NOW()),
  ('KEB Hana Bank',                                          'HANA',     '', 1, 'system', NOW(), NOW()),
  ('NH NongHyup Bank',                                       'NH',       '', 1, 'system', NOW(), NOW()),
  ('IBK Industrial Bank of Korea',                           'IBK',      '', 1, 'system', NOW(), NOW()),
  ('KakaoBank',                                              'KAKAO',    '', 1, 'system', NOW(), NOW()),
  ('K Bank',                                                 'KBANK',    '', 1, 'system', NOW(), NOW()),
  ('Toss Bank',                                              'TOSS',     '', 1, 'system', NOW(), NOW()),
  ('SC First Bank',                                          'SC',       '', 1, 'system', NOW(), NOW()),
  ('Citibank Korea',                                         'CITI',     '', 1, 'system', NOW(), NOW()),
  ('Busan Bank',                                             'BUSAN',    '', 1, 'system', NOW(), NOW()),
  ('Daegu Bank',                                             'DAEGU',    '', 1, 'system', NOW(), NOW()),
  ('Gwangju Bank',                                           'GWANGJU',  '', 1, 'system', NOW(), NOW()),
  ('Kyongnam Bank',                                          'KYONGNAM', '', 1, 'system', NOW(), NOW()),
  ('Jeonbuk Bank',                                           'JEONBUK',  '', 1, 'system', NOW(), NOW()),
  ('Jeju Bank',                                              'JEJU',     '', 1, 'system', NOW(), NOW()),
  ('Suhyup Bank',                                            'SUHYUP',   '', 1, 'system', NOW(), NOW()),
  ('MG Community Credit',                                    'MG',       '', 1, 'system', NOW(), NOW()),
  ('Korean Federation of Community Credit Cooperatives',     'SHINHYUP', '', 1, 'system', NOW(), NOW()),
  ('Post Office Bank',                                       'POST',     '', 1, 'system', NOW(), NOW());
