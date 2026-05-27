package com.vinplay.vbee.common.config;

import com.vinplay.vbee.common.utils.CommonUtils;

public class VBeePath {
    public static String basePath = "";
    /**
     * init base path of project
     */
    public static String initBasePath(Class cls) {
        basePath = CommonUtils.getBasePath(cls);
        return basePath;
    }
}
