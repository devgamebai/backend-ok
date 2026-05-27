package com.vinplay;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class HazelcastTest {

    public static void main(String[] args) {
        /*List<String> users = new ArrayList<String>();
        users.add("dailic1hungvv");
        CommonUtils.clearHazelcastMap("users", users);*/
        long a = HazelcastTest.getMoneyUserCache("anhyeu", "vin");
        System.out.println(a);
    }

    public static long getMoneyUserCache(String nickname, String moneyType) {
        ArrayList<String> address = new ArrayList<String>();
        address.add("127.0.0.1:5701");
        HazelcastClientFactory.init(address, "dev", "52ZqZJ278l3uH391Z");
        HazelcastInstance client = HazelcastClientFactory.getInstance();
        IMap<String, UserModel> userMap = client.getMap("users");
        if (userMap.containsKey((Object)nickname)) {
            UserCacheModel user = (UserCacheModel)userMap.get((Object)nickname);
            return user.getMoney(moneyType);
        }
        //instance.shutdown();
        return 0L;
    }
}
