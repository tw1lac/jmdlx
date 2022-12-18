package app.retera.parsers.mdlx;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import app.retera.util.MdlUtils;
import app.retera.util.ParseUtils;
import app.retera.util.War3ID;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

/**
 * A Warcraft 3 model. Supports loading from and saving to both the binary MDX
 * and text MDL file formats.
 */
public class MdlxModel {
	// Below, these can't call a function on a string to make their value because
	// switch/case statements require the value to be compile-time defined in order
	// to be legal, and it appears to only allow basic binary operators for that.
	// I would love a clearer way to just type 'MDLX' in a character constant in
	// Java for this
	private static final int MDLX = ('M' << 24) | ('D' << 16) | ('L' << 8) | ('X');// War3ID.fromString("MDLX").getValue();
	private static final int VERS = ('V' << 24) | ('E' << 16) | ('R' << 8) | ('S');// War3ID.fromString("VERS").getValue();
	private static final int MODL = ('M' << 24) | ('O' << 16) | ('D' << 8) | ('L');// War3ID.fromString("MODL").getValue();
	private static final int SEQS = ('S' << 24) | ('E' << 16) | ('Q' << 8) | ('S');// War3ID.fromString("SEQS").getValue();
	private static final int GLBS = ('G' << 24) | ('L' << 16) | ('B' << 8) | ('S');// War3ID.fromString("GLBS").getValue();
	private static final int MTLS = ('M' << 24) | ('T' << 16) | ('L' << 8) | ('S');// War3ID.fromString("MTLS").getValue();
	private static final int TEXS = ('T' << 24) | ('E' << 16) | ('X' << 8) | ('S');// War3ID.fromString("TEXS").getValue();
	private static final int TXAN = ('T' << 24) | ('X' << 16) | ('A' << 8) | ('N');// War3ID.fromString("TXAN").getValue();
	private static final int GEOS = ('G' << 24) | ('E' << 16) | ('O' << 8) | ('S');// War3ID.fromString("GEOS").getValue();
	private static final int GEOA = ('G' << 24) | ('E' << 16) | ('O' << 8) | ('A');// War3ID.fromString("GEOA").getValue();
	private static final int BONE = ('B' << 24) | ('O' << 16) | ('N' << 8) | ('E');// War3ID.fromString("BONE").getValue();
	private static final int LITE = ('L' << 24) | ('I' << 16) | ('T' << 8) | ('E');// War3ID.fromString("LITE").getValue();
	private static final int HELP = ('H' << 24) | ('E' << 16) | ('L' << 8) | ('P');// War3ID.fromString("HELP").getValue();
	private static final int ATCH = ('A' << 24) | ('T' << 16) | ('C' << 8) | ('H');// War3ID.fromString("ATCH").getValue();
	private static final int PIVT = ('P' << 24) | ('I' << 16) | ('V' << 8) | ('T');// War3ID.fromString("PIVT").getValue();
	private static final int PREM = ('P' << 24) | ('R' << 16) | ('E' << 8) | ('M');// War3ID.fromString("PREM").getValue();
	private static final int PRE2 = ('P' << 24) | ('R' << 16) | ('E' << 8) | ('2');// War3ID.fromString("PRE2").getValue();
	private static final int RIBB = ('R' << 24) | ('I' << 16) | ('B' << 8) | ('B');// War3ID.fromString("RIBB").getValue();
	private static final int CAMS = ('C' << 24) | ('A' << 16) | ('M' << 8) | ('S');// War3ID.fromString("CAMS").getValue();
	private static final int EVTS = ('E' << 24) | ('V' << 16) | ('T' << 8) | ('S');// War3ID.fromString("EVTS").getValue();
	private static final int CLID = ('C' << 24) | ('L' << 16) | ('I' << 8) | ('D');// War3ID.fromString("CLID").getValue();
	private int version = 800;
	private String name = "";
	/**
	 * (Comment copied from Ghostwolf JS) To the best of my knowledge, this should
	 * always be left empty. This is probably a leftover from the Warcraft 3 beta.
	 * (WS game note: No, I never saw any animation files in the RoC 2001-2002 Beta.
	 * So it must be from the Alpha)
	 *
	 * @member {string}
	 */
	private String animationFile = "";
	private final Extent extent = new Extent();
	private long blendTime = 0;
	private final List<Sequence> sequences = new ArrayList<Sequence>();
	private final List<Long /* UInt32 */> globalSequences = new ArrayList<>();
	private final List<Material> materials = new ArrayList<>();
	private final List<Texture> textures = new ArrayList<>();
	private final List<TextureAnimation> textureAnimations = new ArrayList<>();
	private final List<Geoset> geosets = new ArrayList<>();
	private final List<GeosetAnimation> geosetAnimations = new ArrayList<>();
	private final List<Bone> bones = new ArrayList<>();
	private final List<Light> lights = new ArrayList<>();
	private final List<Helper> helpers = new ArrayList<>();
	private final List<Attachment> attachments = new ArrayList<>();
	private final List<float[]> pivotPoints = new ArrayList<>();
	private final List<ParticleEmitter> particleEmitters = new ArrayList<>();
	private final List<ParticleEmitter2> particleEmitters2 = new ArrayList<>();
	private final List<RibbonEmitter> ribbonEmitters = new ArrayList<>();
	private final List<Camera> cameras = new ArrayList<>();
	private final List<EventObject> eventObjects = new ArrayList<>();
	private final List<CollisionShape> collisionShapes = new ArrayList<>();
	private final List<UnknownChunk> unknownChunks = new ArrayList<>();

	public MdlxModel(final InputStream buffer) throws IOException {
		if (buffer != null) {
			// In ghostwolf JS, this function called load()
			// which decided whether the buffer was an MDL.
			loadMdx(buffer);
		}
	}

	public MdlxModel() {
	}

	public void loadMdx(final InputStream buffer) throws IOException {
		final LittleEndianDataInputStream stream = new LittleEndianDataInputStream(buffer);
		if (Integer.reverseBytes(stream.readInt()) != MDLX) {
			throw new IllegalStateException("WrongMagicNumber");
		}

		while (stream.available() > 0) {
			final int tag = Integer.reverseBytes(stream.readInt());
			final long size = ParseUtils.readUInt32(stream);

			switch (tag) {
				case VERS -> loadVersionChunk(stream);
				case MODL -> loadModelChunk(stream);
				case SEQS -> loadStaticObjects(this.sequences, Sequence::new, stream, size / 132);
				case GLBS -> loadGlobalSequenceChunk(stream, size);
				case MTLS -> loadDynamicObjects(this.materials, Material::new, stream, size);
				case TEXS -> loadStaticObjects(this.textures, Texture::new, stream, size / 268);
				case TXAN -> loadDynamicObjects(this.textureAnimations, TextureAnimation::new, stream, size);
				case GEOS -> loadDynamicObjects(this.geosets, Geoset::new, stream, size);
				case GEOA -> loadDynamicObjects(this.geosetAnimations, GeosetAnimation::new, stream, size);
				case BONE -> loadDynamicObjects(this.bones, Bone::new, stream, size);
				case LITE -> loadDynamicObjects(this.lights, Light::new, stream, size);
				case HELP -> loadDynamicObjects(this.helpers, Helper::new, stream, size);
				case ATCH -> loadDynamicObjects(this.attachments, Attachment::new, stream, size);
				case PIVT -> loadPivotPointChunk(stream, size);
				case PREM -> loadDynamicObjects(this.particleEmitters, ParticleEmitter::new, stream, size);
				case PRE2 -> loadDynamicObjects(this.particleEmitters2, ParticleEmitter2::new, stream, size);
				case RIBB -> loadDynamicObjects(this.ribbonEmitters, RibbonEmitter::new, stream, size);
				case CAMS -> loadDynamicObjects(this.cameras, Camera::new, stream, size);
				case EVTS -> loadDynamicObjects(this.eventObjects, EventObject::new, stream, size);
				case CLID -> loadDynamicObjects(this.collisionShapes, CollisionShape::new, stream, size);
				default -> this.unknownChunks.add(new UnknownChunk(stream, size, new War3ID(tag)));
			}
		}

	}

	private void loadVersionChunk(final LittleEndianDataInputStream stream) throws IOException {
		this.version = (int) ParseUtils.readUInt32(stream);
	}

	/**
	 * Restricts us to only be able to parse models on one thread at a time, in
	 * return for high performance.
	 */
	private static final byte[] NAME_BYTES_HEAP = new byte[80];
	private static final byte[] ANIMATION_FILE_BYTES_HEAP = new byte[260];

	private void loadModelChunk(final LittleEndianDataInputStream stream) throws IOException {
		this.name = ParseUtils.readString(stream, NAME_BYTES_HEAP);
		this.animationFile = ParseUtils.readString(stream, ANIMATION_FILE_BYTES_HEAP);
		this.extent.readMdx(stream);
		this.blendTime = ParseUtils.readUInt32(stream);
	}
	private <E extends MdlxBlock> void loadStaticObjects(final List<E> out,
	                                                     final Supplier<E> constructor,
	                                                     final LittleEndianDataInputStream stream,
	                                                     final long count) throws IOException {
		for (int i = 0; i < count; i++) {
			final E object = constructor.get();

			object.readMdx(stream);

			out.add(object);
		}
	}

	private void loadGlobalSequenceChunk(final LittleEndianDataInputStream stream, final long size) throws IOException {
		for (long i = 0, l = size / 4; i < l; i++) {
			this.globalSequences.add(ParseUtils.readUInt32(stream));
		}
	}

	private <E extends MdlxBlock & Chunk> void loadDynamicObjects(final List<E> out,
	                                                              final Supplier<E>  constructor,
	                                                              final LittleEndianDataInputStream stream,
	                                                              final long size) throws IOException {
		long totalSize = 0;
		while (totalSize < size) {
			final E object = constructor.get();

			object.readMdx(stream);

			totalSize += object.getByteLength();

			out.add(object);
		}
	}

	private void loadPivotPointChunk(final LittleEndianDataInputStream stream, final long size) throws IOException {
		for (long i = 0, l = size / 12; i < l; i++) {
			this.pivotPoints.add(ParseUtils.readFloatArray(stream, 3));
		}
	}

	public void saveMdx(final OutputStream outputStream) throws IOException {
		final LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(outputStream);
		stream.writeInt(Integer.reverseBytes(MDLX));
		this.saveVersionChunk(stream);
		this.saveModelChunk(stream);
		this.saveStaticObjectChunk(stream, SEQS, this.sequences, 132);
		this.saveGlobalSequenceChunk(stream);
		this.saveDynamicObjectChunk(stream, MTLS, this.materials);
		this.saveStaticObjectChunk(stream, TEXS, this.textures, 268);
		this.saveDynamicObjectChunk(stream, TXAN, this.textureAnimations);
		this.saveDynamicObjectChunk(stream, GEOS, this.geosets);
		this.saveDynamicObjectChunk(stream, GEOA, this.geosetAnimations);
		this.saveDynamicObjectChunk(stream, BONE, this.bones);
		this.saveDynamicObjectChunk(stream, LITE, this.lights);
		this.saveDynamicObjectChunk(stream, HELP, this.helpers);
		this.saveDynamicObjectChunk(stream, ATCH, this.attachments);
		this.savePivotPointChunk(stream);
		this.saveDynamicObjectChunk(stream, PREM, this.particleEmitters);
		this.saveDynamicObjectChunk(stream, PRE2, this.particleEmitters2);
		this.saveDynamicObjectChunk(stream, RIBB, this.ribbonEmitters);
		this.saveDynamicObjectChunk(stream, CAMS, this.cameras);
		this.saveDynamicObjectChunk(stream, EVTS, this.eventObjects);
		this.saveDynamicObjectChunk(stream, CLID, this.collisionShapes);

		for (final UnknownChunk chunk : this.unknownChunks) {
			chunk.writeMdx(stream);
		}
	}

	private void saveVersionChunk(final LittleEndianDataOutputStream stream) throws IOException {
		stream.writeInt(Integer.reverseBytes(VERS));
		ParseUtils.writeUInt32(stream, 4);
		ParseUtils.writeUInt32(stream, this.version);
	}

	private void saveModelChunk(final LittleEndianDataOutputStream stream) throws IOException {
		stream.writeInt(Integer.reverseBytes(MODL));
		ParseUtils.writeUInt32(stream, 372);
		final byte[] bytes = this.name.getBytes(ParseUtils.UTF8);
		stream.write(bytes);
		for (int i = 0; i < (NAME_BYTES_HEAP.length - bytes.length); i++) {
			stream.write((byte) 0);
		}
		final byte[] animationFileBytes = this.animationFile.getBytes(ParseUtils.UTF8);
		stream.write(animationFileBytes);
		for (int i = 0; i < (ANIMATION_FILE_BYTES_HEAP.length - animationFileBytes.length); i++) {
			stream.write((byte) 0);
		}
		this.extent.writeMdx(stream);
		ParseUtils.writeUInt32(stream, this.blendTime);
	}

	private <E extends MdlxBlock> void saveStaticObjectChunk(final LittleEndianDataOutputStream stream, final int name,
			final List<E> objects, final long size) throws IOException {
		if (!objects.isEmpty()) {
			stream.writeInt(Integer.reverseBytes(name));
			ParseUtils.writeUInt32(stream, objects.size() * size);

			for (final E object : objects) {
				object.writeMdx(stream);
			}
		}
	}

	private void saveGlobalSequenceChunk(final LittleEndianDataOutputStream stream) throws IOException {
		if (!this.globalSequences.isEmpty()) {
			stream.writeInt(Integer.reverseBytes(GLBS));
			ParseUtils.writeUInt32(stream, this.globalSequences.size() * 4);

			for (final Long globalSequence : this.globalSequences) {
				ParseUtils.writeUInt32(stream, globalSequence);
			}
		}
	}

	private <E extends MdlxBlock & Chunk> void saveDynamicObjectChunk(final LittleEndianDataOutputStream stream,
			final int name, final List<E> objects) throws IOException {
		if (!objects.isEmpty()) {
			stream.writeInt(Integer.reverseBytes(name));
			ParseUtils.writeUInt32(stream, getObjectsByteLength(objects));

			for (final E object : objects) {
				object.writeMdx(stream);
			}
		}
	}

	private void savePivotPointChunk(final LittleEndianDataOutputStream stream) throws IOException {
		if (this.pivotPoints.size() > 0) {
			stream.writeInt(Integer.reverseBytes(PIVT));
			ParseUtils.writeUInt32(stream, this.pivotPoints.size() * 12);

			for (final float[] pivotPoint : this.pivotPoints) {
				ParseUtils.writeFloatArray(stream, pivotPoint);
			}
		}
	}

	public void loadMdl(final InputStream inputStream) throws IOException {
		String token;
		final MdlTokenInputStream stream = new MdlTokenInputStream(new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)));

		while ((token = stream.read()) != null) {
			switch (token) {
				case MdlUtils.TOKEN_VERSION             -> this.loadVersionBlock(stream);
				case MdlUtils.TOKEN_MODEL               -> this.loadModelBlock(stream);
				case MdlUtils.TOKEN_SEQUENCES           -> this.loadNumberedObjectBlock(this.sequences, Sequence::new, MdlUtils.TOKEN_ANIM, stream);
				case MdlUtils.TOKEN_GLOBAL_SEQUENCES    -> this.loadGlobalSequenceBlock(stream);
				case MdlUtils.TOKEN_TEXTURES            -> this.loadNumberedObjectBlock(this.textures, Texture::new, MdlUtils.TOKEN_BITMAP, stream);
				case MdlUtils.TOKEN_MATERIALS           -> this.loadNumberedObjectBlock(this.materials, Material::new, MdlUtils.TOKEN_MATERIAL, stream);
				case MdlUtils.TOKEN_TEXTURE_ANIMS       -> this.loadNumberedObjectBlock(this.textureAnimations, TextureAnimation::new, MdlUtils.TOKEN_TVERTEX_ANIM, stream);
				case MdlUtils.TOKEN_GEOSET              -> this.loadObject(this.geosets, Geoset::new, stream);
				case MdlUtils.TOKEN_GEOSETANIM          -> this.loadObject(this.geosetAnimations, GeosetAnimation::new, stream);
				case MdlUtils.TOKEN_BONE                -> this.loadObject(this.bones, Bone::new, stream);
				case MdlUtils.TOKEN_LIGHT               -> this.loadObject(this.lights, Light::new, stream);
				case MdlUtils.TOKEN_HELPER              -> this.loadObject(this.helpers, Helper::new, stream);
				case MdlUtils.TOKEN_ATTACHMENT          -> this.loadObject(this.attachments, Attachment::new, stream);
				case MdlUtils.TOKEN_PIVOT_POINTS        -> this.loadPivotPointBlock(stream);
				case MdlUtils.TOKEN_PARTICLE_EMITTER    -> this.loadObject(this.particleEmitters, ParticleEmitter::new, stream);
				case MdlUtils.TOKEN_PARTICLE_EMITTER2   -> this.loadObject(this.particleEmitters2, ParticleEmitter2::new, stream);
				case MdlUtils.TOKEN_RIBBON_EMITTER      -> this.loadObject(this.ribbonEmitters, RibbonEmitter::new, stream);
				case MdlUtils.TOKEN_CAMERA              -> this.loadObject(this.cameras, Camera::new, stream);
				case MdlUtils.TOKEN_EVENT_OBJECT        -> this.loadObject(this.eventObjects, EventObject::new, stream);
				case MdlUtils.TOKEN_COLLISION_SHAPE     -> this.loadObject(this.collisionShapes, CollisionShape::new, stream);
				default -> throw new IllegalStateException("Unsupported block: " + token);
			}
		}
	}

	private void loadVersionBlock(final MdlTokenInputStream stream) {
		for (final String token : stream.readBlock()) {
			if (MdlUtils.TOKEN_FORMAT_VERSION.equals(token)) {
				this.version = stream.readInt();
			} else {
				throw new IllegalStateException("Unknown token in Version: " + token);
			}
		}
	}

	private void loadModelBlock(final MdlTokenInputStream stream) {
		this.name = stream.read();
		for (final String token : stream.readBlock()) {
			if (token.startsWith("Num")) {
				/*-
				 * Don't care about the number of things, the arrays will grow as they wish.
				 * This includes:
				 *      NumGeosets
				 *      NumGeosetAnims
				 *      NumHelpers
				 *      NumLights
				 *      NumBones
				 *      NumAttachments
				 *      NumParticleEmitters
				 *      NumParticleEmitters2
				 *      NumRibbonEmitters
				 *      NumEvents
				 */
				stream.read();
			}
			else {
				switch (token) {
					case MdlUtils.TOKEN_BLEND_TIME -> this.blendTime = stream.readUInt32();
					case MdlUtils.TOKEN_MINIMUM_EXTENT -> stream.readFloatArray(this.extent.min);
					case MdlUtils.TOKEN_MAXIMUM_EXTENT -> stream.readFloatArray(this.extent.max);
					case MdlUtils.TOKEN_BOUNDSRADIUS -> this.extent.boundsRadius = stream.readFloat();
					default -> throw new IllegalStateException("Unknown token in Model: " + token);
				}
			}
		}
	}

	private <E extends MdlxBlock> void loadNumberedObjectBlock(final List<E> out,
	                                                           final Supplier<E> constructor,
	                                                           final String name,
	                                                           final MdlTokenInputStream stream) throws IOException {
		stream.read(); // Don't care about the number, the array will grow

		for (final String token : stream.readBlock()) {
			if (token.equals(name)) {
				final E object = constructor.get();

				object.readMdl(stream);

				out.add(object);
			}
			else {
				throw new IllegalStateException("Unknown token in " + name + ": " + token);
			}
		}
	}

	private void loadGlobalSequenceBlock(final MdlTokenInputStream stream) {
		stream.read(); // Don't care about the number, the array will grow

		for (final String token : stream.readBlock()) {
			if (token.equals(MdlUtils.TOKEN_DURATION)) {
				this.globalSequences.add(stream.readUInt32());
			}
			else {
				throw new IllegalStateException("Unknown token in GlobalSequences: " + token);
			}
		}
	}

	private <E extends MdlxBlock> void loadObject(final List<E> out,
	                                              final Supplier<E> descriptor,
	                                              final MdlTokenInputStream stream) throws IOException {
		final E object = descriptor.get();

		object.readMdl(stream);

		out.add(object);
	}

	private void loadPivotPointBlock(final MdlTokenInputStream stream) {
		final int count = stream.readInt();

		stream.read(); // {

		for (int i = 0; i < count; i++) {
			this.pivotPoints.add(stream.readFloatArray(new float[3]));
		}

		stream.read(); // }
	}

	public void saveMdl(final OutputStream outputStream) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
			final MdlTokenOutputStream stream = new MdlTokenOutputStream(writer);
			this.saveVersionBlock(stream);
			this.saveModelBlock(stream);
			this.saveStaticObjectsBlock(stream, MdlUtils.TOKEN_SEQUENCES, this.sequences);
			this.saveGlobalSequenceBlock(stream);
			this.saveStaticObjectsBlock(stream, MdlUtils.TOKEN_TEXTURES, this.textures);
			this.saveStaticObjectsBlock(stream, MdlUtils.TOKEN_MATERIALS, this.materials);
			this.saveStaticObjectsBlock(stream, MdlUtils.TOKEN_TEXTURE_ANIMS, this.textureAnimations);
			this.saveObjects(stream, this.geosets);
			this.saveObjects(stream, this.geosetAnimations);
			this.saveObjects(stream, this.bones);
			this.saveObjects(stream, this.lights);
			this.saveObjects(stream, this.helpers);
			this.saveObjects(stream, this.attachments);
			this.savePivotPointBlock(stream);
			this.saveObjects(stream, this.particleEmitters);
			this.saveObjects(stream, this.particleEmitters2);
			this.saveObjects(stream, this.ribbonEmitters);
			this.saveObjects(stream, this.cameras);
			this.saveObjects(stream, this.eventObjects);
			this.saveObjects(stream, this.collisionShapes);
		}
	}

	private void saveVersionBlock(final MdlTokenOutputStream stream) {
		stream.startBlock(MdlUtils.TOKEN_VERSION);
		stream.writeAttrib(MdlUtils.TOKEN_FORMAT_VERSION, this.version);
		stream.endBlock();
	}

	private void saveModelBlock(final MdlTokenOutputStream stream) {
		stream.startObjectBlock(MdlUtils.TOKEN_MODEL, this.name);
		stream.writeAttribUInt32(MdlUtils.TOKEN_BLEND_TIME, this.blendTime);
		this.extent.writeMdl(stream);
		stream.endBlock();
	}

	private void saveStaticObjectsBlock(final MdlTokenOutputStream stream, final String name,
			final List<? extends MdlxBlock> objects) throws IOException {
		if (!objects.isEmpty()) {
			stream.startBlock(name, objects.size());

			for (final MdlxBlock object : objects) {
				object.writeMdl(stream);
			}

			stream.endBlock();
		}
	}

	private void saveGlobalSequenceBlock(final MdlTokenOutputStream stream) {
		if (!this.globalSequences.isEmpty()) {
			stream.startBlock(MdlUtils.TOKEN_GLOBAL_SEQUENCES, this.globalSequences.size());

			for (final Long globalSequence : this.globalSequences) {
				stream.writeAttribUInt32(MdlUtils.TOKEN_DURATION, globalSequence);
			}

			stream.endBlock();
		}
	}

	private void saveObjects(final MdlTokenOutputStream stream, final List<? extends MdlxBlock> objects)
			throws IOException {
		for (final MdlxBlock object : objects) {
			object.writeMdl(stream);
		}
	}

	private void savePivotPointBlock(final MdlTokenOutputStream stream) {
		if (!this.pivotPoints.isEmpty()) {
			stream.startBlock(MdlUtils.TOKEN_PIVOT_POINTS, this.pivotPoints.size());

			for (final float[] pivotPoint : this.pivotPoints) {
				stream.writeFloatArray(pivotPoint);
			}

			stream.endBlock();
		}
	}

	private long getByteLength() {
		long size = 396;

		size += getStaticObjectsChunkByteLength(this.sequences, 132);
		size += this.getStaticObjectsChunkByteLength(this.globalSequences, 4);
		size += this.getDynamicObjectsChunkByteLength(this.materials);
		size += this.getStaticObjectsChunkByteLength(this.textures, 268);
		size += this.getDynamicObjectsChunkByteLength(this.textureAnimations);
		size += this.getDynamicObjectsChunkByteLength(this.geosets);
		size += this.getDynamicObjectsChunkByteLength(this.geosetAnimations);
		size += this.getDynamicObjectsChunkByteLength(this.bones);
		size += this.getDynamicObjectsChunkByteLength(this.lights);
		size += this.getDynamicObjectsChunkByteLength(this.helpers);
		size += this.getDynamicObjectsChunkByteLength(this.attachments);
		size += this.getStaticObjectsChunkByteLength(this.pivotPoints, 12);
		size += this.getDynamicObjectsChunkByteLength(this.particleEmitters);
		size += this.getDynamicObjectsChunkByteLength(this.particleEmitters2);
		size += this.getDynamicObjectsChunkByteLength(this.ribbonEmitters);
		size += this.getDynamicObjectsChunkByteLength(this.cameras);
		size += this.getDynamicObjectsChunkByteLength(this.eventObjects);
		size += this.getDynamicObjectsChunkByteLength(this.collisionShapes);
		size += this.getObjectsByteLength(this.unknownChunks);

		return size;
	}

	private <E extends Chunk> long getObjectsByteLength(final List<E> objects) {
		long size = 0;
		for (final E object : objects) {
			size += object.getByteLength();
		}
		return size;
	}

	private <E extends Chunk> long getDynamicObjectsChunkByteLength(final List<E> objects) {
		if (!objects.isEmpty()) {
			return 8 + this.getObjectsByteLength(objects);
		}

		return 0;
	}

	private <E> long getStaticObjectsChunkByteLength(final List<E> objects, final long size) {
		if (!objects.isEmpty()) {
			return 8 + (objects.size() * size);
		}

		return 0;
	}

	public List<Long> getGlobalSequences() {
		return this.globalSequences;
	}

	public List<Sequence> getSequences() {
		return this.sequences;
	}

	public List<float[]> getPivotPoints() {
		return this.pivotPoints;
	}

	public int getVersion() {
		return this.version;
	}

	public String getName() {
		return this.name;
	}

	public String getAnimationFile() {
		return this.animationFile;
	}

	public Extent getExtent() {
		return this.extent;
	}

	public long getBlendTime() {
		return this.blendTime;
	}

	public List<Material> getMaterials() {
		return this.materials;
	}

	public List<Texture> getTextures() {
		return this.textures;
	}

	public List<TextureAnimation> getTextureAnimations() {
		return this.textureAnimations;
	}

	public List<Geoset> getGeosets() {
		return this.geosets;
	}

	public List<GeosetAnimation> getGeosetAnimations() {
		return this.geosetAnimations;
	}

	public List<Bone> getBones() {
		return this.bones;
	}

	public List<Light> getLights() {
		return this.lights;
	}

	public List<Helper> getHelpers() {
		return this.helpers;
	}

	public List<Attachment> getAttachments() {
		return this.attachments;
	}

	public List<ParticleEmitter> getParticleEmitters() {
		return this.particleEmitters;
	}

	public List<ParticleEmitter2> getParticleEmitters2() {
		return this.particleEmitters2;
	}

	public List<RibbonEmitter> getRibbonEmitters() {
		return this.ribbonEmitters;
	}

	public List<Camera> getCameras() {
		return this.cameras;
	}

	public List<EventObject> getEventObjects() {
		return this.eventObjects;
	}

	public List<CollisionShape> getCollisionShapes() {
		return this.collisionShapes;
	}

	public List<UnknownChunk> getUnknownChunks() {
		return this.unknownChunks;
	}
}
