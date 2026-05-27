/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.service;

import game.third.usecase.game568win.request.GetGameList;
import game.third.usecase.game568win.request.Login;
import game.third.usecase.game568win.request.RegisterAgent;
import game.third.usecase.game568win.request.RegisterPlayer;
import game.third.usecase.game568win.request.UpdateAgent;
import game.third.usecase.game568win.response.GetGameListResult;
import game.third.usecase.game568win.response.LoginResult;
import game.third.usecase.game568win.response.RegisterPlayerResult;
import game.third.usecase.game568win.response.UpdateAgentResult;

public interface APIGame568winService {
    public UpdateAgentResult RegisterAgent(RegisterAgent var1);

    public UpdateAgentResult UpdateAgent(UpdateAgent var1);

    public RegisterPlayerResult RegisterPlayer(RegisterPlayer var1);

    public LoginResult Login(Login var1);

    public GetGameListResult GetGameList(GetGameList var1);
}

