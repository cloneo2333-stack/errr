LootJS.modifiers(event => {
  const TERMITE = "born_in_chaos_v1:diamond_termite";
  const SHARD   = "born_in_chaos_v1:diamond_termite_shard";
  event
    .addEntityLootModifier(TERMITE)
    .removeLoot(SHARD)
    .addLoot("minecraft:diamond");
});