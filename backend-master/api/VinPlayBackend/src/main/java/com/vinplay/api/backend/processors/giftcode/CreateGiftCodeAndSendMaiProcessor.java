package com.vinplay.api.backend.processors.giftcode;

import com.vinplay.giftcode.GiftCodeModel;
import com.vinplay.giftcode.GiftCodeType;
import com.vinplay.giftcode.GiftCodeUtil;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.MailBoxServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.response.BaseResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class CreateGiftCodeAndSendMaiProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String) "backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();

        String listNickNameRaw = request.getParameter("nns");
        String amount = request.getParameter("am");
        String startTime = request.getParameter("st");
        String endTime = request.getParameter("et");
        String created_by = request.getParameter("cb");
        String event = request.getParameter("ev");
        String title = request.getParameter("tm");
        String content = request.getParameter("cm");

        if (created_by == null || created_by.isEmpty()) {
            return BaseResponse.error("-1", "cb: Thiếu người tạo");
        }

        if (title == null || title.isEmpty()) {
            return BaseResponse.error("-1", "tm: Thiếu tieu de mail");
        }

        String[] listNickName = listNickNameRaw.split(",");
        if (listNickName.length == 0) {
            return BaseResponse.error("-1", "nns: Thiếu user de gui mail");
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

        Timestamp startDate, endDate;
        if (endTime == null || endTime.compareTo(startTime) <= 0) {
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

        if (event == null || event.isEmpty()) {
            return BaseResponse.error("-1", "ev: Event không được trống");
        }


        int eventNumber = 0;
        try {
            eventNumber = Integer.parseInt(event);
        } catch (NumberFormatException e) {
            return BaseResponse.error("-1", "ev: Event phải là số");
        }


        if (content == null) {
            content = "";
        }

        Map<String, String> resultMap = new HashMap<>();
        UserService userService = new UserServiceImpl();
        MailBoxServiceImpl mailService = new MailBoxServiceImpl();

        for (String nick_name : listNickName) {
            try {

                // Create gift code
                GiftCodeModel giftCodeModel = new GiftCodeModel();
                giftCodeModel.type = GiftCodeType.ONE_FOR_THIS_USER;
                giftCodeModel.money = money;
                giftCodeModel.max_use = 1;
                giftCodeModel.from = startDate;
                giftCodeModel.exprired = endDate;
                giftCodeModel.created_by = created_by;
                giftCodeModel.user_name = nick_name;
                giftCodeModel.giftcode = createRandomGiftCode(5, GiftCodeType.ONE_FOR_THIS_USER, nick_name);
                giftCodeModel.event = eventNumber;

                String nn = nick_name.trim();
                if (userService.getUser(nn) == null) {
                    resultMap.put(nick_name, "User này không tồn tại , nickname=" + nn);
                    continue;
                }

                GiftCodeUtil.insertGiftCode(giftCodeModel);

                // send mail to user
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                String contentSend = "Mã code nhận thưởng bạn là "
                        + giftCodeModel.giftcode
                        + " có hạn sử dụng đến ngày "
                        + formatter.format(giftCodeModel.exprired) + ".";
               if (content != null && !content.isEmpty()) {
                   contentSend = content + "\n" + contentSend;
               }

                boolean sendSuccess = mailService.sendMailBoxFromByNickNameAdmin(nick_name, title, contentSend);
                if (!sendSuccess) {
                    resultMap.put(nick_name, "gửi mail đến người dùng lỗi");

                    // remove gift code
                    GiftCodeUtil.deleteGiftCode(giftCodeModel.giftcode);
                    continue;
                }

                // result
                resultMap.put(nick_name, "success");
            } catch (Exception e) {
                logger.trace(e);
                resultMap.put(nick_name, e.getMessage());
            }
        }

        return BaseResponse.success(resultMap, 1);
    }

    public static final Random rd = new Random();
    private static final String s = "0123456789ABCDEFGHJKMNOPQRSTUVWXYZ";

    public String createRandomGiftCode(int length, int typeGift, String nick_name) {
        String value = typeGift + (nick_name == null || nick_name.isEmpty() ? "LOT" : nick_name.substring(0, 3).toUpperCase());
        for (int i = 0; i < length; i++) {
            int pos = rd.nextInt(s.length());
            value += s.charAt(pos);
        }
        return value;
    }


}
