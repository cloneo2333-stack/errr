ServerEvents.recipes(event => {
    event.remove({
        output: 'dixtas_armory:diamond_longsword'
    });
    event.shaped(
        '1x dixtas_armory:diamond_longsword',
        [
            '  A',
            'AB ',
            'CA '
        ],
        {
            A: 'minecraft:diamond',
            B: 'minecraft:diamond_sword',
            C: 'minecraft:stick'
        }
    );
});