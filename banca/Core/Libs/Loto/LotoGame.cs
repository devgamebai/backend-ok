using BanCa.Libs;
using BanCa.Libs.UnifiedWallet;
using BanCa.Redis;
using Entites.General;
using LotoService;
using SimpleJSON;
using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace Loto
{
    public class LotoGame
    {
        public const string APP_ID = "xxeng";

        private static LotoGame instance;

        private static List<string> _listChat = new List<string>
        {
            "anh em đánh con gì thế?", "nay làm con 68 cho có lộc.", "dm bay mẹ 5M đen kẹp", "ơ trúng này.",
            "game nạp rút nhanh.", "dm cho số nào", "ai lộc đê", "xin tý lộc nào",
            "cầu trời cho con đổi đời", "đi bay nào", "ô zê ngon như múi mít", "tại sao ts", "bố tổ sư", "xiên đê",
            "lộc về", "thanh gấu", "solo ko", "dcm", "biết tao là ai ko", "oánh nhau đuê", "tuổi con rệp",
            "thằng con im mồm", "bố mày đây", "ra cầu rồng gặp tao", "nhảy cầu thôi", "cờ bạc là bác thằng bần",
            "ra đê....",
            "ae cứu tôi với", "chết mẹ rồi", "dcmmmmmmmmm", "xin nỗi", "đời ko như mơ", "dm cay", "bú dc chục củ thơm",
            "ai cho tôi theo với", "tinh tinh", "tiền về tiền về", "ối làng nác ơi", "chém nhau k", "rút nhanh vl",
            "vào làm ván tx nào", "hũ đê", "vcl chưa", "game mượt","ra khỏi hang thôi","chén em đi nào","giá 3 củ khoai 1 đêm","đi với em nào","ai đánh con 53 ko","49 53","66 99 con số hôm nay","lên chưa lên chưa","trúng nào trúng nào","lô ơi ơi lô","đề ơi là đê","xem em mà đánh này"
            ,"500k khu vực hà nội","a nào đi với em không","nay đề kép a e nhé","không về 22 a e chém tôi đi","đừng mà a","ô tiền về rồi ae ơi","chán...","ư ư ưu","16 nhé ae","ae cứ tin tôi đêm nay sáng nhất","tin nhà mày đêm nay sáng nhat ko","đừng có mà căng","đám chết mẹ mày giờ","0988232442 tìm một ai thương em","đầu 0 chắc chắn","đít 3 nhé","6h30 tôi là ai","trả thưởng rồi deezzzz"
            ,"sao lại ra đầu này dm","sao là sao","đcmddddd","bạn như cc"," con chó này thích chết ko","sao mà vội mà vàng","tiền tươi thóc thật","chán như con dán","đề ơi về đi","đừng như tôi","dm tối nay làm quả tiết canh giải đen","tìm gái","ng ơi hãy đến bên em","ae thấy chưa theo tôi tất thắng","vn vô địch","cờ bạc là bác thằng bằn","cứu em cứu em","để mai tính"
            ,"nay đen vậy ta","ban ban cái loz","đi về đâu","nhẩy cầu dm...","trốn nợ thôi","a ơi đừng theo em","ae hãy như tôi không cờ bạc","dm dmmmm","chịu.......","cung cấp con số lh zalo: 0925665844","ai SG đi chơi đê cờ bạc cdg","tội thằng em","muốn đập mẹ máy đen vcl","support nhanh","sao sao ccccc"
        };

        private static List<string> _listNickName = new List<string>
        {
            "vedau", "mangon", "codungchoi", "bante", "nohu33", "dangkien", "kiemtieno", "tieuvy", "vaohang", "fanmu",
            "damdang69", "bccnha", "bonbanhs", "banhanh", "vuiaa", "dethude", "derade", "xerax", "babich", "tramcu",
            "trieudo", "quangbg", "handrio", "ronaldo22", "messi", "rodddr", "bancailo", "boncailo", "bocal", "concho2",
            "meomeo", "huzzzz", "gdfgdfgd", "sangsang", "vcsssyyu", "dungchuae", "tinhyeuo", "thudongayb", "nangcuc69",
            "chongay", "muabuonroi", "dichoi", "condinhaycam", "banghoi", "namsaonam", "saocailo", "bidiloz",
            "tuongthu", "tuongdanh", "khung23", "quank", "baroday", "divaobar", "choiconnha", "langbiang", "dalatan",
            "backanday", "dan29", "xongladi", "bietroinoi", "conchim", "chimsedi", "muabuon", "chieuthu", "concunong",
            "trasua", "xanhvi", "biennhoem", "saranghe", "banca", "bentren", "lauxanhxx", "baoto", "sangcaimom",
            "bu8cu", "emlacuocsong", "hananh3nam", "choemve", "khongthude", "dethudi", "iphone15", "lacthucuocdoi"
            ,"saodem","bonphan","cuongaed","thuanht","qweqwe","ccsdds","mincung","longmebaola","sansenoiban","timbantinh","dairamau","bacsts"
        };

        private static Boolean isRun = false;


        public static LotoGame Instance
        {
            get
            {
                if (instance == null) instance = new LotoGame();
                return instance;
            }
        }

        private LobbyService Lobby;
        private NetworkServer NetworkServer;

        private List<LotoChannel> allowChannels = new List<LotoChannel>(); // empty = all
        private List<LotoGameMode> allowModes = new List<LotoGameMode>(); // empty = all

        private JSONArray playHistories = new JSONArray();

        public LotoGame()
        {
        }

        public void Dispose()
        {
            instance = null;
        }

        public void onRemovePeer(string clientId)
        {
        }

        public bool onClientNotify(BanCaServer server, string clientId, string route, JSONNode msg)
        {
            return false;
        }

        public bool onClientRequest(BanCaServer server, string clientId, int msgId, string route, JSONNode msg)
        {
            if (!isRun)
                Task.Run(sendMessBot);
            NetworkServer = server.NetworkServer;
            Lobby = server.Lobby;
            switch (route)
            {
                case "LOTO1": // play request
                {
                    var currentDate = DateTime.Now;
                    Logger.Info("onClientRequest Play request " + currentDate.Hour);
                    if ((currentDate.Hour == 18 && currentDate.Minute > 10) || currentDate.Hour >= 19)
                    {
                        var res = new JSONObject {["code"] = 301, ["msg"] = "Time request from 0h to 18h05 VN Time"};
                        NetworkServer.ResponseToClient(clientId, msgId, res);
                        return true;
                    }

                    var user = Lobby.CheckLogin(clientId, msgId);
                    if (user == null)
                        return true;

                    var number = msg["number"];
                    var mode = (LotoGameMode) msg["mode"].AsInt;
                    if (LotoSql.NeedArrayOfNumbers(mode))
                    {
                        if (!(number is JSONArray))
                        {
                            var res = new JSONObject();
                            res["code"] = 301;
                            res["msg"] = "Need array of number";
                            NetworkServer.ResponseToClient(clientId, msgId, res);
                        }
                    }
                    else
                    {
                        if (number is JSONArray)
                        {
                            var res = new JSONObject();
                            res["code"] = 301;
                            res["msg"] = "Need number";
                            NetworkServer.ResponseToClient(clientId, msgId, res);
                        }
                    }

                    var newNumber = number is JSONArray ? number.ToString() : number.Value;
                    if (!LotoSql.checkInputValid(mode, number))
                    {
                        var res = new JSONObject();
                        res["code"] = 302;
                        res["msg"] = "Input invalid";
                        NetworkServer.ResponseToClient(clientId, msgId, res);
                    }

                    long pay = msg["pay"];
                    var channel = (LotoChannel) msg["channel"].AsInt;
                    var nn = user.Nickname;
                    server.TaskRun.QueueAction(async () =>
                    {
                        long cost = (long) Math.Round(pay * await LotoSql.GetPayRate(mode, channel));
                        // Phase 5c — Loto bet debit via unified wallet.
                        long cash;
                        if (MoneyGatewayClient.IsEnabled())
                        {
                            var sid = "bc-loto-bet-" + user.UserId + "-" + mode + "-" + TimeUtil.TimeStamp;
                            bool ok = false;
                            try { ok = await MoneyGatewayClient.SettleAsync(user.UserId, -cost, sid, "WAGER_DEBIT_BANCA"); }
                            catch (Exception ex) { Logger.Error("Phase5c LOTO_PAY settle err uid=" + user.UserId + " " + ex.Message); }
                            cash = ok ? cost : -1;
                        }
                        else
                        {
                            cash = await RedisManager.IncEpicCash(user.UserId, -cost, user.Platform,
                                "lotopay:" + mode.ToString(), TransType.LOTO_PAY);
                        }
                        if (cash < 0)
                        {
                            var res = new JSONObject();
                            res["code"] = 303;
                            res["msg"] = "Not enough cash";
                            res["cost"] = cost;
                            NetworkServer.ResponseToClient(clientId, msgId, res);
                        }
                        else
                        {
                            cost = await LotoSql.AddPlayRequest(APP_ID, user.UserId.ToString(), msg["session"],
                                mode, newNumber, channel, pay);

                            var res = new JSONObject();
                            res["code"] = cost > 0 ? 200 : 302;
                            res["msg"] = cost > 0 ? "Success" : "Fail";
                            res["cash"] = cash;
                            res["cost"] = cost;
                            NetworkServer.ResponseToClient(clientId, msgId, res);

                            var response = new JSONObject();
                            response["nickname"] = nn;
                            response["mode"] = (int) mode;
                            response["channel"] = (int) channel;
                            response["number"] = number;
                            response["cost"] = cost;
                            response["time"] = TimeUtil.TimeStamp;
                            NetworkServer.PushAll("onLOTO1", response, SendMode.ReliableOrdered);

                            lock (playHistories)
                            {
                                playHistories.Add(response);
                                if (playHistories.Count > 50)
                                {
                                    playHistories.Remove(0);
                                }
                            }
                        }
                    });
                }
                    return true;
                case "LOTO2": // get pay/win rate
                {
                    User user = Lobby.CheckLogin(clientId, msgId);
                    if (user == null)
                        return true;

                    var gameMode = (LotoGameMode) msg["mode"].AsInt;
                    var channel = (LotoChannel) msg["channel"].AsInt;

                    server.TaskRun.QueueAction(async () =>
                    {
                        var payRate = await LotoSql.GetPayRate(gameMode, channel);
                        var winRate = await LotoSql.GetWinRate(gameMode, channel);
                        var res = new JSONObject();
                        res["code"] = 200;
                        res["msg"] = "Success";
                        res["payRate"] = payRate;
                        res["winRate"] = winRate;
                        NetworkServer.ResponseToClient(clientId, msgId, res);
                    });
                }
                    return true;
                case "LOTO3": // getCalculateResult
                {
                    User user = Lobby.CheckLogin(clientId, msgId);
                    if (user == null)
                        return true;

                    int session = msg["session"].AsInt;
                    server.TaskRun.QueueAction(async () =>
                    {
                        var result = await LotoSql.GetCalculateResult(session, APP_ID, user.UserId.ToString());
                        var res = new JSONObject();
                        res["code"] = 200;
                        res["msg"] = "Success";
                        res["data"] = JSON.ListObjToJson(result);
                        NetworkServer.ResponseToClient(clientId, msgId, res);
                    });
                }
                    return true;
                case "LOTO4": // getPlayRequest
                {
                    User user = Lobby.CheckLogin(clientId, msgId);
                    if (user == null)
                        return true;

                    server.TaskRun.QueueAction(async () =>
                    {
                        var result = await LotoSql.GetPlayRequest(APP_ID, user.UserId.ToString());
                        var res = new JSONObject();
                        if (result != null)
                        {
                            res["code"] = 200;
                            res["msg"] = "Success";
                            res["data"] = JSON.ListObjToJson(result);
                        }
                        else
                        {
                            res["code"] = 301;
                            res["msg"] = "Fail, too many request";
                        }

                        NetworkServer.ResponseToClient(clientId, msgId, res);
                    });
                }
                    return true;
                case "LOTO5": // getLotoResult
                {
                    User user = Lobby.CheckLogin(clientId, msgId);
                    if (user == null)
                        return true;

                    int session = msg["session"].AsInt;
                    var channel = (LotoChannel) msg["channel"].AsInt;
                    server.TaskRun.QueueAction(async () =>
                    {
                        var result = await LotoSql.GetLotoResult((LotoChannel) channel, session);
                        var res = new JSONObject();
                        res["code"] = 200;
                        res["msg"] = "Success";
                        res["data"] = result.ToJson();
                        NetworkServer.ResponseToClient(clientId, msgId, res);
                    });
                }
                    return true;
                case "LOTO6": // help
                {
                    User user = Lobby.CheckLogin(clientId, msgId);
                    if (user == null)
                        return true;

                    server.TaskRun.QueueAction(async () =>
                    {
                        var result = await LotoSql.GetGameModes();
                        var res = new JSONObject {["code"] = 200, ["msg"] = "Success", ["data"] = result};
                        NetworkServer.ResponseToClient(clientId, msgId, res);
                    });
                }
                    return true;
                case "LOTO7": // chat
                {
                    var user = Lobby.CheckLogin(clientId, msgId);
                    if (user == null)
                        return true;

                    TaskRunner.RunOnPool(() =>
                    {
                        string message = msg["msg"];
                        Chat(clientId, user.Nickname, message);
                        var response = new JSONObject();
                        response["code"] = 200;
                        response["msg"] = "Success";
                        NetworkServer.ResponseToClient(clientId, msgId, response);
                    });
                }
                    return true;
                case "LOTO8": // chat history
                {
                    var user = Lobby.CheckLogin(clientId, msgId);
                    if (user == null)
                        return true;

                    TaskRunner.RunOnPool(() =>
                    {
                        var response = new JSONObject();
                        response["code"] = 200;
                        response["msg"] = "Success";
                        response["data"] = ChatHistory(user.Nickname);
                        NetworkServer.ResponseToClient(clientId, msgId, response);
                    });
                }
                    return true;
                case "LOTO9": // allow mode and channel
                {
                    NetworkServer.ResponseToClient(clientId, msgId, GetAllowsData());
                }
                    return true;
                case "LOTO10": // recent play history
                {
                    var response = new JSONObject();
                    response["code"] = 200;
                    response["msg"] = "Success";
                    lock (playHistories)
                    {
                        response["data"] = playHistories;
                        NetworkServer.ResponseToClient(clientId, msgId, response);
                    }
                }
                    return true;
            }

            return false;
        }

        public JSONNode GetAllowsData()
        {
            var response = new JSONObject();
            var modes = new JSONArray();
            lock (allowModes)
                foreach (var item in allowModes)
                {
                    modes.Add((int) item);
                }

            var channels = new JSONArray();
            lock (allowChannels)
                foreach (var item in allowChannels)
                {
                    channels.Add((int) item);
                }

            response["code"] = 200;
            response["msg"] = "Success";
            response["channels"] = channels;
            response["modes"] = modes;
            return response;
        }

        public void SetAllowsData(JSONNode data)
        {
            if (data.HasKey("channels"))
            {
                var channels = data["channels"].AsArray;
                lock (allowChannels)
                {
                    allowChannels.Clear();
                    for (int i = 0, n = channels.Count; i < n; i++)
                    {
                        allowChannels.Add((LotoChannel) channels[i].AsInt);
                    }
                }
            }

            if (data.HasKey("modes"))
            {
                var modes = data["modes"].AsArray;
                lock (allowModes)
                {
                    allowModes.Clear();
                    for (int i = 0, n = modes.Count; i < n; i++)
                    {
                        allowModes.Add((LotoGameMode) modes[i].AsInt);
                    }
                }
            }
        }

        private class ChatMessage
        {
            public string Nickname, Message;
        }

        private LinkedList<ChatMessage> ChatHistories = new LinkedList<ChatMessage>();

        public async Task sendMessBot()
        {
            isRun = true;
            while (true)
            {
                var random = new Random();
                string nickName = _listNickName[random.Next(_listNickName.Count)];
                string mess = _listChat[random.Next(_listChat.Count)];

                Thread.Sleep(random.Next(3000));

                Chat(null, nickName, mess);
            }
        }

        public void Chat(string clientId, string nickname, string message)
        {
            lock (ChatHistories)
            {
                if (ChatHistories.Count > 50)
                {
                    var item = ChatHistories.First.Value;
                    ChatHistories.RemoveFirst();
                    item.Nickname = nickname;
                    item.Message = message;
                    ChatHistories.AddLast(item);
                }
                else
                {
                    var item = new ChatMessage();
                    item.Nickname = nickname;
                    item.Message = message;
                    ChatHistories.AddLast(item);
                }
            }

            {
                var response = new JSONObject();
                response["nickname"] = nickname;
                response["msg"] = message;
                if (clientId == null)
                {
                    NetworkServer.PushAll("onLOTO7", response, SendMode.ReliableOrdered);
                }
                else
                {
                    NetworkServer.PushToClient(clientId, "onLOTO7", response, SendMode.ReliableOrdered);
                }
            }
        }

        public JSONArray ChatHistory(string nickName)
        {
            lock (ChatHistories)
            {
                var arr = new JSONArray();
                foreach (var item in ChatHistories)
                {
                    if (nickName.Equals(item.Nickname))
                    {
                        var json = new JSONObject();
                        json["nickname"] = item.Nickname;
                        json["msg"] = item.Message;
                        arr.Add(json);
                    }
                }

                return arr;
            }
        }
    }
}