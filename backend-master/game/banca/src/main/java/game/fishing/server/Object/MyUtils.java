package game.fishing.server.Object;

public class MyUtils {
    private static MyUtils instances;

    public static MyUtils getInstances() {
        if (instances == null)
            instances = new MyUtils();
        return instances;
    }


    public boolean rectContainsPoint(int starPosX, int startPosY, int width, int height, CPoint point) {
        return (point.x >= starPosX && point.x <= width && point.y >= startPosY && point.y <= height);
    }

    public double getAngleByTwoPoint(CPoint param, CPoint param1) {
        CPoint point = new CPoint(param.x - param1.x, param.y - param1.y);
        return (90 - Math.toDegrees(Math.atan2(point.y, point.x)));

    }

    public int randomWithRange(int min, int max) {
        int range = (max - min) + 1;
        return (int) (Math.random() * range) + min;
    }

    public int calculatorSupplyType(){

        return 0;
    }
}
