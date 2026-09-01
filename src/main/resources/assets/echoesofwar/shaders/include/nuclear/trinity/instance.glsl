// ---------------------------------------------------------------------------
// Trinity's instance block: the entirety of the boss, in one uniform buffer:
//
//
// HEADER (0-47)
//   0-2   centre xyz      Trinity's fixed world position
//   3     seed
//   4     time            seconds, partial-tick smoothed
//   5     bodyRadius      blocks; every attack is rooted on this sphere
//   6     bodyScale       0..1, grows out of the reformation
//   7     coreIntensity   runs away as a meltdown builds
//   8     phase           TrinityPhase ordinal, see PHASE_* below
//   9     phaseProgress   seconds into the current phase
//   10    criticality     0..100
//   11    degree          meltdowns completed, 0..3
//   12    meltdown        0..1
//   13    reform          0..1
//   14    detActive       0/1
//   15    detAge          seconds
//   16    detLifetime     seconds
//   17    detH            nominal cap height
//   18    detR1           nominal cap radius
//   19    detR0           vent radius
//   20    detIntensity
//   21    detSurge        ground-surge scale
//   22    attackCount     slots 0..count-1 may be live
//   23    turbulence      body noise gain, rises with the meltdown
//   24    cloudHeight     residual cloud height above centre, blocks
//   25-27 detBase xyz     GROUND ZERO - the Crucible, SPAWN_HEIGHT below centre
//   28-31 spare
//
// PROJECTILE FIELD (32-47): the entire attack, from which every projectile is
// generated. See bullets.glsl and BulletFieldMath.java.
//   32    active          0/1
//   33    age             seconds since the attack started
//   34    seed            integer-valued; hashed as a uint
//   35    waveWindow      pulses that may still be airborne, from firstWave
//   36    interval        seconds between pulses
//   37    lead            seconds of surface telegraph before each pulse
//   38    speed           blocks/second, difficulty already applied
//   39    travel          blocks covered before a projectile has dissolved
//   40    launch          radius projectiles start from
//   41    radius          projectile radius, before per-projectile variation
//   42    jitter          positional jitter, in cells; must be <= 1
//   43    speedJitter     per-projectile speed spread, as a fraction
//   44    fadeFrom        fraction of the flight at which dissolving starts
//   45    cut             external dissolve 0..1, driven by a meltdown
//   46    firstWave       absolute index of the oldest live pulse
//   47    spare
//
// ATTACK SLOT s, base = 48 + s * 11
//   +0    type            ATK_* below; 0 = empty
//   +1    telegraph       0..1, the glowing spot before anything emerges
//   +2    extend          0..1 of length, how far it currently reaches
//   +3    fade            0..1, retraction on top of extend
//   +4-6  dir             unit; both the root on the surface AND the direction
//                         of travel, since every rooted attack here is radial
//   +7    length          blocks past the surface at extend == 1
//   +8    radius          cone base / cylinder radius, blocks
//   +9    intensity
//   +10   seed
// ---------------------------------------------------------------------------

const int TRINITY_HEADER_SIZE   = 48;
const int TRINITY_ATTACK_STRIDE = 11;
const int TRINITY_ATTACK_SLOTS  = 80;
const int TRINITY_DATA_SIZE     = 928; // HEADER + STRIDE * SLOTS

layout(std140) uniform InstanceData {
    int count;
    float data[928];
};

const int PHASE_SPAWNING   = 0;
const int PHASE_DETONATING = 1;
const int PHASE_REFORMING  = 2;
const int PHASE_FIGHTING   = 3;
const int PHASE_MELTDOWN   = 4;
const int PHASE_DYING      = 5;
const int PHASE_DESPAWNING = 6;

// attack types, must match AttackSlot's TYPE_.
const int ATK_NONE        = 0;
const int ATK_PREDICTIVE  = 1; // cone
const int ATK_LASER       = 2; // cylinder
const int ATK_CONTAINMENT = 3; // cylinder
const int ATK_WANDERING   = 4; // cylinder
bool atkIsCone(int type) { return type == ATK_PREDICTIVE; }

// --- HEADER ACCESSORS

vec3  tCentre()        { return vec3(data[0], data[1], data[2]); }
float tSeed()          { return data[3]; }
float tTime()          { return data[4]; }
float tBodyRadius()    { return data[5]; }
float tBodyScale()     { return data[6]; }
float tCoreIntensity() { return data[7]; }
int   tPhase()         { return int(data[8] + 0.5); }
float tPhaseProgress() { return data[9]; }
float tCriticality()   { return data[10]; }
float tDegree()        { return data[11]; }
float tMeltdown()      { return data[12]; }
float tReform()        { return data[13]; }
float tDetActive()     { return data[14]; }
float tDetAge()        { return data[15]; }
float tDetLifetime()   { return data[16]; }
float tDetH()          { return data[17]; }
float tDetR1()         { return data[18]; }
float tDetR0()         { return data[19]; }
float tDetIntensity()  { return data[20]; }
float tDetSurge()      { return data[21]; }
int   tAttackCount()   { return int(data[22] + 0.5); }
float tTurbulence()    { return data[23]; }
float tCloudHeight()   { return data[24]; }

// ground zero for Trinity
vec3  tDetBase()       { return vec3(data[25], data[26], data[27]); }

// ----- PROJ FIELD ACCESSORS

float tBfActive()      { return data[32]; }
float tBfAge()         { return data[33]; }
int   tBfSeed()        { return int(data[34]); }

// pulses to scan, starting at tBfFirstWave. cotinuous absolute index sliding window, not from zero.
int   tBfWindow()      { return int(data[35] + 0.5); }
float tBfInterval()    { return data[36]; }
float tBfLead()        { return data[37]; }
float tBfSpeed()       { return data[38]; }
float tBfTravel()      { return data[39]; }
float tBfLaunch()      { return data[40]; }
float tBfRadius()      { return data[41]; }
float tBfJitter()      { return data[42]; }
float tBfSpeedJitter() { return data[43]; }
float tBfFadeFrom()    { return data[44]; }
float tBfCut()         { return data[45]; }
int   tBfFirstWave()   { return int(data[46] + 0.5); }

// body radius
float tBodyR() { return tBodyRadius() * max(tBodyScale(), 0.0); }

// ---- ATTACK ACCESSORS
int   attackBase(int slot) { return TRINITY_HEADER_SIZE + slot * TRINITY_ATTACK_STRIDE; }

int   aType(int a)      { return int(data[a] + 0.5); }
float aTelegraph(int a) { return data[a + 1]; }
float aExtend(int a)    { return data[a + 2]; }
float aFade(int a)      { return data[a + 3]; }
vec3  aDir(int a)       { return vec3(data[a + 4], data[a + 5], data[a + 6]); } // Both the root direction on the sphere and the direction of travel
float aLength(int a)    { return data[a + 7]; }
float aRadius(int a)    { return data[a + 8]; }
float aIntensity(int a) { return data[a + 9]; }
float aSeed(int a)      { return data[a + 10]; }

// where on the sphere this attack is rooted in world space.
vec3 aRoot(int a) { return tCentre() + aDir(a) * tBodyRadius(); }

// How far past the surface it currently reaches.
float aReach(int a) { return aLength(a) * clamp(aExtend(a), 0.0, 1.0); }
