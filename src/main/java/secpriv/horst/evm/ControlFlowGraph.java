package secpriv.horst.evm;

import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.channels.UnsupportedAddressTypeException;
import java.rmi.server.ExportException;
import java.util.*;

import org.antlr.v4.runtime.misc.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.*;
import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.*;
import org.jgrapht.traverse.*;
import org.jgrapht.io.*;


//TODO: We have many iterators to try: AbstractGraphIterator, BreadthFirstIterator, ClosestFirstIterator, CrossComponentIterator, DegeneracyOrderingIterator, DepthFirstIterator, LexBreadthFirstIterator, MaximumCardinalityIterator, RandomWalkIterator, TopologicalOrderIterator

public class ControlFlowGraph {
    private static final Logger LOGGER = LogManager.getLogger(ControlFlowGraph.class);

    private BigInteger contract;
    private LinkedHashMap<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> blockMap;
    private Graph<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>, DefaultEdge> g;
    private final Map<BigInteger, ContractLexer.ContractInfo> contractInfos;

    public ControlFlowGraph(BigInteger contract, LinkedHashMap<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> blockMap, Map<BigInteger, ContractLexer.ContractInfo> contractInfos) throws ExportException, org.jgrapht.io.ExportException {
        this.contract = contract;
        this.blockMap = blockMap;
        this.contractInfos = contractInfos;

        this.g = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (Map.Entry<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> blockInfo : blockMap.entrySet()) {
            g.addVertex(blockInfo.getValue());
        }

        addStandardEdges();
        addJumpEdges();
        //renderCFG();
        //topologicalWalker();
    }

    public Graph<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>, DefaultEdge> getCfg(){
        return g;
    }

    private void addStandardEdges() {
        for (Map.Entry<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> blockInfo : blockMap.entrySet()) {
            boolean goingToNext = true;
            for (Map.Entry<Integer, ContractLexer.OpcodeInstance> instruction : blockInfo.getValue().entrySet()) {
                ContractLexer.OpcodeInstance opcodeInstance = instruction.getValue();
                switch (opcodeInstance.opcode) {
                    case STOP: case JUMP: case RETURN: case REVERT: case INVALID: case SUICIDE:
                        goingToNext = false;
                        break;

                        default:
                            goingToNext = true;
                }
            }
            if (goingToNext){
                LinkedHashMap<Integer, ContractLexer.OpcodeInstance> nextBlock = findNextBlock(blockInfo.getKey());
                if (nextBlock != null) {
                    g.addEdge(blockInfo.getValue(), nextBlock);
                }
            }
        }
    }

    private void addJumpEdges() {
        for (Map.Entry<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> blockInfo : blockMap.entrySet()) {
            ContractLexer.ContractInfo ci = contractInfos.getOrDefault(contract, new ContractLexer.ContractInfo());
            for (Map.Entry<Integer, ContractLexer.OpcodeInstance> instruction : blockInfo.getValue().entrySet()) {
                ContractLexer.OpcodeInstance opcodeInstance = instruction.getValue();
                switch (opcodeInstance.opcode) {
                    case JUMP:
                    case JUMPI:
                        BigInteger pc = BigInteger.valueOf(opcodeInstance.position);
                        List<BigInteger> targets = ci.jumps.getOrDefault(pc, Collections.emptyList());
                        for (BigInteger target : targets) {
                            Integer t = target.intValue();
                            LinkedHashMap<Integer, ContractLexer.OpcodeInstance> targetBlock = findTargetBlock(t);
                            if (targetBlock != null) {
                                g.addEdge(blockInfo.getValue(), targetBlock);
                            }
                            else{
                                LOGGER.error("We cannot find a block with a JUMPDEST, should not happen!");
                                throw new UnsupportedAddressTypeException();
                            }
                        }
                }
            }
        }
    }

    private LinkedHashMap<Integer, ContractLexer.OpcodeInstance> findTargetBlock(Integer target) {
        for (Map.Entry<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> blockInfo : blockMap.entrySet()) {
            Pair blockRange = getBlockRange(blockInfo.getKey());
            int first = (Integer) blockRange.a;
            int last = (Integer) blockRange.b;
            if (target >= first && target <= last){
                return blockInfo.getValue();
            }
        }
        return null;
    }

    private LinkedHashMap<Integer, ContractLexer.OpcodeInstance> findNextBlock(Integer blockNum){
        Pair blockRange = getBlockRange(blockNum);
        //int first = (Integer) blockRange.a;
        int last = (Integer) blockRange.b;
        LinkedHashMap<Integer, ContractLexer.OpcodeInstance> nextBlock = null;
        int minFirst = 2147483647;
        for (Map.Entry<Integer, LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> blockInfo : blockMap.entrySet()) {
            Integer nextFirst = (Integer) getBlockRange(blockInfo.getKey()).a;
            //Integer nextLast = (Integer) getBlockRange(blockInfo.getKey()).b;
            if (nextFirst < last) {

            }
            else{
                if (nextFirst < minFirst){
                    minFirst = nextFirst;
                    nextBlock = blockInfo.getValue();
                }
            }
        }
        return nextBlock;
    }


    private Pair getBlockRange(Integer blockNum){
        Integer first, last;
        LinkedHashMap<Integer, ContractLexer.OpcodeInstance> block = blockMap.get(blockNum);
        try {
            first = block.entrySet().iterator().next().getKey();
        }
        catch (NoSuchElementException e){
            return new Pair(0, 0);
        }
        last = first;
        for (Map.Entry<Integer, ContractLexer.OpcodeInstance> blockInfo : block.entrySet()) {
            last = blockInfo.getKey();
        }
        return new Pair(first, last);
    }


    private void renderCFG() throws ExportException, org.jgrapht.io.ExportException {
        ComponentNameProvider<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> vertexIdProvider =
                new ComponentNameProvider<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>>() {
            public String getName(LinkedHashMap<Integer, ContractLexer.OpcodeInstance> block) {
                return Integer.toString(block.hashCode());
            }
        };
        ComponentNameProvider<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> vertexLabelProvider = new ComponentNameProvider<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>>()
        {
            public String getName(LinkedHashMap<Integer, ContractLexer.OpcodeInstance> block) {
                return block.toString();
            }
        };
        GraphExporter<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>, DefaultEdge> exporter =
                new DOTExporter<>(vertexIdProvider, vertexLabelProvider, null);
        Writer writer = new StringWriter();
        exporter.exportGraph(this.g, writer);
        LOGGER.debug(writer.toString());
    }
    public boolean hasCycle(){
        CycleDetector<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>, DefaultEdge> cycleDetector = new CycleDetector<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>, DefaultEdge>(g);
        return cycleDetector.detectCycles();
    }

    private boolean cyclesDetector(){
        // Checking for cycles in the dependencies
        CycleDetector<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>, DefaultEdge> cycleDetector = new CycleDetector<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>, DefaultEdge>(g);
        // Cycle(s) detected.
        if (cycleDetector.detectCycles()) {
            Iterator<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> iterator;
            Set<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> cycleVertices;
            Set<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>> subCycle;
            LinkedHashMap<Integer, ContractLexer.OpcodeInstance> cycle;

            LOGGER.info("Cycles detected.");

            // Get all vertices involved in cycles.
            cycleVertices = cycleDetector.findCycles();

            // Loop through vertices trying to find disjoint cycles.
            while (! cycleVertices.isEmpty()) {
                LOGGER.debug("Cycle:");

                // Get a vertex involved in a cycle.
                iterator = cycleVertices.iterator();
                cycle = iterator.next();

                // Get all vertices involved with this vertex.
                subCycle = cycleDetector.findCyclesContainingVertex(cycle);
                for (LinkedHashMap<Integer, ContractLexer.OpcodeInstance> sub : subCycle) {
                    LOGGER.debug("   " + sub);
                    // Remove vertex so that this cycle is not encountered again
                    cycleVertices.remove(sub);
                }
            }
            return true;
        }
        else{
            return false;
        }
    }

    void topologicalWalker(){
        // only when there are no cycles!
        if (!cyclesDetector()) {
            LinkedHashMap<Integer, ContractLexer.OpcodeInstance> v;
            TopologicalOrderIterator<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>, DefaultEdge> orderIterator;

            orderIterator =
                    new TopologicalOrderIterator<LinkedHashMap<Integer, ContractLexer.OpcodeInstance>, DefaultEdge>(g);
            LOGGER.debug("\nTopological Ordering:");
            while (orderIterator.hasNext()) {
                v = orderIterator.next();
                LOGGER.debug(v);
            }
        }
        else{
            LOGGER.debug("We are not topologically walking! Cycles!");
        }
    }
}
