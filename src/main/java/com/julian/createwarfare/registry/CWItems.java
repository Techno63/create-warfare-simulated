package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.items.GeigerCounterItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

public class CWItems {
    public static final CreateRegistrate REGISTRATE = CreateWarfare.REGISTRATE;

    public static final ItemEntry<GeigerCounterItem> GEIGER_COUNTER = REGISTRATE
            .item("geiger_counter", GeigerCounterItem::new)
            .properties(p -> p.stacksTo(1))
            .lang("Geiger Counter")
            .register();

    public static void register() {}
}