package bitzero.server.controllers;

public enum SystemRequest {
     Handshake(Short.valueOf((short)0)),
     Login(Short.valueOf((short)1)),
     LoginWebsocket((short)9002),
     Logout(Short.valueOf((short)2)),
     GetRoomList(Short.valueOf((short)3)),
     JoinRoom(Short.valueOf((short)4)),
     AutoJoin(Short.valueOf((short)5)),
     CreateRoom(Short.valueOf((short)6)),
     GenericMessage(Short.valueOf((short)7)),
     ChangeRoomName(Short.valueOf((short)8)),
     ChangeRoomPassword(Short.valueOf((short)9)),
     ObjectMessage(Short.valueOf((short)10)),
     SetRoomVariables(Short.valueOf((short)11)),
     SetUserVariables(Short.valueOf((short)12)),
     CallExtension(Short.valueOf((short)13)),
     LeaveRoom(Short.valueOf((short)14)),
     SubscribeRoomGroup(Short.valueOf((short)15)),
     UnsubscribeRoomGroup(Short.valueOf((short)16)),
     SpectatorToPlayer(Short.valueOf((short)17)),
     PlayerToSpectator(Short.valueOf((short)18)),
     ChangeRoomCapacity(Short.valueOf((short)19)),
     PublicMessage(Short.valueOf((short)20)),
     PrivateMessage(Short.valueOf((short)21)),
     ModeratorMessage(Short.valueOf((short)22)),
     AdminMessage(Short.valueOf((short)23)),
     KickUser(Short.valueOf((short)24)),
     BanUser(Short.valueOf((short)25)),
     ManualDisconnection(Short.valueOf((short)26)),
     GoOnline(Short.valueOf((short)27)),
     InviteUser(Short.valueOf((short)28)),
     InvitationReply(Short.valueOf((short)29)),
     CreateBZGame(Short.valueOf((short)30)),
     QuickJoinGame(Short.valueOf((short)31)),
     OnEnterRoom(Short.valueOf((short)32)),
     OnRoomCountChange(Short.valueOf((short)33)),
     OnUserLost(Short.valueOf((short)34)),
     OnRoomLost(Short.valueOf((short)35)),
     OnUserExitRoom(Short.valueOf((short)36)),
     OnClientDisconnection(Short.valueOf((short)37)),
     BanUserChat(Short.valueOf((short)38)),
     PingPong(Short.valueOf((short)50)),
     CheckOnline(Short.valueOf((short)51)),
     SystemStats((short)1000),
     SetPoolSize((short)1001),
     SetLogLevel((short)1002),
     CrossCommand((short)200),
     ServiceNotify((short)201),
     DashBoard((short)9001),
     IpFilterCommand((short)8000),
     CrossExtCommand((short)300),
     ExecuteCommand((short)9000);

     private Object id;

     public static SystemRequest fromId(Object id) {
          SystemRequest req = null;
          SystemRequest[] asystemrequest;
          int j = (asystemrequest = values()).length;

          for(int i = 0; i < j; ++i) {
               SystemRequest item = asystemrequest[i];
               if (item.getId().equals(id)) {
                    req = item;
                    break;
               }
          }

          if (req == null) {
               req = CallExtension;
          }

          return req;
     }

     private SystemRequest(Object id) {
          this.id = id;
     }

     public Object getId() {
          return this.id;
     }
}
