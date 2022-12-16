package app.retera.parsers.mdlx.mdl;

import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.Iterator;

import app.retera.parsers.mdlx.MdlTokenInputStream;

public class MdlTokenInputStreamImpl implements MdlTokenInputStream {
	private final Reader reader;
	private int next;
	private final int ident;
	private final int fractionDigits;

	public MdlTokenInputStreamImpl(final Reader reader) {
		this.reader = reader;
		this.next = readOneChar();
		this.ident = 0; // Used for writing blocks nicely.
		this.fractionDigits = 6; // The number of fraction digits when writing floats.
	}

	private int readOneChar() {
		try {
			return reader.read();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String read() {
		boolean inComment = false;
		boolean inString = false;
		final StringBuilder token = new StringBuilder();

		while (next != -1) {
			// Note: cast from 'byte' to 'char' will cause Java incompatibility with Chinese
			// and Russian/Cyrillic and others
			final char c = (char) next;

			if (inComment) {
				if (c == '\n') {
					inComment = false;
				}
			}
			else if (inString) {
				if (c == '"') {
					next = readOneChar();
					return token.toString();
				}
				else {
					token.append(c);
				}
			}
			else if ((c == ' ') || (c == ',') || (c == '\t') || (c == '\n') || (c == ':') || (c == '\r')) {
				if (token.length() > 0) {
					return token.toString();
				}
			}
			else if ((c == '{') || (c == '}')) {
				if (token.length() > 0) {
					return token.toString();
				}
				else {
					next = readOneChar();
					return Character.toString(c);
				}
			}
			else if ((c == '/') && ((next = readOneChar()) == '/')) {
				if (token.length() > 0) {
					return token.toString();
				}
				else {
					inComment = true;
				}
			}
			else if (c == '"') {
				if (token.length() > 0) {
					return token.toString();
				}
				else {
					inString = true;
				}
			}
			else {
				token.append(c);
			}
			next = readOneChar();
		}
		return null;
	}

	@Override
	public String peek() {
		final int prevNext = next;
		try {
			reader.mark(1024);
			final String value = this.read();
			reader.reset();
			next = prevNext;
			return value;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long readUInt32() {
		return Long.parseLong(this.read());
	}

	@Override
	public int readInt() {
		return Integer.parseInt(this.read());
	}

	@Override
	public float readFloat() {
		return Float.parseFloat(this.read());
	}

	@Override
	public void readIntArray(final long[] values) {
		this.read(); // {

		for (int i = 0, l = values.length; i < l; i++) {
			values[i] = this.readInt();
		}

		this.read(); // }
	}

	@Override
	public float[] readFloatArray(final float[] values) {
		this.read(); // {

		for (int i = 0, l = values.length; i < l; i++) {
			values[i] = this.readFloat();
		}

		this.read(); // }
		return values;
	}

	/**
	 * Read an MDL keyframe value. If the value is a scalar, it is just the number.
	 * If the value is a vector, it is enclosed with curly braces.
	 *
	 * @param {Float32Array|Uint32Array} value
	 */
	@Override
	public void readKeyframe(final float[] values) {
		if (values.length == 1) {
			values[0] = this.readFloat();
		}
		else {
			this.readFloatArray(values);
		}
	}

	@Override
	public float[] readVectorArray(final float[] array, final int vectorLength) {
		this.read(); // {

		for (int i = 0, l = array.length / vectorLength; i < l; i++) {
			this.read(); // {

			for (int j = 0; j < vectorLength; j++) {
				array[(i * vectorLength) + j] = this.readFloat();
			}

			this.read(); // }
		}

		this.read(); // }
		return array;
	}

	@Override
	public Iterable<String> readBlock() {
		this.read(); // {
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {
				return new Iterator<String>() {
					String current;
					private boolean hasLoaded = false;

					@Override
					public String next() {
						if (!this.hasLoaded) {
							hasNext();
						}
						this.hasLoaded = false;
						return this.current;
					}

					@Override
					public boolean hasNext() {
						this.current = read();
						this.hasLoaded = true;
						return (this.current != null) && !this.current.equals("}");
					}
				};
			}
		};
	}

	@Override
	public int[] readUInt16Array(final int[] values) {
		this.read(); // {

		for (int i = 0, l = values.length; i < l; i++) {
			values[i] = this.readInt();
		}

		this.read(); // }

		return values;
	}

	@Override
	public short[] readUInt8Array(final short[] values) {
		this.read(); // {

		for (int i = 0, l = values.length; i < l; i++) {
			values[i] = Short.parseShort(this.read());
		}

		this.read(); // }

		return values;
	}

	@Override
	public void readColor(final float[] color) {
		this.read(); // {

		color[2] = this.readFloat();
		color[1] = this.readFloat();
		color[0] = this.readFloat();

		this.read(); // }
	}
}
