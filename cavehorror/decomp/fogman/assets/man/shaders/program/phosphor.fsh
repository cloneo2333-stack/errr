#version 150



// settings

const float phosphor_amount = 0.98; // makes bright pixels stay on screen longer
const float input_curve = 0.5; // this changes how bright the pixels need to be. Making this lower means that darker pixels can stay on screen longer



uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;

out vec4 fragColor;

void main() {
	vec3 CurrTexel = texture(DiffuseSampler, texCoord).rgb;
	vec3 PrevTexel = texture(PrevSampler, texCoord).rgb;
	
	vec3 color = pow(CurrTexel, vec3(input_curve));
	vec3 prevColor = pow(PrevTexel, vec3(input_curve));
	vec3 blurred = max(color, prevColor * phosphor_amount);
	vec3 uncurved = pow(blurred, vec3(1.0 / input_curve)) * 0.01;
	
    fragColor = vec4(uncurved, 1.0);
}
