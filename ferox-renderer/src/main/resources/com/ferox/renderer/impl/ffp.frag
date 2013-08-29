#version 150

uniform int uAlphaComparison; // ordinal of Comparison, or -1 to disable
uniform float uAlphaRefValue;

uniform vec3 uFogConfig; // 0 = start/density 1 = end 2 = signal (0 = linear, > = exp, < = exp squared)
uniform vec4 uFogColor;
uniform bool uEnableFog;

// only one of the 4 sampler types will actually be in use, but we know 16 units are available because that
// is the minimum required for OpenGL 3
uniform sampler1D uTex1D[4];
uniform sampler2D uTex2D[4];
uniform samplerCube uTexCube[4];
uniform sampler2DShadow uTexShadow[4];
uniform int uTexConfig[4]; // neg. value is disabled, 0 = 1D, 1 = 2D, 2 = cube, 3 = 2D shadow

uniform ivec3 uCombineSrcAlpha[4]; // xyz represent 0,1,2 arguments to the functions
uniform ivec3 uCombineSrcRGB[4]; // values are ordinal of CombineSource and CombineOperand
uniform ivec3 uCombineOpAlpha[4];
uniform ivec3 uCombineOpRGB[4];
uniform int uCombineFuncAlpha[4];
uniform int uCombineFuncRGB[4];
uniform vec4 uCombineColor[4];

in vec4 vPrimaryColor;
in vec4 vSecondaryColor;
in vec4 vTexCoord[4];
in vec4 vEyePos;

out vec4 fColor;

bool compare(const float testValue, const float refValue, const int test) {
    switch(test) {
        case 0: // EQUAL
            return testValue == refValue;
        case 1: // GREATER
            return testValue > refValue;
        case 2: // LESS
            return testValue < refValue;
        case 3: // GEQUAL
            return testValue >= refValue;
        case 4: // LEQUAL
            return testValue <= refValue;
        case 5: // NOT_EQUAL
            return testValue != refValue;
        case 6: // NEVER
            return false;
        default: // ALWAYS
            return true;
    }
}

vec4 sampleTexture(const int tex) {
    switch(uTexConfig[tex]) {
        case 0: // 1D
            return texture(uTex1D[tex], vTexCoord[tex].s / vTexCoord[tex].q);
            //return vec4(1.0, 0.0, 0.0, 1.0);
        case 1: // 2D
            return texture(uTex2D[tex], vTexCoord[tex].st / vTexCoord[tex].q);
            //return vec4(0.0, 1.0, 0.0, 1.0);
        case 2: // CUBE
            return texture(uTexCube[tex], vTexCoord[tex].stp); // no divide needed for cube maps
            //return vec4(1.0, 1.0, 0.0, 1.0);
        case 4: // 2D Shadow
            return vec4(texture(uTexShadow[tex], vTexCoord[tex].stp / vTexCoord[tex].q));
        default: // disabled
            return vec4(0.0, 0.0, 0.0, 1.0);
    }
}

vec4 select(const int arg, const int unit, const vec4 tex[4], const vec4 prevTex) {
    vec4 selectRGB = vec4(0.0, 0.0, 0.0, 1.0);
    float selectAlpha = 0.0;
    vec3 postRGB = vec3(0.0, 0.0, 0.0);
    float postAlpha = 0.0;

    // compute postRGB = opRGB(srcRBG)
    switch(uCombineSrcRGB[unit][arg]) {
        case 0: // CURR_TEX
            selectRGB = tex[unit];
            break;
        case 1: // PREV_TEX
            selectRGB = prevTex;
            break;
        case 2: // CONSTANT
            selectRGB = uCombineColor[unit];
            break;
        case 3: // PRIMARY
            selectRGB = vPrimaryColor;
            break;
        case 4: // TEX0
            selectRGB = tex[0];
            break;
        case 5: // TEX1
            selectRGB = tex[1];
            break;
        case 6: // TEX2
            selectRGB = tex[2];
            break;
        case 7: // TEX3
            selectRGB = tex[3];
            break;
    }
    switch(uCombineOpRGB[unit][arg]) {
        case 0: // COLOR
            postRGB = selectRGB.rgb;
            break;
        case 1: // ALPHA
            postRGB = vec3(selectRGB.a);
            break;
        case 2: // ONE_MINUS_COLOR
            postRGB = vec3(1.0) - selectRGB.rgb;
            break;
        case 3: // ONE_MINUS_ALPHA
            postRGB = vec3(1.0 - selectRGB.a);
            break;
    }

    // compute postAlpha = opAlpha(srcAlpha)
    switch(uCombineSrcAlpha[unit][arg]) {
        case 0: // CURR_TEX
            selectAlpha = tex[0].a;
            break;
        case 1: // PREV_TEX
            selectAlpha = prevTex.a;
            break;
        case 2: // CONSTANT
            selectAlpha = uCombineColor[unit].a;
            break;
        case 3: // PRIMARY
            selectAlpha = vPrimaryColor.a;
            break;
        case 4: // TEX0
            selectAlpha = tex[0].a;
            break;
        case 5: // TEX1
            selectAlpha = tex[1].a;
            break;
        case 6: // TEX2
            selectAlpha = tex[2].a;
            break;
        case 7: // TEX3
            selectAlpha = tex[3].a;
            break;
    }
    switch(uCombineOpAlpha[unit][arg]) {
        case 1: // ALPHA
            postAlpha = selectAlpha;
            break;
        case 3: // ONE_MINUS_ALPHA
            postAlpha = 1.0 - selectAlpha;
            break;
        // 0 and 2 (COLOR and ONE_MINUS_COLOR) are not valid for opAlpha
    }

    return vec4(postRGB, postAlpha);
}

float dot3(vec3 arg0, vec3 arg1) {
    return 4.0 * dot(arg0 - vec3(0.5), arg1 - vec3(0.5));
}

vec4 evaluateUnit(const int unit, const vec4 tex[4], const vec4 prevColor) {
    vec4 arg0 = select(0, unit, tex, prevColor);
    vec4 arg1 = select(1, unit, tex, prevColor);
    vec4 arg2 = select(2, unit, tex, prevColor);

    float alpha = 0.0;
    switch(uCombineFuncAlpha[unit]) {
        case 0: // REPLACE
            alpha = arg0.a;
            break;
        case 1: // MODULATE
            alpha = arg0.a * arg1.a;
            break;
        case 2: // ADD
            alpha = arg0.a + arg1.a;
            break;
        case 3: // ADD_SIGNED
            alpha = arg0.a + arg1.a - 0.5;
            break;
        case 4: // INTERPOLATE
            alpha = arg0.a * arg2.a + arg1.a * (1.0 - arg2.a);
            break;
        case 5: // SUBTRACT
            alpha = arg0.a - arg1.a;
            break;
        // ignore 6 and 7 (DOT3_RGB... since they ignore alpha)
    }

    vec3 rgb = vec3(0.0);
    switch(uCombineFuncRGB[unit]) {
        case 0: // REPLACE
            rgb = arg0.rgb;
            break;
        case 1: // MODULATE
            rgb = arg0.rgb * arg1.rgb;
            break;
        case 2: // ADD
            rgb = arg0.rgb + arg1.rgb;
            break;
        case 3: // ADD_SIGNED
            rgb = arg0.rgb + arg1.rgb - vec3(0.5);
            break;
        case 4: // INTERPOLATE
            rgb = arg0.rgb * arg2.rgb + arg1.rgb * (vec3(1.0) - arg2.rgb);
            break;
        case 5: // SUBTRACT
            rgb = arg0.rgb - arg1.rgb;
            break;
        case 6: // DOT3_RGB
            rgb = vec3(dot3(arg0.rgb, arg1.rgb));
            break;
        case 7: // DOT3_RGBA
            float d = dot3(arg0.rgb, arg1.rgb);
            rgb = vec3(d);
            alpha = d; // override the alpha
            break;
    }

    return vec4(rgb, alpha);
}

void main() {
    // texturing
    // FIXME hella slow
    vec4 tex[4];
    tex[0] = sampleTexture(0);
    tex[1] = sampleTexture(1);
    tex[2] = sampleTexture(2);
    tex[3] = sampleTexture(3);

    vec4 prev = vPrimaryColor;
    if (uTexConfig[0] >= 0) {
        prev = evaluateUnit(0, tex, prev);
    }
    if (uTexConfig[1] >= 0) {
        prev = evaluateUnit(1, tex, prev);
    }
    if (uTexConfig[2] >= 0) {
        prev = evaluateUnit(2, tex, prev);
    }
    if (uTexConfig[3] >= 0) {
        prev = evaluateUnit(3, tex, prev);
    }

    // combine primary and secondary colors
    vec4 color = prev + vSecondaryColor;

    // fog
    if (uEnableFog) {
        float eyeDepth = abs(vEyePos.z);

        float factor = 0.0;
        if (uFogConfig.z > 0) {
            // EXP
            factor = exp(-uFogConfig.x * eyeDepth);
        } else if (uFogConfig.z < 0) {
            // EXP2
            factor = exp(-(uFogConfig.x * uFogConfig.x * eyeDepth * eyeDepth));
        } else {
            // LINEAR
            factor = (uFogConfig.y - eyeDepth) / (uFogConfig.y - uFogConfig.x);
        }

        // clamp to [0, 1]
        if (factor < 0.0) {
            factor = 0.0;
        } else if (factor > 1.0) {
            factor = 1.0;
        }

        // blend with fog color
        color = factor * color + (1.0 - factor) * uFogColor;
    }

    // alpha test
    if (uAlphaComparison >= 0) {
        if (!compare(color.a, uAlphaRefValue, uAlphaComparison)) {
            discard;
        }
    }

    fColor = color;
}
