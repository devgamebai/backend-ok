package com.vinplay.api.backend.processors.money;

import com.vinplay.api.backend.report.utils.BackendUtils;
import com.vinplay.api.backend.response.TransferMoneyBankSmsResponse;
import com.vinplay.usercore.entities.TransferMoneyResponse;
import com.vinplay.usercore.service.impl.OtpServiceImpl;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.response.BaseResponseModel;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateMoneyUserWithSmsProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger((String) "backend");

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = (HttpServletRequest) param.get();
        int code = 1;
        TransferMoneyBankSmsResponse response = new TransferMoneyBankSmsResponse(false, "1001");
        try {
            String actionName = request.getParameter("ac");
            String moneyType = request.getParameter("mt");
            String reason = request.getParameter("rs");
            String content = request.getParameter("ct");
            String ime = request.getParameter("im");
            String address = request.getParameter("address");
            String ann = "bankingttbv";
            if(ann == null || ann.isEmpty()){
                return response.toJson();
            }

            String imeConfig = GameCommon.getValueStr((String) "BANK_IME");

            String codeConfig = GameCommon.getValueStr((String) "BANK_CODE");

            if (!ime.equals(imeConfig) || content.isEmpty() || codeConfig.isEmpty()) {
                logger.debug((Object) ("Request UpdateMoneyUserWithSms:  content: " + content + "ime wrong: " + ime + "," + imeConfig));
                return response.toJson();
            }

            String addressConfig = GameCommon.getValueStr((String) "BANK_ADDRESS");
            if (!address.equals(addressConfig)) {
                logger.debug((Object) ("Request UpdateMoneyUserWithSms:  content: " + content + "address wrong: " + address + "," + addressConfig));
                return response.toJson();
            }

            UserServiceImpl service = new UserServiceImpl();
            UserModel user = service.getUserByNickName(ann);
            if(user == null || user.getDaily() != 1){
                return response.toJson();
            }

            if (content.contains("TK")) {
                int status = 0;
                logger.debug((Object) ("Request UpdateMoneyUserWithSms: content: " + content + ", ime: " + ime + ", imeConfig: " + imeConfig));

                String accountNumber = GameCommon.getValueStr((String) "BANK_NUMBER");

                String firstAccount = accountNumber.substring(0,3);
                String endAccount = accountNumber.substring(10);

                String regex = "^TK" + firstAccount + "xxx" + endAccount + " tai BIDV ([0-9+,]+?)VND";

                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(content);
                long money1 = 0;
                String nickname = "";
                if (m.find()) {
                    String theGroup = m.group(1);
                    theGroup = theGroup.replace("+", "");
                    theGroup = theGroup.replace(",", "");
                    money1 = Long.valueOf(theGroup);
                    System.out.format("'%s'\n", theGroup);
                }

                if (money1 == 0) {
                    logger.debug((Object) ("Request not money UpdateMoneyUserWithSms: content: " + content + ", ime: " + ime + ", imeConfig: " + imeConfig + ", account " + accountNumber));
                    return response.toJson();
                }

                String regexN = "([a-zA-Z0-9]+?)-" + codeConfig;
                Pattern pN = Pattern.compile(regexN);
                Matcher mN = pN.matcher(content);
                if (mN.find()) {
                    nickname = mN.group(1);

                    System.out.format("'%s'\n", nickname);
                }


                if (nickname != null && !nickname.isEmpty() && money1 != 0L && moneyType != null && (moneyType.equals("vin") || moneyType.equals("xu"))) {



                    ann = user.getNickname();
                    TransferMoneyResponse moneyres2 = service.transferMoney(ann, nickname, money1, reason, false);
                    code = moneyres2.getCode();
                    if (code == 0) {
                        response.setSuccess(true);
                    }
                    if (code == 0) {
                        ArrayList<String> listUser = new ArrayList<>();
                        listUser.add(nickname);
                        listUser.add(ann);
                        BackendUtils.sendUpdateUserMoneyInfo(listUser);
                        status = 1;
                    }
                    response.setAmount(money1);
                    response.setContent(nickname);
                    response.setSms(content);
                    response.setErrorCode(String.valueOf(code));
                    long id = service.insertBankSms(nickname, content, money1, status);
                    response.setId(id);
                } else {
                    logger.debug((Object) ("Request not nick name UpdateMoneyUserWithSms: content: " + content + ", ime: " + ime + ", imeConfig: " + imeConfig + ", account " + accountNumber));
                }
            } else {
                logger.debug((Object) ("Request not SD TK UpdateMoneyUserWithSms: content: " + content + ", ime: " + ime + ", imeConfig: " + imeConfig ));
                return response.toJson();
            }


        } catch (Exception e) {
            System.out.format("'%s'\n", e);
            logger.debug((Object) e);
        }
        logger.debug((Object) ("Response UpdateMoneyUser: " + response.toJson()));
        return response.toJson();
    }
}
