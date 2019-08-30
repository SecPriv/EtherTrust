package secpriv.horst.evm;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public class EvmStatistics {
    LinkedHashSet<File> filesToProcess = new LinkedHashSet<>();
    LinkedHashSet<FileStatistics> fileStatistics = new LinkedHashSet<>();
    List<String> contractsWithCallcode = new ArrayList<>();
    List<String> contractsWithCreate = new ArrayList<>();
    List<String> contractsWithCreate2 = new ArrayList<>();
    List<String> contractsWithCall = new ArrayList<>();
    List<String> contractsWithStaticcall = new ArrayList<>();
    List<String> contractsWithDelegatecall = new ArrayList<>();
    List<String> contractsWithInvalid = new ArrayList<>();

    private static void getFilesInDir(File dir, Set<File> smartFiles, String ending) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    if (file.getName().endsWith(ending)) {
                        smartFiles.add(file);
                    }
                } else if (file.isDirectory())
                    getFilesInDir(file.getAbsoluteFile(), smartFiles, ending);
            }
        }
    }
    private class FileStatistics {
        public File file;
        public Map<ContractLexer.Opcode, Integer> numberOfOpcodesTotal = new LinkedHashMap<>();
        public Map<ContractLexer.Opcode, Integer> numberOfOpcodesWithResult = new LinkedHashMap<>();
        public Map<ContractLexer.Opcode, Integer> numberOfOpcodesWith1and2 = new LinkedHashMap<>();
        public Map<ContractLexer.Opcode, Integer> numberOfOpcodesWith1 = new LinkedHashMap<>();
        public Map<ContractLexer.Opcode, Integer> numberOfOpcodesWith2 = new LinkedHashMap<>();
        public Map<ContractLexer.Opcode, Integer> numberOfOpcodesWithNone = new LinkedHashMap<>();
    }
    public EvmStatistics(String folder){
        getFilesInDir(new File(folder), filesToProcess, ".txt");
        if (filesToProcess.isEmpty()){
            getFilesInDir(new File(folder), filesToProcess, ".json");
        }

    }
    private void printMap(Map<ContractLexer.Opcode, Integer> map){
        Map<ContractLexer.Opcode, Integer>  sortedT =
                map.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        sortedT.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(System.out::println);
    }
    private void pp(Map<ContractLexer.Opcode, Integer> numberTotal, Map<ContractLexer.Opcode, Integer> numberWithResult,
                    Map<ContractLexer.Opcode, Integer> numberWith1and2, Map<ContractLexer.Opcode, Integer> numberWith1,
                    Map<ContractLexer.Opcode, Integer> numberWith2, Map<ContractLexer.Opcode, Integer> numberWithNone){
        System.out.println("Total");
        System.out.println("-----");
        printMap(numberTotal);
        System.out.println("-----");
        System.out.println("With result");
        printMap(numberWithResult);
        System.out.println("-----");
        System.out.println("With first and second arg");
        printMap(numberWith1and2);
        System.out.println("-----");
        System.out.println("With first arg");
        printMap(numberWith1);
        System.out.println("-----");
        System.out.println("With second arg");
        printMap(numberWith2);
        System.out.println("-----");
        System.out.println("With none");
        printMap(numberWithNone);
    }
    public void display(){
        // how many opcodes in contracts
        System.out.println("-------------BYTECODES---------------");
        Map<ContractLexer.Opcode, Integer> numberTotal = new LinkedHashMap<>();
        Map<ContractLexer.Opcode, Integer> numberWithResult = new LinkedHashMap<>();
        Map<ContractLexer.Opcode, Integer> numberWith1and2 = new LinkedHashMap<>();
        Map<ContractLexer.Opcode, Integer> numberWith1 = new LinkedHashMap<>();
        Map<ContractLexer.Opcode, Integer> numberWith2 = new LinkedHashMap<>();
        Map<ContractLexer.Opcode, Integer> numberWithNone = new LinkedHashMap<>();
        for (FileStatistics fs: fileStatistics) {
            fs.numberOfOpcodesTotal.forEach((k, v) -> numberTotal.merge(k, v, (v1, v2) -> v1 + v2));
            fs.numberOfOpcodesWithResult.forEach((k, v) -> numberWithResult.merge(k, v, (v1, v2) -> v1 + v2));
            fs.numberOfOpcodesWith1and2.forEach((k, v) -> numberWith1and2.merge(k, v, (v1, v2) -> v1 + v2));
            fs.numberOfOpcodesWith1.forEach((k, v) -> numberWith1.merge(k, v, (v1, v2) -> v1 + v2));
            fs.numberOfOpcodesWith2.forEach((k, v) -> numberWith2.merge(k, v, (v1, v2) -> v1 + v2));
            fs.numberOfOpcodesWithNone.forEach((k, v) -> numberWithNone.merge(k, v, (v1, v2) -> v1 + v2));
        }
        pp(numberTotal, numberWithResult, numberWith1and2, numberWith1, numberWith2, numberWithNone);
        // how many contracts have opcodes
        numberTotal.clear();
        numberWithResult.clear();
        numberWith1.clear();
        numberWith2.clear();
        numberWith1and2.clear();
        numberWithNone.clear();
        for (ContractLexer.Opcode op : ContractLexer.Opcode.values()) {

            if (ContractLexer.Opcode.isPush(op)){ op = ContractLexer.Opcode.PUSH1; }
            if (ContractLexer.Opcode.isDup(op)){ op = ContractLexer.Opcode.DUP1; }
            if (ContractLexer.Opcode.isSwap(op)){ op = ContractLexer.Opcode.SWAP1; }
            if (ContractLexer.Opcode.isLog(op)){ op = ContractLexer.Opcode.LOG0; }

            int tot = 0; int res = 0; int w1= 0; int w2 = 0; int w1_2 = 0; int none =0;
            for (FileStatistics fs: fileStatistics) {
                if (fs.numberOfOpcodesTotal.containsKey(op)){ ++ tot; }
                if (fs.numberOfOpcodesWithResult.containsKey(op)){ ++ res; }
                if (fs.numberOfOpcodesWith1and2.containsKey(op)){ ++ w1_2; }
                if (fs.numberOfOpcodesWith1.containsKey(op)){ ++ w1; }
                if (fs.numberOfOpcodesWith2.containsKey(op)){ ++ w2; }
                if (fs.numberOfOpcodesWithNone.containsKey(op)){ ++ none; }
            }
            numberTotal.put(op, tot);
            numberWithResult.put(op, res);
            numberWith1.put(op, w1);
            numberWith2.put(op, w2);
            numberWith1and2.put(op, w1_2);
            numberWithNone.put(op, none);
        }
        System.out.println("-------------CONTRACTS---------------");
        pp(numberTotal, numberWithResult, numberWith1and2, numberWith1, numberWith2, numberWithNone);
        System.out.println("Contract names with opcodes relevant to reentrancy");
        System.out.println("Contracts with CALL " + contractsWithCall.size());
        contractsWithCall.forEach(c -> System.out.println(c));
        System.out.println("Contracts with STATICCALL " + contractsWithStaticcall.size());
        contractsWithStaticcall.forEach(c -> System.out.println(c));
        System.out.println("Contracts with CREATE " + contractsWithCreate.size());
        contractsWithCreate.forEach(c -> System.out.println(c));
        System.out.println("Contracts with CREATE2 " + contractsWithCreate2.size());
        contractsWithCreate2.forEach(c -> System.out.println(c));
        System.out.println("Contracts with DELEGATECALL " + contractsWithDelegatecall.size());
        contractsWithDelegatecall.forEach(c -> System.out.println(c));
        System.out.println("Contracts with CALLCODE " + contractsWithCallcode.size());
        contractsWithCallcode.forEach(c -> System.out.println(c));
        System.out.println("Contracts with DELEGATECALL but also CREATE or CALL or STATICCALL");
        for (String c: contractsWithDelegatecall){
            if (contractsWithCall.contains(c) || contractsWithCreate.contains(c) || contractsWithCreate2.contains(c) || contractsWithStaticcall.contains(c)){
                System.out.println(c);
            }
        }
        System.out.println("Contracts with CASLLCODE but also CREATE or CALL or STATICCALL");
        for (String c: contractsWithCallcode){
            if (contractsWithCall.contains(c) || contractsWithCreate.contains(c) || contractsWithCreate2.contains(c) || contractsWithStaticcall.contains(c)){
                System.out.println(c);
            }
        }
        System.out.println("Contracts with INVALID " + contractsWithInvalid.size());
        contractsWithInvalid.forEach(c -> System.out.println(c));
    }
    public void compute(){
        for (File f : filesToProcess) {
            FileStatistics fs = new FileStatistics();
            fileStatistics.add(fs);
            fs.file = f;
            String filename = FilenameUtils.getName(f.getAbsolutePath());
            ArrayList<String> arr = new ArrayList<>();
            arr.add(f.getAbsolutePath());
            boolean hasCreate = false;
            boolean hasCreate2 = false;
            boolean hasCall = false;
            boolean hasStaticcall = false;
            boolean hasDelegateCall = false;
            boolean hasCallcode = false;
            boolean hasInvalid = false;
            try {
                ContractInfoReader cir = new ContractInfoReader(arr, true);
                ConstantAnalysis ca = new ConstantAnalysis(cir.getContractInfos());
                ca.getBlocksFromBytecode();
                ca.runBlocks();
                HashMap<BigInteger, LinkedHashMap<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>>> bc = ca.getBlocksOfContracts();
                for (Map.Entry<BigInteger, LinkedHashMap<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>>> entry : bc.entrySet()) {
                    for (Map.Entry<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> blockInfo : entry.getValue().entrySet()) {
                        LinkedHashMap<Integer, ContractLexer.OpcodeInstance> blockInstructions = (LinkedHashMap<Integer, ContractLexer.OpcodeInstance>) blockInfo.getValue();
                        for (Map.Entry<Integer, ContractLexer.OpcodeInstance> block : blockInstructions.entrySet()) {
                                ContractLexer.OpcodeInstance oi = block.getValue();
                                ContractLexer.Opcode op = oi.opcode;
                                if (op == ContractLexer.Opcode.CREATE && !hasCreate){
                                    hasCreate = true;
                                    contractsWithCreate.add(filename);
                                }
                            if (op == ContractLexer.Opcode.CREATE2 && !hasCreate2){
                                hasCreate2 = true;
                                contractsWithCreate2.add(filename);
                            }
                            if (op == ContractLexer.Opcode.CALL && !hasCall){
                                hasCall = true;
                                contractsWithCall.add(filename);
                            }
                            if (op == ContractLexer.Opcode.STATICCALL && !hasStaticcall){
                                hasStaticcall = true;
                                contractsWithStaticcall.add(filename);
                            }
                            if (op == ContractLexer.Opcode.DELEGATECALL && !hasDelegateCall){
                                hasDelegateCall = true;
                                contractsWithDelegatecall.add(filename);
                            }
                            if (op == ContractLexer.Opcode.CALLCODE && !hasCallcode){
                                hasCallcode = true;
                                contractsWithCallcode.add(filename);
                            }
                            if (op == ContractLexer.Opcode.INVALID && !hasInvalid){
                                hasInvalid = true;
                                contractsWithInvalid.add(filename);
                            }
                                if (ContractLexer.Opcode.isPush(oi.opcode)){
                                    op = ContractLexer.Opcode.PUSH1;
                                }
                                if (ContractLexer.Opcode.isDup(oi.opcode)){
                                    op = ContractLexer.Opcode.DUP1;
                                }
                                if (ContractLexer.Opcode.isSwap(oi.opcode)){
                                    op = ContractLexer.Opcode.SWAP1;
                                }
                                if (ContractLexer.Opcode.isLog(oi.opcode)){
                                    op = ContractLexer.Opcode.LOG0;
                                }
                                int num = fs.numberOfOpcodesTotal.getOrDefault(op, 0);
                                ++num;
                                fs.numberOfOpcodesTotal.put(op, num);
                                if (oi.rez.isPresent()){
                                    num = fs.numberOfOpcodesWithResult.getOrDefault(op, 0);
                                    ++num;
                                    fs.numberOfOpcodesWithResult.put(op, num);
                                }
                                /* if (oi.arg1.isPresent() && oi.arg2.isPresent() && !oi.rez.isPresent()){
                                    num = fs.numberOfOpcodesWith1and2.getOrDefault(op, 0);
                                    ++num;
                                    fs.numberOfOpcodesWith1and2.put(op, num);
                                }
                                if (oi.arg1.isPresent() && !oi.arg2.isPresent() && !oi.rez.isPresent()){
                                    num = fs.numberOfOpcodesWith1.getOrDefault(op, 0);
                                    ++num;
                                    fs.numberOfOpcodesWith1.put(op, num);
                                }
                                if (!oi.arg1.isPresent() && oi.arg2.isPresent() && !oi.rez.isPresent()){
                                    num = fs.numberOfOpcodesWith2.getOrDefault(op, 0);
                                    ++num;
                                    fs.numberOfOpcodesWith2.put(op, num);
                                }
                                if (!oi.arg1.isPresent() && !oi.arg2.isPresent() && !oi.rez.isPresent()){
                                    num = fs.numberOfOpcodesWithNone.getOrDefault(op, 0);
                                    ++num;
                                    fs.numberOfOpcodesWithNone.put(op, num);
                                } */
                            }
                        }
                    }
                }
        catch (java.rmi.server.ExportException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
