/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.commons.lang3.StringUtils
 *  org.apache.log4j.Logger
 */
package com.vinplay.usercore.service.impl;

import com.vinplay.usercore.dao.UserLevelDao;
import com.vinplay.usercore.dao.impl.UserLevelDaoImpl;
import com.vinplay.usercore.entities.UserLevel;
import com.vinplay.usercore.service.UserLevelService;
import com.vinplay.usercore.service.impl.UserServiceImpl;
import com.vinplay.vbee.common.models.UserModel;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class UserLevelServiceImpl
implements UserLevelService {
    private static final Logger logger = Logger.getLogger(UserLevelServiceImpl.class);
    private UserLevelDao dao = new UserLevelDaoImpl();

    private String Validate(UserLevel userLevel) {
        try {
            if (StringUtils.isBlank((CharSequence)userLevel.getNick_name())) {
                return "Nickname ng\u01b0\u1eddi \u0111\u01b0\u1ee3c gi\u1edbi thi\u1ec7u kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng";
            }
            if (StringUtils.isBlank((CharSequence)userLevel.getParent_user())) {
                return "Nickname ng\u01b0\u1eddi gi\u1edbi thi\u1ec7u kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng";
            }
            return "success";
        }
        catch (Exception e) {
            logger.error(("Error Validate: user_level" + e.getMessage()));
            return e.getMessage();
        }
    }

    @Override
    public String create(String nickname, String nickname_parent) {
        UserLevel userLevel = new UserLevel();
        userLevel.setNick_name(nickname);
        userLevel.setParent_user(nickname_parent);
        return this.create(userLevel);
    }

    @Override
    public String create(UserLevel userLevel) {
        try {
            String result = "";
            result = this.Validate(userLevel);
            if (!"success".equals(result)) {
                return result;
            }
            UserServiceImpl userService = new UserServiceImpl();
            UserModel userModel = userService.getUserByNickName(userLevel.getNick_name());
            if (userModel == null) {
                return "Nickname ng\u01b0\u1eddi \u0111\u01b0\u1ee3c gi\u1edbi thi\u1ec7u kh\u00f4ng t\u1ed3n t\u1ea1i";
            }
            userLevel.setCode(userModel.getReferralCode());
            userModel = userService.getUserByNickName(userLevel.getParent_user());
            if (userModel == null) {
                return "Nickname ng\u01b0\u1eddi gi\u1edbi thi\u1ec7u kh\u00f4ng t\u1ed3n t\u1ea1i";
            }
            UserLevel userLevelExist = this.dao.getByNickName(userLevel.getNick_name());
            if (userLevelExist != null) {
                return "Ng\u01b0\u1eddi \u0111\u01b0\u1ee3c gi\u1edbi thi\u1ec7u \u0111\u00e3 tham gia ch\u01b0\u01a1ng tr\u00ecnh r\u1ed3i";
            }
            UserLevel userLevelParent = this.dao.getByNickName(userLevel.getParent_user());
            if (userLevelParent == null) {
                userLevel.setAncestor(userLevel.getParent_user());
            } else {
                String ancestorParent = userLevelParent.getAncestor();
                if (StringUtils.isBlank((CharSequence)ancestorParent)) {
                    return "Nickname ng\u01b0\u1eddi \u0111\u01b0\u1ee3c gi\u1edbi thi\u1ec7u kh\u00f4ng \u0111\u00fang";
                }
                String[] arrAncestorParent = ancestorParent.split(",");
                if (arrAncestorParent.length > 1) {
                    userLevel.setAncestor(userLevel.getNick_name());
                }
                if (arrAncestorParent.length == 1) {
                    userLevel.setAncestor(ancestorParent + "," + userLevel.getParent_user());
                }
            }
            return this.dao.insert(userLevel);
        }
        catch (Exception e) {
            logger.error(("Error create user_level: " + e.getMessage()));
            return e.getMessage();
        }
    }

    @Override
    public String update(String oldNickname, String newNickname) {
        UserLevel userLevel = new UserLevel();
        try {
            userLevel = this.dao.getByNickName(oldNickname);
        }
        catch (SQLException e) {
            userLevel = null;
        }
        if (userLevel == null) {
            return "Nickname nh\u00e2n v\u1eadt kh\u00f4ng t\u1ed3n t\u1ea1i";
        }
        String newAncestor = userLevel.getAncestor();
        newAncestor.replace(oldNickname, newNickname);
        userLevel.setAncestor(newAncestor);
        userLevel.setNick_name(newNickname);
        try {
            return this.dao.insert(userLevel);
        }
        catch (SQLException e) {
            logger.error(("Error update user_level: " + e.getMessage() + " | data: " + userLevel.toJson()));
            return e.getMessage();
        }
    }

    @Override
    public UserLevel getByNickName(String nickname, String parent_user) {
        try {
            return this.dao.getByNickName(nickname, parent_user);
        }
        catch (SQLException e) {
            return null;
        }
    }

    @Override
    public UserLevel getByNickName(String nickname) {
        try {
            return this.dao.getByNickName(nickname);
        }
        catch (SQLException e) {
            return null;
        }
    }

    @Override
    public Map<String, Object> findChilds(String nickname, String statDate, String endDate, int pageIndex, int limit) {
        try {
            return this.dao.findChilds(nickname, statDate, endDate, pageIndex, limit);
        }
        catch (Exception e) {
            logger.error(("Error findChilds user_level: " + e.getMessage()));
            return new HashMap<String, Object>();
        }
    }
}

