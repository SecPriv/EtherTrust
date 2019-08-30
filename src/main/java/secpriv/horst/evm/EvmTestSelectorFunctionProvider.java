package secpriv.horst.evm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import secpriv.horst.data.tuples.Tuple2;
import secpriv.horst.data.tuples.Tuple3;
import secpriv.horst.data.tuples.Tuple4;
import secpriv.horst.data.tuples.Tuple5;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class EvmTestSelectorFunctionProvider implements EvmSelectorFunctionProviderTemplate {
    private static final Logger LOGGER = LogManager.getLogger(EvmTestSelectorFunctionProvider.class);
    private EvmSelectorFunctionProviderTemplate evmSelectorFunctionProvider;
    private final Map<BigInteger, ContractLexer.ContractInfo> contractInfos;
    private final Map<BigInteger, Map<BigInteger, BigInteger>> postStorage = new HashMap<>();
    private static final Logger logger = LogManager.getLogger(EvmTestSelectorFunctionProvider.class);
    private final ContractInfoReader contractInfoReader;

    private static String ignore0x(String bytecode) {
        if (bytecode.startsWith("0x")) {
            return bytecode.substring(2);
        } else
            return bytecode;
    }

    public void setEvmSelectorFunctionProvider(EvmSelectorFunctionProviderTemplate evmSelectorFunctionProvider) {
        this.evmSelectorFunctionProvider = evmSelectorFunctionProvider;
    }

    public EvmTestSelectorFunctionProvider(String[] evmTestsSelectorFunctionProviderArguments, boolean stat) throws IOException {
        contractInfoReader = new ContractInfoReader();

        for (String fileName : evmTestsSelectorFunctionProviderArguments) {
            logger.info("Now reading: " + fileName);
            JsonObject jsonObject = new JsonParser().parse(new String(Files.readAllBytes(Paths.get(fileName)))).getAsJsonObject();
            Set<Map.Entry<String, JsonElement>> entrySet = jsonObject.entrySet();

            Map.Entry<String, JsonElement> i = entrySet.iterator().next();

            JsonObject root = i.getValue().getAsJsonObject();
            String source = root.get("exec").getAsJsonObject().get("code").getAsString();
            String idAsString = root.get("exec").getAsJsonObject().get("address").getAsString();
            BigInteger id = new BigInteger(ignore0x(idAsString), 16);

            postStorage.put(id, readToStorage(root, idAsString));

            contractInfoReader.contractInfos.put(id, ContractLexer.generateContractInfo(ignore0x(source), stat));
        }

        contractInfos = contractInfoReader.getContractInfos();
    }

    private BigInteger hexStringToBigInteger(String hex) {
        return new BigInteger(ignore0x(hex), 16);
    }

    private Map<BigInteger, BigInteger> readToStorage(JsonObject root, String idAsString) {
        JsonElement entriesElement  = root.get("post");

        if(entriesElement != null) {
             JsonElement storageOfId = entriesElement.getAsJsonObject().get(idAsString);

             if(storageOfId != null) {
                 Set<Map.Entry<String, JsonElement>> entries = storageOfId.getAsJsonObject().get("storage").getAsJsonObject().entrySet();

                 Map<BigInteger, BigInteger> ret = new HashMap<>();
                 for (Map.Entry<String, JsonElement> entry : entries) {
                     ret.put(hexStringToBigInteger(entry.getKey()), hexStringToBigInteger(entry.getValue().getAsString()));
                 }
                 return ret;
             }
        }

        return Collections.emptyMap();
    }

    public Iterable<Tuple2<BigInteger, BigInteger>> postStorageForId(BigInteger id) {
        return postStorage.getOrDefault(id, Collections.emptyMap()).entrySet().stream().map(e -> new Tuple2<>(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    public Iterable<Boolean> emptyListIfNoPostConditionForId (BigInteger id) {
        for (BigInteger idd : postStorage.keySet()) {
            //TODO technically incorrect, but we only ever have one id
            if (postStorage.get(idd).isEmpty()) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList(true);
            }
        }
        return Collections.emptyList();
    }

    public Iterable<BigInteger> lastPcsForId (BigInteger id) {
        for (BigInteger idd : postStorage.keySet()) {
            //TODO technically incorrect, but we only ever have one id
            if (postStorage.get(idd).isEmpty()) {
                LOGGER.warn("Empty post condition, test is trivially passed by reachability!");
            }
        }
        return evmSelectorFunctionProvider.lastPcsForId(id);
    }

    /*public Iterable<Tuple2<BigInteger, BigInteger>> idsAndLastPc() {
        for (BigInteger id : postStorage.keySet()) {
            //TODO technically incorrect, but we only ever have one id
            if (postStorage.get(id).isEmpty()) {
                LOGGER.warn("Empty post condition, test is trivially passed!");
                return Collections.emptyList();
            }
        }
        return evmSelectorFunctionProvider.idsAndLastPc();
    } */

    public Iterable<Tuple2<BigInteger, BigInteger>> sizeAndOffSetForWordsize(BigInteger a) {
        return evmSelectorFunctionProvider.sizeAndOffSetForWordsize(a);
    }

    /*public Iterable<Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>> idsAndPcsAndValuesAndOffsetsForPush() {
        return evmSelectorFunctionProvider.idsAndPcsAndValuesAndOffsetsForPush();
    }*/

    //public Iterable<BigInteger> interval(BigInteger a) {
    //    return evmSelectorFunctionProvider.interval(a);
    //}

    @Override
    public Iterable<BigInteger> interval(BigInteger a, BigInteger b) {
        return evmSelectorFunctionProvider.interval(a, b);
    }

    @Override
    public Iterable<BigInteger> binOps() {
        return evmSelectorFunctionProvider.binOps();
    }

    @Override
    public Iterable<BigInteger> unOps() {
        return evmSelectorFunctionProvider.unOps();
    }

    @Override
    public Iterable<BigInteger> terOps() {
        return evmSelectorFunctionProvider.terOps();
    }

    @Override
    public Iterable<BigInteger> unitOps() {
        return evmSelectorFunctionProvider.unitOps();
    }

    @Override
    public Iterable<BigInteger> copyOps() {
        return evmSelectorFunctionProvider.copyOps();
    }

    /*public Iterable<BigInteger> jumpDestsForID(BigInteger id) {
        return evmSelectorFunctionProvider.jumpDestsForID(id);
    }


    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndOffsetsForDup() {
        return evmSelectorFunctionProvider.idsAndPcsAndOffsetsForDup();
    }


    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndOffsetsForSwap() {
        return evmSelectorFunctionProvider.idsAndPcsAndOffsetsForSwap();
    }

    public Iterable<BigInteger> idInit() {
        return evmSelectorFunctionProvider.idInit();
    }

    public Iterable<Tuple2<BigInteger, BigInteger>> allIdsAndPcs() {
        return evmSelectorFunctionProvider.allIdsAndPcs();
    }

    public Iterable<Tuple2<BigInteger, BigInteger>> idsAndPcsForOpcode(BigInteger opcode) {
        return evmSelectorFunctionProvider.idsAndPcsForOpcode(opcode);
    }

    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndValuesForTargetOpcode(BigInteger opcode) {
        return evmSelectorFunctionProvider.idsAndPcsAndValuesForTargetOpcode(opcode);
    }

    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> idsAndPcsAndJumpDestsForOpcode(BigInteger opcode) {
        return evmSelectorFunctionProvider.idsAndPcsAndJumpDestsForOpcode(opcode);
    }

    public Iterable<Tuple5<BigInteger, BigInteger, BigInteger, BigInteger, BigInteger>> idsAndPcsAndArgumentsForOpcode(BigInteger opcode) {
        return evmSelectorFunctionProvider.idsAndPcsAndArgumentsForOpcode(opcode);
    }*/

    public ContractInfoReader getContractInfoReader() {
        return contractInfoReader;
    }
    public Iterable<BigInteger> ids () { return evmSelectorFunctionProvider.ids(); }
    public Iterable<BigInteger> pcsForIdAndOpcode (BigInteger id, BigInteger opcode) { return evmSelectorFunctionProvider.pcsForIdAndOpcode(id, opcode); }
    public Iterable<BigInteger> pcsForId (BigInteger id) { return evmSelectorFunctionProvider.pcsForId(id); }
    public Iterable<BigInteger> jumpDestsForIdAndPc (BigInteger id, BigInteger pc) { return evmSelectorFunctionProvider.jumpDestsForIdAndPc(id, pc); }
    public Iterable<BigInteger> argumentsZeroForIdAndPc (BigInteger id, BigInteger pc) { return evmSelectorFunctionProvider.argumentsZeroForIdAndPc(id, pc); }
    public Iterable<BigInteger> argumentsOneForIdAndPc (BigInteger id, BigInteger pc) { return evmSelectorFunctionProvider.argumentsOneForIdAndPc(id, pc); }
    public Iterable<Tuple2<BigInteger, BigInteger>> argumentsTwoForIdAndPc (BigInteger id, BigInteger pc) { return evmSelectorFunctionProvider.argumentsTwoForIdAndPc(id, pc);}
    public Iterable<Tuple3<BigInteger, BigInteger, BigInteger>> argumentsThreeForIdAndPc (BigInteger id, BigInteger pc) { return evmSelectorFunctionProvider.argumentsThreeForIdAndPc(id, pc); }
    public Iterable<Tuple2<BigInteger, BigInteger>> resultsForIdAndPc (BigInteger id, BigInteger pc) {
        return evmSelectorFunctionProvider.resultsForIdAndPc(id, pc); }
    public Iterable<Boolean> jumpDestUniqueForIdAndPc (BigInteger id, BigInteger pc) { return evmSelectorFunctionProvider.jumpDestUniqueForIdAndPc(id, pc); }
}
