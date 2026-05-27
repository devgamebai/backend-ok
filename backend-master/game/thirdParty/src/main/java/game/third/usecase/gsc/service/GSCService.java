package game.third.usecase.gsc.service;

import game.third.usecase.gsc.model.GSCGameLaunchResponse;
import game.third.usecase.gsc.model.GSCGameProviderResponse;
import game.third.usecase.gsc.model.GSCProductInfo;
import game.third.usecase.gsc.model.LaunchSuperLobbyResponse;

import java.util.List;

public interface GSCService {
    /**
     * Retrieves the list of games provided by the operator.
     *
     * @return a GSCGameProviderResponse object containing the list of games.
     */
    GSCGameProviderResponse gameList(int productCode, String gameType) throws Exception;

    /**
     * Retrieves the list of available products.
     *
     * @param offset the start record number of this retrieval (optional).
     * @param size   the number of records retrieved this time (optional, default: 500).
     * @return a List of GSCProductInfo objects containing the available products.
     */
    List<GSCProductInfo> productList(Integer offset, Integer size);

    /**
     * Retrieves the game history for a specific wager code.
     *
     * @param wagerCode the unique identifier for the wager.
     * @return a String containing the game history in HTML format.
     */
    String gameHistory(String wagerCode);

    /**
     * Launches a game for a specific user.
     *
     * @param nickname       the nickname of the user.
     * @param memberPassword the member's password (MD5-hashed) — required by GSC
     *                       per the launch-game spec; sending empty here returns
     *                       400 invalid parameters from GSC.
     * @param productCode    the product code of the game.
     * @param gameCode       the game code.
     * @param gameType       the type of the game.
     * @param currency       the currency used (e.g., IDR, USD).
     * @param platform       the platform used (e.g., desktop, mobile).
     * @param ipClient       the IP address of the client.
     * @return a GSCGameLaunchResponse object containing the launch details.
     */
    GSCGameLaunchResponse launchGame(String nickname, String memberPassword, String productCode, String gameCode, String gameType, String currency, String platform, String ipClient);


    /**
     * Launches the super lobby for a specific user.
     *
     * @param nickname        The member's nickname displayed in the game.
     * @param platform        The platform type (e.g., web, desktop, mobile).
     * @return A string containing the URL for launching the super lobby.
     * @throws Exception If an error occurs during the request.
     */
    LaunchSuperLobbyResponse launchSuperLobby(String nickname, String platform) throws Exception;

}