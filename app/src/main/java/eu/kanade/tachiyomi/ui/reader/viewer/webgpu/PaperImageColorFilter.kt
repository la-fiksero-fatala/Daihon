package eu.kanade.tachiyomi.ui.reader.viewer.webgpu

import ca.mpreg.webgpuviewer.renderer.ImageFilterWgsl

/** Match the classic reader's page-only color matrix without affecting loading/error UI. */
internal fun paperImageColorFilter(grayscale: Boolean, inverted: Boolean): ImageFilterWgsl? {
    if (!grayscale && !inverted) return null
    return ImageFilterWgsl(
        """
        //!DESC Reader image colors
        //!HOOK MAIN
        //!BIND HOOKED
        //!COMPUTE 8 8
        @group(0) @binding(0) var input: texture_2d<f32>;
        @group(0) @binding(1) var output: texture_storage_2d<rgba16float, write>;
        @compute @workgroup_size(8, 8)
        fn main(@builtin(global_invocation_id) id: vec3<u32>) {
            if (any(id.xy >= textureDimensions(output))) { return; }
            let color = textureLoad(input, vec2<i32>(id.xy), 0);
            var rgb = color.rgb;
            ${if (grayscale) "rgb = vec3<f32>(dot(rgb, vec3<f32>(0.213, 0.715, 0.072)));" else ""}
            ${if (inverted) "rgb = vec3<f32>(1.0) - rgb;" else ""}
            textureStore(output, vec2<i32>(id.xy), vec4<f32>(rgb, color.a));
        }
        """.trimIndent(),
        "Reader image colors",
    )
}
