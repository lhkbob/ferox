#version 150

uniform mat4 uProjection;

in vec4 aPos;
in vec2 aTC;

out vec2 vTC;
//out vec2 vGaussTC[16];

void main() {
//    vGaussTC[0] = aTC + vec2(1.5 / 800.0, 1.5 / 800.0);
//    vGaussTC[1] = aTC + vec2(1.5 / 800.0, 0.5 / 800.0);
//    vGaussTC[2] = aTC + vec2(1.5 / 800.0, -0.5 / 800.0);
//    vGaussTC[3] = aTC + vec2(1.5 / 800.0, -1.5 / 800.0);
//    vGaussTC[4] = aTC + vec2(0.5 / 800.0, 1.5 / 800.0);
//    vGaussTC[5] = aTC + vec2(0.5 / 800.0, 0.5 / 800.0);
//    vGaussTC[6] = aTC + vec2(0.5 / 800.0, -0.5 / 800.0);
//    vGaussTC[7] = aTC + vec2(0.5 / 800.0, -1.5 / 800.0);
//    vGaussTC[8] = aTC + vec2(-0.5 / 800.0, 1.5 / 800.0);
//    vGaussTC[9] = aTC + vec2(-0.5 / 800.0, 0.5 / 800.0);
//    vGaussTC[10] = aTC + vec2(-0.5 / 800.0, -0.5 / 800.0);
//    vGaussTC[11] = aTC + vec2(-0.5 / 800.0, -1.5 / 800.0);
//    vGaussTC[12] = aTC + vec2(-1.5 / 800.0, 1.5 / 800.0);
//    vGaussTC[13] = aTC + vec2(-1.5 / 800.0, 0.5 / 800.0);
//    vGaussTC[14] = aTC + vec2(-1.5 / 800.0, -0.5 / 800.0);
//    vGaussTC[15] = aTC + vec2(-1.5 / 800.0, -1.5 / 800.0);

    vTC = aTC;
    gl_Position = uProjection * aPos;
}
