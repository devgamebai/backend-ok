package bitzero.server.controllers.system;

import bitzero.engine.io.IRequest;
import bitzero.server.controllers.BaseControllerCommand;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.core.BZEvent;
import bitzero.server.core.BZEventParam;
import bitzero.server.core.BZEventType;
import bitzero.server.entities.User;
import bitzero.server.extensions.data.DataCmd;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class CrossCommand extends BaseControllerCommand {
     public CrossCommand() {
          super(SystemRequest.CrossCommand);
     }

     public boolean validate(IRequest irequest) {
          return true;
     }

     public void execute(IRequest request) throws Exception {
          DataCmd params = new DataCmd(((ByteBuffer)request.getContent()).array());
          short CmdId = params.readShort();
          if (CmdId == 0) {
               User user = this.bz.getUserManager().getUserById(params.readInt());
               if (user == null) {
                    return;
               }

               Map evtParams = new HashMap();
               evtParams.put(BZEventParam.USER, user);
               this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.XU_UPDATE, evtParams));
          } else {
               Integer playerId;
               Integer userId;
               if (CmdId == 1) {
                    userId = params.readInt();
                    Integer money = params.readInt();
                    playerId = params.readInt();
                    Map evtParams = new HashMap();
                    evtParams.put(BZEventParam.PLAYER_ID, userId);
                    evtParams.put(BZEventParam.VARIABLES, money);
                    evtParams.put(BZEventParam.PAYMENT_TYPE, playerId);
                    this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.CHARGE_XU, evtParams));
               } else if (CmdId == 11) {
                    userId = params.readInt();
                    User user = this.bz.getUserManager().getUserById(userId);
                    playerId = userId;
                    Integer money = params.readInt();
                    String paymentType = params.readString();
                    Integer grossNum = params.readInt();
                    Integer netNum = params.readInt();
                    Integer cashNum = params.readInt();
                    Integer promoNum = params.readInt();
                    String transaction = params.readString();
                    Map evtParams = new HashMap();
                    evtParams.put(BZEventParam.USER, user);
                    evtParams.put(BZEventParam.PLAYER_ID, playerId);
                    evtParams.put(BZEventParam.VARIABLES, money);
                    evtParams.put(BZEventParam.PAYMENT_TYPE, paymentType);
                    evtParams.put(BZEventParam.GROSS_NUM, grossNum);
                    evtParams.put(BZEventParam.NET_NUM, netNum);
                    evtParams.put(BZEventParam.CASH_NUM, cashNum);
                    evtParams.put(BZEventParam.PROMO_NUM, promoNum);
                    evtParams.put(BZEventParam.TRANSACTIONID, transaction);
                    this.bz.getEventManager().dispatchEvent(new BZEvent(BZEventType.CHARGE_XU, evtParams));
               }
          }

     }
}
