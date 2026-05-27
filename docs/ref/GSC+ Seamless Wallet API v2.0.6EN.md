# 

# 

# 

![][image1]

# 

# 

# GSC+  API

v 2.0.6

# 

# 

[**Version information	4**](#version-information)

[**1\. Summary	5**](#1.-summary)

[**2.Single wallet	5**](#2.single-wallet)

[2.1 Balance	5](#2.1-balance)

[batch\_requests	6](#batch_requests)

[Data	7](#data)

[2.2 Withdraw	8](#2.2-withdraw)

[Batch\_Requests	9](#batch_requests-1)

[Data	10](#data-1)

[2.3 Deposit	11](#2.3-deposit)

[Batch\_Requests	12](#batch_requests-2)

[Data	13](#data-2)

[2.4 Push Bet Data	14](#2.4-push-bet-data)

[**3.Operator (Operator)	17**](#3.operator-\(operator\))

[3.1 Launch Game （Launch Game）	17](#3.1-launch-game-（launch-game）)

[3.2 Wager List (Wager List)	19](#3.2-wager-list-\(wager-list\))

[3.3 Wager (Wager)	21](#3.3-wager-\(wager\))

[3.4 Game List (Game List)	23](#3.4-game-list-\(game-list\))

[3.5 Game History (Game History)	28](#3.5-game-history-\(game-history\))

[3.6 Product List (Product List)	28](#3.6-product-list-\(product-list\))

[3.7 Turn on Super Lobby	30](#3.7-turn-on-super-lobby)

[3.8 Create Free Round for Player	32](#3.8-create-free-round-for-player)

[3.9 Cancel Free Round (CancelFreeRound)	34](#3.9-cancel-free-round-\(cancelfreeround\))

[3.10 Get Player Free Round Bonus (GetPlayerFRB)	36](#3.10-get-player-free-round-bonus-\(getplayerfrb\))

[3.11 Get Game Bet Scales( GetGamesBetScales)	38](#3.11-get-game-bet-scales\(-getgamesbetscales\))

[3.12 Wallet Balance Inquiry	41](#3.12-wallet-balance-inquiry)

[**Appendix	44**](#appendix)

[Seamless wallet code	44](#seamless-wallet-code)

[Carrier Code	44](#carrier-code)

[Pagination	45](#pagination)

[Game Type	45](#game-type)

[Product Code	46](#product-code)

[Transaction (Transaction)	51](#transaction-\(transaction\))

[Wagers (Wagers)for Push Bet Data	52](#wagers-\(wagers\)for-push-bet-data)

[Wager (Wager)	53](#wager-\(wager\))

[Wager Status (Wager Status)	54](#wager-status-\(wager-status\))

[Language Code (Language Code)	55](#language-code-\(language-code\))

[Currency Code (Currency Code)	57](#currency-code-\(currency-code\))

[Transaction Action Type（Transaction Action Type）	63](#transaction-action-type（transaction-action-type）)

[Games	65](#games)

[FreeRound bet parameter settings table	66](#freeround-bet-parameter-settings-table)

# 

# Version information {#version-information}

| Date  | Version | Content |
| :---- | :---- | :---- |
| 2024/10/20 | v2.0.0 | First version released |
| 2024/11/25 | v2.0.1 | 3.4 and 3.6 without size, the default is full display without paging. |
| 2024/12/27 | v2.0.1 | Modify the **request\_time** format for **2.1, 2.2, 2.3, and 2.4**. |
| 2025/1/23 | v2.0.2  | A new **round\_id** parameter has been added to the **Transaction**. |
| 2025/4/21 | v2.0.3 | There has been an adjustment in API 2.4 Push Bet Data. The previously documented parameter **"transactions"** has been updated to **"wagers"**. |
| 2025/7/14 | v2.0.4 | Added Operator FreeRound Feature. |
| 2025/7/23 | v2.0.4 | Added Launch Game SABA quick betting component widget ID parameter |
| 2025/10/7 | v2.0.4 | Updated 3.1 Game List — added creation time parameter. Added Free Round Bet Parameter Configuration Table. |
| 2025/10/28 | v2.0.5 | Added 3.12 Wallet Balance Inquiry |
| 2025/11/4 | v2.0.5 | Optimize API 3.2 Wager List Increase the time range limit from 1 minute to 5 minutes Increase the maximum number of wagers per response from 1,000 to 5,000 |
| 2026/3/23 | v2.0.6 | Added section 3.13 Auto Deposit Added new response parameter entry\_type in 3.6 Product List API |

# 1\. **Summary** {#1.-summary}

* Contact the GSC+ window and provide the following information when requesting a proxy connection  
  * The currency the agent wishes to connect to (multiple currencies supported)  
  * Platform server IP: API, BO  
  * Agent name  
  * callback URL

*  After creating relevant information, provide the following information to the platform:  
  * Operator code \- A 4-character alphanumeric operator identification code (not case sensitive). (Agent code is a unique value for GSC+)  
  * secret\_key  
  * operator\_url   
    * GSC+： [https://staging.gsimw.com](https://staging.gsimw.com)  
    * Aurora Gaming：[https://staging-idr.pglsucs.com](https://staging-idr.pglsucs.com)

# 2\.**Single wallet** {#2.single-wallet}

* All API requests will use the content-type header of application/json  
* The time format for all requests will be a second timestamp with time zone GMT+8

## **2.1 Balance** {#2.1-balance}

**Describe**

Providers wishing to obtain the balance of a specific player will obtain it through this API.  
Operator-side API for Seamless Wallet to retrieve a specific player's balance for the provider

**EndPoint**

* **POST** `{{callback_url}}/v1/api/seamless/balance`

**Parameter**

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| batch\_requests | \[\][batch\_requests](#batch_requests) | request information |
| operator\_code | string | The unique identifier of the operator as the username to log in to the backend. |
| currency | string | refer to[Currency Code](#currency-code-\(currency-code\)) |
| sign | string | md5(operator\_code \+ request\_time \+ “getbalance” \+ secret\_key)Example: MD5(ABCD \+ 1698219740 \+ getbalance \+ XXXX) ＊Signature verification tool https://testcase.gscplusmd.com/ |
| request\_time | string | timestamp of request time(Second)。 |

### **batch\_requests** {#batch_requests}

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| member\_account | string | The unique identifier of the member in the operator(Limit 50 characters) |
| product\_code | int | Product's unique identifier. refer to[Currency Code](#currency-code-\(currency-code\)) |

**Example**

| { "batch\_requests":\[ { "member\_account":"user1", "product\_code":1002 }, { "member\_account":"user2", "product\_code":1020 }, { "member\_account":"user3", "product\_code":1009 } \], "operator\_code":"ABCD", "currency":"CNY", "sign":"369af7416deef76a9cc4f019b8559f99", "request\_time":"1694617425" } |
| :---- |

**Response**

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| data | \[\][data](#data) | data |

### **Data** {#data}

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| member\_account | string | The unique identifier of the member in the operator(Limit 50 characters) |
| product\_code | int | The unique identifier of the product. See[Product Code](#product-code) |
| balance | float64 | player balance (Support up to the fourth decimal place) |
| code | int | See [Seamless wallet code](#seamless-wallet-code) |
| message | string | response message |

**Example**

| {  "data":\[ { "member\_account":"user1", "product\_code":1002, “balance":12345, "code":0, "message":"" }, { "member\_account":"user2", "product\_code":1020, "balance":1000, "code":0, "message":"" }, { "member\_account":"user3", "product\_code":1009, "balance":1000, "code":0, "message":"" } \] } |
| :---- |

## 

## **2.2 Withdraw** {#2.2-withdraw}

**Describe**  
The API is used by Seamless Wallet on the operator side for players to place bets or similar deduction operations such as tips.

**EndPoint**

* **POST** `{{callback_url}}/v1/api/seamless/withdraw`

**Parameter**

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| batch\_requests | \[\][batch\_requests](#batch_requests-1) | request information |
| operator\_code | string | The unique identifier of the operator logged in as username bo |
| game\_type | optional | Most values are empty (used in some cases). |
| currency | string | See[Currency Code](#currency-code-\(currency-code\)) |
| sign | string | md5(operator\_code \+ request\_time \+ “withdraw” \+ secret\_key)Example: md5(ABCD+ 1698219740 \+withdraw+XXXX) ＊Signature verification tool https://testcase.gscplusmd.com/ |
| request\_time | string | timestamp of request time(Second)。 |

### **Batch\_Requests** {#batch_requests-1}

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| member\_account | string | The unique identifier of the member in the operator(Limit 50 characters) |
| product\_code | int | The unique identifier of the product. See[Product Code](#product-code) |
| game\_type | string | See[Game Type](#game-type) |
| transactions | \[\][Transaction](#transaction-\(transaction\)) | transactions |

**Example**

| { "batch\_requests":\[ { "member\_account":"user1", "Product\_code":1002, "game\_type":"POKER", "transactions":\[ { "id":"23746", "action":"bet", "wager\_code":"tZDwLV3ayzBeP4Nvwxhcti", "wager\_status":"BET", "round\_id":"95978", "channel\_code":"gscp", "amount":10, "bet\_amount":10, "valid\_bet\_amount":10, "prize\_amount":0, "tip\_amount":0, "settled\_at":0, "game\_code":"moreturkeyv10000", "round\_id":"95978", "Channel\_code":"gscp" "wager\_type": "NORMAL" } \] } \], "operator\_code":"ABCD", "game\_type": "", "currency":"CNY", "sign":"369af7416deef76a9cc4f019b8559f99", "request\_time":"1694617425" }​ |
| :---- |

**Response**

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| data | \[\][data](#data-1) | data |

### **Data** {#data-1}

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| member\_account | string | The unique identifier of the member in the operator(Limit 50 characters) |
| product\_code | int | The unique identifier of the product.See[Product Code](#product-code) |
| before\_balance | float64 | Player balance before operation (Support up to the fourth decimal place) |
| balance | float64 | Player balance after operation (Support up to the fourth decimal place) |
| code | int | See[Seamless wallet code](#seamless-wallet-code) |
| message | string | response message |

💡**Does the transaction ID exist? If the tx\_id exists in the carrier system and has been refunded before, please return the duplicate transaction**

**Example**

| { "data":\[ { "member\_account":"user1", "product\_code":1002, "before\_balance":12345, "Balance":12340, "code":0, "message":"" } \] }​​ |
| :---- |

## 

## **2.3 Deposit** {#2.3-deposit}

**Describe**  
The API is used by Seamless Wallet on the operator side for players to receive bonuses or similar incremental actions, such as giving credits based on the activities the player participates in.

**Note:**

1.  You must accept settled deposits even if there are no bets, as some providers issue additional promotional bonuses. Rejecting settlements due to a lack of bets will result in players not receiving their bonus payouts.  
2. The WBET product uses a special mechanism where winnings are not distributed via the /deposit API. Instead, the operator must handle the payout manually after receiving the /push-bet-data notification.

**EndPoint**

* POST `{{callback_url}}/v1/api/seamless/deposit`

**Parameter**

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| batch\_requests | \[\][batch\_requests](#batch_requests-2) | request information |
| operator\_code | string | The unique identifier of the operator logged in as username bo |
| currency | string | See[Product Code](#product-code) |
| sign | string | md5(operator\_code \+ request\_time \+ “deposit” \+ secret\_key)Example: md5(ABCD+ 1698219740 \+deposit+XXXX) ＊Signature verification tool https://testcase.gscplusmd.com/ |
| request\_time | string | timestamp of request time(Second)。 |

### **Batch\_Requests** {#batch_requests-2}

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| member\_account | string | The unique identifier of the member in the operator(Limit 50 characters) |
| product\_code | int | The unique identifier of the product. See[Product Code](#product-code) |
| game\_type | string | See[Game Type](#game-type) |
| transactions | \[\][Transaction](#transaction-\(transaction\)) | trade |

**Example**

| { "batch\_requests":\[ { "member\_account":"user1", "Product\_code":1002, "game\_type":"POKER", "transactions":\[ { "id":"23746", "action":"settled", "wager\_code":"tZDwLV3ayzBeP4Nvwxhcti", "wager\_status":"SETTLED", "amount":10, "bet\_amount":10, "valid\_bet\_amount":10, "prize\_amount":10, "tip\_amount":0, "settled\_at":1729134752372, "game\_code":"moreturkeyv10000", "round\_id":"95978", "Channel\_code":"gscp" "wager\_type": "NORMAL" } \] } \], "operator\_code":"ABCD", "currency":"CNY", "sign":"369af7416deef76a9cc4f019b8559f99", "request\_time":"1694617425" }​ |
| :---- |

**Response**

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| data | \[\][data](#data-2) | data |

### **Data** {#data-2}

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| member\_account | string | The unique identifier of the member in the operator(Limit 50 characters) |
| product\_code | int | The unique identifier of the product. See[Product Code](#product-code) |
| before\_balance | float64 | Player balance before operation (Support up to the fourth decimal place) |
| balance | float64 | Player balance after operation (Support up to the fourth decimal place) |
| code | int | See[Seamless wallet code](#seamless-wallet-code) |
| message | string | response message |

💡**Does the transaction ID exist? If the id exists in the carrier system and has been refunded before, please return the duplicate transaction**

**Example**

| {  "data":\[ { "member\_account":"user1", "product\_code":1002, "before\_balance":12345, "balance":12340, "code":0, "message":"" } \] }​ |
| :---- |

## **2.4 Push Bet Data** {#2.4-push-bet-data}

**Describe**  
It is a seamless wallet API on the operator side, used to synchronize all data and status of bets.

**EndPoint**

* **POST** `{{callback_url}}/v1/api/seamless/pushbetdata`

**Parameter**

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| operator\_code | string | The unique identifier of the operator, used as the background login username. |
| wagers | [wagers](#wager-\(wager\)) | wagers for this operation. |
| sign | string | md5(operator\_code \+ request\_time \+ “pushbetdata” \+ secret\_key) Example: md5(ABCD+ 1698219740+pushbetdata\+XXXX) ＊Signature verification tool https://testcase.gscplusmd.com/ |
| request\_time | string | timestamp of request time(Second)。 |

**Example**

| String parseJS eval fails { "operator\_code":"CMUT\_V2", "wagers":\[ { "member\_account":"s0350", "bet\_amount":"10", "valid\_bet\_amount":"10", "prize\_amount":"10", "tip\_amount":"0", "wager\_type":"NORMAL", "wager\_code":"tZDwLV3ayzBeP4Nvwxhcti", "wager\_status":"SETTLED", "round\_id":"95978", "channel\_code":"gscp", "game\_type":"POKER", "settled\_at":1697439181000, "created\_at":1697435181000, "payload":{  }, "product\_code":"1001", "game\_code":"1001", "currency":"CNY" }, { "member\_account":"s0351", "bet\_amount":"100", "valid\_bet\_amount":"100", "prize\_amount":"10", "tip\_amount":"0", "wager\_type":"NORMAL", "wager\_code":"txDwLV5aazBeP4evwxhcti", "wager\_status":"SETTLED", "round\_id":"95785", "game\_type":"POKER", "settled\_at":1697439181000, "created\_at":1697438182000, "payload":{  }, "product\_code":"1001", "game\_code":"1001", "currency":"CNY" } \], "sign":"369af7416deef76a9cc4f019b8559f99", "request\_time":"1694617425" } |
| :---- |

**Response**

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| code | int | See[Seamless wallet code](#seamless-wallet-code) |
| message | string | response message |

**Example**

| { "code":0, "message":"", } |
| :---- |

# **3.Operator (Operator)** {#3.operator-(operator)}

## **3.1 Launch Game （Launch Game）** {#3.1-launch-game-（launch-game）}

**EndPoint**

* **POST** `{{operator_url}}/api/operators/launch-game`

**Parameter**

| Parameter | Type | Introduce | Must |
| ----- | ----- | ----- | :---- |
| operator\_code | String | The unique identifier of the operator, used as the background login user name。 | Must |
| member\_account | String | The unique identifier of the member in the operator.(Limit 50 characters) | Must |
| password | String | The member's password in the operator's system, used to verify identity. | Must |
| nickname | String | The member's nickname displayed in the game. | No |
| currency | String | The currency used by members in the game. Make sure the currency is supported by the provider. See[Currency code](#currency-code-\(currency-code\)) | Must |
| game\_code | String | A unique identifier within the game list API given by the provider. Required if the provider supports direct play, no otherwise. | Optional, depending on provider |
| product\_code | int | The unique identifier of the product. See[Product code](#product-code) | Must |
| game\_type | String | See[Game type](#game-type) | Must |
| language\_code | String | The member's language code. See[Language code](#language-code-\(language-code\))。 | No, default is 0 |
| ip | String | The member’s IP address. | Must |
| platform | String | platform type (SABA Sports Match Link Please select Widget) | Must.Enum includes WEB, DESKTOP, and MOBILE, Widget |
| widget\_id | String | SABA Sports Quick Bet Widget ID. If a value is passed, the default card widget is obtained (staging environment classic widget ID: 1lx2FADe)[Reference Documentation](https://gsc-plus.notion.site/SABA-Sports-Speed-Betting-Components-23efc5d788a98070a8f4d60280deb930?source=copy_link) | No |
| is\_widget\_login | bool | SABA Sports Quick Bet Widget ID Login Status true \= Login mode false or not provided \= Non-login mode | No |
| sign | String | md5(request\_time \+ secret\_key \+ “launchgame” \+ operator\_code) Example: md5(1694617425XXXXlaunchgameCMUT\_V2) ＊Signature verification tool https://testcase.gscplusmd.com/ | Must |
| request\_time | int | timestamp of request time(Second)。 | Must |
| operator\_lobby\_url | string | Client site URL | Must  |

**Example**

| { "operator\_code":"CMUT\_V2", "member\_account":"s0350", "password":"e10adc3949ba59abbe56e057f20f883e", "nickname":"test123", "currency":"IDR", "game\_code":*null*, "product\_code":1001, "game\_type":"Slot", "language\_code":0, "ip":"127.0.0.1", "platform":"WEB", "sign":"977e0ad6dd5c9f953a5b7681d2fa9fb8", "request\_time":1694617425, "operator\_lobby\_url":"https://URL" } |
| :---- |

**Response**

| Parameter | Type | Introduce |
| ----- | ----- | ----- |
| Code | int | See[Carrier code](#carrier-code) |
| Message | string | error message |
| URL | string | The URL used to launch the game or provider's lobby. |
| Content | string | HTML content for displaying games from a specific provider |

**Example**

| { "code":200, "message":"", "url":"https://dev-test.spribe.io/games/launch/aviator?currency=USD\&lang=EN\&user=test9\&operator=efinity\&token=NCVKnX2cTPDDfLUfQ7UtXB" } |
| :---- |

## **3.2 Wager List** (**Wager List)** {#3.2-wager-list-(wager-list)}

**EndPoint**

* **GET** `{{operator_url}}/api/operators/wagers`

**Parameter**

| Parameter | Type | Introduce | Must |
| ----- | ----- | ----- | :---- |
| operator\_code | string | The unique identifier of the operator, used as the background login username. | Must  |
| start | int | The start time of the settlement time search range（Timestamp millisecond）。 | Must  |
| end | int | The end time of the settlement time search range（Timestampmillisecond）。 | Must (≤ 5 minutes.)  |
| offset | int | The starting record number for this capture. | No |
| size | int | The number of records fetched this time. | No (default=5000) |
| sign | String | md5(request\_time \+ secret\_key \+ “getwagers” \+ operator\_code) Example: md5(1694617425XXXXgetwagersCMUT\_V2) ＊Signature verification tool https://testcase.gscplusmd.com/ | Must  |
| request\_time | int64 | Timestamp of request time. | Must |

**Example**

https://example.com/api/operators/wagers?operator\_code=E004\&sign=xxxxxx\&request\_time=169155100?start=169155100000\&end=169155100000

**Response**

| Name  | Type |
| :---- | :---- |
| wagers | [Wager](#3.3-wager-\(wager\)) |
| pagination | Pagination |

**Example**

| { "wagers":\[ { "id":3, "code":"e8bf16ae-07c8-4663-a22e-4e441f12e65e", "member\_account":"test3", "round\_id":"95978", "currency":"USD", "provider\_id":0, "provider\_line\_id":0, "provider\_product\_id":0, "provider\_product\_oid":1138, "game\_type":"poker", "game\_code":"dice", "valid\_bet\_amount":0.10, "bet\_amount":0.10, "prize\_amount":0, "status":"BET", "payload":*null*, "settled\_at":0, "created\_at":1691149098011, "updated\_at":1691149098011 } \], "pagination":{ "size":1000, "total":1234 } } |
| :---- |

## **3.3 Wager (Wager)** {#3.3-wager-(wager)}

**EndPoint**

* **GET** `{{opeartor_url}}/api/operators/wagers/{{id/code}}`

**Parameter**

| Parameter | Type | Introduce | Must |
| ----- | ----- | ----- | :---- |
| operator\_code | string | The unique identifier of the operator, used as the background login username. | Must |
| sign | String | md5(request\_time \+ secret\_key \+ “getwager” \+ operator\_code) Example: md5(1694617425XXXXgetwagerCMUT\_V2) ＊Signature verification tool https://testcase.gscplusmd.com/ | Must |
| request\_time | int64 | Timestamp of request time. | Must |

**Request**  
https://staging.effinitymd.com/api/operators/wagers/3?operator\_code=E004\&sign=xxxxxx\&request\_time=169155100

**Response**

| Name  | Type |
| :---- | :---- |
| wager | [Wager](#wager-\(wager\)) |

**Example**

| { "wager":{ "id":3, "code":"e8bf16ae-07c8-4663-a22e-4e441f12e65e", "member\_account":"test3", "round\_id":"95978", "currency":"USD", "provider\_id":0, "provider\_line\_id":0, "provider\_product\_id":0, "provider\_product\_oid":1138, "game\_type":"poker", "game\_code":"dice", "valid\_bet\_amount":0.10, "bet\_amount":0.10, "prize\_amount":0, "status":"BET", "payload":*null*, "settled\_at":0, "created\_at":1691149098011, "updated\_at":1691149098011 } } |
| :---- |

## **3.4 Game List (Game List)** {#3.4-game-list-(game-list)}

Used to obtain all games that the operator has signed with GSC+. Only games with signed contracts will be displayed.

**EndPoint**

* **GET** `{{operator_url}}/api/operators/provider-games`

**Parameter**

| Parameter | Type | Introduce | Must |
| ----- | ----- | ----- | :---- |
| product\_code | int | The unique identifier of the product. | Must |
| operator\_code | string | The unique identifier of the operator, used as the background login username. | Must |
| game\_type | String | See[Game Type](#game-type) | No |
| sign | String | md5(request\_time \+ secret\_key \+ “gamelist” \+ operator\_code) Example: md5(1694617425XXXXgamelistCMUT\_V2) ＊Signature verification tool https://testcase.gscplusmd.com/ | Must |
| request\_time | int64 | Timestamp of request time. | Must |
| offset | int | The starting record number for this search. | No |
| size | int | The number of records retrieved this time. | No (if not included, all will be displayed) |

**Response**

| Scope | Type | Introduce |
| :---- | ----- | ----- |
| code | int | See operator code [Operator Code](#carrier-code) |
| message | string | error message |
| provider\_games | array | Reference game[Games](#games) |
| pagination | object | [Pagination](#pagination) |

**Response**

| { "code":0, "message":"", "provider\_games":\[ { "game\_code":"aviator", "game\_name":"Aviator", "game\_type":"POKER", "image\_url":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "product\_id":1, "product\_code":1138, "support\_currency":"USD", "status":"ACTIVAT" "allow\_free\_round": true "lang\_name": {                 "0": "Aviator",                 "1": "Aviator",                 "12": "Aviator",                 "2": "Aviator",                 "3": "Aviator",                 "33": "Aviator",                 "39": "Aviator",                 "4": "Aviator",                 "5": "Aviator",                 "6": "Aviator",                 "7": "Aviator",                 "9": "Aviator"}, "lang\_icon": { "0":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "1":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "12":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "2":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "3":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "33":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "39":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "4":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "5":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "6":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "7":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "9":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png"}, "created\_at": 1738570027673               }, { "game\_code":"aviator", "game\_name":"Aviator", "game\_type":"POKER", "image\_url":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "product\_id":1, "product\_code":1138, "support\_currency":"IDR", "status":"ACTIVATED" "allow\_free\_round": true "lang\_name": {                 "0": "Aviator",                 "1": "Aviator",                 "12": "Aviator",                 "2": "Aviator",                 "3": "Aviator",                 "33": "Aviator",                 "39": "Aviator",                 "4": "Aviator",                 "5": "Aviator",                 "6": "Aviator",                 "7": "Aviator",                 "9": "Aviator"}, "lang\_icon": { "0":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "1":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "12":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "2":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "3":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "33":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "39":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "4":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "5":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "6":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "7":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png", "9":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/aviator.png"}, "created\_at": 1738570027673               }, { "game\_code":"dice", "game\_name":"Dice", "game\_type":"POKER", "image\_url":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/dice.png", "product\_id":1, "product\_code":1138, "support\_currency":"USD", "status":"ACTIVATED" "allow\_free\_round": true "lang\_name": {                 "0": "Dice",                 "1": "Dice",                 "12": "Dice",                 "2": "Dice",                 "3": "Dice",                 "33": "Dice",                 "39": Dice",                 "4": "Dice",                 "5": "Dice",                 "6": "Dice",                 "7": "Dice",                 "9": "Dice"}, "lang\_icon": { "0":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "1":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "12":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "2":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "3":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "33":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "39":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "4":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "5":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "6":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "7":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "9":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png"}, "created\_at": 1738570027673               }, { "game\_code":"dice", "game\_name":"Dice", "game\_type":"POKER", "image\_url":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/dice.png", "product\_id":1, "product\_code":1138, "support\_currency":"IDR", "status":"ACTIVATED" "allow\_free\_round": true "lang\_name": {                 "0": "Dice",                 "1": "Dice",                 "12": "Dice",                 "2": "Dice",                 "3": "Dice",                 "33": "Dice",                 "39": Dice",                 "4": "Dice",                 "5": "Dice",                 "6": "Dice",                 "7": "Dice",                 "9": "Dice"}, "lang\_icon": { "0":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "1":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "12":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "2":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "3":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "33":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "39":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "4":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "5":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "6":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "7":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png", "9":"https://images.gscplusmd.com/statics/staging/images/games/1/POKER/Dice.png"}}, "created\_at": 1738570027673               }, \], "pagination":{ "size":10, "offset":0, "total":"2000" } } |
| :---- |

## **3.5 Game History (Game History)** {#3.5-game-history-(game-history)}

**EndPoint**

* **GET** `{{operator_url}}/api/operators/{wager_code}/game-history`

**Parameter**

| Parameter | Type | Introduce | Must |
| ----- | ----- | ----- | :---- |
| operator\_code | string | The operator's unique identifier, used as the backend login username. | Must |
| sign | String | md5(request time \+ secret\_key \+ "gamehistory" \+ operator code) example : md5(1694617425XXXXproductlistCMUT\_V2) ＊Signature verification tool https://testcase.gscplusmd.com/ | Must |
| request\_time | int64 | Timestamp of request time. | Must |

**Response**

| { "content":"https://" }  |
| :---- |

*＊The PG Soft product returns content in HTML format, and you will need to handle the display adjustment on your end.*

## **3.6 Product List (Product List)** {#3.6-product-list-(product-list)}

**EndPoint**

* **GET** `{{operator_url}}/api/operators/available-products`

**Parameter**

| Parameter | Type | Introduce | Must |
| ----- | ----- | ----- | :---- |
| operator\_code | string | The unique identifier of the operator, used as the background login username. | Must |
| sign | String | md5(request time \+ secret\_key \+ "product list" \+ operator code) example : md5(1694617425XXXXproductlistCMUT\_V2) ＊Signature verification tool https://testcase.gscplusmd.com/ | No |
| request\_time | int64 | Timestamp of request time. | Must |
| offset | int | The starting record number obtained this time | No |
| size | int | The number of records obtained this time | No (if not included, all will be displayed) |

**Response**

| Parameter | Type | Introduce |
| :---- | :---- | :---- |
| provider | string | Provider's name |
| currency | string | currency code, see[Currency code](#currency-code-\(currency-code\)) |
| status | string | product status |
| provider\_id | int | Provider's unique identifier |
| product\_id | int | product unique identifier |
| product\_code | int | OID unique identifier of the product, see[Product code](#product-code) |
| game\_type | string  | Game types in Seamless Games, see[Game type](#game-type) |
| product\_name | string | Provider's product name |
| entry\_type | int | entry\_type indicates how the game should be launched 1 \= Direct game access via game\_code 2 \= Lobby access only (no game\_code required) |

**Example**

| \[   {     "provider": "JILI",     "currency": "IDR2",     "status": "MAINTAINED",     "provider\_id": 2,     "product\_id": 1091,     "product\_code": 1011,     "game\_type": "SLOT",     "product\_name": "jili",     "entry\_type": 1   },   {     "provider": "SBO",     "currency": "IDR2",     "status": "ACTIVATED",     "provider\_id": 2,     "product\_id": 1189,     "product\_code": 1012,     "game\_type": "SPORT\_BOOK",     "product\_name": "sbo",     "entry\_type": 2   } \] |
| :---- |

## **3.7 Turn on Super Lobby** {#3.7-turn-on-super-lobby}

***\*Please first ensure that you have completed the integration and docking of the single wallet API. Once completed, please contact us if you would like to connect with Super Lobby.***

**EndPoint**

* **POST** `{{operator_url}}/superlobby/launch`

**Parameter**

| Parameter | Type | Introduce | Must |
| ----- | ----- | ----- | :---- |
| operator\_code | String | The unique identifier of the operator, used as the background login username. | Must  |
| member\_account | String | The unique identifier of the member in the operator. | Must |
| nickname | String | The member's nickname displayed in the game. | Must |
| currency | String | The currency used by members in the game. Make sure the currency is supported by the provider. See[Currency Code](#currency-code-\(currency-code\))。 | Must  |
| language\_code | String | The member's language code. See[Language Code](#language-code-\(language-code\))。 | No, default is 0 |
| platform | String | Platform Type | Must.Enum includes WEB, DESKTOP, and MOBILE. |
| sign | String | md5(request\_time \+ secret\_key \+ “launchsuperlobby” \+ operator\_code) Example: md5(1694617425XXXXlaunchsuperlobbyCMUT\_V2) ＊Signature verification tool https://testcase.gscplusmd.com/ | Must  |
| request\_time | int | timestamp of request time（Second）。 | Must  |
| type  | int | Open lobby type  | No, default 0: SUPER LOBBY 1: AURORA LIVE |
| operator\_lobby\_url | string | Client site URL | Must  |

**Example**

| { "operator\_code":"CMUT\_V2", "member\_account":"s0350", "currency":"IDR", "language\_code":0, "platform":"WEB", "sign":"fc607f037e87fbf9cfac419423e43144", "request\_time":1694617425, "type":0, "operator\_lobby\_url":"https://test.com" } |
| :---- |

**Respond**

| Parameter | Type | Introduce |
| :---- | :---- | :---- |
| url | string | The URL used to launch Super Lobby/Aurora LIVE. |

**Example**

|  { "url":"https://lobby.gsimw.com/\#/home?t=iQhpYErFQBm6Wqu8WL5abc" } |
| :---- |

## **3.8 Create Free Round for Player** {#3.8-create-free-round-for-player}

Please ensure the Single Wallet API integration has been completed. If you wish to integrate the FreeRound feature, please contact us.

**Endpoint**

* POST {{operator\_url}}/api/operators/create-free-round

**Parameters**

| Parameter | Type | Description | Required |
| :---- | :---- | :---- | :---- |
| operator\_code | String | Unique identifier of the operator (same as backend login username) | Yes |
| member\_account | String | Unique member account identifier in the operator system (max 50 characters) | Yes |
| currency | String | Player’s in-game currency. Make sure it is supported by the provider. [See currency codes](#currency-code-\(currency-code\)). | Yes |
| product\_code | Integer | Product unique ID. See [product code lis](#product-code)t. | Yes |
| game\_type | String | See game types. | Yes |
| start\_at | Integer | Free round start time (Unix timestamp) | Yes |
| end\_at | Integer | Free round end time (Unix timestamp) | Yes |
| rounds | Integer | Number of free rounds | Yes |
| sign | String | Signature: md5(request\_time \+ secret\_key \+ "createfreeround" \+ operator\_code)＊Signature verification tool https://testcase.gscplusmd.com | Yes |
| request\_time | Integer | Request timestamp (in seconds) | Yes |
| channel\_code | String | Channel code, e.g., gscp | Yes |
| game\_list | Array | List of games and corresponding bet values | Yes |
| game\_list\[\].gameId | String | Unique game identifier | Yes |
| game\_list\[\].betValues | Array | List of betting values. Refer to section [3.11 GetGamesBetScales](#3.11-get-game-bet-scales\(-getgamesbetscales\)) | Yes |
| betValues.betPerLine | Float | Bet per line amount.Please refer to the [FreeRound bet parameter settings table](#freeround-bet-parameter-settings-table). | Yes (choose one, mutually exclusive with `betValues.totalBetAmount`) |
| betValues.totalBetAmount | float | otal bet amount. Please refer to the [FreeRound bet parameter settings table](#freeround-bet-parameter-settings-table). | Yes(choose one, mutually exclusive with `betValues.betPerLine`) |
| betValues.currency | String | [Currency](#currency-code-\(currency-code\)) used in the game  | Yes |

**Example** 

| {     "operator\_code": "CMUT\_V2",     "member\_account": "s1023",     "currency": "IDR",     "product\_code": 1006,     "game\_type": "SLOT",     "start\_at": 1752105600,     "end\_at": 1752940800,     "rounds": 10,     "game\_list": \[         {             "gameId": "vs20sugarrush",             "betValues": \[                 {                     "betPerLine": 0.05,                    "totalBetAmount": 0,                     "currency": "IDR"                 }             \]         }     \],     "sign": "36227604e220ea0e2c8e806736f58401",     "request\_time": 1752054762,     "channel\_code": "gscp" }  |
| :---- |

**Response**

| Parameter | Type | SS |
| ----- | ----- | ----- |
| bonus\_code | String | Unique FreeRound ID, can be referenced in wallet transaction via `Payload.bonus_code` |

**Example** 

| { "bonus\_code":"7dfc41a2-960e-4fd8-b812-6ebe9b194777" } |
| :---- |

## **3.9 Cancel Free Round (CancelFreeRound)** {#3.9-cancel-free-round-(cancelfreeround)}

***lease ensure the Single Wallet API integration has been completed. If you wish to integrate the FreeRound feature, please contact us.***

***Use this API to cancel a previously created FreeRound campaign by specifying the `bonus_code`.***

**EndPoint**

* **POST** `{{operator_url}}/api/operators/cancel-free-round`

**Parameters**

| Parameter | Type | Description | Required |
| :---- | :---- | :---- | :---- |
| operator\_code | String | Operator’s unique ID (backend login username) | Yes |
| currency | String | Player’s game currency. Ensure it is supported by the [provider](#currency-code-\(currency-code\)) | Yes |
| product\_code | Integer | [Product unique ID](#product-code) | Yes |
| bonus\_code | String | Unique FreeRound ID [(from section 3.8)](#3.8-create-free-round-for-player) | Yes |
| sign | String | Signature: md5(request\_time \+ secret\_key \+ "cancelfreeround" \+ operator\_code)＊Signature verification tool https://testcase.gscplusmd.com | Yes |
| request\_time | Integer | Request timestamp (in seconds) | Yes |
| channel\_code | String | VVVVVChannel code, e.g., gscp | Yes |

**Example**

| {     "operator\_code": "CMUT\_V2",     "currency": "IDR",     "product\_code": 1006,     "game\_type": "SLOT",     "bonus\_code": "10868087-1ef9-4b9d-bd62-7226cb26e7f4",     "sign": "01ee0b21e2e127fa6df08ef4fc5129c5",     "request\_time": 1752054762,     "channel\_code": "gscp" } |
| :---- |

**Response**

| Parameter | type  | Description |
| :---- | :---- | :---- |
| bonus\_code | string | FreeRound unique order number |

**例子**

| { "bonus\_code":"7dfc41a2-960e-4fd8-b812-6ebe9b194777" } |
| :---- |

## **3.10 Get Player Free Round Bonus (GetPlayerFRB)** {#3.10-get-player-free-round-bonus-(getplayerfrb)}

Please ensure the Single Wallet API integration has been completed. If you wish to integrate the FreeRound feature, please contact us.

This API retrieves a player's current FreeRound bonus information using their account and related identifiers.

 **EndPoint**

* **GET** `{{operator_url}}/api/operators/get-player-frb`

**Parameter**

| Parameter | Type | Description | Required |
| :---- | :---- | :---- | :---- |
| operator\_code | String | Operator’s unique identifier | Yes |
| member\_account | String | Member’s unique identifier (max 50 characters) | Yes |
| currency | String | Player’s [currency](#currency-code-\(currency-code\)) | Yes |
| product\_code | Integer | [Product ID](#product-code) | Yes |
| game\_type | String | Game type | Yes |
| sign | String | Signature: md5(request\_time \+ secret\_key \+ "getplayersfrb" \+ operator\_code)＊Signature verification tool https://testcase.gscplusmd.com | Yes |
| request\_time | Integer | Timestamp in seconds | Yes |
| channel\_code | String | Channel code, e.g., gscp | Yes |

**Example**

https://example.com/api/operators/get-player-frb?operator\_code=CMUT\_V2\&member\_account=s1023\&currency=IDR\&product\_code=1006\&sign=22711668739a77102f4a5b54cd64d233\&request\_time=1752054762\&channel\_code=gscp\&game\_type=SLOT

**回应**

| Parameter | Type | Description |
| :---- | :---- | :---- |
| bonuses | Array | List of Free Round bonus entries |
| bonuses.currency | String | Currency used by the player in-game. Ensure it is supported by the provider. See [currency codes](#currency-code-\(currency-code\)). |
| bonuses.gameIDList | String | List of unique game identifiers |
| bonuses.rounds | int | Total number of Free Rounds granted |
| bonuses.roundsPlayed | int | Number of Free Rounds that have been used |
| bonuses.bonus\_code | String | Unique FreeRound order ID. Refer to section [3.8 Create Free Round](#3.8-create-free-round-for-player) |
| bonuses.expirationDate | String | The time when the free round expires (format: YYYY-MM-DD HH:mm). |

**Response**

| {     "code": 0,     "message": "",     "bonuses": \[         {             "currency": "IDR",             "gameIDList": \[                 "vs20sugarrush",            \],             "rounds": 10,             "roundsPlayed": 0,             "bonus\_code": "7dfc41a2-960e-4fd8-b812-6ebe9b194777",             "expirationDate": "2025-07-19 16:00"         }     \] }  |
| :---- |

## **3.11 Get Game Bet Scales( GetGamesBetScales)** {#3.11-get-game-bet-scales(-getgamesbetscales)}

Please ensure the Single Wallet API integration has been completed. If you wish to integrate the FreeRound feature, please contact us.

Use this API to query supported bet configurations per game and currency.

**EndPoint**

* **GET** `{{operator_url}}/api/operators/get-bet-scales`

**Parameters**

| Parameter | Type | Description | Required |
| :---- | :---- | :---- | :---- |
| operator\_code | String | Operator’s unique identifier | Yes |
| currency | String | [Currency](#currency-code-\(currency-code\)) used in game. Must be supported by provider | Yes |
| product\_code | Integer | [Product unique ID](#product-code) | Yes |
| game\_type | String | [Game type](#game-type) | Yes |
| bet\_game\_list | String | Comma-separated game IDs (max 50). Commas must be URL encoded (%2C) | Yes |
| sign | String | Signature: md5(request\_time \+ secret\_key \+ "getbetscales" \+ operator\_code)＊Signature verification tool https://testcase.gscplusmd.com | Yes |
| request\_time | Integer | Timestamp in seconds | Yes |
| channel\_code | String | Channel code, e.g., gscp | Yes |

**Example**

https://example.com/api/operators/get-bet-scales?operator\_code=CMUT\_V2\&currency=IDR\&product\_code=1006\&bet\_game\_list=vs20olympgold\&sign=0b98fd6db161704c6a476fbd1ac7d0fe\&request\_time=1752054762\&channel\_code=gscp\&game\_type=SLOT

**Response**

| Parameter | Type | Description |
| :---- | :---- | :---- |
| betScales | Array | List of game bet scale configurations |
| betScales.gameID | String | Unique identifier of the game |
| betScales.betScaleList | Array | Currency-based bet scale configurations for the game |
| betScaleList.currency | String | Currency used by the player in-game. Ensure it is supported by the provider. [See currency codes.](#currency-code-\(currency-code\)) |
| betScaleList.betPerLineScales | float | Array of bet amounts per line, sorted in ascending order(**Display Condition**: Applicable only to products that use the “bet per line” [mode](#freeround-bet-parameter-settings-table).) |
| betScaleList.totalBetScales | float | Array of total bet amount options (**Display Condition**: Applicable only to products that use the “total bet amount” [mode](#freeround-bet-parameter-settings-table).) |

Response

|  {     "code": 0,     "message": "",     "betScales": \[         {             "gameID": "vs20olympgold",             "betScaleList": \[                 {                     "currency": "IDR",                     "betPerLineScales": \[                         10,                         20,                         30,                         40,                         50,                         60,                         80,                        100,                        150,                        200,                        250,                        300,                        350,                       400,                       450,                       500,                       750,                       1000,                        1500,                       2000,                       2500,                       3000,                       4000,                       4500,                       5000,                        7500,                       10000,                       15000,                       20000,                       25000,                       30000,                        35000,                       40000,                       45000,                       50000,                       60000                     \],                     "totalBetScales": \[\]                 },             \]         },    \] } |
| :---- |

## **3.12 Wallet Balance Inquiry** {#3.12-wallet-balance-inquiry}

Retrieve the current wallet balance for the currencies contracted under the operator.

**EndPoint**

* **GET `{{operator_url}}/api/operators/wallet-balance`**

**Parameters**

| Parameter | Type | Description | Required |
| :---- | :---- | :---- | :---- |
| operator\_code | string | The unique identifier of the operator; used as one of the signature parameters. | yes |
| sign | string | md5(request\_time \+ secret\_key \+ "getwalletcurrencies" \+ operator\_code)Example： md5(1761195600123 \+ secret\_key \+ "getwalletcurrencies" \+ GSC1) ＊Signature verification tool https://testcase.gscplusmd.com | yes |
| request\_time | int64 | Request timestamp (milliseconds). | yes |

**Example**

https://{{operator\_url}}/api/operators/wallet-balance?operator\_code=GSC1\&sign=xxxxxxxx\&request\_time=1761195600123

**Response**

| Parameter | Type | Description |
| :---- | :---- | :---- |
| code | int | Status code (0 \= success) |
| message | string | Response message |
| data.operator\_code | string | Unique identifier of the operator, used as the back-office login username. |
| data.is\_credit | bool | Whether it is credit mode (true \= credit, false \= buy-in mode) |
| data.currencies\[\].currency | string | Currency code |
| data.currencies\[\].current\_balance | number | Current balance, displayed up to 4 decimal places |
| data.currencies\[\].updated\_at | string | Last updated time (Unix timestamp in milliseconds) |

**Example**

| {     "code": 0,     "message": "Success",     "data": {         "operator\_code": "GSC1",         "is\_credit": false,         "currencies": \[             {                 "currency": "MYR",                 "current\_balance": 12000.5000,                 "updated\_at": "1761532461134"             },             {                 "currency": "IDR",                 "current\_balance": 4500000.2500,                 "updated\_at": "1761532461134"             },             {                 "currency": "VND2",                 "current\_balance": 350000.0000,                 "updated\_at": "1761532461134"             },             {                 "currency": "THB",                 "current\_balance": 87000.0000,                 "updated\_at": "1761532461134"             },             {                 "currency": "USD",                 "current\_balance": 950.0000,                 "updated\_at": "1761532461134"             }         \]     } }           |
| :---- |

## **3.13 Auto Deposit**

This API allows the operator to generate a **deposit URL** via an API request for agent wallet top-up.

Once the URL is opened, the user will be redirected to a **third-party payment page**. After the payment is completed, the system will automatically credit the balance.

⚠️ **Note:** Auto Deposit feature must be enabled by our team before use.

**Endpoint**

* **POST {{operator\_url}}/api/operators/recharge/order**

**Request Parameters**

| Parameter | Type | Required | Description |
| ----- | ----- | ----- | ----- |
| operator\_code | string | Yes | Operator code |
| payment\_currency | string | Yes | Payment currency (e.g. USDT). Currently only USDT is supported |
| deposit\_currency | string | Yes | Deposit currency (e.g. BRL). Please refer to the supported currency list |
| amount | number | Yes | Deposit amount (unit: payment currency). Supported range depends on currency configuration |
| request\_time | string | Yes | Unix timestamp (in seconds) |
| sign | string | Yes | MD5 signature: `MD5(request_time + secret_key + "autodeposit" + operator_code)` |

**Example:**

| {     "operator\_code": "GSC1",     "payment\_currency": "USDT",     "deposit\_currency": "BRL",     "amount": 1000,     "request\_time": "1762688877",     "sign": "1a2b3c4d5e6f7g8h9i0j" } |
| :---- |

**Request Example**

| Parameter | Type | Description |
| ----- | ----- | ----- |
| code | int | Status code (0 \= success) |
| message | string | Response message |
| url | string | Generated deposit URL. Redirects to third-party payment page (valid for 900 seconds) |

**Response Example**

| {     "code": 0,     "message": "",     "url": "https://secured.gspay.online/usdt?id=27572\&key=d8f4bfb766ab422084f4e351ec2cbe09" } |
| :---- |

# Appendix {#appendix}

### **Seamless wallet code** {#seamless-wallet-code}

| Code | Describe |
| :---- | :---- |
| 0 | success |
| 999 | Internal Server Error |
| 1000 | API member does not exist |
| 1001 | API member balance is insufficient |
| 1002 | API proxy key error |
| 1003 | Duplicate API transactions |
| 1004 | API signature is invalid |
| 1005 | API not getting game list |
| 1006 | API bet does not exist |
| 2000 | API product is under maintenance |

### **Carrier Code** {#carrier-code}

| Code | Describe |
| :---- | :---- |
| 200 | success |
| 999 | Internal Server Error |
| 10002 | Invalid parameter |

### **Pagination** {#pagination}

| Name | Type | Describe |
| :---- | :---- | :---- |
| size | integer | The number of records you get |
| total | integer | Total number of records in a given time interval |
| offset | Integer | Start record number of this retrieval. |

### 

### **Game Type** {#game-type}

| Code | Describe |
| :---- | :---- |
| SLOT | Slot |
| LIVE\_CASINO | Live Casino |
| SPORT\_BOOK | Sport Book |
| VIRTUAL\_SPORT | Virtual Sport |
| LOTTERY | Lottery |
| QIPAI | Qipai |
| P2P | P2P |
| FISHING | Fishing |
| COCK\_FIGHTING | Cock Fighting |
| BONUS | Bonus |
| ESPORT | ESport |
| POKER | Poker |
| OTHERS | Others |
| LIVE\_CASINO\_PREMIUM | Live Casino  Premium |

### **Product Code** {#product-code}

| ID | Code |
| ----- | :---- |
| 1009 | CQ9 |
| 1020 | WM Casino |
| 1022 | Sexy\_gaming  |
| 1033 | SV388cockfighting  |
| 1050 | PlayStar |
| 1055 | MrSlotty |
| 1056 | TrueLab |
| 1058 | BGaming |
| 1060 | Volt Entertainment |
| 1062 | Fazi |
| 1064 | Netgame |
| 1065 | Kiron |
| 1067 | RedRake |
| 1070 | Booongo |
| 1080 | Venus  |
| 1097 | FuntaGaming |
| 1098 | Felix |
| 1101 | ZeusPlay |
| 1102 | KAGaming |
| 1138 | Spribe |
| 1139 | Fastspin  |
| 1149 | AI Live Casino |
| 1148 | WOW Gaming |
| 1006 | Pragmatic Play |
| 1011 | Play Tech |
| 1016 | YeeBet |
| 1091 | Jili tcg  |
| 1018 | live\_22 |
| 1012 | SBO |
| 1052 | DreamGaming |
| 1085 | JDB |
| 1049 | Evoplay |
| 1153 | Hacksaw |
| 1154 | Bigpot |
| 1157 | IMoon |
| 1161 | TADA |
| 1166 | NO LIMIT CITY (ASIA) |
| 1167 | BIG TIME GAMING (ASIA) |
| 1172 | WORLD ENTERTAINMENT |
| 1183 | FB SPORT |
| 1152 | 1XBET |
| 1168 | Netent（ASIA） |
| 1169 | Red Tiger（ASIA） |
| 1040 | WBET |
| 1184 | RICH88 |
| 1079 | Fachai |
| 1046 | IBC-SABA |
| 1185 | SA Gaming |
| 1002 | Evolution Gaming（ASIA） |
| 1038 | King855/CT855 |
| 1191 | King855/CT855 |
| 1007 | PG Soft  |
| 1156 | Betfair |
| 1158 | Pascal Gaming |
| 1004 | BigGaming |
| 1160 | EPICWIN |
| 1163 | NOVOMATIC |
| 1162 | Octoplay |
| 1165 | aviatrix |
| 1164 | DIGITAIN |
| 1170 | smartsoft |
| 1171 | FIABLE GAMES(Product delisted) |
| 1173 | Evolution (LATAM) |
| 1174 | Netent (LATAM) |
| 1175 | Red Tiger (LATAM) |
| 1176 | no limit city (LATAM) |
| 1177 | big time gaming(LATAM) |
| 1115 | BOOMING GAMES |
| 1186 | ENDORPHINA |
| 1187 | WINFINITY |
| 1192 | AMIGO GAMING |
| 1197 | Habanero |
| 1194 | PRETTY GAMING |
| 1203 |  **PlayAce** |
| 1221 | SPADE GAMING |
| 1204 | ADVANTPLAY |
| 1222 | TF Gaming |
| 1220 | ASTAR |
| 1205 | AMBPOKER |
| 1206 | SlotXO(AMB) |
| 1207 | PG SOFT (AMB) |
| 1225 | JOKER |
| 1229 | PANDA SPORTS |
| 1223 | ALLBET |
| 1224 | GEMINI |
| 1230 | BETSOLUTIONS |
| 1231 | SIMPLE PLAY |
| 1232 | QQKENO |
| 1233 | NEX4D |
| 1227 | UG |
| 1237 | KAIYUANGAMING |
| 1239 | Hotdog |
| 1228 | CMD |
| 1238 | KA Gaming (Direct Line) |
| 1007 | PG Soft (THB) |
| 1091 | JE:JILI |
| 1240 | TCG SEA LOTTO |
| 1241 | TCG LOTTO |
| 1244 | BETBY |
| 1242 | PLAYTECH(Q6) |
| 1235 | IM SPORTS |
| 1236 | IM ESPORTS |
| 1250 | UUSlots |
| 1258 | LE GAMING |
| 1251 | Micro Gaming |
| 1249 | 3SING |
| 1253 | Gaming Panda |
| 1261 | ATG |
| 1255 | DRAGOON SOFT |
| 1256 | YGR |
| 1257 | BOLE GAMING |
| 1259 | 5G |
| 1252 | Q Tech |
| 1263 | Oriental Gaming |
| 1247 | DB LOTTERY |
| 1254 | AVIATOR |
| 1262 | BNG |
| 1274 | EVOPLAY YFG |
| 1266 | VG棋牌 |
| 1260 | BGAMING |
| 1268 | MegaWin |
| 1269 | MT LIVE |
| 1273 | PNG |
| 1264 | Vimplay |
| 1271 | GX WICKETS |
| 1243 | BTI |
| 1270 | LUCKYSPORTS |
| 1275 | YGGDRASIL YGG |
| 1276 | AVATAR UX YGG |
| 1277 | WINFAST YGG |
| 1278 | RELAX GAMING YGG |
| 1281 | SABAPLAY |
| 1284 | ROYAL SLOT GAMING |
| 1290 | YEEBET2 |
| 1272 | TURBO |

### 

### 

### 

### 

### 

### 

### 

### 

### 

### 

### **Transaction (Transaction)** {#transaction-(transaction)}

| Parameter | Type | Describe |
| :---- | :---- | :---- |
|  |  |  |
| id | string | Transaction ID |
| action | string | Transaction action type,  Withdraw: Supported action types include BET,TIP,ROLLBACK,ADJUSTMENT, etc. Deposit: Supported action types include SETTLED, JACKPOT, BONUS, PROMO, LEADERBOARD, ROLLBACK, CANCEL ,ADJUSTMENT, etc. Push Bet Data: Does not involve balance changes, only used for synchronizing bet data and status. reference[Transaction Action Type](#transaction-action-type（transaction-action-type）) |
| amount | string | The amount needs to be changed to the player's wallet. Positive value indicates recharge, negative value indicates deduction. |
| currency | string | See[Currency Code](#currency-code-\(currency-code\))。 |
| valid\_bet\_amount | stirng | Effective bet amount |
| bet\_amount | string | Bet amount |
| prized\_amount | string | Payout amount |
| tip\_amount | string | Tip amount |
| wager\_code | string | Bet ID in Seamless |
| wager\_status | string | bet status,reference bet status |
| round\_id | string | Provider's turn ID in the game |
| payload | JSON | Provider transaction details |
| settled\_at | int64 | Reward time(timestamp) |
| product\_code | int | Unique identifier for the product, see product code (Appears only with 2.4 PushBet) |
| game\_code | string | Game Code (Required if the provider supports direct game entry; otherwise, null.) |

### **Wagers (Wagers)for Push Bet Data** {#wagers-(wagers)for-push-bet-data}

| Parameter | Type | Describe |
| :---- | :---- | :---- |
| member\_account | string | The account number of the member in the operator |
| bet\_amount | decimal | Total bet amount. |
| valid\_bet\_amount | decimal | The effective bet amount actually deducted. May differ from bet amount |
| prize\_amount | decimal | The player's winning amount. If the player lost, it is 0\. |
| tip\_amount | string | Tip amount |
| wager\_type | string | Bet mark (bet=NORMALl , manual free spin=FREEROUND) |
| wager\_code | string | Code for bet (for checking) |
| wager\_status | string | bet status, see bet [status](#wager-status-\(wager-status\)) |
| round\_id | string | Provider's turn ID in the game |
| channel\_code | string | Channel code, such as gscp |
| game\_type | string  | Game types in Seamless, see[Game Type](#game-type) |
| settled\_at | int64 | Time when bets are settled |
| created\_at | int64 | The time the bet was created |
| payload | json | Some information about the provider |
| product\_code | int | The OID unique identification of the product, see[Product Code](#product-code) |
| game\_code | string | Provider's game code (Required if the provider supports direct game entry; otherwise, null.) |
| currency | string | Currency code, see[Currency Code](#currency-code-\(currency-code\)) |

### **Wager (Wager)** {#wager-(wager)}

| Parameter | Type | Describe |
| :---- | :---- | :---- |
| id | id | ID of the bet (used for association) |
| wager\_code | string | Code for bet (is wager\_code for checking) |
| agent\_code | string | The code for the agent in the operator |
| member\_account | string | The account number of the member in the operator |
| round\_id | string | Provider's turn ID in the game |
| currency | string | Currency code, see[Currency Code](#currency-code-\(currency-code\)) |
| provider\_id | int | Provider's unique identifier |
| provider\_line\_id | int | Product line unique identifier |
| povider\_product\_id | int | Product unique identifier |
| product\_code | int | The OID unique identification of the product, see[Product Code](#product-code) |
| product\_name | int | Product Name |
| wager\_type | string | Bet mark (bet=NORMALl , manual free spin=FREEROUND) |
| game\_type | string  | Game types in Seamless, see[Game Type](#game-type) |
| game\_code | string | Provider's game code (Required if the provider supports direct game entry; otherwise, null.) |
| valid\_bet\_amount | decimal | The effective bet amount actually deducted. May differ from bet amount |
| bet\_amount | decimal | Total bet amount. |
| prize\_amount | decimal | The player's winning amount. If the player lost, it is 0\. |
| status | string | bet status, see bet statu |
| payload | json | Some information about the provider |
| settled\_at | int64 | Time when bets are settled |
| created\_at | int64 | The time the bet was created |
| updated\_at | int64 | When bets are updated |

### **Wager Status (Wager Status)** {#wager-status-(wager-status)}

| State | Describe |
| ----- | ----- |
| BET | The bet is in the betting stage |
| BONUS | Multiple prizes are distributed in the same round |
| SETTLED | Bet settled |
| RESETTLED | Bets have been resettled |
| VOID | Bets are void |

💡**NOTE: In some edge cases, transfer requests fitaction type “RESETTLED” ，It is possible to cause a player's balance to become negative, such as a fractional change, which often occurs in sports betting games.**

### **Language Code (Language Code)** {#language-code-(language-code)}

## 

| State | Describe |
| ----- | ----- |
| 0 | English |
| 1 | Traditional Chinese |
| 2 | Simplify Chinese |
| 3 | Thai |
| 4 | Indonesia |
| 5 | Japanese |
| 6 | Korea |
| 7 | Vietnamese |
| 8 | Deutsch |
| 9 | Espanol |
| 10 | Francais |
| 11 | Russia |
| 12 | Portuguese |
| 13 | Burmese |
| 14 | Danish |
| 15 | Finnish |
| 16 | Italian |
| 17 | Dutch |
| 18 | Norwegian |
| 19 | Polish |
| 20 | Romanian |
| 21 | Swedish |
| 22 | Turkish |
| 23 | Bulgarian |
| 24 | Czech |
| 25 | Greek |
| 26 | Hungarian |
| 27 | Brazilian Portugese |
| 28 | Slovak |
| 29 | Georgian |
| 30 | Latvian |
| 31 | Ukrainian |
| 32 | Estonian |
| 33 | Filipino |
| 34 | Cambodian |
| 35 | Lao |
| 36 | Malay |
| 37 | Cantonese |
| 38 | Tamil |
| 39 | Hindi |
| 40 | European Spanish |
| 41 | Azerbaijani |
| 42 | Brunei Darussalam |
| 43 | Croatian |

### **Currency Code (Currency Code)** {#currency-code-(currency-code)}

Note: When the currency ratio is 1:1000, operators are required to perform the conversion themselves before responding with the balances. This is due to certain provider requirements, as the currency value is too small.

| 幣別（Currency） | 比例 |
| :---: | :---: |
| AED | 1:1 |
| AFN | 1:1 |
| ALL | 1:1 |
| AMD | 1:1 |
| ANG | 1:1 |
| AOA | 1:1 |
| ARS | 1:1 |
| AUD | 1:1 |
| AWG | 1:1 |
| AZN | 1:1 |
| BAM | 1:1 |
| BBD | 1:1 |
| BDT | 1:1 |
| BDT2 | 1:1000 |
| BGN | 1:1 |
| BHD | 1:1 |
| BIF | 1:1 |
| BMD | 1:1 |
| BND | 1:1 |
| BOB | 1:1 |
| BRL | 1:1 |
| BRL2 | 1:1000 |
| BSD | 1:1 |
| BTC | 1:1 |
| BTN | 1:1 |
| BWP | 1:1 |
| BYN | 1:1 |
| BZD | 1:1 |
| CAD | 1:1 |
| CDF | 1:1 |
| CDF2 | 1:1000 |
| CHF | 1:1 |
| CLF | 1:1 |
| CLP | 1:1 |
| CNY | 1:1 |
| CNY2 | 1:1000 |
| COP | 1:1 |
| COP2 | 1:1000 |
| CRC | 1:1 |
| CSD | 1:1 |
| CUC | 1:1 |
| CUP | 1:1 |
| CVE | 1:1 |
| CZK | 1:1 |
| DJF | 1:1 |
| DKK | 1:1 |
| DOGE | 1:1 |
| DOP | 1:1 |
| DZD | 1:1 |
| EGP | 1:1 |
| ERN | 1:1 |
| ETB | 1:1 |
| ETH | 1:1 |
| EUR | 1:1 |
| EUR2 | 1:1000 |
| FJD | 1:1 |
| FKP | 1:1 |
| FRF | 1:1 |
| FTN | 1:1 |
| GBP | 1:1 |
| GC | 1:1 |
| GEL | 1:1 |
| GGP | 1:1 |
| GHS | 1:1 |
| GIP | 1:1 |
| GMD | 1:1 |
| GNF | 1:1 |
| GTQ | 1:1 |
| GYD | 1:1 |
| HKD | 1:1 |
| HKD2 | 1:1000 |
| HNL | 1:1 |
| HRK | 1:1 |
| HTG | 1:1 |
| HUF | 1:1 |
| IDR | 1:1 |
| IDR2 | 1:1000 |
| IDR3 | 1:100 |
| ILS | 1:1 |
| IMP | 1:1 |
| INR | 1:1 |
| INR2 | 1:1000 |
| IQD | 1:1 |
| IRR | 1:1 |
| IRR2 | 1:1000 |
| ISK | 1:1 |
| JEP | 1:1 |
| JMD | 1:1 |
| JOD | 1:1 |
| JPY | 1:1 |
| JPY2 | 1:1000 |
| KES | 1:1 |
| KGS | 1:1 |
| KHR | 1:1 |
| KHR2 | 1:1000 |
| KRW | 1:1 |
| KRW2 | 1:1000 |
| KSH | 1:1 |
| KWD | 1:1 |
| KZT | 1:1 |
| LAK | 1:1 |
| LAK2 | 1:1000 |
| LBP | 1:1 |
| LBP2 | 1:1000 |
| LKR | 1:1 |
| LRD | 1:1 |
| LSL | 1:1 |
| LTC | 1:1 |
| LYD | 1:1 |
| MAD | 1:1 |
| MAD2 | 1:1000 |
| MBTC | 1:1 |
| MDL | 1:1 |
| METH | 1:1 |
| MGA | 1:1 |
| MKD | 1:1 |
| MMK | 1:1 |
| MMK2 | 1:1000 |
| MMK3 | 1:100 |
| MNT | 1:1 |
| MNT2 | 1:1000 |
| MOP | 1:1 |
| MRU | 1:1 |
| MVR | 1:1 |
| MWK | 1:1 |
| MXBT | 1:1 |
| MXN | 1:1 |
| MXN2 | 1:1000 |
| MYR | 1:1 |
| MYR2 | 1:1000 |
| MYR3 | 1:100 |
| MZN | 1:1 |
| NAD | 1:1 |
| NGN | 1:1 |
| NGN2 | 1:1000 |
| NIO | 1:1 |
| NOK | 1:1 |
| NOT | 1:1 |
| NPR | 1:1 |
| NPR2 | 1:1000 |
| NTD | 1:1 |
| NZD | 1:1 |
| OMR | 1:1 |
| PAB | 1:1 |
| PEN | 1:1 |
| PGK | 1:1 |
| PHP | 1:1 |
| PHP2 | 1:1000 |
| PKR | 1:1 |
| PKR2 | 1:1000 |
| PLN | 1:1 |
| PTI | 1:1 |
| PTV | 1:1 |
| PYG | 1:1 |
| PYG | 1:1 |
| PYG2 | 1:1000 |
| QAR | 1:1 |
| RON | 1:1 |
| RSD | 1:1 |
| RUB | 1:1 |
| RWF | 1:1 |
| SAR | 1:1 |
| SBD | 1:1 |
| SC | 1:1 |
| SCR | 1:1 |
| SDG | 1:1 |
| SEK | 1:1 |
| SGD | 1:1 |
| SGD2 | 1:1000 |
| SHP | 1:1 |
| SLL | 1:1 |
| SOS | 1:1 |
| SRD | 1:1 |
| SSP | 1:1 |
| STD | 1:1 |
| STN | 1:1 |
| SVC | 1:1 |
| SYP | 1:1 |
| SZL | 1:1 |
| THB | 1:1 |
| THB2 | 1:1000 |
| TJS | 1:1 |
| TMT | 1:1 |
| TND | 1:1 |
| TON | 1:1 |
| TOP | 1:1 |
| TRY | 1:1 |
| TRY2 | 1:1000 |
| TTD | 1:1 |
| TWD | 1:1 |
| TWD2 | 1:1000 |
| TWD5 | 130:1 |
| TZS | 1:1 |
| TZS2 | 1:1000 |
| UBTC | 1:1 |
| UGX | 1:1 |
| UGX2 | 1:1000 |
| USD | 1:1 |
| USD2 | 1:1000 |
| USDC | 1:1 |
| USDT | 1:1 |
| USDT2 | 1:1000 |
| UXBT | 1:1 |
| UYU | 1:1 |
| UZS | 1:1 |
| UZS2 | 1:1000 |
| VES | 1:1 |
| VND | 1:1 |
| VND2 | 1:1000 |
| VND3 | 1:100 |
| VUV | 1:1 |
| WST | 1:1 |
| XAF | 1:1 |
| XCD | 1:1 |
| XDR | 1:1 |
| XOF | 1:1 |
| XPF | 1:1 |
| YER | 1:1 |
| ZAR | 1:1 |
| ZMW | 1:1 |
| ZWL | 1:1 |

### **Transaction Action Type（Transaction Action Type）** {#transaction-action-type（transaction-action-type）}

| Action Code | Describe |
| ----- | ----- |
| BET | The transaction type for placing bets. The operator shall deduct the amount from the player's wallet. |
| FREEBET | Bonus deal type for free bets. Operators should increase the amount of players' wallets. |
| SETTLED | Bonus transaction type for win or draw. Operators should increase the amount of players' wallets. |
| ROLLBACK | Type of transaction to cancel bets and/or wins. When the amount is positive, the operator should increase the amount in the player's wallet, otherwise it should be deducted. You need to confirm that the bet exists and is a SETTLE |
| CANCEL | Cancel bet transaction type. Operators should increase the amount of players' wallets. You need to confirm that the bet exists and is a BET |
| ADJUSTMENT | The type of transaction that adjusts the bet amount. When the amount is positive, the operator should increase the amount in the player's wallet, otherwise it should be deducted. |
| JACKPOT | Bonus transaction type for the jackpot. Operators should increase the amount of players' wallets. |
| BONUS | The type of transaction on which the bonus is placed. Operators should increase the amount of players' wallets. |
| TIP | The type of transaction in which the tip is placed. The operator shall deduct the amount from the player's wallet. |
| PROMO | The transaction type of event reward. Operators should increase the amount of players' wallets. |
| LEADERBOARD | The transaction type rewarded by the leaderboard event. Operators should increase the amount of players' wallets. |
| BET\_PRESERVE | A type of transaction that reserves an amount of money before placing a bet, in case the player's balance is insufficient for subsequent bets. The operator shall deduct the amount from the player's wallet. |
| PRESERVE\_REFUND | The transaction type that refunds the reserved amount. Operators should increase the amount of players' wallets. |

### **Games** {#games}

| Description | Type | Description |
| :---- | :---- | :---- |
| game\_code | string | Game code, used for launch game |
| game\_name | string | Game name |
| game\_type | string | Game type, see [Game Type](#game-type) |
| product\_id | int32 |  |
| product\_code | int32 | Id for each provider product, see [Product Code](#product-code) |
| status | string | Current game status DEACTIVATED: Game is deactivated. Unable to launch ACTIVATED: Game is valid to launch MAINTAINED: game is under maintained. |
| support\_currency | string | Support currency, related to currency that operator signed with GSC+. |
| allow\_free\_round | int | Whether manual Free Round are supported |
| lang\_name | string | Display game names in multiple languages (default to English).[Language Code](#language-code-\(language-code\)) |
| lang\_icon | string | Display localized game entry image URL (default to English image)[Language Code](#language-code-\(language-code\)) |
| created\_at | Unix Timestamp (milliseconds) | Creation time |

### FreeRound bet parameter settings table {#freeround-bet-parameter-settings-table}

| Code | Product Name | Parameter Used | Notes |
| ----- | ----- | ----- | ----- |
| 1006 | Pragmatic Play | betValues.betPerLine | The game adopts the “bet per line” mode. Must return **betPerLine**, and leave **totalBetAmount** empty. |
| 1148 | WOW Gaming | betValues.totalBetAmount | The game adopts the “total bet amount” mode. Must return **totalBetAmount**, and leave **betPerLine** empty. |

### 

### Automatic recharge currency list

| Currency | Minimum amount | at | Maximum amount |
| :---: | :---: | :---: | :---: |
| BRL | 2745 | \~ | 274500 |
| AUD | 765 | \~ | 7650 |
| BDT | 61295 | \~ | 612950 |
| BDT2 | 61.295 | \~ | 612.95 |
| CNY | 3585 | \~ | 35850 |
| CNY2 | 3.585 | \~ | 35.85 |
| HKD | 3925 | \~ | 39250 |
| HKD2 | 3.925 | \~ | 39.25 |
| IDR | 8117950 | \~ | 81179500 |
| IDR2 | 8117.95 | \~ | 81179.5 |
| INR | 42765 | \~ | 427650 |
| INR2 | 42.765 | \~ | 427.65 |
| KRW | 682450 | \~ | 6824500 |
| KRW2 | 682.45 | \~ | 6824.5 |
| MMK | 1049625 | \~ | 10496250 |
| MMK2 | 1049.625 | \~ | 10496.25 |
| MMK3 | 10496.25 | \~ | 104962.5 |
| MYR | 2100 | \~ | 21000 |
| MYR2 | 2.1 | \~ | 21 |
| PHP | 28170 | \~ | 281700 |
| PHP2 | 28.17 | \~ | 281.7 |
| SGD | 635 | \~ | 6350 |
| THB | 16210 | \~ | 162100 |
| THB2 | 16.21 | \~ | 162.1 |
| USDT | 500 | \~ | 5000 |
| USDT2 | 0.5 | \~ | 5 |
| VND | 13065080 | \~ | 130650800 |
| VND2 | 13065.08 | \~ | 130650.8 |

[image1]: <data:image/png;base64,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>