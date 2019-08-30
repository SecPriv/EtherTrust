package secpriv.horst.evm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.util.Arrays;

public class EvmTypes {
    private static final Logger LOGGER = LogManager.getLogger(EvmTypes.class);

    static BigInteger max = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639936");
    static public UInt256 UZERO = new UInt256(BigInteger.ZERO);
    static public UInt256 UONE = new UInt256(BigInteger.ONE);

    public EvmTypes() {
    }

    public static BigInteger rotateLeft256(BigInteger value, int shift) {
        BigInteger topBits = value.shiftRight(256 - shift);
        BigInteger mask = BigInteger.ONE.shiftLeft(256).subtract(BigInteger.ONE);
        return value.shiftLeft(shift).or(topBits).and(mask);
    }
    public static BigInteger rotateRight256(BigInteger value, int shift) {
        BigInteger topBits = value.shiftLeft(256 - shift);
        BigInteger mask = BigInteger.ONE.shiftRight(256).subtract(BigInteger.ONE);
        return value.shiftRight(shift).or(topBits).and(mask);
    }

    public static class SInt256 {
        private final BigInteger value;

        public SInt256(UInt256 value){
            byte[] byteRepresentation = value.getValue().toByteArray();
            if (byteRepresentation.length > 32) {
                byteRepresentation = Arrays.copyOfRange(byteRepresentation, byteRepresentation.length - 32, byteRepresentation.length);
                this.value = new BigInteger(byteRepresentation);
            } else {
                this.value = value.getValue();
            }
        }

        public UInt256 div(SInt256 v){
            return new UInt256(value.divide(v.value).mod(max));
        }
        public UInt256 mod(SInt256 v){
            return new UInt256(value.mod(v.value).mod(max));
        }
        public UInt256 neg(){
            return new UInt256(value.not().mod(max));
        }
        private UInt256 abs(){
            if (this.value.compareTo(BigInteger.ZERO) == -1){
                return new UInt256(BigInteger.ZERO.subtract(this.value));
            }
            return new UInt256(this.value);
        }
        public UInt256 slt(SInt256 v){
            return new UInt256(value.compareTo(v.value) == -1 ? BigInteger.ONE : BigInteger.ZERO );
        }
        public UInt256 sgt(SInt256 v){
            return new UInt256(value.compareTo(v.value) == 1 ? BigInteger.ONE : BigInteger.ZERO );
        }
        public UInt256 shr(int v){
            if (v >= 256){
                return new UInt256(BigInteger.ZERO);
            }
            else{
                return new UInt256(value.shiftRight(v).mod(max));
            }
        }
    }

    public static class UInt256 {
        private BigInteger value;

        // 32 bytes plus one for the sign (always zero to ensure that it is treated unsigned)
        private byte byteSize = 33;

        void print() {
            byte[] a = this.getValue().toByteArray();

            for (int i = 0; i < a.length; ++i) {
                String s = String.format("%8s", Integer.toBinaryString(a[i] & 0xFF)).replace(' ', '0');
                System.out.print(s);
                System.out.print(" ");
            }
            System.out.println();
        }

        public UInt256(BigInteger value) {
            this.value = enforceUnsigned(value);
        }

        public BigInteger getValue(){
            return value;
        }

        private void check(BigInteger value){
            if (value.compareTo(EvmTypes.max) == 1) {
                throw new ArithmeticException("UInt256 constructor: limit exceeded!");
            }
        }

        private BigInteger enforceUnsigned(BigInteger value){
            byte[] originalByteRepresentation = value.toByteArray();
            byte[] unsignedByteRepresentation = new byte[33];

            if (value.compareTo(BigInteger.ZERO) < 0) {
                Arrays.fill(unsignedByteRepresentation, (byte) 0xFF);
                unsignedByteRepresentation[0] = 0x00;
            }
            for (int i = 0; i < Math.min(originalByteRepresentation.length, 32); i++) {
                int uj = unsignedByteRepresentation.length - i - 1;
                int oj = originalByteRepresentation.length - i - 1;
                unsignedByteRepresentation[uj] = originalByteRepresentation[oj];
            }
            return new BigInteger(unsignedByteRepresentation);
        }

        public UInt256 add(UInt256 v){
            return new UInt256(value.add(v.value).mod(max));
        }
        public UInt256 sub(UInt256 v){
            return new UInt256(value.subtract(v.value).mod(max));
        }
        public UInt256 mul(UInt256 v){
            return new UInt256(value.multiply(v.value).mod(max));
        }
        public UInt256 div(UInt256 v){
            if (BigInteger.ZERO.equals(v.value)){
                return new UInt256(BigInteger.ZERO);
            }
            else {
                return new UInt256(value.divide(v.value).mod(max));
            }
        }
        public UInt256 sdiv(UInt256 v){
            if (BigInteger.ZERO.equals(v.value)){
                return new UInt256(BigInteger.ZERO);
            }
            else {
                return new SInt256(this).div(new SInt256(v));
            }
        }
        public UInt256 mod(UInt256 v){
            if (BigInteger.ZERO.equals(v.value)){
                return new UInt256(BigInteger.ZERO);
            }
            else {
                return new UInt256(value.mod(v.value).mod(max));
            }
        }
        public UInt256 smod(UInt256 v){
            if (BigInteger.ZERO.equals(v.value)){
                return new UInt256(BigInteger.ZERO);
            }
            else {
                SInt256 tt = new SInt256(this);
                SInt256 vv = new SInt256(v);
                if (tt.value.compareTo(BigInteger.ZERO) == -1){
                    UInt256 zz = new UInt256(BigInteger.ZERO);
                    return zz.sub(tt.abs().mod(vv.abs()));
                }
                else {
                    return this.mod(vv.abs());
                }
            }
        }
        public UInt256 addmod(UInt256 v2, UInt256 v3){
            if (BigInteger.ZERO.equals(v3.value)){
                return new UInt256(BigInteger.ZERO);
            }
            else {
                return new UInt256(value.add(v2.value).mod(v3.value).mod(max));
            }
        }
        public UInt256 mulmod(UInt256 v2, UInt256 v3){
            if (BigInteger.ZERO.equals(v3.value)){
                return new UInt256(BigInteger.ZERO);
            }
            else {
                return new UInt256(value.multiply(v2.value).mod(v3.value).mod(max));
            }
        }
        public UInt256 and(UInt256 v){
            return new UInt256(value.and(v.value).mod(max));
        }
        public UInt256 or(UInt256 v){
            return new UInt256(value.or(v.value).mod(max));
        }
        // not should be applied only on the values, not the signed, hence the conversion to singed
        public UInt256 neg(){
            return new SInt256(this).neg();
        }
        public UInt256 xor(UInt256 v){
            return new UInt256(value.xor(v.value).mod(max));
        }
        public UInt256 lt(UInt256 v){
            return new UInt256(value.compareTo(v.value) == - 1 ? BigInteger.ONE : BigInteger.ZERO );
        }
        public UInt256 slt(UInt256 v){
            return new SInt256(this).slt(new SInt256(v));
        }
        public UInt256 sgt(UInt256 v){
            return new SInt256(this).sgt(new SInt256(v));
        }
        public UInt256 gt(UInt256 v){
            return new UInt256(value.compareTo(v.value) == 1 ? BigInteger.ONE : BigInteger.ZERO );
        }
        public UInt256 eq(UInt256 v){
            return new UInt256(value.compareTo(v.value) == 0 ? BigInteger.ONE : BigInteger.ZERO );
        }
        public boolean isZeroBool(){
            return value.compareTo(BigInteger.ZERO) == 0 ? true : false;
        }
        public boolean isOneBool(){
            return value.compareTo(BigInteger.ONE) == 0 ? true : false;
        }
        public UInt256 isZero(){
            return new UInt256(value.compareTo(BigInteger.ZERO) == 0 ? BigInteger.ONE : BigInteger.ZERO );
        }
        public UInt256 pow(UInt256 v){
            return new UInt256(value.modPow(v.getValue(), max));
        }
        public UInt256 byteExtract(int v){
            byte[] byteRepresentation = value.toByteArray();
            v = 31 - v; // "mirror" v, because we are addressing from the other side
            final int realIndex = byteRepresentation.length - 1 - v;
            byte b = realIndex >= 0 && realIndex < byteRepresentation.length ? byteRepresentation[realIndex] : 0x00b;

            return new UInt256(BigInteger.valueOf(0xFF & b));
        }
        public UInt256 signExtend(int v){
            if (v >= 32) {
                throw new ArithmeticException("UInt256 signExtend: limit exceeded!");
            }
            else{
                byte[] byteRepresentation = this.getValue().toByteArray();
                if (byteRepresentation.length <= v) {
                    return this;
                }
                else{
                    final int startIndex = byteRepresentation.length - 1 - v;

                    byteRepresentation = Arrays.copyOfRange(byteRepresentation, startIndex, byteRepresentation.length);
                    BigInteger rez;
                    try {
                        rez = new BigInteger(byteRepresentation);
                    }
                    catch (java.lang.NumberFormatException e){
                        rez = this.value;
                    }
                    return new UInt256(rez);
                }
            }
        }
        public UInt256 shl(int v){
            if (v >= 256){
                return new UInt256(BigInteger.ZERO);
            }
            else{
                return new UInt256(value.shiftLeft(v).mod(max));
            }
        }
        public UInt256 shr(int v){
            if (v >= 256){
                return new UInt256(BigInteger.ZERO);
            }
            else{
                return new UInt256(value.shiftRight(v).mod(max));
            }
        }
        public UInt256 sar(int v){
            if (v >= 256){
                return new UInt256(BigInteger.ZERO);
            }
            else{
                return new SInt256(this).shr(v);
            }
        }
        public UInt256 rol(int v){
            if (v >= 256){
                return new UInt256(BigInteger.ZERO);
            }
            else{
                return new UInt256(rotateLeft256(value, v).mod(max));
            }
        }
        public UInt256 ror(int v){
            if (v >= 256){
                return new UInt256(BigInteger.ZERO);
            }
            else{
                return new UInt256(rotateRight256(value, v).mod(max));
            }
        }
    }
}
