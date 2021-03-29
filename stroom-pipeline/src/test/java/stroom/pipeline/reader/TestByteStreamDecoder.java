package stroom.pipeline.reader;

import stroom.pipeline.reader.ByteStreamDecoder.DecodedChar;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class TestByteStreamDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestByteStreamDecoder.class);

    @Test
    void testByteStreamDecoder() throws IOException {

        final Charset charset = StandardCharsets.UTF_8;

        final String input = "𝄞🌉hello↵byeᚏ"; // 𝄞 is 4 bytes, 𩸽 is 4 bytes, ↵ is 2 bytes, ᚏ is three
        final int inputByteCount = input.getBytes(charset).length;
        LOGGER.info("Input: {}, byteCount: {}, bytesPerChar: {}",
                input, inputByteCount, (double) inputByteCount / input.length());

        final MyByteBuffer myByteBuffer = new MyByteBuffer();
        myByteBuffer.write(input.getBytes(charset));

        final StringBuilder outputStringBuilder = new StringBuilder();

        final AtomicInteger byteOffset = new AtomicInteger(0);
        final ByteStreamDecoder byteStreamDecoder = new ByteStreamDecoder(charset.name());

        while (byteOffset.get() < input.getBytes(charset).length) {

            final DecodedChar decodedChar = byteStreamDecoder.decodeNextChar(() ->
                    myByteBuffer.getByte(byteOffset.getAndIncrement()));

            outputStringBuilder.append(decodedChar.getAsString());
        }

        LOGGER.info("outputStringBuilder: {}", outputStringBuilder.toString());

        Assertions.assertThat(outputStringBuilder.toString())
                .isEqualTo(input);

        Assertions.assertThat(byteOffset)
                .hasValue(inputByteCount);
    }

    @Test
    void testFlagEmoji() throws IOException {
        final String gbFlag = fromUnicode(0x1F1EC, 0x1F1E7);
        doTest(gbFlag);
    }

    @Test
    void testCompoundEmoji() throws IOException {
        final String womanMediumDarkSkinToneWhiteHair = fromUnicode(
                0x1F469,
                0x1F3FE,
                0x200D,
                0x1F9B3);
        doTest(womanMediumDarkSkinToneWhiteHair);
    }

    @Test
    void testHeartsEmoji() throws IOException {
        final String heartsEmoji = "💕";
        doTest(heartsEmoji);
    }

    @Test
    void testSmilyEmoji() throws IOException {
        final String heartsEmoji = "😀";
        doTest(heartsEmoji);
    }

    private String fromUnicode(final int... unicodePoints) {
        return new String(unicodePoints, 0, unicodePoints.length);
    }

    void doTest(final String input) throws IOException {

        final Charset charset = StandardCharsets.UTF_8;

        final int inputByteCount = input.getBytes(charset).length;
        LOGGER.info("Input: [{}], byteCount: {}, bytesPerChar: {}",
                input, inputByteCount, (double) inputByteCount / input.length());

        final MyByteBuffer myByteBuffer = new MyByteBuffer();
        myByteBuffer.write(input.getBytes(charset));

        final StringBuilder outputStringBuilder = new StringBuilder();

        final AtomicInteger byteOffset = new AtomicInteger(0);
        final ByteStreamDecoder byteStreamDecoder = new ByteStreamDecoder(charset.name());

        int outputByteCount = 0;
        int outputVisibleCharCount = 0;
        while (byteOffset.get() < input.getBytes(charset).length) {

            final DecodedChar decodedChar = byteStreamDecoder.decodeNextChar(() ->
                    myByteBuffer.getByte(byteOffset.getAndIncrement()));

            outputByteCount += decodedChar.getByteCount();
            outputVisibleCharCount++;

            LOGGER.info("decodedChar {}", decodedChar);

            outputStringBuilder.append(decodedChar.getAsString());
        }

        LOGGER.info("outputStringBuilder: [{}], byte count {}, visible char count {}",
                outputStringBuilder.toString(),
                outputByteCount,
                outputVisibleCharCount);

        Assertions.assertThat(outputStringBuilder.toString())
                .isEqualTo(input);

        Assertions.assertThat(byteOffset)
                .hasValue(inputByteCount);
    }

    @Test
    void testByteStreamDecoder_withBOM() throws IOException {

        final Charset charset = StandardCharsets.UTF_8;

        final String input = "hello";
//                .append(0xef)
//                .append(0xbb)
//                .append(0xbf)
//                .append("hello");
//        final int inputByteCount = input.toString().getBytes(charset).length;
//        LOGGER.info("Input: [{}], byteCount: {}, bytesPerChar: {}",
//                input, inputByteCount, (double) inputByteCount / input.length());

        final MyByteBuffer myByteBuffer = new MyByteBuffer();
        myByteBuffer.write(new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf});
        myByteBuffer.write(input.getBytes(charset));

        final StringBuilder outputStringBuilder = new StringBuilder();

        final AtomicInteger byteOffset = new AtomicInteger(0);
        final ByteStreamDecoder byteStreamDecoder = new ByteStreamDecoder(charset.name());

        List<DecodedChar> decodedChars = new ArrayList<>();
        while (byteOffset.get() < myByteBuffer.size()) {

            final DecodedChar decodedChar = byteStreamDecoder.decodeNextChar(() ->
                    myByteBuffer.getByte(byteOffset.getAndIncrement()));

            decodedChars.add(decodedChar);

            LOGGER.info("SizedString: {}");

            if (decodedChar.isByteOrderMark()) {
                LOGGER.info("BOM found");
            }

            outputStringBuilder.append(decodedChar.getAsString());
        }

        LOGGER.info("outputStringBuilder: [{}]", outputStringBuilder.toString());

        Assertions.assertThat(decodedChars.get(0).isNonVisibleCharacter())
                .isTrue();
        Assertions.assertThat(decodedChars.get(1).isNonVisibleCharacter())
                .isFalse();
    }

    private static class MyByteBuffer extends ByteArrayOutputStream {

        byte getByte(final int index) {
            return buf[index];
        }
    }
}
