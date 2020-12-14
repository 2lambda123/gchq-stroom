package stroom.query.common.v2;

import stroom.dashboard.expression.v1.Expression;
import stroom.dashboard.expression.v1.Generator;
import stroom.dashboard.expression.v1.ValSerialiser;
import stroom.pipeline.refdata.util.PooledByteBufferOutputStream;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import javax.inject.Provider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ItemSerialiser {
    private final Provider<PooledByteBufferOutputStream> outputStreamProvider;
    private final CompiledField[] fields;

//    private static final LinkedBlockingQueue<byte[]> bytesPool = new LinkedBlockingQueue<>(1000);

    public ItemSerialiser(final Provider<PooledByteBufferOutputStream> outputStreamProvider,
                          final CompiledField[] fields) {
        this.outputStreamProvider = outputStreamProvider;
        this.fields = fields;
    }

    Key readKey(final Input input) {
        return Metrics.measure("Key read", () -> {
            final int size = input.readInt();
            final List<KeyPart> list = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                final boolean grouped = input.readBoolean();
                if (grouped) {
                    list.add(new GroupKeyPart(ValSerialiser.readArray(input)));
                } else {
                    list.add(new UngroupedKeyPart(input.readLong()));
                }
            }
            return new Key(list);
        });
    }

    void writeKey(final Key key, final Output output) {
        Metrics.measure("Key write", () -> {
            output.writeInt(key.size());
            for (final KeyPart keyPart : key) {
                output.writeBoolean(keyPart.isGrouped());
                keyPart.write(output);
            }
        });
    }

    byte[] toBytes(final Key key) {
        return Metrics.measure("Key toBytes", () ->
                toBytes(output ->
                        writeKey(key, output)));
    }

    byte[] toBytes(final Consumer<Output> outputConsumer) {
//        byte[] buffer = bytesPool.poll();
//        if (buffer == null) {
//            buffer = new byte[100];
//        }

        byte[] result;
        try (final Output output = new Output(100, 4096)) {
            outputConsumer.accept(output);
            output.flush();



            result = output.toBytes();
//            } catch (final IOException e) {
//                throw new UncheckedIOException(e);

//            buffer = output.getBuffer();
        }

//        bytesPool.add(buffer);

        return result;

//        try (final Output output = new Output(100, 4096)) {
//            outputConsumer.accept(output);
//            output.flush();
//            return output.toBytes();
////            } catch (final IOException e) {
////                throw new UncheckedIOException(e);
//        }


//        try (final PooledByteBufferOutputStream byteArrayOutputStream = outputStreamProvider.get()) {
//            try (final Output output = new Output(byteArrayOutputStream)) {
//                writeKey(key, output);
//                output.flush();
//                byteArrayOutputStream.flush();
//                return getBytes(byteArrayOutputStream);
//            } catch (final IOException e) {
//                throw new UncheckedIOException(e);
//            }
//        }

    }

    RawKey toRawKey(final Key key) {
        return Metrics.measure("Key toRawKey", () ->
                new RawKey(toBytes(key)));
    }

    Key toKey(final RawKey rawKey) {
        return Metrics.measure("Key toKey (rawKey)", () ->
                toKey(rawKey.getBytes()));
    }

    Key toKey(final byte[] bytes) {
        return Metrics.measure("Key toKey (bytes)", () -> {
            try (final Input input = new Input(bytes)) {
                return readKey(input);
            }
        });
    }


    byte[] toBytes(final RawItem rawItem) {
        return Metrics.measure("Item toBytes rawItem", () ->
                toBytes(output ->
                        writeRawItem(rawItem, output)));
    }

    RawItem readRawItem(final Input input) {
        return Metrics.measure("Item readRawItem input", () -> {
            try {
                final int groupKeyLength = input.readInt();
                final byte[] key = input.readBytes(groupKeyLength);
                final byte[] generators = input.readAllBytes();
                return new RawItem(key, generators);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    RawItem readRawItem(final byte[] bytes) {
        return Metrics.measure("Item readRawItem bytes", () -> {
            try (final Input input = new Input(bytes)) {
                return readRawItem(input);
            }
        });
    }

    void writeRawItem(final RawItem rawItem, final Output output) {
        Metrics.measure("Item writeRawItem", () -> {
            if (rawItem.getKey() != null) {
                output.writeInt(rawItem.getKey().length);
                output.writeBytes(rawItem.getKey());
            } else {
                output.writeInt(0);
            }

            if (rawItem.getGenerators() != null) {
                output.writeBytes(rawItem.getGenerators());
            }
        });
    }

    byte[] toBytes(final Generator[] generators) {
        return Metrics.measure("Item toBytes", () ->
                toBytes(output ->
            writeGenerators(generators, output)));
    }

    Generator[] readGenerators(final byte[] bytes) {
        return Metrics.measure("Item readGenerators bytes", () -> {
            try (final Input input = new Input(bytes)) {
                return readGenerators(input);
            }
        });
    }

    private Generator[] readGenerators(final Input input) {
        return Metrics.measure("Item readGenerators input", () -> {
            final Generator[] generators = new Generator[fields.length];
            int pos = 0;
            for (final CompiledField compiledField : fields) {
                final boolean nonNull = input.readBoolean();
                if (nonNull) {
                    final Expression expression = compiledField.getExpression();
                    final Generator generator = expression.createGenerator();
                    generator.read(input);
                    generators[pos] = generator;
                }
                pos++;
            }
            return generators;
        });
    }

    private void writeGenerators(final Generator[] generators, final Output output) {
        Metrics.measure("Item writeGenerators", () -> {
            if (generators.length > Byte.MAX_VALUE) {
                throw new RuntimeException("You can only write a maximum of " + 255 + " values");
            }
            for (final Generator generator : generators) {
                if (generator != null) {
                    output.writeBoolean(true);
                    generator.write(output);
                } else {
                    output.writeBoolean(false);
                }
            }
        });
    }

//    byte[] toBytes(final Item item) {
//        return Metrics.measure("Item toBytes", () -> {
//            final RawItem rawItem = new RawItem(toBytes(item.getKey()), toBytes(item.getGenerators()));
//            return toBytes(rawItem);
//        });
//    }
//
//    Item readItem(final byte[] bytes) {
//        return Metrics.measure("Item readItem", () -> {
//            final RawItem rawItem = readRawItem(bytes);
//            Generator[] generators = readGenerators(rawItem.getGenerators());
//            return new Item(rawItem.getGroupKey(), generators);
//        });
//    }


//    private byte[] getBytes(final PooledByteBufferOutputStream bufferOutputStream) {
//        return getBytes(bufferOutputStream.getPooledByteBuffer().getByteBuffer());
//    }
//
//    private byte[] getBytes(final ByteBuffer byteBuffer) {
//        final byte[] arr = new byte[byteBuffer.remaining()];
//        byteBuffer.get(arr);
//        return arr;
//    }

}
