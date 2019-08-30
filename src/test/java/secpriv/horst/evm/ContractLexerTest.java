package secpriv.horst.evm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContractLexerTest {

    @Test
    public void testIsPush() {
        for (ContractLexer.Opcode opcode : ContractLexer.Opcode.values()) {
            assertThat(ContractLexer.Opcode.isPush(opcode)).isEqualTo(opcode.toString().startsWith("PUSH"));
        }
    }

    @Test
    public void testPushValues() {
        for (ContractLexer.Opcode opcode : ContractLexer.Opcode.values()) {
            if(opcode.toString().startsWith("PUSH")) {
                int i = Integer.parseInt(opcode.toString().substring(4));
                assertThat(ContractLexer.Opcode.getNumberOfPushedBytes(opcode)).isEqualTo(i);
            } else {
                assertThatThrownBy(() -> ContractLexer.Opcode.getNumberOfPushedBytes(opcode)).isInstanceOf(IllegalArgumentException.class);
            }
        }
    }
}