package app.retera.parsers.mdlx;

import java.io.IOException;
import java.util.EnumSet;

import app.retera.util.MdlUtils;
import app.retera.util.ParseUtils;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

public class Sequence implements MdlxBlock {
	private String name = "";
	private final long[] interval = new long[2];
	private float moveSpeed = 0;
	private int flags = 0;
	private float rarity = 0;
	private long syncPoint = 0;
	private final Extent extent = new Extent();

	/**
	 * Restricts us to only be able to parse models on one thread at a time, in
	 * return for high performance.
	 */
	private static final byte[] NAME_BYTES_HEAP = new byte[80];

	@Override
	public void readMdx(final LittleEndianDataInputStream stream) throws IOException {
		this.name = ParseUtils.readString(stream, NAME_BYTES_HEAP);
		ParseUtils.readUInt32Array(stream, this.interval);
		this.moveSpeed = stream.readFloat();
		this.flags = (int) ParseUtils.readUInt32(stream);
		this.rarity = stream.readFloat();
		this.syncPoint = ParseUtils.readUInt32(stream);
		this.extent.readMdx(stream);
	}

	@Override
	public void writeMdx(final LittleEndianDataOutputStream stream) throws IOException {
		final byte[] bytes = this.name.getBytes(ParseUtils.UTF8);
		stream.write(bytes);
		for (int i = 0; i < (NAME_BYTES_HEAP.length - bytes.length); i++) {
			stream.write((byte) 0);
		}
		ParseUtils.writeUInt32Array(stream, this.interval);
		stream.writeFloat(this.moveSpeed);
		ParseUtils.writeUInt32(stream, this.flags);
		stream.writeFloat(this.rarity);
		ParseUtils.writeUInt32(stream, this.syncPoint);
		this.extent.writeMdx(stream);
	}

	@Override
	public void readMdl(final MdlTokenInputStream stream) {
		this.name = stream.read();

		for (final String token : stream.readBlock()) {
			switch (token) {
				case MdlUtils.TOKEN_INTERVAL -> stream.readIntArray(this.interval);
				case MdlUtils.TOKEN_NONLOOPING -> this.flags = 1;
				case MdlUtils.TOKEN_MOVESPEED -> this.moveSpeed = stream.readFloat();
				case MdlUtils.TOKEN_RARITY -> this.rarity = stream.readFloat();
				case MdlUtils.TOKEN_MINIMUM_EXTENT -> stream.readFloatArray(this.extent.min);
				case MdlUtils.TOKEN_MAXIMUM_EXTENT -> stream.readFloatArray(this.extent.max);
				case MdlUtils.TOKEN_BOUNDSRADIUS -> this.extent.boundsRadius = stream.readFloat();
				default -> throw new IllegalStateException("Unknown token in Sequence \"" + this.name + "\": " + token);
			}
		}
	}

	@Override
	public void writeMdl(final MdlTokenOutputStream stream) {
		stream.startObjectBlock(MdlUtils.TOKEN_ANIM, this.name);
		stream.writeArrayAttrib(MdlUtils.TOKEN_INTERVAL, this.interval);

		if (this.flags == 1) {
			stream.writeFlag(MdlUtils.TOKEN_NONLOOPING);
		}

		if (this.moveSpeed != 0) {
			stream.writeFloatAttrib(MdlUtils.TOKEN_MOVESPEED, this.moveSpeed);
		}

		if (this.rarity != 0) {
			stream.writeFloatAttrib(MdlUtils.TOKEN_RARITY, this.rarity);
		}

		this.extent.writeMdl(stream);
		stream.endBlock();
	}

	public long[] getInterval() {
		return this.interval;
	}

	public int getFlags() {
		return this.flags;
	}

	public String getName() {
		return this.name;
	}

	public float getRarity() {
		return this.rarity;
	}

	public float getMoveSpeed() {
		return this.moveSpeed;
	}

	public long getSyncPoint() {
		return this.syncPoint;
	}

	public Extent getExtent() {
		return this.extent;
	}
}
