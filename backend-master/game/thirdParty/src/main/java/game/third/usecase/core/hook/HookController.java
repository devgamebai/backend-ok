/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package game.third.usecase.core.hook;

import game.third.usecase.core.hook.Context;
import game.third.usecase.core.hook.HookProcessor;
import game.third.usecase.core.hook.NoHookRegistered;
import game.third.usecase.core.hook.NoWhitelistRegistered;
import game.third.usecase.core.hook.Param;
import game.third.usecase.core.hook.ProcessorInfo;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;

public class HookController<T, R> {
    private List<ProcessorInfoExtra> listProcessor = new ArrayList<ProcessorInfoExtra>();
    private static final Logger logger = Logger.getLogger((String)"api");

    public void initPaths(Map<String, ProcessorInfo> commandMap, Context context) throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        for (String url : commandMap.keySet()) {
            ProcessorInfo processorInfo = commandMap.get(url);
            if (processorInfo == null) continue;
            try {
                Class<?> clazz = Class.forName(processorInfo.getPath());
                Constructor<?> ctor = clazz.getConstructor(new Class[0]);
                HookProcessor processor = (HookProcessor)ctor.newInstance(new Object[0]);
                ProcessorInfoExtra infoExtra = new ProcessorInfoExtra();
                infoExtra.processor = processor;
                infoExtra.path = url;
                infoExtra.whiteList = processorInfo.getWhiteList();
                processor.context(context);
                this.listProcessor.add(infoExtra);
            }
            catch (Exception ex) {
                logger.info((Object)("HookController: " + ex.getMessage()));
            }
        }
    }

    public R processHook(String path, String ip, Param<T> param) throws Exception {
        for (ProcessorInfoExtra processorInfoExtra : this.listProcessor) {
            if (!processorInfoExtra.matches(path)) continue;
            if (!processorInfoExtra.isIPWhitelisted(ip) && !processorInfoExtra.isIPWhitelisted("*")) {
                System.out.println("Ip " + ip + " not access!");
                throw new NoWhitelistRegistered("Ip " + ip + " not access path  !");
            }
            return processorInfoExtra.processor.execute(param);
        }
        throw new NoHookRegistered("Path " + path + " not process");
    }

    public class ProcessorInfoExtra
    extends ProcessorInfo {
        public HookProcessor<T, R> processor;

        public boolean matches(String urlPath) {
            return Pattern.matches(this.getPath(), urlPath);
        }

        public boolean isIPWhitelisted(String ip) {
            return this.getWhiteList().contains(ip);
        }
    }
}

