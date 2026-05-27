package game.modules.SlotUtils;

/**
 * Created by TuanTri on 8/9/2017.
 */
public class RowValue {

    public byte icon;
    public byte number;

    public RowValue(byte icon, byte number){
        this.icon = icon;
        this.number = number;
    }

    @Override
    public boolean equals(Object obj) {
        RowValue other = (RowValue) obj;
        return ((this.icon == other.icon) && (this.number == other.number));
    }

}
