package secpriv.horst.evm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;

public abstract class EvmSourceProvider {
    public static EvmSourceProvider fromAnyFile(File file) {
        if (file.getName().endsWith(".json")) {
            return fromJsonFile(file);
        }
        return fromPlainFile(file);
    }

    public abstract String getSource();

    public abstract BigInteger getId();

    private static class BufferedEvmSourceProvider extends EvmSourceProvider {
        private final String source;
        private final BigInteger id;

        private BufferedEvmSourceProvider(String source, BigInteger id) {
            this.source = source;
            this.id = id;
        }

        @Override
        public String getSource() {
            return source;
        }

        @Override
        public BigInteger getId() {
            return id;
        }
    }

    public static EvmSourceProvider fromPlainFile(File file) {
        try {
            String code = new String(Files.readAllBytes(file.toPath()));
            return new BufferedEvmSourceProvider(code, BigInteger.ZERO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static EvmSourceProvider fromJsonFile(File file) {
        try {
            JsonObject jsonObject = new JsonParser().parse(new String(Files.readAllBytes(file.toPath()))).getAsJsonObject();
            String code = ignore0x(jsonObject.get("bytecode").getAsString());
            BigInteger id = new BigInteger(ignore0x(jsonObject.get("address").getAsString()), 16);
            return new BufferedEvmSourceProvider(code, BigInteger.ZERO);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String ignore0x(String bytecode) {
        if (bytecode.startsWith("0x")) {
            return bytecode.substring(2);
        } else
            return bytecode;
    }
}
