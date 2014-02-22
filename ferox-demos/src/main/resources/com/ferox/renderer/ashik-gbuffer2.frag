#version 150

const float PI = 3.1415927;

uniform sampler2D uNormalTexA;
uniform sampler2D uNormalTexB;

uniform float uNormalAlpha;

uniform vec2 uTCScale;

uniform mat4 uModel;

in mat3 vTanToView;
in vec3 vViewDir;
in vec2 vTC;

out vec4 fNormalTangent;
out vec4 fTCAndView;


vec3 specularNormal() {
    vec3 n1 = 2.0 * texture(uNormalTexA, vTC * uTCScale.x).rgb - 1.0;
    vec3 n2 = 2.0 * texture(uNormalTexB, vTC * uTCScale.y).rgb - 1.0;
    return normalize((1.0 - uNormalAlpha) * n1 + uNormalAlpha * n2);
}

vec2 encode(vec3 n) {
    vec2 r = normalize(n.xy) * sqrt(-n.z * 0.5 + 0.5);
    r = 0.5 * r + 0.5;
    return r;
}

void main() {
    vec3 tN = specularNormal();
    vec3 tT = normalize(cross(vec3(0.0, 1.0, 0.0), tN));
    tN = normalize(vTanToView * tN);
    tT = normalize(vTanToView * tT);

    fNormalTangent = vec4(encode(tN), encode(tT));
    fTCAndView = vec4(vTC, encode(vViewDir));
}
