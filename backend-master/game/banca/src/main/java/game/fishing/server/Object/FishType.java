package game.fishing.server.Object;

public enum FishType {
    CaGaiVangNho(0),               // ca gai vang nho
    CaTramCo(1),                 // ca tram nho
    CaDuoiVangNho(2),                // ca duoi vang nhiet doi
    CaVangMatTo(3),                 //  ca vang mat to
    CaTim(4),                 //  ca tim nhiet doi
    CaHe(5),                  //  ca he
    CaBoc(6),                   //  ca boc
    CaSuTu(7),                  //  ca dau su tu
    CaDenLong(8),                  //  ca den long
    Rua(9),                   //  Rua
    CaThienThan(10),                  //  ca thien than
    CaBuom(11),                    //  ca buom
    CaMuc(12),                 //  muc ong
    CaKiem(13),                   //  Ca kiem
    CaDuoiTo(14),                   //  Ca quy
    CaMapTrang(15),                  //  ca map trang lon
    CaMapVang(16),                 //  ca map vang lon
    ChimCanhCut(17),                //  chim canh cut doi
    CaVoi(18),              //  ca map vang khong lo
    RongVang(19),                   //  rong vang
    CuopBien(20),                    //  ly quy
    TienCa(21),                  //  le nuoc
    NaTra(22),                  //  bang nhieu
    CaBoom(23),                // phi tieu no
    KhoBau(24),                 //  kho bau
    JackPot(25),         
    Max(26);


    private int type;

    FishType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
