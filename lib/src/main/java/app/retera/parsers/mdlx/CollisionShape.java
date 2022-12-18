package app.retera.parsers.mdlx;

import java.io.IOException;

import app.retera.util.MdlUtils;
import app.retera.util.ParseUtils;
import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

public class CollisionShape extends GenericObject {
	public static enum CollisionShapeType {
		BOX(false, 2, MdlUtils.TOKEN_BOX),
		PLANE(false, 2, MdlUtils.TOKEN_PLANE),
		SPHERE(true, 1, MdlUtils.TOKEN_SPHERE),
		CYLINDER(true, 2, MdlUtils.TOKEN_CYLINDER);

		private static final CollisionShapeType[] VALUES = values();

		private final boolean boundsRadius;
		private final int vertices;
		private final String mdlName;

		private CollisionShapeType(final boolean boundsRadius, final int vertices, String mdlName) {
			this.boundsRadius = boundsRadius;
			this.vertices = vertices;
			this.mdlName = mdlName;
		}

		public boolean isBoundsRadius() {
			return this.boundsRadius;
		}

		public int getVertices() {
			return vertices;
		}

		public String getMdlName() {
			return mdlName;
		}

		public static CollisionShapeType from(final int index) {
			return VALUES[index];
		}
	}

	private CollisionShapeType type;
	private final float[][] vertices = { new float[3], new float[3] };
	private float boundsRadius;

	public CollisionShape() {
		super(0x2000);
	}

	@Override
	public void readMdx(final LittleEndianDataInputStream stream) throws IOException {
		super.readMdx(stream);

		final long typeIndex = ParseUtils.readUInt32(stream);
		type = CollisionShapeType.from((int) typeIndex);

		for (int i = 0; i < type.getVertices(); i++) {
			ParseUtils.readFloatArray(stream, this.vertices[i]);
		}

		if (type.isBoundsRadius()) {
			this.boundsRadius = stream.readFloat();
		}
	}

	@Override
	public void writeMdx(final LittleEndianDataOutputStream stream) throws IOException {
		super.writeMdx(stream);

		if (type != null) {
			ParseUtils.writeUInt32(stream, type.ordinal());

			for (int i = 0; i < type.getVertices(); i++) {
				ParseUtils.writeFloatArray(stream, vertices[i]);
			}
			if (type.isBoundsRadius()) {
				stream.writeFloat(this.boundsRadius);
			}
		} else {
			ParseUtils.writeUInt32(stream, -1);
		}
	}

	@Override
	public void readMdl(final MdlTokenInputStream stream) {
		for (final String token : super.readMdlGeneric(stream)) {
			switch (token) {
				case MdlUtils.TOKEN_BOX -> this.type = CollisionShapeType.BOX;
				case MdlUtils.TOKEN_PLANE -> this.type = CollisionShapeType.PLANE;
				case MdlUtils.TOKEN_SPHERE -> this.type = CollisionShapeType.SPHERE;
				case MdlUtils.TOKEN_CYLINDER -> this.type = CollisionShapeType.CYLINDER;
				case MdlUtils.TOKEN_VERTICES -> {
					final int count = stream.readInt();
					stream.read(); // {
					for (int i = 0; i < count; i++) {
						stream.readFloatArray(this.vertices[i]);
					}
					stream.read(); // }
				}
				case MdlUtils.TOKEN_BOUNDSRADIUS -> this.boundsRadius = stream.readFloat();
				default -> throw new RuntimeException("Unknown token in CollisionShape " + this.name + ": " + token);
			}
		}
	}

	@Override
	public void writeMdl(final MdlTokenOutputStream stream) throws IOException {
		stream.startObjectBlock(MdlUtils.TOKEN_COLLISION_SHAPE, this.name);
		writeGenericHeader(stream);

		if (type != null) {
			stream.writeFlag(type.getMdlName());

			stream.startBlock(MdlUtils.TOKEN_VERTICES, type.getVertices());
			for (int i = 0; i < type.getVertices(); i++) {
				stream.writeFloatArray(vertices[i]);
			}
			stream.endBlock();

			if (type.isBoundsRadius()) {
				stream.writeFloatAttrib(MdlUtils.TOKEN_BOUNDSRADIUS, this.boundsRadius);
			}
		} else {
			throw new IllegalStateException("Invalid type in CollisionShape " + this.name + ": " + null);
		}

		writeGenericTimelines(stream);
		stream.endBlock();
	}

	@Override
	public long getByteLength() {
		long size = super.getByteLength() + 4 + (12L * type.getVertices());

		if (type.isBoundsRadius()) {
			size += 4;
		}

		return size;
	}

	public float[][] getVertices() {
		return this.vertices;
	}

	public CollisionShapeType getType() {
		return this.type;
	}

	public float getBoundsRadius() {
		return this.boundsRadius;
	}

}
