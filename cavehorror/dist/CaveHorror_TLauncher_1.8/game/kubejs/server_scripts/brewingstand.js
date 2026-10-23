ServerEvents.recipes(event => {
    event.remove({
        output: 'minecraft:brewing_stand'
    });
    event.shaped(
        '1x minecraft:brewing_stand',
        [
            '   ',
            ' A ',
            'BBB'
        ],
        {
            A: 'minecraft:blaze_rod',
            B: 'minecraft:smooth_stone'
        }
    );
});