#version 150

#define s2(a, b)				temp = a; a = min(a, b); b = max(temp, b);
#define mn3(a, b, c)			s2(a, b); s2(a, c);
#define mx3(a, b, c)			s2(b, c); s2(a, c);

#define mnmx3(a, b, c)			mx3(a, b, c); s2(a, b);                                   // 3 exchanges
#define mnmx4(a, b, c, d)		s2(a, b); s2(c, d); s2(a, c); s2(b, d);                   // 4 exchanges
#define mnmx5(a, b, c, d, e)	s2(a, b); s2(c, d); mn3(a, c, e); mx3(b, d, e);           // 6 exchanges
#define mnmx6(a, b, c, d, e, f) s2(a, d); s2(b, e); s2(c, f); mn3(a, b, c); mx3(d, e, f); // 7 exchanges

const float PI = 3.1415927;

const mat3 RGB_TO_XYZ = mat3(0.4124, 0.2126, 0.0193, 0.3576, 0.7152, 0.1192, 0.1805, 0.0722, 0.9502);
const mat3 XYZ_TO_RGB = mat3(3.2406, -0.9689, 0.0557, -1.5372, 1.8758, -0.2040, -0.4986, 0.0415, 1.0570);
uniform sampler2D uDiffuse;
uniform sampler2D uSpecular;
uniform sampler2D uLuminance;

uniform sampler2D uDepth;

uniform float uPreScale;
uniform float uPostScale;
uniform float uBurn;
uniform float uAvgLuminance;
uniform float uLocality;

in vec2 vTC;
out vec4 fColor;

void main() {
    // med3 filter the specular to remove noise
    vec3 v[9];

    // Add the pixels which make up our window to the pixel array.
    ivec2 iTC = ivec2(gl_FragCoord.xy);
    vec4 spec;
    for(int dX = -1; dX <= 1; ++dX) {
        for(int dY = -1; dY <= 1; ++dY) {
            ivec2 offset = ivec2(dX, dY);

            vec4 spec = texelFetch(uSpecular, iTC + offset, 0);
            if (spec.a > 0.0) {
                spec = spec / spec.a;

            }
            // If a pixel in the window is located at (x+dX, y+dY), put it at index (dX + R)(2R + 1) + (dY + R) of the
            // pixel array. This will fill the pixel array, with the top left pixel of the window at pixel[0] and the
            // bottom right pixel of the window at pixel[N-1].
            v[(dX + 1) * 3 + (dY + 1)] = spec.rgb;
        }
    }

    vec3 temp;

    // Starting with a subset of size 6, remove the min and max each time
    mnmx6(v[0], v[1], v[2], v[3], v[4], v[5]);
    mnmx5(v[1], v[2], v[3], v[4], v[6]);
    mnmx4(v[2], v[3], v[4], v[7]);
    mnmx3(v[3], v[4], v[8]);

    vec3 high = texture(uDiffuse, vTC).rgb + v[4];
    vec3 color = RGB_TO_XYZ * high;

    float Ywa = uAvgLuminance;
    if (Ywa < 0.0)
        Ywa = textureLod(uLuminance, vTC, uLocality).r;

    float alpha = 0.1;

    float Yw = uPreScale * alpha * uBurn;
    float invY2 = 1.0 / (Yw * Yw);
    float pScale = uPostScale * uPreScale * alpha / Ywa;

    color = color * (pScale * (1.0 + color.g * invY2) / (1.0 + color.g));
    color = XYZ_TO_RGB * color;

    fColor = vec4(pow(color, vec3(1.0 / 2.2)), 1.0);
    gl_FragDepth = texture(uDepth, vTC).r;
}
