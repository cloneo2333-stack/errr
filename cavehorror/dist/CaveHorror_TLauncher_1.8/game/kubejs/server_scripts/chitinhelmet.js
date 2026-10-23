ServerEvents.recipes(event => {
    event.remove({
        output: 'arphex:chitin_armour_helmet'
    });
    event.shapeless(
        '1x arphex:chitin_armour_helmet',
        [
            'minecraft:golden_helmet',
            'arphex:chitin',
            'arphex:chitin'
        ]
    );
});