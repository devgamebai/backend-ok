package com.vinplay.api.processors;

import com.hazelcast.core.IMap;
import com.vinplay.api.utils.PortalUtils;
import com.vinplay.usercore.utils.GameCommon;
import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.enums.StatusGames;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import com.vinplay.vbee.common.models.UserModel;
import com.vinplay.vbee.common.models.cache.UserCacheModel;
import com.vinplay.vbee.common.response.LoginResponse;
import com.vinplay.vbee.common.utils.VinPlayUtils;
import com.vinplay.usercore.service.UserService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;

public class LoginByTokenProcessor implements BaseProcessor<HttpServletRequest, String> {
	private static final Logger logger = Logger.getLogger("api");

	public String execute(Param<HttpServletRequest> param) {
		HttpServletRequest request = (HttpServletRequest) param.get();
		String nickname = request.getParameter("u");
		String accessToken = request.getParameter("at");
		LoginResponse res = new LoginResponse(false, "1001");

		try {
			if (nickname == null || accessToken == null || nickname.isEmpty() || accessToken.isEmpty())
				return res.toJson();

			// Check maintenance mode
			int statusGame = StatusGames.RUN.getId();
			try {
				statusGame = GameCommon.getValueInt("STATUS_GAME");
			} catch (Exception e) {
				statusGame = StatusGames.RUN.getId();
			}

			if (statusGame == StatusGames.MAINTAIN.getId()) {
				res.setErrorCode("1114");
				return res.toJson();
			}

			// SUN-auth: source of truth for "is this token still valid" is
			// the cacheToken map (3-day TTL). The users map has a shorter 1h
			// cache TTL for freshness, so after an idle gap the users entry
			// may be evicted even while cacheToken is still good. Treat the
			// users-map check as a fast-path; fall back to cacheToken when
			// the entry is missing and re-hydrate from DB via loginSuccess.
			com.hazelcast.core.HazelcastInstance hz = HazelcastClientFactory.getInstance();
			IMap<String, UserCacheModel> userMap  = hz.getMap("users");
			IMap<String, String>         tokenMap = hz.getMap("cacheToken");

			boolean tokenValid = false;
			boolean sessionFresh = false;

			if (userMap.containsKey(nickname)) {
				try {
					userMap.lock(nickname);
					UserCacheModel userCache = userMap.get(nickname);
					if (userCache != null && accessToken.equals(userCache.getAccessToken())) {
						tokenValid = true;
						sessionFresh = !VinPlayUtils.sessionTimeout(
								(long) userCache.getLastActive().getTime());
					}
				} finally {
					try { userMap.unlock(nickname); } catch (Exception ignore) {}
				}
			}

			// Fast-path miss → check cacheToken directly. If the token maps
			// to this nickname it's still authentic within its 3-day TTL.
			if (!tokenValid) {
				String owner = tokenMap.get(accessToken);
				if (owner != null && owner.equals(nickname)) {
					tokenValid = true;
					sessionFresh = true;   // cacheToken hit implies within TTL
				}
			}

			if (!tokenValid) {
				res.setErrorCode("1014"); // Token mismatch / unknown
				return res.toJson();
			}
			if (!sessionFresh) {
				res.setErrorCode("1015"); // Session expired
				return res.toJson();
			}

			// Token valid. Re-hydrate via loginSuccess — that path rotates the
			// token, refreshes cacheToken TTL (3 days), re-populates the users
			// cache from DB, and fires the force-logout listener on any older
			// tokens for this nickname (SUN-767/832 behaviour preserved).
			try {
				UserService userService = new UserServiceImpl();
				UserModel user = userService.getUserByNickName(nickname);
				if (user == null) {
					user = userService.getUserByUserName(nickname);
				}
				if (user != null) {
					res = PortalUtils.loginSuccess(user, request);
				}
			} catch (Exception e) {
				logger.debug(e);
				return res.toJson();
			}
		} catch (Exception e2) {
			logger.debug(e2);
		}
		return res.toJson();
	}
}
