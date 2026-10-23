ServerEvents.recipes(event => {
    event.remove({
        output: 'minecraft:leather'
    });
    event.shapeless(
        '1x minecraft:rabbit_hide',
        [
            'minecraft:leather',
            'minecraft:leather',
            'minecraft:leather',
            'minecraft:leather'
        ]
    );
});