package com.vinplay.api.backend.processors.lottery;

import com.vinplay.dal.service.LoDeService;
import com.vinplay.dal.service.impl.LoDeServiceImpl;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.messages.minigame.LotteryMessage;
import com.vinplay.vbee.common.response.lode.LoDeModelResponse;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class ListLotteryTransactionProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("backend");

    private static LoDeService lotteryService = new LoDeServiceImpl();;
    @Override
    public String execute(Param<HttpServletRequest> param) {
        LoDeModelResponse response = new LoDeModelResponse(false, "1001");
        HttpServletRequest request = (HttpServletRequest) param.get();
        String nickname = request.getParameter("nn");
        String ticket = request.getParameter("ti");
        String startDate = request.getParameter("ts");
        String endDate = request.getParameter("te");
        String model = request.getParameter("m");
        int page = Integer.parseInt(request.getParameter("p"));
        int limit = Integer.parseInt(request.getParameter("li"));


        try {
            List<LotteryMessage> transactions = lotteryService.search(nickname, ticket, model, startDate, endDate, page, limit);
            long total = lotteryService.count(nickname, ticket, model, startDate, endDate);
            response.setTransactions(transactions);
            response.setTotal(total);
            response.setSuccess(true);
            response.setErrorCode("0");
        } catch (Exception e) {
            e.printStackTrace();
            logger.debug((Object) e);
        }
        return response.toJson();
    }
}
