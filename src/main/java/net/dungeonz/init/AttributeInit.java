package net.dungeonz.init;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class AttributeInit {

    // Must be a static field initializer: createMobAttributes() is called from EntityType's
    // static initializer, which runs before onInitialize(), so init() would be too late.
    public static final EntityAttribute SPELL_POWER = Registry.register(
        Registries.ATTRIBUTE,
        new Identifier("dungeonz", "spell_power"),
        new ClampedEntityAttribute("attribute.dungeonz.spell_power", 1.0, 0.0, 2048.0).setTracked(true)
    );

    public static void init() {}

}
