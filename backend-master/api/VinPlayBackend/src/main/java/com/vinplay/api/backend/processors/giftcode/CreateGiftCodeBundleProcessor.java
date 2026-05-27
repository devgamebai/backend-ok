package com.vinplay.api.backend.processors.giftcode;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.dal.dao.EventDAO;
import com.vinplay.dal.dao.impl.EventDAOImpl;
import com.vinplay.dal.entities.giftcode.GiftCodeBundleModel;
import com.vinplay.giftcode.GiftCodeModel;
import com.vinplay.giftcode.GiftCodeUtil;
import com.vinplay.payment.utils.Constant;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.messages.BaseMessage;
import com.vinplay.vbee.common.messages.LogMoneyUserMessage;
import com.vinplay.vbee.common.messages.MoneyMessageInMinigame;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.BaseResponse;
import com.vinplay.vbee.common.messagebus.MessageBusFactory;
import com.vinplay.vbee.common.rmq.RMQApi;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class CreateGiftCodeBundleProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String) "backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();

        String name = request.getParameter("bn");
        String amount = request.getParameter("am");
        String type = request.getParameter("type");
        String quantity = request.getParameter("qt");
        String startTime = request.getParameter("st");
        String endTime = request.getParameter("et");
        String created_by = request.getParameter("cb");
        String nick_name = request.getParameter("nn");
        String event = request.getParameter("ev");
        int resource = Integer.parseInt(request.getParameter("rs"));

        UserServiceImpl userService = new UserServiceImpl();
        UserModel userModel = null;
        try {
            userModel = userService.getUserByNickName(nick_name);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (userModel == null) {
            return BaseResponse.error(Constant.ERROR_PARAM, "Agent code can not empty");
        }

        if (created_by == null || created_by.isEmpty()) {
            return BaseResponse.error("-1", "cb: Thiếu người tạo");
        }

        int money = 0;
        try {
            money = Integer.parseInt(amount);
            if (money <= 0) {
                return BaseResponse.error("-1", "am: Giá trị giftcode cần > 0");
            } else if (money > 7000000) {
                return BaseResponse.error("-1", "am: Giá trị giftcode tối đa là 7 triệu Win");
            }
        } catch (Exception e) {
            return BaseResponse.error("-1", "am: Giá trị giftcode phải là định dạng số");
        }
        try {
            int quantityTemp = Integer.parseInt(quantity);
            if (quantityTemp <= 0) {
                return BaseResponse.error("-1", "am: Số lượng giftcode phải > 0");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        // Check daily con du balance ko
        int totalMoney = 0;
        try {
            totalMoney = Integer.parseInt(amount) * Integer.parseInt(quantity);
            if (resource == 0 && totalMoney > userModel.getVin()) {
                return BaseResponse.error("-1", "am: Tổng Giá trị giftcode vượt quá số dư tài khoản");
            }
            if (resource == 1 && totalMoney > userModel.getGift_total()) {
                return BaseResponse.error("-1", "am: Tổng Giá trị giftcode vượt quá số dư giftcode");
            }
        } catch (Exception e) {
            return BaseResponse.error("-1", "am: Kiểm tra số dư giftcode lỗi");
        }

        Timestamp startDate, endDate;
        if (endTime.compareTo(startTime) <= 0) {
            return BaseResponse.error("-1", "et: hạn gift code cần lớn hơn thời điểm bắt đầu sử dụng");
        } else {
            try {
                long sDate = Long.parseLong(startTime);
                startDate = new Timestamp(sDate);
            } catch (NumberFormatException e) {
                startDate = new Timestamp(new Date().getTime());
            }

            try {
                long eDate = Long.parseLong(endTime);
                endDate = new Timestamp(eDate);

                if (endDate.compareTo(new Timestamp(new Date().getTime())) <= 0) {
                    return BaseResponse.error("-1", "et: hạn gift code cần sau thời điểm hiện tại");
                }
            } catch (NumberFormatException e) {
                //            default expire is 30 day from to day
                long time_bonus = 30L * 24 * 60 * 60 * 1000;
                endDate = new Timestamp(new Date().getTime() + time_bonus);
            }
        }

        try {
            if (resource == 0) {
                HazelcastInstance client = HazelcastClientFactory.getInstance();
                IMap<String, UserModel> userMap = client.getMap("users");
                if (userMap.containsKey((Object) nick_name)) {
                    try {
                        userMap.lock(nick_name);
                        UserCacheModel user = (UserCacheModel) userMap.get((Object) nick_name);

                        long moneyUser = user.getVin();
                        long currentMoney = user.getVinTotal();

                        user.setVin(moneyUser -= totalMoney);
                        user.setVinTotal(currentMoney -= totalMoney);
                        MoneyMessageInMinigame message = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nick_name, "CreateGiftCode", moneyUser, currentMoney, -totalMoney, "vin", 0L, 0, 0);
                        LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nick_name, "CreateGiftCode", "Agency create giftcode", currentMoney, -totalMoney, "vin", "Agency create giftcode", 0L, false, user.isBot());
                        messageLog.setReferralCode(user.getReferralCode());
                        MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) message, (int) 16);
                        MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLog, 601);
                        userMap.put(nick_name, user);
                    } catch (Exception e) {
                        logger.debug((Object) e);
                    } finally {
                        userMap.unlock(nick_name);
                    }
                } else {
                    UserModel user = null;
                    user = userService.getUserByNickName(nick_name);
//                    userService.updateBalanceAgent(nick_name, totalMoney);
                    long moneyUser = user.getVin() - totalMoney;
                    long currentMoney = user.getVinTotal() - totalMoney;

                    MoneyMessageInMinigame message = new MoneyMessageInMinigame(VinPlayUtils.genMessageId(), user.getId(), nick_name, "CreateGiftCode", moneyUser, currentMoney, -totalMoney, "vin", 0L, 0, 0);
                    LogMoneyUserMessage messageLog = new LogMoneyUserMessage(user.getId(), nick_name, "CreateGiftCode", "Agency create giftcode", currentMoney, -totalMoney, "vin", "Agency create giftcode", 0L, false, user.isBot());
                    messageLog.setReferralCode(user.getReferralCode());
                    MessageBusFactory.get("queue_payment").publishOrThrow("queue_payment", (BaseMessage) message, (int) 16);
                    MessageBusFactory.get("queue_log_money").publish("queue_log_money", (LogMoneyUserMessage) messageLog, 601);
                }
            } else {
                long gift_end = userModel.getGift_total() - totalMoney;
                boolean resUpdateGift = userService.updateGifCodeAgent(nick_name, gift_end);

                if (!resUpdateGift) {
                    return BaseResponse.error("-1", "et: hạn gift code cần sau thời điểm hiện tại");
                }
            }

            // Tao bundle giftcode
            logger.info(String.format("CreateGiftCodeBundleProcessor - start create bundle, name=%s, created_by=%s, nick_name=%s, amount=%s, quantity=%s, type=%s, resource=%d, start=%s, end=%s",
                    name, created_by, nick_name, amount, quantity, type, resource, startDate, endDate));
            GiftCodeBundleModel newBundle = new GiftCodeBundleModel();
            newBundle.setName(name);
            newBundle.setCreated_by(created_by);
            long bundleId = GiftCodeUtil.insertGiftCodeBundle(newBundle);
            logger.info("CreateGiftCodeBundleProcessor - created bundleId=" + bundleId);

            // Tao giftcode
            int typeGift = Integer.parseInt(type);

            // Tao event
            int eventId = 0;
            if (typeGift == 3) {
                EventDAO eventDao = new EventDAOImpl();
                // Định dạng lại thời gian theo pattern mà EventDAOImpl đang parse ("yyyy-MM-dd hh:mm:ss")
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String startStr = df.format(startDate);
                String endStr = df.format(endDate);
                logger.info(String.format("CreateGiftCodeBundleProcessor - create event name=%s, amount=%d, start=%s, end=%s, agent=%s",
                        nick_name + " - " + name, totalMoney, startStr, endStr, nick_name));
                eventId = eventDao.addNewEventByAgent(nick_name + " - " + name, startStr, Long.valueOf(totalMoney), endStr, nick_name);
                logger.info("CreateGiftCodeBundleProcessor - addNewEventByAgent returned eventId=" + eventId);
                if (eventId == -1) {
                    return BaseResponse.error("-1", "et: Không tạo được sự kiện!");
                }
                logger.info("CreateGiftCodeBundleProcessor - created eventId=" + eventId);
            }

            for (int i = 0; i < Integer.parseInt(quantity); i++) {
                GiftCodeModel giftCodeModel = new GiftCodeModel();
                giftCodeModel.type = typeGift;
                giftCodeModel.money = money;
                giftCodeModel.max_use = 1;
                giftCodeModel.from = startDate;
                giftCodeModel.exprired = endDate;
                giftCodeModel.giftcode = createRandomGiftCode(8, typeGift, created_by);
                giftCodeModel.event = typeGift == 3 ? eventId : 0;
                giftCodeModel.created_by = created_by;
                giftCodeModel.bundle_id = bundleId;

//            String giftCodeGen = getGiftCodeValidate(typeGift);
//            giftCodeModel.giftcode = giftCodeGen;

                logger.info(String.format("CreateGiftCodeBundleProcessor - try insert giftcode index=%d code=%s type=%d money=%d bundleId=%d eventId=%d",
                        i, giftCodeModel.giftcode, giftCodeModel.type, giftCodeModel.money, giftCodeModel.bundle_id, giftCodeModel.event));

                if (GiftCodeUtil.giftCodeIsExits(giftCodeModel.giftcode)) {
                    logger.info("CreateGiftCodeBundleProcessor - duplicate giftcode detected, will regenerate. code=" + giftCodeModel.giftcode);
                    i--;
                    continue;
                }
                GiftCodeUtil.insertGiftCodeWithBundle(giftCodeModel);
                logger.info("CreateGiftCodeBundleProcessor - inserted giftcode=" + giftCodeModel.giftcode);
            }


            return BaseResponse.success(newBundle, 1);
        } catch (Exception e) {
            logger.trace(e);
            return BaseResponse.error("-1", e.getMessage());
        }
    }

    public static final Random rd = new Random();
    private static final String s = "0123456789ABCDEFGHJKMNOPQRSTUVWXYZ";

    public String createRandomGiftCode(int length, int typeGift, String nick_name) {
//        String value = typeGift + (nick_name == null || nick_name.isEmpty() ? "LOT" : nick_name.substring(0, 3).toUpperCase());
        String value = "SUN";
        for (int i = 0; i < length - 2; i++) {
            int pos = rd.nextInt(s.length());
            value += s.charAt(pos);
        }
        return value;
    }


}
