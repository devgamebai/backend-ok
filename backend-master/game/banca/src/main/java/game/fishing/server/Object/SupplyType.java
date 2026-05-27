package game.fishing.server.Object;

public enum SupplyType {
    EST_Gold(0),                //金币
    EST_Cold(1),                //金币
    EST_YuanBao(2),   //元宝
    EST_Laser(3),            //激光
    EST_Speed(4),            //加速
    EST_Bignet(5),                //赠送
    EST_Electric(6),                //赠送
    EST_Bomb(7),                //赠送
    EST_NULL(8);

    private int type;

    SupplyType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
