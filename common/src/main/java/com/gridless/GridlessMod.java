package com.gridless;

import com.gridless.config.GridlessConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GridlessMod {
    public static final String MOD_ID = "gridless";
    public static final String MOD_NAME = "Gridless Crafting";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static void init() {
        LOGGER.info("Initializing Gridless Crafting (Terraria-Style)...");
        GridlessConfig.load();
    }
}
