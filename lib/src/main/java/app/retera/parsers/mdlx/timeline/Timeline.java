package app.retera.parsers.mdlx.timeline;

import java.io.IOException;

import app.retera.parsers.mdlx.AnimationMap;
import app.retera.parsers.mdlx.Chunk;
import app.retera.parsers.mdlx.InterpolationType;
import app.retera.parsers.mdlx.MdlTokenInputStream;
import app.retera.parsers.mdlx.MdlTokenOutputStream;
import app.retera.util.MdlUtils;
import app.retera.util.ParseUtils;
import app.retera.util.War3ID;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

public abstract class Timeline<TYPE> implements Chunk {
	private War3ID name;
	private InterpolationType interpolationType;
	private int globalSequenceId = -1;

	private long[] frames;
	private TYPE[] values;
	private TYPE[] inTans;
	private TYPE[] outTans;

	/**
	 * Restricts us to only be able to parse models on one thread at a time, in
	 * return for high performance.
	 */
	private static StringBuffer STRING_BUFFER_HEAP = new StringBuffer();

	public War3ID getName() {
		return this.name;
	}

	public Timeline(War3ID name) {
	}
	public Timeline() {
	}

	public void readMdx(final LittleEndianDataInputStream stream, final War3ID name) throws IOException {
		this.name = name;

		final long keyFrameCount = ParseUtils.readUInt32(stream);

		this.interpolationType = InterpolationType.VALUES[stream.readInt()];
		this.globalSequenceId = stream.readInt();

		this.frames = new long[(int) keyFrameCount];
		this.values = (TYPE[]) new Object[(int) keyFrameCount];
		if (this.interpolationType.tangential()) {
			this.inTans = (TYPE[]) new Object[(int) keyFrameCount];
			this.outTans = (TYPE[]) new Object[(int) keyFrameCount];
		}

		for (int i = 0; i < keyFrameCount; i++) {
			this.frames[i] = (stream.readInt()); // TODO autoboxing is slow
			this.values[i] = (this.readMdxValue(stream));

			if (this.interpolationType.tangential()) {
				this.inTans[i] = (this.readMdxValue(stream));
				this.outTans[i] = (this.readMdxValue(stream));
			}
		}
	}

	public void writeMdx(final LittleEndianDataOutputStream stream) throws IOException {
		stream.writeInt(Integer.reverseBytes(this.name.getValue()));
		final int keyframeCount = this.frames.length;
		stream.writeInt(keyframeCount);
		stream.writeInt(this.interpolationType.ordinal());
		stream.writeInt(this.globalSequenceId);

		for (int i = 0; i < keyframeCount; i++) {
			stream.writeInt((int) this.frames[i]);
			writeMdxValue(stream, this.values[i]);
			if (this.interpolationType.tangential()) {
				writeMdxValue(stream, this.inTans[i]);
				writeMdxValue(stream, this.outTans[i]);
			}
		}
	}

	public void readMdl(final MdlTokenInputStream stream, final War3ID name) throws IOException {
		this.name = name;

		final int keyFrameCount = stream.readInt();

		stream.read(); // {

		final String token = stream.read();
		final InterpolationType interpolationType = switch (token) {
			case MdlUtils.TOKEN_DONT_INTERP -> InterpolationType.DONT_INTERP;
			case MdlUtils.TOKEN_LINEAR -> InterpolationType.LINEAR;
			case MdlUtils.TOKEN_HERMITE -> InterpolationType.HERMITE;
			case MdlUtils.TOKEN_BEZIER -> InterpolationType.BEZIER;
			default -> InterpolationType.DONT_INTERP;
		};

		this.interpolationType = interpolationType;

		if (stream.peek().equals(MdlUtils.TOKEN_GLOBAL_SEQ_ID)) {
			stream.read();
			this.globalSequenceId = stream.readInt();
		}
		else {
			this.globalSequenceId = -1;
		}

		this.frames = new long[keyFrameCount];
		this.values = (TYPE[]) new Object[keyFrameCount];
		if (this.interpolationType.tangential()) {
			this.inTans = (TYPE[]) new Object[keyFrameCount];
			this.outTans = (TYPE[]) new Object[keyFrameCount];
		}
		for (int i = 0; i < keyFrameCount; i++) {
			this.frames[i] = (stream.readInt());
			this.values[i] = (this.readMdlValue(stream));
			if (interpolationType.tangential()) {
				stream.read(); // InTan
				this.inTans[i] = (this.readMdlValue(stream));
				stream.read(); // OutTan
				this.outTans[i] = (this.readMdlValue(stream));
			}
		}

		stream.read(); // }
	}

	public void writeMdl(final MdlTokenOutputStream stream) throws IOException {
		final int tracksCount = this.frames.length;
		stream.startBlock(AnimationMap.ID_TO_TAG.get(this.name).getMdlToken(), tracksCount);

		String token = switch (this.interpolationType) {
			case DONT_INTERP -> MdlUtils.TOKEN_DONT_INTERP;
			case LINEAR -> MdlUtils.TOKEN_LINEAR;
			case HERMITE -> MdlUtils.TOKEN_HERMITE;
			case BEZIER -> MdlUtils.TOKEN_BEZIER;
			default -> MdlUtils.TOKEN_DONT_INTERP;
		};

		stream.writeFlag(token);

		if (this.globalSequenceId != -1) {
			stream.writeAttrib(MdlUtils.TOKEN_GLOBAL_SEQ_ID, this.globalSequenceId);
		}

		for (int i = 0; i < tracksCount; i++) {
			STRING_BUFFER_HEAP.setLength(0);
			STRING_BUFFER_HEAP.append(this.frames[i]);
			STRING_BUFFER_HEAP.append(':');
			this.writeMdlValue(stream, STRING_BUFFER_HEAP.toString(), this.values[i]);
			if (this.interpolationType.tangential()) {
				stream.indent();
				this.writeMdlValue(stream, "InTan", this.inTans[i]);
				this.writeMdlValue(stream, "OutTan", this.outTans[i]);
				stream.unindent();
			}
		}

		stream.endBlock();
	}

	@Override
	public long getByteLength() {
		final int tracksCount = this.frames.length;
		int size = 16;

		if (0 < tracksCount) {
			final int bytesPerValue = size() * 4;
			int valuesPerTrack = 1;
			if (this.interpolationType.tangential()) {
				valuesPerTrack = 3;
			}

			size += (4 + (valuesPerTrack * bytesPerValue)) * tracksCount;
		}
		return size;
	}

	protected abstract int size();

	protected abstract TYPE readMdxValue(LittleEndianDataInputStream stream) throws IOException;

	protected abstract TYPE readMdlValue(MdlTokenInputStream stream);

	protected abstract void writeMdxValue(LittleEndianDataOutputStream stream, TYPE value) throws IOException;

	protected abstract void writeMdlValue(MdlTokenOutputStream stream, String prefix, TYPE value);

	public int getGlobalSequenceId() {
		return this.globalSequenceId;
	}

	public InterpolationType getInterpolationType() {
		return this.interpolationType;
	}

	public long[] getFrames() {
		return this.frames;
	}

	public TYPE[] getValues() {
		return this.values;
	}

	public TYPE[] getInTans() {
		return this.inTans;
	}

	public TYPE[] getOutTans() {
		return this.outTans;
	}
}
