ServerEvents.recipes(event => {
    event.remove({
        output: 'dixtas_armory:diamond_battle_axe'
    });
    event.shaped(
        '1x dixtas_armory:diamond_battle_axe',
        [
            'BCB',
            'BAB',
            ' A '
        ],
        {
            A: 'minecraft:stick',
            B: 'minecraft:diamond',
            C: 'dixtas_armory:diamond_longsword'
        }
    );
});