LootJS.modifiers(event => {
    const LIFESTEALER = "born_in_chaos_v1:lifestealer_true_form";
    const SKULL = "born_in_chaos_v1:lifestealer_skull";
    event
        .addEntityLootModifier(LIFESTEALER)
        .removeLoot(SKULL)
        .randomChance(0.5)
        .addLoot(SKULL);
});