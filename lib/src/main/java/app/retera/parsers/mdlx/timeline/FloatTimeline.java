package app.retera.parsers.mdlx.timeline;

import java.io.IOException;

import app.retera.parsers.mdlx.MdlTokenInputStream;
import app.retera.parsers.mdlx.MdlTokenOutputStream;
import app.retera.util.War3ID;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

public final class FloatTimeline extends Timeline<float[]> {

	public FloatTimeline(War3ID name) {
		super(name);
	}
	public FloatTimeline() {
	}

	@Override
	protected int size() {
		return 1;
	}

	@Override
	protected float[] readMdxValue(final LittleEndianDataInputStream stream) throws IOException {
		return new float[] { stream.readFloat() };
	}

	@Override
	protected float[] readMdlValue(final MdlTokenInputStream stream) {
		return new float[] { stream.readFloat() };
	}

	@Override
	protected void writeMdxValue(final LittleEndianDataOutputStream stream, final float[] value) throws IOException {
		stream.writeFloat(value[0]);
	}

	@Override
	protected void writeMdlValue(final MdlTokenOutputStream stream, final String prefix, final float[] value) {
		stream.writeKeyframe(prefix, value[0]);
	}

}
