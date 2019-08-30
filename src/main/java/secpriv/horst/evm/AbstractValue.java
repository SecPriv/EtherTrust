package secpriv.horst.evm;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Optional;

public class AbstractValue {
    public final Optional<EvmTypes.UInt256> value;
    public final static AbstractValue Top = new AbstractValue();

    private AbstractValue() {
        value = Optional.empty(); //top value constructor
    }

    public AbstractValue(EvmTypes.UInt256 val) {
        value = Optional.of(Objects.requireNonNull(val, "Argument may not be null!"));
    }

    public EvmTypes.UInt256 get() {
        return value.get();
    }

    public boolean isAbstract() {
        return !value.isPresent();
    }

    public boolean isConcrete() {
        return value.isPresent();
    }
}