// ---------------------------------------------------------------------------
// The uniform block every Lodestone PostProcessor pass is handed automatically,
// plus the view-ray reconstruction that depends on it.
// ---------------------------------------------------------------------------

// Provided automatically by every PostProcessor pass.
layout(std140) uniform LodestoneDefaults {
    vec3 cameraPos;
    vec3 lookVector;
    vec3 upVector;
    vec3 leftVector;
    mat4 invViewMat;
    mat4 invProjMat;
    vec3 bobOffset;
    float time;
    float nearPlaneDistance;
    float farPlaneDistance;
    float fov;
    float aspectRatio;
};

// far plane world position for this texCoord.
vec3 getFarWorldPos(vec2 uv) {
	vec4 clipSpacePosition = vec4(uv * 2.0 - 1.0, 1.0, 1.0);
	vec4 viewSpacePosition = invProjMat * clipSpacePosition;
	viewSpacePosition /= viewSpacePosition.w;
	vec4 localSpacePosition = invViewMat * viewSpacePosition;
	return cameraPos + localSpacePosition.xyz;
}
