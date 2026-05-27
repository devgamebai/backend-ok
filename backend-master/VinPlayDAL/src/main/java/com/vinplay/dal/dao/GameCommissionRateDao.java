package com.vinplay.dal.dao;

import com.vinplay.dal.entities.agent.GameCommissionRate;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface GameCommissionRateDao {

    List<GameCommissionRate> searchByNickName(String nickName) throws SQLException;

    Map<Integer, Double> getRateMapByNickName(String nickName) throws SQLException;

    Map<String, Map<Integer, Double>> getRateMapByNickNames(List<String> nickNames) throws SQLException;

    boolean insertOrUpdate(GameCommissionRate rate) throws SQLException;

    boolean delete(long id) throws SQLException;

    boolean deleteByNickNameAndGameId(String nickName, int gameId) throws SQLException;
}
