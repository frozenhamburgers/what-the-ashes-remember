// ---------------------------------------------------------------------------
// Reformation: the mushroom cloud collapsing back down into Trinity.
//
// NO GEOMETRY, just animates remnant of detonation to reform.
// ---------------------------------------------------------------------------

// STAGING
// Where each beat sits in the reformation, as fractions of 'reform', overlapping into each other

const float RF_CAP_START   = 0.00;
const float RF_CAP_END     = 0.55; // cap fully closed by here
const float RF_PLUME_START = 0.28; // the plume starts going before the cap is done
const float RF_PLUME_END   = 0.96;

// how much of its radius the stalk keeps at the end
const float RF_PLUME_KEEP = 0.10;

// lattice dragged downward and inward
const float RF_DRAW_IN = 55.0;
const float RF_DRAW_DOWN = 90.0;

// only erode stalk density at the very end, or else the top cloud would float
const float RF_ERODE_START = 0.78;
const float RF_ERODE_MAX   = 2.40;

// Contracts live detonation back into Trinity. call immediately after setupDetonation. No op unless reformation is running
void applyReformCollapse(inout Detonation d) {
	float k = tReform();
	if (k <= 0.0001) return;

	float toY = tCentre().y - d.base.y;

	float capK = smoothstep(RF_CAP_START, RF_CAP_END, k);
	float plumeK = smoothstep(RF_PLUME_START, RF_PLUME_END, k);

	// CAP CLOUD
	// close radius, ball descend
	d.capR = mix(d.capR, d.capR * 0.02, capK);
	d.headY = mix(d.headY, toY, capK);
	d.headTop = mix(d.headTop, toY, capK);
	d.headBot = mix(d.headBot, toY, capK);
	d.capDroop *= (1.0 - capK); // flatten droop
	d.headAmt *= (1.0 - smoothstep(0.82, 1.0, capK)); // decrease density at very end

	// STALK
	d.plumeWiden *= mix(1.0, RF_PLUME_KEEP, plumeK);
	d.plumeTop = mix(d.plumeTop, toY, plumeK);
	d.frontH = mix(d.frontH, toY, plumeK);
	d.plumeErode = max(d.plumeErode,
			smoothstep(RF_ERODE_START, 1.0, k) * RF_ERODE_MAX);

	// MATERIAL & LATTICE
	d.drawIn = RF_DRAW_IN * k;
	d.scroll -= RF_DRAW_DOWN * k;

	// EVERYTHING ELSE
	// rings and ground smoke are just sped up to nohting
	d.ringAmt *= (1.0 - capK);
	d.groundErode = max(d.groundErode, capK);
	d.groundAmt *= (1.0 - smoothstep(0.55, 1.0, k));
}
