package game.third.processors.gscSeamless;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.models.UserModel;
import game.third.processors.common.AuthProcessor;
import game.third.processors.response.LaunchGameResponse;
import game.third.usecase.gsc.model.GSCGameLaunchResponse;
import game.third.usecase.gsc.service.GSCService;
import game.third.usecase.gsc.service.impl.GSCServiceImpl;
import game.third.utils.Request;
import org.apache.log4j.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;


public class LaunchLobbySportBookProcessor extends AuthProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = Logger.getLogger("api");

    private final GSCService gscService = new GSCServiceImpl();

    String regex = "https://b6y9to\\.(.*)\\.com/";

    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        LaunchGameResponse baseResponse = new LaunchGameResponse(false, "1");
        UserModel userService = getUser(param);
        if (userService == null) {
            return notAuth;
        }

        try {

            String nickname = userService.getNickname();
            String productCode = request.getParameter("pc");
            if (productCode == null || productCode.isEmpty()) {
               productCode = "1046";
            }

            String gameType = request.getParameter("gt");
            if (gameType == null || gameType.isEmpty()) {
                gameType = "SPORT_BOOK";
            }

            String platform = request.getParameter("pf");
            if (platform == null || platform.isEmpty()) {
                baseResponse.setMessage("Platform is required");
                return baseResponse.toJson();
            }

            String currency = request.getParameter("cc");
            if (currency == null || currency.isEmpty()) {
                baseResponse.setMessage("Currency is required");
                return baseResponse.toJson();
            }

            String ipClient = Request.getIpAddress(request);
            if (ipClient == null || ipClient.isEmpty()) {
                baseResponse.setMessage("IP client is required");
                return baseResponse.toJson();
            }

            ipClient = ipClient.split(",")[0].trim();

            GSCGameLaunchResponse res = gscService.launchGame(nickname, userService.getPassword(), productCode, "", gameType, currency, platform, ipClient);
            if (res != null && res.getCode() == 200) {
                baseResponse.setSuccess(true);
                baseResponse.setErrorCode("0");

                // Compile the pattern
                Pattern pattern = Pattern.compile(regex);
                // Create a matcher with the original text
                Matcher matcher = pattern.matcher(res.getUrl());
                // Replace the matched URL with the new one
                String url = matcher.replaceAll("https://gamebong.xyz/");

                baseResponse.setUrl(url);
            }
            if (res != null) {
                baseResponse.setMessage(res.getMessage());
            } else {
                baseResponse.setMessage("Server have error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            baseResponse.setMessage("Server have error: " + e.getMessage());
        }
        return baseResponse.toJson();
    }
}