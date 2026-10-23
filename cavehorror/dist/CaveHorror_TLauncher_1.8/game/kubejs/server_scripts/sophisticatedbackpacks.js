ServerEvents.recipes(event => {
    event.remove({
        output: 'sophisticatedbackpacks:backpack'
    });
    event.shaped(
        '1x sophisticatedbackpacks:backpack',
        [
            'BAB',
            'BCB',
            'AAA'
        ],
        {
            A: 'minecraft:rabbit_hide',
            B: 'minecraft:string',
            C: 'minecraft:chest'
        }
    );
});