package app.retera.parsers.mdlx;

import java.util.HashMap;
import java.util.Map;

import app.retera.parsers.mdlx.timeline.*;
import app.retera.util.MdlUtils;
import app.retera.util.War3ID;


/**
 * A map from MDX animation tags to their equivalent MDL tokens, and the
 * implementation objects.
 *
 * <p>
 * Based on the works of Chananya Freiman.
 *
 */
public enum AnimationMap {
	// Layer
	KMTF(MdlUtils.TOKEN_TEXTURE_ID, TimelineType.BITMAP_TIMELINE),
	KMTA(MdlUtils.TOKEN_ALPHA, TimelineType.FLOAT_TIMELINE),
	// TextureAnimation
	KTAT(MdlUtils.TOKEN_TRANSLATION, TimelineType.VECTOR3_TIMELINE),
	KTAR(MdlUtils.TOKEN_ROTATION, TimelineType.VECTOR4_TIMELINE),
	KTAS(MdlUtils.TOKEN_SCALING, TimelineType.VECTOR3_TIMELINE),
	// GeosetAnimation
	KGAO(MdlUtils.TOKEN_ALPHA, TimelineType.FLOAT_TIMELINE),
	KGAC(MdlUtils.TOKEN_COLOR, TimelineType.VECTOR3_TIMELINE),
	// Light
	KLAS(MdlUtils.TOKEN_ATTENUATION_START, TimelineType.FLOAT_TIMELINE),
	KLAE(MdlUtils.TOKEN_ATTENUATION_END, TimelineType.FLOAT_TIMELINE),
	KLAC(MdlUtils.TOKEN_COLOR, TimelineType.VECTOR3_TIMELINE),
	KLAI(MdlUtils.TOKEN_INTENSITY, TimelineType.FLOAT_TIMELINE),
	KLBI(MdlUtils.TOKEN_AMB_INTENSITY, TimelineType.FLOAT_TIMELINE),
	KLBC(MdlUtils.TOKEN_AMB_COLOR, TimelineType.VECTOR3_TIMELINE),
	KLAV(MdlUtils.TOKEN_VISIBILITY, TimelineType.FLOAT_TIMELINE),
	// Attachment
	KATV(MdlUtils.TOKEN_VISIBILITY, TimelineType.FLOAT_TIMELINE),
	// ParticleEmitter
	KPEE(MdlUtils.TOKEN_EMISSION_RATE, TimelineType.FLOAT_TIMELINE),
	KPEG(MdlUtils.TOKEN_GRAVITY, TimelineType.FLOAT_TIMELINE),
	KPLN(MdlUtils.TOKEN_LONGITUDE, TimelineType.FLOAT_TIMELINE),
	KPLT(MdlUtils.TOKEN_LATITUDE, TimelineType.FLOAT_TIMELINE),
	KPEL(MdlUtils.TOKEN_LIFE_SPAN, TimelineType.FLOAT_TIMELINE),
	KPES(MdlUtils.TOKEN_INIT_VELOCITY, TimelineType.FLOAT_TIMELINE),
	KPEV(MdlUtils.TOKEN_VISIBILITY, TimelineType.FLOAT_TIMELINE),
	// ParticleEmitter2
	KP2S(MdlUtils.TOKEN_SPEED, TimelineType.FLOAT_TIMELINE),
	KP2R(MdlUtils.TOKEN_VARIATION, TimelineType.FLOAT_TIMELINE),
	KP2L(MdlUtils.TOKEN_LATITUDE, TimelineType.FLOAT_TIMELINE),
	KP2G(MdlUtils.TOKEN_GRAVITY, TimelineType.FLOAT_TIMELINE),
	KP2E(MdlUtils.TOKEN_EMISSION_RATE, TimelineType.FLOAT_TIMELINE),
	KP2N(MdlUtils.TOKEN_LENGTH, TimelineType.FLOAT_TIMELINE),
	KP2W(MdlUtils.TOKEN_WIDTH, TimelineType.FLOAT_TIMELINE),
	KP2V(MdlUtils.TOKEN_VISIBILITY, TimelineType.FLOAT_TIMELINE),
	// RibbonEmitter
	KRHA(MdlUtils.TOKEN_HEIGHT_ABOVE, TimelineType.FLOAT_TIMELINE),
	KRHB(MdlUtils.TOKEN_HEIGHT_BELOW, TimelineType.FLOAT_TIMELINE),
	KRAL(MdlUtils.TOKEN_ALPHA, TimelineType.FLOAT_TIMELINE),
	KRCO(MdlUtils.TOKEN_COLOR, TimelineType.VECTOR3_TIMELINE),
	KRTX(MdlUtils.TOKEN_TEXTURE_SLOT, TimelineType.UINT32_TIMELINE),
	KRVS(MdlUtils.TOKEN_VISIBILITY, TimelineType.FLOAT_TIMELINE),
	// Camera
	KCTR(MdlUtils.TOKEN_TRANSLATION, TimelineType.VECTOR3_TIMELINE),
	KTTR(MdlUtils.TOKEN_TRANSLATION, TimelineType.VECTOR3_TIMELINE),
	KCRL(MdlUtils.TOKEN_ROTATION, TimelineType.UINT32_TIMELINE),
	// GenericObject
	KGTR(MdlUtils.TOKEN_TRANSLATION, TimelineType.VECTOR3_TIMELINE),
	KGRT(MdlUtils.TOKEN_ROTATION, TimelineType.VECTOR4_TIMELINE),
	KGSC(MdlUtils.TOKEN_SCALING, TimelineType.VECTOR3_TIMELINE);
	private final String mdlToken;
	private final TimelineType type;
	private final War3ID war3id;

	private AnimationMap(final String mdlToken, final TimelineType type) {
		this.mdlToken = mdlToken;
		this.war3id = War3ID.fromString(this.name());
		this.type = type;
	}

	public String getMdlToken() {
		return this.mdlToken;
	}

	public Timeline<?> getNewTimeline() {
		return switch (type) {
			case BITMAP_TIMELINE -> new UInt32Timeline(war3id);
			case UINT32_TIMELINE -> new UInt32Timeline(war3id);
			case FLOAT_TIMELINE -> new FloatTimeline(war3id);
			case VECTOR3_TIMELINE -> new FloatArrayTimeline(3, war3id);
			case VECTOR4_TIMELINE -> new FloatArrayTimeline(4, war3id);
		};
	}

	public War3ID getWar3id() {
		return this.war3id;
	}

	public static final Map<War3ID, AnimationMap> ID_TO_TAG = new HashMap<>();

	static {
		for (final AnimationMap tag : AnimationMap.values()) {
			ID_TO_TAG.put(tag.getWar3id(), tag);
		}
	}
}
