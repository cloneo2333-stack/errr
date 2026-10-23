ServerEvents.recipes(event => {
    event.remove({
        output: 'minecraft:enchanting_table'
    });
    event.shaped(
        '1x minecraft:enchanting_table',
        [
            'DCD',
            'BAB',
            'AAA'
        ],
        {
            A: 'minecraft:obsidian',
            B: 'minecraft:diamond',
            C: 'minecraft:book',
            D: 'minecraft:red_wool'
        }
    );
});