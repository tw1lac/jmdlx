package app.retera.parsers.mdlx;

import java.io.IOException;

import app.retera.util.MdlUtils;
import app.retera.util.ParseUtils;
import app.retera.util.War3ID;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

public class EventObject extends GenericObject {
	private static final War3ID KEVT = War3ID.fromString("KEVT");
	private int globalSequenceId = -1;
	private long[] keyFrames = { 1 };

	public EventObject() {
		super(0x400);
	}

	@Override
	public void readMdx(final LittleEndianDataInputStream stream) throws IOException {
		super.readMdx(stream);
		stream.readInt(); // KEVT skipped
		final long count = ParseUtils.readUInt32(stream);
		this.globalSequenceId = stream.readInt();

		this.keyFrames = new long[(int) count];
		for (int i = 0; i < count; i++) {
			this.keyFrames[i] = stream.readInt();
		}
	}

	@Override
	public void writeMdx(final LittleEndianDataOutputStream stream) throws IOException {
		super.writeMdx(stream);
		ParseUtils.writeWar3ID(stream, KEVT);
		ParseUtils.writeUInt32(stream, this.keyFrames.length);
		stream.writeInt(this.globalSequenceId);
		for (long keyFrame : this.keyFrames) {
			ParseUtils.writeUInt32(stream, keyFrame);
		}
	}

	@Override
	public void readMdl(final MdlTokenInputStream stream) {
		for (final String token : super.readMdlGeneric(stream)) {
			if (MdlUtils.TOKEN_EVENT_TRACK.equals(token)) {
				this.keyFrames = new long[stream.readInt()];
				stream.readIntArray(this.keyFrames);
			}
			else {
				throw new RuntimeException("Unknown token in EventObject " + this.name + ": " + token);
			}
		}
	}

	@Override
	public void writeMdl(final MdlTokenOutputStream stream) throws IOException {
		stream.startObjectBlock(MdlUtils.TOKEN_EVENT_OBJECT, this.name);
		writeGenericHeader(stream);
		stream.startBlock(MdlUtils.TOKEN_EVENT_TRACK, this.keyFrames.length);

		for (final long keyFrame : this.keyFrames) {
			stream.writeFlagUInt32(keyFrame);
		}

		stream.endBlock();

		writeGenericTimelines(stream);
		stream.endBlock();
	}

	@Override
	public long getByteLength() {
		return 12 + (this.keyFrames.length * 4L) + super.getByteLength();
	}

	public int getGlobalSequenceId() {
		return this.globalSequenceId;
	}

	public void setGlobalSequenceId(final int globalSequenceId) {
		this.globalSequenceId = globalSequenceId;
	}

	public long[] getKeyFrames() {
		return this.keyFrames;
	}

	public void setKeyFrames(final long[] keyFrames) {
		this.keyFrames = keyFrames;
	}

}
