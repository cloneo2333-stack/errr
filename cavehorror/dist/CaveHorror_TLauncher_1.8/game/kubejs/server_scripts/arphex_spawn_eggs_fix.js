const ARPHEX_CONSUMABLE_EGGS = [
    'arphex:centipede_evictor_egg',
    'arphex:centipede_stalker_egg',
    'arphex:spider_snatcher_egg',
    'arphex:ant_arsonist_egg'
]

BlockEvents.rightClicked(event => {
    if (event.level.clientSide) return
    if (!ARPHEX_CONSUMABLE_EGGS.includes(event.item.id)) return
    if (event.player.isCreative()) return

    const hand = event.hand
    const beforeCount = event.item.count

    event.server.scheduleInTicks(1, () => {
        const stack = hand == 'OFF_HAND'
            ? event.player.offHandItem
            : event.player.mainHandItem

        if (!ARPHEX_CONSUMABLE_EGGS.includes(stack.id)) return
        if (stack.count >= beforeCount) {
            stack.shrink(1)
        }
    })
})