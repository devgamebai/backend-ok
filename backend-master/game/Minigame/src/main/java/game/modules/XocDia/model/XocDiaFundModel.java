package game.modules.XocDia.model;

import com.vinplay.dal.service.MiniGameService;
import com.vinplay.dal.service.impl.MiniGameServiceImpl;
import bitzero.util.common.business.Debug;

public class XocDiaFundModel {
    private long fund;
    public MiniGameService mgService;
    public static final String fundName = "FundXocDiaFull";
    public static XocDiaFundModel _instance;

    public static XocDiaFundModel getInstance() {
        if (_instance == null) {
            _instance = new XocDiaFundModel();
        }
        return _instance;
    }

    private XocDiaFundModel() {
        this.mgService = new MiniGameServiceImpl();
        try {
            this.fund = this.mgService.getFund(fundName);
        } catch (Exception e) {
            Debug.trace(new Object[]{"Load XocDia fund error", e.getMessage()});
            this.fund = 0;
        }
    }

    public synchronized void addMoneyToFund(long amount) {
        this.fund += amount;
    }

    public void saveFund() {
        try {
            this.mgService.saveFund(fundName, this.fund);
        } catch (Exception e) {
            Debug.trace(new Object[]{"Save XocDia fund error", e.getMessage()});
        }
    }

    public long getFund() {
        return this.fund;
    }
}
