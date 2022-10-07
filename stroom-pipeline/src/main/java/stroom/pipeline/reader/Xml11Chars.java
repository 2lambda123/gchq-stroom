package stroom.pipeline.reader;

import java.util.Arrays;

/**
 * THIS IS A COPY OF THE XERCES-2J CLASS com.sun.org.apache.xerces.internal.utls.XMLChar
 * <p>
 * This class defines the basic properties of characters in XML 1.1. The data
 * in this class can be used to verify that a character is a valid
 * XML 1.1 character or if the character is a space, name start, or name
 * character.
 * <p>
 * A series of convenience methods are supplied to ease the burden
 * of the developer.  Using the character as an index into the <code>XML11CHARS</code>
 * array and applying the appropriate mask flag (e.g.
 * <code>MASK_VALID</code>), yields the same results as calling the
 * convenience methods. There is one exception: check the comments
 * for the <code>isValid</code> method for details.
 */
public class Xml11Chars implements XmlChars {
    //
    // Constants
    //
    /**
     * Character flags for XML 1.1.
     */
    private static final byte[] XML11CHARS = new byte[1 << 16];

    /**
     * XML 1.1 Valid character mask.
     */
    public static final int MASK_XML11_VALID = 0x01;

    /**
     * XML 1.1 Restricted character mask.
     */
    public static final int MASK_XML11_RESTRICTED = 0x02;

    /**
     * XML 1.1 Name character mask.
     */
    public static final int MASK_XML11_NAME = 0x08;

    /**
     * XML 1.1 control character mask
     */
    public static final int MASK_XML11_CONTROL = 0x10;

    /**
     * XML 1.1 content for external entities (valid - "special" chars - control chars)
     */
    public static final int MASK_XML11_CONTENT = 0x20;

    /**
     * XML namespaces 1.1 NCNameStart
     */
    public static final int MASK_XML11_NCNAME_START = 0x40;

    //
    // Static initialization
    //
    static {
        // Initializing the Character Flag Array
        // Code generated by: XML11CharGenerator.
        Arrays.fill(XML11CHARS, 1, 9, (byte) 2); // Fill 8 of value (byte) 2
        XML11CHARS[9] = 35;
        XML11CHARS[10] = 3;
        Arrays.fill(XML11CHARS, 11, 13, (byte) 2); // Fill 2 of value (byte) 2
        XML11CHARS[13] = 3;
        Arrays.fill(XML11CHARS, 14, 32, (byte) 2); // Fill 18 of value (byte) 2
        XML11CHARS[32] = 35;
        Arrays.fill(XML11CHARS, 33, 38, (byte) 33); // Fill 5 of value (byte) 33
        XML11CHARS[38] = 1;
        Arrays.fill(XML11CHARS, 39, 45, (byte) 33); // Fill 6 of value (byte) 33
        Arrays.fill(XML11CHARS, 45, 47, (byte) -87); // Fill 2 of value (byte) -87
        XML11CHARS[47] = 33;
        Arrays.fill(XML11CHARS, 48, 58, (byte) -87); // Fill 10 of value (byte) -87
        XML11CHARS[58] = 45;
        XML11CHARS[59] = 33;
        XML11CHARS[60] = 1;
        Arrays.fill(XML11CHARS, 61, 65, (byte) 33); // Fill 4 of value (byte) 33
        Arrays.fill(XML11CHARS, 65, 91, (byte) -19); // Fill 26 of value (byte) -19
        Arrays.fill(XML11CHARS, 91, 93, (byte) 33); // Fill 2 of value (byte) 33
        XML11CHARS[93] = 1;
        XML11CHARS[94] = 33;
        XML11CHARS[95] = -19;
        XML11CHARS[96] = 33;
        Arrays.fill(XML11CHARS, 97, 123, (byte) -19); // Fill 26 of value (byte) -19
        Arrays.fill(XML11CHARS, 123, 127, (byte) 33); // Fill 4 of value (byte) 33
        Arrays.fill(XML11CHARS, 127, 133, (byte) 2); // Fill 6 of value (byte) 2
        XML11CHARS[133] = 35;
        Arrays.fill(XML11CHARS, 134, 160, (byte) 2); // Fill 26 of value (byte) 2
        Arrays.fill(XML11CHARS, 160, 183, (byte) 33); // Fill 23 of value (byte) 33
        XML11CHARS[183] = -87;
        Arrays.fill(XML11CHARS, 184, 192, (byte) 33); // Fill 8 of value (byte) 33
        Arrays.fill(XML11CHARS, 192, 215, (byte) -19); // Fill 23 of value (byte) -19
        XML11CHARS[215] = 33;
        Arrays.fill(XML11CHARS, 216, 247, (byte) -19); // Fill 31 of value (byte) -19
        XML11CHARS[247] = 33;
        Arrays.fill(XML11CHARS, 248, 768, (byte) -19); // Fill 520 of value (byte) -19
        Arrays.fill(XML11CHARS, 768, 880, (byte) -87); // Fill 112 of value (byte) -87
        Arrays.fill(XML11CHARS, 880, 894, (byte) -19); // Fill 14 of value (byte) -19
        XML11CHARS[894] = 33;
        Arrays.fill(XML11CHARS, 895, 8192, (byte) -19); // Fill 7297 of value (byte) -19
        Arrays.fill(XML11CHARS, 8192, 8204, (byte) 33); // Fill 12 of value (byte) 33
        Arrays.fill(XML11CHARS, 8204, 8206, (byte) -19); // Fill 2 of value (byte) -19
        Arrays.fill(XML11CHARS, 8206, 8232, (byte) 33); // Fill 26 of value (byte) 33
        XML11CHARS[8232] = 35;
        Arrays.fill(XML11CHARS, 8233, 8255, (byte) 33); // Fill 22 of value (byte) 33
        Arrays.fill(XML11CHARS, 8255, 8257, (byte) -87); // Fill 2 of value (byte) -87
        Arrays.fill(XML11CHARS, 8257, 8304, (byte) 33); // Fill 47 of value (byte) 33
        Arrays.fill(XML11CHARS, 8304, 8592, (byte) -19); // Fill 288 of value (byte) -19
        Arrays.fill(XML11CHARS, 8592, 11264, (byte) 33); // Fill 2672 of value (byte) 33
        Arrays.fill(XML11CHARS, 11264, 12272, (byte) -19); // Fill 1008 of value (byte) -19
        Arrays.fill(XML11CHARS, 12272, 12289, (byte) 33); // Fill 17 of value (byte) 33
        Arrays.fill(XML11CHARS, 12289, 55296, (byte) -19); // Fill 43007 of value (byte) -19
        Arrays.fill(XML11CHARS, 57344, 63744, (byte) 33); // Fill 6400 of value (byte) 33
        Arrays.fill(XML11CHARS, 63744, 64976, (byte) -19); // Fill 1232 of value (byte) -19
        Arrays.fill(XML11CHARS, 64976, 65008, (byte) 33); // Fill 32 of value (byte) 33
        Arrays.fill(XML11CHARS, 65008, 65534, (byte) -19); // Fill 526 of value (byte) -19
    }

    /**
     * Returns true if the specified character is valid. This method
     * also checks the surrogate character range from 0x10000 to 0x10FFFF.
     * <p>
     * If the program chooses to apply the mask directly to the
     * <code>XML11CHARS</code> array, then they are responsible for checking
     * the surrogate character range.
     *
     * @param c The character to check.
     */
    @Override
    public boolean isValid(final int c) {
        return (c < 0x10000 && (XML11CHARS[c] & MASK_XML11_VALID) != 0)
                || (0x10000 <= c && c <= 0x10FFFF);
    }

    /**
     * Returns true if the specified character is valid and not a control
     * character.
     * This method also checks the surrogate character range from 0x10000 to 0x10FFFF.
     *
     * @param c The character to check.
     */
    @Override
    public boolean isValidLiteral(int c) {
        return ((c < 0x10000 && ((XML11CHARS[c] & MASK_XML11_VALID) != 0
                && (XML11CHARS[c] & MASK_XML11_CONTROL) == 0))
                || (0x10000 <= c && c <= 0x10FFFF));
    }
}
