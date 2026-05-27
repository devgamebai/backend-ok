package bitzero.server.controllers.admin;

import bitzero.engine.controllers.IController;
import bitzero.engine.controllers.IControllerManager;
import bitzero.engine.core.BitZeroEngine;
import bitzero.engine.io.IRequest;
import bitzero.engine.sessions.ISession;
import bitzero.server.BitZeroServer;
import bitzero.server.controllers.BaseControllerCommand;
import bitzero.server.controllers.SystemRequest;
import bitzero.server.controllers.admin.utils.PerformanceMonitor;
import bitzero.server.entities.managers.ConnectionStats;
import bitzero.server.entities.managers.IStatsManager;
import bitzero.server.extensions.data.DataCmd;
import bitzero.server.extensions.data.SimpleMsg;
import bitzero.server.util.executor.SmartThreadPoolExecutor;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;

public class DashBoardCommand extends BaseControllerCommand {
     private PerformanceMonitor cpuMonitor = new PerformanceMonitor();
     private IStatsManager statsManager;

     public DashBoardCommand() {
          super(SystemRequest.DashBoard);
          this.cpuMonitor.getCpuUsage();
          this.statsManager = BitZeroServer.getInstance().getStatsManager();
     }

     public boolean validate(IRequest irequest) {
          return this.checkSuperAdmin(irequest.getSender());
     }

     public void execute(IRequest irequest) throws Exception {
          DataCmd cmd = new DataCmd(((ByteBuffer)irequest.getContent()).array());
          this.getData(irequest.getSender(), cmd.readBoolean());
     }

     private void getData(ISession sender, boolean history) {
          SimpleMsg outParams = new SimpleMsg(this.getId());
          outParams.putIntArray(this.bz.getUptime().toArray());
          outParams.putInt(roundToDecimals(this.cpuMonitor.getCpuUsage() * 100.0D, 2));
          outParams.putLong(Runtime.getRuntime().freeMemory());
          outParams.putLong(Runtime.getRuntime().maxMemory());
          outParams.putLong(Runtime.getRuntime().totalMemory());
          long totalThreadsCpuTime = 0L;
          long[] threadIds = ManagementFactory.getThreadMXBean().getAllThreadIds();
          outParams.putShort((short)threadIds.length);

          for(int i = 0; i < threadIds.length; ++i) {
               long id = threadIds[i];
               outParams.putLong(id);
               outParams.putString(ManagementFactory.getThreadMXBean().getThreadInfo(id).getThreadName());
               long cpuTime = 0L;
               if (ManagementFactory.getThreadMXBean().isThreadCpuTimeSupported() && ManagementFactory.getThreadMXBean().isThreadCpuTimeEnabled()) {
                    cpuTime = ManagementFactory.getThreadMXBean().getThreadCpuTime(id);
                    totalThreadsCpuTime += cpuTime;
               }

               outParams.putLong(cpuTime);
          }

          outParams.putLong(totalThreadsCpuTime);
          if (history) {
               outParams.putInt(this.statsManager.getOutgoingTrafficMeter().getSamplingRateMinutes());
               outParams.putInt(Math.round((float)((System.currentTimeMillis() - this.statsManager.getOutgoingTrafficMeter().getLastUpdateMillis()) / 1000L)));
          }

          outParams.putLong(this.statsManager.getTotalOutBytes() / 1024L);
          outParams.putLong(this.statsManager.getTotalInBytes() / 1024L);
          outParams.putLong(this.statsManager.getTotalOutgoingDroppedPackets());
          outParams.putLong(this.statsManager.getTotalIncomingDroppedPackets());
          outParams.putLong(this.statsManager.getTotalOutPackets());
          outParams.putLong(this.statsManager.getTotalInPackets());
          ConnectionStats sessionStats = this.statsManager.getSessionStats();
          outParams.putInt(sessionStats.getSocketCount());
          outParams.putInt(sessionStats.getNpcCount());
          outParams.putInt(this.bz.getUserManager().getUserCountByName());
          outParams.putInt(BitZeroEngine.getInstance().getSessionManager().getHighestCCS());
          outParams.putInt(BitZeroServer.getInstance().getUserManager().getHighestCCU());
          outParams.putInt(BitZeroEngine.getInstance().getEngineWriter().getQueueSize());
          outParams.putInt(BitZeroEngine.getInstance().getEngineWriter().getThreadPoolSize());
          IControllerManager cmg = BitZeroEngine.getInstance().getControllerManager();
          IController sysCtrl = cmg.getControllerById((byte)0);
          outParams.putInt(sysCtrl.getQueueSize());
          outParams.putInt(sysCtrl.getMaxQueueSize());
          outParams.putInt(sysCtrl.getThreadPoolSize());
          IController extCtrl = cmg.getControllerById((byte)1);
          outParams.putInt(extCtrl.getQueueSize());
          outParams.putInt(extCtrl.getMaxQueueSize());
          outParams.putInt(extCtrl.getThreadPoolSize());
          outParams.putLong(this.statsManager.getTotalOutBytesWebsocket() / 1024L);
          outParams.putLong(this.statsManager.getTotalInBytesWebsocket() / 1024L);
          outParams.putLong(this.statsManager.getTotalOutPacketsWebsocket());
          outParams.putLong(this.statsManager.getTotalInPacketsWebsocket());
          outParams.putLong(this.statsManager.getTotalIncomingDroppedWebsocketFrames());
          outParams.putLong(this.statsManager.getTotalIncomingDroppedWebsocketPackets());
          outParams.putInt(sessionStats.getWebsocketSessionCount());
          SmartThreadPoolExecutor systemThreadPool = (SmartThreadPoolExecutor)BitZeroServer.getInstance().getSystemThreadPool();
          outParams.putInt(systemThreadPool.getQueue().size());
          outParams.putInt(systemThreadPool.getQueueSizeTriggeringBackup());
          outParams.putInt(systemThreadPool.getPoolSize());
          outParams.putIntArray(BitZeroEngine.getInstance().getSessionManager().getReconnectStatus());
          this.send(sender, outParams);
     }

     private static int roundToDecimals(double d, int c) {
          int temp = (int)(d * Math.pow(10.0D, (double)c));
          return temp;
     }
}
