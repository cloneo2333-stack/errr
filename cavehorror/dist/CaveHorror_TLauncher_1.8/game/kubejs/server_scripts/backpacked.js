ServerEvents.recipes(event => {
    event.remove({
        output: 'backpacked:backpack'
    });
    event.shaped(
        '1x backpacked:backpack',
        [
            'AAA',
            'BCB',
            'AAA'
        ],
        {
            A: 'minecraft:leather',
            B: 'minecraft:string',
            C: 'minecraft:iron_ingot'
        }
    );
});