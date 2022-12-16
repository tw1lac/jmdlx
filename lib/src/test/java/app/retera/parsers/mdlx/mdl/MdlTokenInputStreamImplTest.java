package app.retera.parsers.mdlx.mdl;

import app.retera.parsers.mdlx.MdlxModel;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MdlTokenInputStreamImplTest {
    @Test
    void basicTokenParsing() {
        MdlxModel model = new MdlxModel();
        MdlTokenInputStreamImpl stream = new MdlTokenInputStreamImpl(new StringReader("This { Is A Test }"));
        assertEquals("This", stream.read());
        assertEquals("{", stream.read());
        assertEquals("Is", stream.read());
        assertEquals("A", stream.read());
        assertEquals("Test", stream.read());
        assertEquals("}", stream.read());
    }

    @Test
    void stringTokenParsing() {
        MdlxModel model = new MdlxModel();
        MdlTokenInputStreamImpl stream = new MdlTokenInputStreamImpl(new StringReader("This { Is A \"Test\" }"));
        assertEquals("This", stream.read());
        assertEquals("{", stream.read());
        assertEquals("Is", stream.read());
        assertEquals("A", stream.read());
        assertEquals("Test", stream.read());
        assertEquals("}", stream.read());
    }
}
