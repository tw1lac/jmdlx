package app.retera.parsers.mdlx.timeline;

import java.io.IOException;

import app.retera.parsers.mdlx.MdlTokenInputStream;
import app.retera.parsers.mdlx.MdlTokenOutputStream;
import app.retera.util.ParseUtils;
import app.retera.util.War3ID;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

public final class FloatArrayTimeline extends Timeline<float[]> {
	private final int arraySize;

	public FloatArrayTimeline(final int arraySize) {
		this.arraySize = arraySize;
	}
	public FloatArrayTimeline(final int arraySize, War3ID name) {
		super(name);
		this.arraySize = arraySize;
	}

	@Override
	protected int size() {
		return this.arraySize;
	}

	@Override
	protected float[] readMdxValue(final LittleEndianDataInputStream stream) throws IOException {
		return ParseUtils.readFloatArray(stream, this.arraySize);
	}

	@Override
	protected float[] readMdlValue(final MdlTokenInputStream stream) {
		final float[] output = new float[this.arraySize];
		stream.readKeyframe(output);
		return output;
	}

	@Override
	protected void writeMdxValue(final LittleEndianDataOutputStream stream, final float[] value) throws IOException {
		ParseUtils.writeFloatArray(stream, value);
	}

	@Override
	protected void writeMdlValue(final MdlTokenOutputStream stream, final String prefix, final float[] value) {
		stream.writeKeyframe(prefix, value);
	}

	public int getArraySize() {
		return this.arraySize;
	}

}
