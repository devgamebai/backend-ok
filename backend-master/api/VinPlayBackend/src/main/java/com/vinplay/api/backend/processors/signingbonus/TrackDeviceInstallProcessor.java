package com.vinplay.api.backend.processors.signingbonus;

import com.vinplay.dal.service.SigningBonusService;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * Public API: Track a device's first-time app install.
 * Called by mobile client when app is opened for the first time.
 * Command ID: 9760
 *
 * Params: device_fp (required), platform (optional), app_version (optional)
 * Note: Does NOT require authentication — called before user registers.
 */
public class TrackDeviceInstallProcessor implements BaseProcessor<HttpServletRequest, String> {

    private static final Logger logger = Logger.getLogger("api");

    public String execute(Param<HttpServletRequest> param) {
        JSONObject response = new JSONObject();
        try {
            HttpServletRequest request = (HttpServletRequest) param.get();

            String deviceFp = request.getParameter("device_fp");
            if (deviceFp == null || deviceFp.trim().isEmpty()) {
                response.put("success", false);
                response.put("errorCode", "4001");
                response.put("message", "device_fp is required");
                return response.toString();
            }

            String platform = request.getParameter("platform");
            String appVersion = request.getParameter("app_version");
            String ipAddress = request.getRemoteAddr();

            SigningBonusService service = new SigningBonusService();
            boolean isFirstInstall = service.trackDeviceInstall(deviceFp, platform, appVersion, ipAddress);

            response.put("success", true);
            response.put("is_first_install", isFirstInstall);

        } catch (Exception e) {
            logger.error("TrackDeviceInstallProcessor error", e);
            response.put("success", false);
            response.put("errorCode", "1001");
        }
        return response.toString();
    }
}
