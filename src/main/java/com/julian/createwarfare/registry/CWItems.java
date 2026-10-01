package com.julian.createwarfare.registry;

import com.julian.createwarfare.CreateWarfare;
import com.julian.createwarfare.items.GeigerCounterItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

public class CWItems {
    public static final CreateRegistrate REGISTRATE = CreateWarfare.REGISTRATE;

    public static final ItemEntry<GeigerCounterItem> GEIGER_COUNTER = REGISTRATE
            .item("geiger_counter", GeigerCounterItem::new)
            .properties(p -> p.stacksTo(1))
            .lang("Geiger Counter")
            .register();

    public static final ItemEntry<Item> NATURAL_URANIUM = REGISTRATE
            .item("natural_uranium", Item::new)
            .lang("Natural Uranium")
            .register();

    public static final ItemEntry<Item> LOW_ENRICHED_URANIUM = REGISTRATE
            .item("low_enriched_uranium", Item::new)
            .lang("Low-Enriched Uranium")
            .register();

    public static final ItemEntry<Item> HIGHLY_ENRICHED_URANIUM = REGISTRATE
            .item("highly_enriched_uranium", Item::new)
            .lang("Highly-Enriched Uranium")
            .register();

    public static final ItemEntry<Item> WEAPONS_GRADE_URANIUM = REGISTRATE
            .item("weapons_grade_uranium", Item::new)
            .lang("Weapons-Grade Uranium")
            .register();

    public static final ItemEntry<Item> DEPLETED_URANIUM = REGISTRATE
            .item("depleted_uranium", Item::new)
            .lang("Depleted Uranium")
            .register();

    public static void register() {}
}