package app.retera.parsers.mdlx;

import app.retera.parsers.mdlx.timeline.FloatArrayTimeline;
import app.retera.parsers.mdlx.timeline.FloatTimeline;
import app.retera.parsers.mdlx.timeline.Timeline;
import app.retera.parsers.mdlx.timeline.UInt32Timeline;

public interface TimelineDescriptor {
	Timeline createTimeline();

	public static final TimelineDescriptor UINT32_TIMELINE = new TimelineDescriptor() {
		@Override
		public Timeline createTimeline() {
			return new UInt32Timeline();
		}
	};

	public static final TimelineDescriptor FLOAT_TIMELINE = new TimelineDescriptor() {
		@Override
		public Timeline createTimeline() {
			return new FloatTimeline();
		}
	};

	public static final TimelineDescriptor VECTOR3_TIMELINE = new TimelineDescriptor() {
		@Override
		public Timeline createTimeline() {
			return new FloatArrayTimeline(3);
		}
	};

	public static final TimelineDescriptor VECTOR4_TIMELINE = new TimelineDescriptor() {
		@Override
		public Timeline createTimeline() {
			return new FloatArrayTimeline(4);
		}
	};
}
