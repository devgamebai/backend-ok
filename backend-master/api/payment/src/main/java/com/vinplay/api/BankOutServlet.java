package com.vinplay.api;

import com.payment.model.Code;
import com.payment.model.Result;
import com.payment.service.impl.ProviderServiceImpl;
import com.vinplay.response.BasePortalResponse;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.utils.RequestUtil;
import com.vinplay.vbee.common.models.UserModel;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.util.Strings;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BankOutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json; charset=utf-8");
        this.onExecute(request, response);
    }

    private void onExecute(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JSONObject obj = null;
        String requestId, nickName, admin, provider, ip;

        try {
            String jsonString = IOUtils.toString(request.getInputStream());
            obj = new JSONObject(jsonString);
            String token = request.getParameter("token");
            nickName = obj.getString("nick_name");
            requestId = obj.getString("request_id");
            admin = obj.getString("admin");
            provider = obj.getString("provider");
            ip = obj.getString("ip");

            if (Strings.isBlank(nickName) || Strings.isBlank(requestId) || Strings.isBlank(admin)) {
                response.setStatus(400);
                response.getWriter().println(
                        BasePortalResponse.New(BasePortalResponse.Error, "Không được để trống các trường!").toJson()
                );
                return;
            }

            if (ip == null || ip.isEmpty()) {
                ip = RequestUtil.getIpAddress(request);
            }

            if (provider == null || provider.isEmpty()) {
                provider = "default";
            }

            UserService userService = new UserServiceImpl();
            boolean isToken = userService.isActiveToken(nickName, token);
            if (isToken) {
                UserModel user = userService.getUserByNickName(nickName);
                Result<String> result = ProviderServiceImpl.getInstance().bankOut(provider, user, requestId, nickName, admin, ip);
                if (result.getCode() == Code.SUCCESS) {
                    response.setStatus(200);
                    response.getWriter().println(
                            BasePortalResponse.Success(
                                    BasePortalResponse.Success,
                                    result.getData()
                            ).toJson()
                    );
                    return;
                }
            }

            response.setStatus(400);
            response.getWriter().println(
                    BasePortalResponse.New(
                            BasePortalResponse.Error,
                            "user not login"
                    ).toJson()
            );
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().println(
                    BasePortalResponse.New(BasePortalResponse.Error, "Server Error").toJson()
            );
        }
    }
}
