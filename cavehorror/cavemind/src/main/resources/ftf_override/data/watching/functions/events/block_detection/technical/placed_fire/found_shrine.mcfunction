tag @e[type=marker,tag=fireTrace,sort=nearest,limit=1,distance=..6] add logged
function watching:events/start_events
execute if score OGshrineMechanicConfig ftf.configOptions matches 1 as @e[type=marker,tag=fireTrace,sort=nearest,limit=1,distance=..6] at @s align xz run summon lightning_bolt ~0.5 ~ ~0.5
playsound watching:message.warning master @s
execute if entity @s[advancements={watching:main/root=true}] run advancement grant @s only watching:main/built_shrine
execute as @s[type=player] run cavemind hero shrine
