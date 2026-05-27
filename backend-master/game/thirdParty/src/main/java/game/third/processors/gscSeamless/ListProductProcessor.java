package game.third.processors.gscSeamless;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import game.third.processors.common.AuthProcessor;
import game.third.processors.response.ListGameResponse;
import game.third.usecase.gsc.model.GSCGameProviderResponse;
import game.third.usecase.gsc.service.GSCService;
import game.third.usecase.gsc.service.impl.GSCServiceImpl;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;


public class ListProductProcessor extends AuthProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");

    private GSCService gscService = new GSCServiceImpl();

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        ListGameResponse baseResponse = new ListGameResponse(false,"1");
        UserModel userService = getUser(param);
        if (userService == null) {
            return notAuth;
        }

        try {
            GSCGameProviderResponse gscGameProviderResponse;
            gscGameProviderResponse = (GSCGameProviderResponse) gscService.productList(0, 999);

            // get list game from third party gscService
            baseResponse.setSuccess(true);
            baseResponse.setErrorCode("0");
            baseResponse.setTotal(gscGameProviderResponse.getPagination().getIntTotal());
            baseResponse.setData(gscGameProviderResponse.getProviderGames());
        } catch (Exception e) {
            logger.error(e);
            e.printStackTrace();
            baseResponse.setMessage("Server have error: " + e.getMessage());
        }
        return baseResponse.toJson();
    }
}