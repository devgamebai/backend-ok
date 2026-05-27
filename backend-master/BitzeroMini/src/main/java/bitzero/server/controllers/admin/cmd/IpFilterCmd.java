package bitzero.server.controllers.admin.cmd;

import bitzero.server.extensions.data.BaseCmd;
import bitzero.server.extensions.data.DataCmd;

public class IpFilterCmd extends BaseCmd {
     public String mode = "";
     public String targetList = "";
     public String ip = "";

     public IpFilterCmd(DataCmd dataCmd) {
          super(dataCmd);
     }
}
