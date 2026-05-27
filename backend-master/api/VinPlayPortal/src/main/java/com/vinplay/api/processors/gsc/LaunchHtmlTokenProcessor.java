package com.vinplay.api.processors.gsc;

import com.vinplay.vbee.common.cp.BaseProcessor;
import com.vinplay.vbee.common.cp.Param;
import com.vinplay.vbee.common.hazelcast.HazelcastClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * c=3092 — serve an inline-HTML game launcher by token.
 *
 * <p>Counterpart to {@link LaunchGSCGameProcessor}: when GSC returns
 * inline HTML in the {@code content} field (PG Soft etc.), the launch
 * processor stashes the HTML in Hazelcast keyed by a UUID token and
 * returns a URL pointing at this endpoint. Mobile clients (which open
 * the launch in a new browser tab and can't render HTML inline) navigate
 * to that URL, this processor reads the token, and writes the HTML back
 * with {@code Content-Type: text/html}.
 *
 * <p>Tokens are single-instance, TTL-bound (default 1h, configured by
 * {@code LAUNCH_URL_TTL_SECONDS}). Re-loading the same token URL within
 * the window keeps working; after expiry the processor returns 410.
 *
 * <p>Public route via nginx: {@code https://pgsoft-sunkr.live/p/{token}}
 * → portal-api {@code /api?c=3092&t={token}}.
 *
 * <p>This endpoint is intentionally unauthenticated — the token itself
 * is the credential. Tokens are 32-hex random and TTL-bound, so the
 * exposure window is the same as a short-lived signed URL.
 */
public class LaunchHtmlTokenProcessor implements BaseProcessor<HttpServletRequest, String> {
    private static final Logger logger = LoggerFactory.getLogger("portal");

    @Override
    public String execute(Param<HttpServletRequest> param) {
        HttpServletRequest request = param.get();
        HttpServletResponse response = (HttpServletResponse) request.getAttribute("javax.servlet.http.HttpServletResponse");

        try {
            String token = request.getParameter("t");
            if (token == null || token.isEmpty()) {
                if (response != null) response.setStatus(400);
                return "<html><body>Missing token</body></html>";
            }

            com.hazelcast.core.IMap<String, String> htmlMap =
                    HazelcastClientFactory.getInstance().getMap(LaunchGSCGameProcessor.HTML_TOKEN_MAP_NAME);
            String html = htmlMap.get(token);

            if (html == null || html.isEmpty()) {
                // 410 = "this URL was valid but has expired or been consumed".
                // Clearer than 404 for callers that may retry.
                if (response != null) response.setStatus(410);
                return "<html><body>Launcher expired. Please launch the game again from the app.</body></html>";
            }

            if (response != null) {
                response.setContentType("text/html; charset=UTF-8");
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("X-Frame-Options", "SAMEORIGIN");
                response.setStatus(200);
            }
            return html;
        } catch (Exception e) {
            logger.error("LaunchHtmlTokenProcessor error", e);
            if (response != null) response.setStatus(500);
            return "<html><body>Internal error</body></html>";
        }
    }
}
