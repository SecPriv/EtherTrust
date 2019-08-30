package secpriv.horst.evm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ContractInfoReader {
    private static final String apiKey = "RWM6D4DV2C2PWY3CFWV3F81A38X1WHPYBY";
    private static final Logger LOGGER = LogManager.getLogger(ContractInfoReader.class);
    protected Map<BigInteger, ContractLexer.ContractInfo> contractInfos = new LinkedHashMap<>();

    private static String ignore0x(String bytecode) {
        if (bytecode.startsWith("0x")) {
            return bytecode.substring(2);
        } else
            return bytecode;
    }

    public static JsonObject getData(String targetURL) throws IOException {
        StringBuilder json = new StringBuilder();
        URL url = new URL(targetURL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            json.append(line);
        }
        rd.close();
        JsonObject obj = new JsonParser().parse(json.toString()).getAsJsonObject();
        return obj;
    }

    public static String getBytecodeFromEtherscan(String address) throws IOException {
        String targetURL = "https://api.etherscan.io/api?module=proxy&action=eth_getCode&address="
                + address + "&tag=latest&apikey=" + apiKey;
        JsonObject obj = getData(targetURL);
        return obj.get("result").toString().substring(1, obj.get("result").toString().length() - 1);
    }

    public ContractInfoReader() {
        contractInfos = new HashMap<>();
    }

    public ContractInfoReader(EvmSourceProvider sourceProvider, boolean stat) {
        BigInteger id = sourceProvider.getId();
        ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo(sourceProvider.getSource(), stat);
        contractInfos.put(id, ci);
    }

    public ContractInfoReader(List<String> args, boolean stat) throws IOException {
        for (String arg : args) {
            /*Graph<BigInteger, DefaultEdge> g = new DefaultDirectedGraph<>(DefaultEdge.class);
            ArrayList<BlockData> bdList = new ArrayList<>();
            boolean hasCycles = true;
            //TODO: super ugly cfg read is happening right here
            for (String cfgPath: cfgs) {
                List<String> lines = Files.readAllLines(Paths.get(cfgPath), StandardCharsets.US_ASCII);
                // extracting jump targets
                Set<BigInteger> dests = new HashSet<>();
                for (int i = 0; i < lines.size(); i ++) {
                    String line = lines.get(i);
                    if (line.contains("JUMPDEST") && !line.contains(":")) {
                        dests.add(new BigInteger(line.split(" ")[0].replace("0x", ""), 16));
                    }
                }
                BlockData bd = null;
                BigInteger jumpPc = null;
                for (int i = 0; i < lines.size(); i ++) {
                    String line = lines.get(i);
                    LOGGER.debug(line);
                    if (line.contains("Has unresolved jump.")) {
                        LOGGER.error("Unresolved jumps");
                        bd.hasUnresolvedJumps = true;
                        throw new UnsupportedOperationException();
                    }
                    if (line.startsWith("Block")) {
                        BigInteger blockNumber = new BigInteger(line.split(" ")[1].replace("0x", ""), 16);
                        String nextLine = lines.get(i + 1);
                        String[] beginEnd = nextLine.replace("[", "").replace("]", "").split(":");
                        bd = new BlockData(blockNumber, new BigInteger(beginEnd[0].replace("0x", ""), 16), new BigInteger(beginEnd[1].replace("0x", ""), 16));
                    }
                    if (line.startsWith("Exit") && line.split(" ")[1].equals("stack:")) {
                        bdList.add(bd);
                    }
                    if (line.contains("JUMP") && !line.contains("DEST")) {
                        jumpPc = new BigInteger(line.split(" ")[0].replace("0x", "").replace(":", ""), 16);
                        bd.jPc = jumpPc;
                    }
                    if (line.startsWith("Successors")) {
                        String[] targets = line.replace("Successors: ", "").replace("[", "").replace("]", "").replace(",", "").split(" ");
                        for (String target : targets) {
                            if (target.length() == 0) {
                                continue;
                            }
                            BigInteger successor = new BigInteger(target.replace("0x", ""), 16);
                            if (dests.contains(successor)) {
                                // we add to targets only successors which are valid jumps
                                bd.targets.add(successor);
                            }
                            bd.successors.add(successor);
                        }
                    }
                    if (line.startsWith("0x") && ! line.contains(":")) {
                        bd.opcodes.add(new BigInteger(line.split(" ")[0].split("0x")[1], 16));
                    }
                }
                for (BlockData bdd : bdList) {
                    LOGGER.debug("--------------------");
                    LOGGER.debug(bdd.id + ":" + bdd.begin + "-" + bdd.end);
                    LOGGER.debug(bdd.jPc);
                    LOGGER.debug("Successors:");
                    for (BigInteger successor : bdd.successors) {
                        LOGGER.debug(successor);
                    }
                    LOGGER.debug("Opcodes:");
                    for (BigInteger opcode : bdd.opcodes) {
                        LOGGER.debug(opcode);
                    }
                }
                for (BlockData bdd : bdList) {
                    g.addVertex(bdd.id);
                }
                for (BlockData bdd : bdList) {
                    for (BigInteger successor : bdd.successors) {
                        g.addEdge(bdd.id, successor);
                    }
                }
                CycleDetector<BigInteger, DefaultEdge> cycleDetector = new CycleDetector<BigInteger, DefaultEdge>(g);
                hasCycles = cycleDetector.detectCycles();
                if (hasCycles) {
                    LOGGER.info("Cycles exist!");
                } else {
                    LOGGER.info("No cycles!");
                }
            }
            //TODO: super ugly stuff ends here
             */
            String source;
            BigInteger id;
            Map<BigInteger, List<BigInteger>> jumps = new LinkedHashMap<>();
            if (FilenameUtils.getExtension(arg).equals("json")) {
                JsonObject jsonObject = new JsonParser().parse(new String(Files.readAllBytes(Paths.get(arg)))).getAsJsonObject();
                source = ignore0x(jsonObject.get("bytecode").getAsString());
                id = new BigInteger(ignore0x(jsonObject.get("address").getAsString()), 16);
                JsonElement element = jsonObject.get("jumpDestinations");
                if (element != null) {
                    JsonObject jumpDest = jsonObject.get("jumpDestinations").getAsJsonObject();
                    jumpDest.entrySet().forEach(el ->
                            jumps.put(new BigInteger(el.getKey()), StreamSupport.stream(el.getValue().getAsJsonArray().spliterator(), false).map(JsonElement::getAsBigInteger
                            ).collect(Collectors.toList())));
                }
            } else if ((arg.charAt(0) == '0') && (arg.charAt(1) == 'x') && !arg.endsWith("txt")) {
                source = ignore0x(getBytecodeFromEtherscan(arg));
                id = new BigInteger(ignore0x(arg), 16);
            } else {
                source = ignore0x(new String(Files.readAllBytes(Paths.get(arg))));
                id = BigInteger.valueOf(arg.hashCode()).abs();
            }
            LOGGER.info("Lexing..." + arg);
            ContractLexer.ContractInfo ci = ContractLexer.generateContractInfo(source, stat);
            // in jump recovery we do not write jumps that have no valid jump destinations, this code accounts for it
            if (jumps.size() > 0) {
                for (BigInteger jump: ci.jumps.keySet()){
                    if (!jumps.keySet().contains(jump)){
                        jumps.put(jump, Collections.emptyList());
                    }
                }
                ci.jumps = jumps;
            }
            LOGGER.info("Contract size is " + ci.getSize().toString() + " opcodes");
            contractInfos.put(id, ci);
        }
    }

    public Map<BigInteger, ContractLexer.ContractInfo> getContractInfos() {
        return contractInfos;
    }
}
