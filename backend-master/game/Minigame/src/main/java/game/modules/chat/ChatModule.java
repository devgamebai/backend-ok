/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.core.BZEventParam
 *  bitzero.server.core.BZEventType
 *  bitzero.server.core.IBZEvent
 *  bitzero.server.core.IBZEventListener
 *  bitzero.server.core.IBZEventParam
 *  bitzero.server.core.IBZEventType
 *  bitzero.server.entities.User
 *  bitzero.server.exceptions.BZException
 *  bitzero.server.extensions.BZExtension
 *  bitzero.server.extensions.BaseClientRequestHandler
 *  bitzero.server.extensions.data.BaseMsg
 *  bitzero.server.extensions.data.DataCmd
 *  com.vinplay.dal.service.ChatLobbyService
 *  com.vinplay.dal.service.impl.ChatLobbyServiceImpl
 *  com.vinplay.usercore.service.UserService
 *  com.vinplay.usercore.service.impl.UserServiceImpl
 *  com.vinplay.vbee.common.utils.DateTimeUtils
 *  org.apache.log4j.Logger
 *  org.json.simple.JSONArray
 *  org.json.simple.JSONObject
 */
package game.modules.chat;

import bitzero.server.BitZeroServer;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.core.IBZEvent;
import bitzero.server.core.IBZEventListener;
import bitzero.server.core.IBZEventParam;
import bitzero.server.core.IBZEventType;
import bitzero.server.entities.User;
import bitzero.server.exceptions.BZException;
import bitzero.server.extensions.BZExtension;
import bitzero.server.extensions.BaseClientRequestHandler;
import bitzero.server.extensions.data.BaseMsg;
import bitzero.server.extensions.data.DataCmd;
import com.vinplay.dal.service.ChatLobbyService;
import com.vinplay.dal.service.impl.ChatLobbyServiceImpl;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.config.VBeePath;
import com.vinplay.vbee.common.utils.DateTimeUtils;
import game.modules.chat.cmd.rev.ChatCmd;
import game.modules.chat.cmd.send.ChatInfoMsg;
import game.modules.chat.cmd.send.ChatMsg;
import game.modules.chat.entities.ChatEntry;
import game.modules.minigame.TaiXiuModule;
import game.utils.ConfigGame;
import game.utils.ServerUtil;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ChatModule
extends BaseClientRequestHandler {
    private static final int MAX_LOG_CHAT = 20;
    private ChatLobbyService chatService = new ChatLobbyServiceImpl();
    private List<ChatEntry> entries = new ArrayList<ChatEntry>();
    private Set<User> users = new HashSet<User>();
    private UserService userService = new UserServiceImpl();
    private List<String> listChat = new ArrayList<String>();
    private List<String> listBotChat = new ArrayList<>();
    private Logger logger = Logger.getLogger((String)"BlockChat");
    private static String basePath = VBeePath.basePath;
    private final Runnable ChatBot = new ChatBot();



    public void init() {
        super.init();
        this.loadData();
        this.getParentExtension().addEventListener((IBZEventType)BZEventType.USER_DISCONNECT, (IBZEventListener)this);
        BitZeroServer.getInstance().getTaskScheduler().scheduleAtFixedRate(this.ChatBot, 10,8, TimeUnit.SECONDS);

    }

    public void loadData() {
        try {
            String entry;
            // list chat
            BufferedReader br = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(basePath.concat("config/list_chat.txt")), "UTF8"));
            while ((entry = br.readLine()) != null) {
                this.listChat.add(entry);
            }
            br.close();

            // list bot chat
            br = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(basePath.concat("config/bots.txt")), "UTF8"));
            while ((entry = br.readLine()) != null) {
                this.listBotChat.add(entry);
            }
            br.close();

        }
        catch (Exception entry) {
            entry.printStackTrace();
        }
    }

    public void handleServerEvent(IBZEvent ibzevent) throws BZException {
        if (ibzevent.getType() == BZEventType.USER_DISCONNECT) {
            User user = (User)ibzevent.getParameter((IBZEventParam)BZEventParam.USER);
            this.userDis(user);
        }
    }

    private void userDis(User user) {
        this.unsubscribe(user);
    }

    public void handleClientRequest(User user, DataCmd dataCmd) {
        switch (dataCmd.getId()) {
            case 18001: {
                this.subscribe(user);
                break;
            }
            case 18002: {
                this.unsubscribe(user);
                break;
            }
            case 18000: {
                this.chat(user, dataCmd);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void subscribe(User user) {
        Set<User> set;
        Set<User> set2 = set = this.users;
        synchronized (set2) {
            this.users.add(user);
        }
        this.sendChatLobbyInfo(user);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void unsubscribe(User user) {
        Set<User> set;
        Set<User> set2 = set = this.users;
        synchronized (set2) {
            this.users.remove(user);
        }
    }

    private void chat(User user, DataCmd dataCmd) {
        int daiLy = this.getStatusDaiLy(user);
        ChatCmd cmd = new ChatCmd(dataCmd);
        String username = user.getName();
        if (this.allowUserChat(user.getName(), daiLy) && !this.containBadword(username, cmd.message)) {
            if (daiLy == 100) {
                username = "Admin";
            }
            this.chat(username, cmd.message);
        } else {
            ChatMsg msg = new ChatMsg();
            msg.Error = 2;
            this.send((BaseMsg)msg, user);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void chat(String username, String content) {
        Set<User> set;
        ChatEntry newEntry = new ChatEntry(username, content);
        ChatMsg msg = new ChatMsg();
        msg.nickname = username;
        msg.mesasge = content;
        Set<User> set2 = set = this.users;
        synchronized (set2) {
            for (User u : this.users) {
                if (u == null) continue;
                ServerUtil.sendMsgToUser((BaseMsg)msg, u);
            }
        }
        this.logChat(newEntry);
    }

    public void chatBot(String nickname, String content) throws InterruptedException {
        Thread.sleep(new Random().nextInt(7000));
        ChatEntry newEntry = new ChatEntry(nickname, content);
        ChatMsg msg = new ChatMsg();
        msg.nickname = nickname;
        msg.mesasge = content;
        Set<User> set = this.users;
        synchronized (set) {
            for (User u : this.users) {
                if (u == null) continue;
                ServerUtil.sendMsgToUser((BaseMsg)msg, u);
            }
        }
        this.logChat(newEntry);
    }

    private final class ChatBot
            implements Runnable {
        private ChatBot() {
        }

        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                ChatModule.this.chatBot(GetRandomBot(),GetRandomMessageChat());
            }
            catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public String GetRandomBot(){
        Random rand = new Random();
        return listBotChat.get(rand.nextInt(listBotChat.size()));
    }

    public String GetRandomMessageChat(){
        Random rand = new Random();
        return listChat.get(rand.nextInt(listChat.size()));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void logChat(ChatEntry newEntry) {
        List<ChatEntry> list;
        List<ChatEntry> list2 = list = this.entries;
        synchronized (list2) {
            this.entries.add(newEntry);
            while (this.entries.size() > MAX_LOG_CHAT) {
                this.entries.remove(0);
            }
        }
    }

    private boolean containBadword(String username, String content) {
        if (ConfigGame.checkBadword(content)) {
            this.logger.debug((DateTimeUtils.getCurrentTime() + "\t" + username + " \t " + content));
            this.chatService.banChatUser(username, (long)(ConfigGame.TIME_BLOCK_CHAT * 60 * 1000));
            return true;
        }
        return false;
    }

    private void sendChatLobbyInfo(User user) {
        JSONArray arr = new JSONArray();
        for (ChatEntry entry : this.entries) {
            arr.add(entry.toJson());
        }
        String str = arr.toString();
        ChatInfoMsg chatInfoMsg = new ChatInfoMsg();
        chatInfoMsg.msg = str;
        chatInfoMsg.minVipPointRequire = (byte)ConfigGame.getIntValue("chat_min_vp_require", 20);
        chatInfoMsg.timeUnBan = this.chatService.getBanTime(user.getName());
        chatInfoMsg.userType = (byte)this.getStatusDaiLy(user);
        this.send((BaseMsg)chatInfoMsg, user);
    }

    private boolean allowUserChat(String username, int daiLy) {
        long timeUnBan;
        if (daiLy == 100) {
            return true;
        }
        boolean allow = true;
        int minVPRequire = ConfigGame.getIntValue("chat_min_vp_require", 20);
        int vipPointSave = this.userService.getVipPointSave(username);
        if (vipPointSave < minVPRequire) {
            allow = false;
        }
        if (0 < daiLy && daiLy < 100) {
            allow = false;
        }
        if ((timeUnBan = this.chatService.getBanTime(username)) != 0L) {
            allow = false;
        }
        return allow;
    }

    private int getStatusDaiLy(User user) {
        String status = (String)user.getProperty("dai_ly");
        int daiLy = 0;
        if (status != null && !status.isEmpty()) {
            daiLy = Integer.parseInt(status);
        }
        return daiLy;
    }
}

