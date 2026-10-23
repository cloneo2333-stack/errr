ServerEvents.recipes(event => {
    event.remove({
        output: 'minecraft:ender_eye'
    });
    event.shapeless(
        '1x minecraft:ender_eye',
        [
            'minecraft:ender_pearl',
            'minecraft:blaze_powder',
            'minecraft:blaze_powder'
        ]
    );
});