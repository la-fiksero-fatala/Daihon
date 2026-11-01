// MIT License

// Copyright (c) 2019-2021 bloc97
// All rights reserved.

// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:

// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

// Adapted for WGSL by Daihon. Source: bloc97/Anime4K @ 7684e9586f8dcc738af08a1cdceb024cc184f426
// Gaussian/luma correction follows Upscale_DoG_x2 at source resolution. Catmull-Rom
// performs the sole resize to the viewport; alpha is preserved. No neural model.
//!HALO 3
//!DESC Anime4K DoG luma
//!HOOK MAIN
//!BIND HOOKED
//!SAVE LUMA
//!WHEN OUTPUT.w HOOKED.w / 1 > OUTPUT.h HOOKED.h / 1 > *
//!COMPUTE 8 8
@group(0) @binding(0) var input: texture_2d<f32>;
@group(0) @binding(1) var output: texture_storage_2d<rgba16float, write>;
@compute @workgroup_size(8, 8)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    if (any(id.xy >= textureDimensions(output))) { return; }
    let color = textureLoad(input, vec2<i32>(id.xy), 0);
    let luma = dot(color.rgb, vec3<f32>(0.299, 0.587, 0.114));
    textureStore(output, vec2<i32>(id.xy), vec4<f32>(luma, 0.0, 0.0, 1.0));
}

//!DESC Anime4K DoG horizontal Gaussian and range
//!HOOK MAIN
//!BIND LUMA
//!SAVE GAUSS_X
//!WHEN OUTPUT.w HOOKED.w / 1 > OUTPUT.h HOOKED.h / 1 > *
//!COMPUTE 8 8
@group(0) @binding(0) var input: texture_2d<f32>;
@group(0) @binding(1) var output: texture_storage_2d<rgba16float, write>;
fn load(p: vec2<i32>) -> f32 {
    return textureLoad(input, clamp(p, vec2<i32>(0), vec2<i32>(textureDimensions(input)) - 1), 0).r;
}
@compute @workgroup_size(8, 8)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    if (any(id.xy >= textureDimensions(output))) { return; }
    let p = vec2<i32>(id.xy);
    let a = load(p - vec2<i32>(1, 0));
    let b = load(p);
    let c = load(p + vec2<i32>(1, 0));
    let g = (load(p - vec2<i32>(2, 0)) + load(p + vec2<i32>(2, 0))) * 0.06136
        + (a + c) * 0.24477 + b * 0.38774;
    textureStore(output, p, vec4<f32>(g, min(a, min(b, c)), max(a, max(b, c)), 1.0));
}

//!DESC Anime4K DoG vertical Gaussian and range
//!HOOK MAIN
//!BIND GAUSS_X
//!SAVE GAUSS
//!WHEN OUTPUT.w HOOKED.w / 1 > OUTPUT.h HOOKED.h / 1 > *
//!COMPUTE 8 8
@group(0) @binding(0) var input: texture_2d<f32>;
@group(0) @binding(1) var output: texture_storage_2d<rgba16float, write>;
fn load(p: vec2<i32>) -> vec3<f32> {
    return textureLoad(input, clamp(p, vec2<i32>(0), vec2<i32>(textureDimensions(input)) - 1), 0).rgb;
}
@compute @workgroup_size(8, 8)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    if (any(id.xy >= textureDimensions(output))) { return; }
    let p = vec2<i32>(id.xy);
    let a = load(p - vec2<i32>(0, 1));
    let b = load(p);
    let c = load(p + vec2<i32>(0, 1));
    let g = (load(p - vec2<i32>(0, 2)).r + load(p + vec2<i32>(0, 2)).r) * 0.06136
        + (a.r + c.r) * 0.24477 + b.r * 0.38774;
    textureStore(output, p, vec4<f32>(g, min(a.g, min(b.g, c.g)), max(a.b, max(b.b, c.b)), 1.0));
}

//!DESC Anime4K DoG source-space clamped luma correction
//!HOOK MAIN
//!BIND HOOKED
//!BIND LUMA
//!BIND GAUSS
//!WHEN OUTPUT.w HOOKED.w / 1 > OUTPUT.h HOOKED.h / 1 > *
//!COMPUTE 8 8
@group(0) @binding(0) var input: texture_2d<f32>;
@group(0) @binding(1) var luma: texture_2d<f32>;
@group(0) @binding(2) var gauss: texture_2d<f32>;
@group(0) @binding(3) var linearSampler: sampler;
@group(0) @binding(4) var output: texture_storage_2d<rgba16float, write>;
@compute @workgroup_size(8, 8)
fn main(@builtin(global_invocation_id) id: vec3<u32>) {
    let size = textureDimensions(output);
    if (any(id.xy >= size)) { return; }
    let uv = (vec2<f32>(id.xy) + 0.5) / vec2<f32>(size);
    let color = textureSampleLevel(input, linearSampler, uv, 0.0);
    let y = textureSampleLevel(luma, linearSampler, uv, 0.0).r;
    let g = textureSampleLevel(gauss, linearSampler, uv, 0.0).rgb;
    let corrected = clamp(y + (y - g.r) * 0.8, g.g, g.b) - y;
    textureStore(output, vec2<i32>(id.xy), vec4<f32>(color.rgb + corrected, color.a));
}
