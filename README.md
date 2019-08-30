# EtherTrust

## Build

Before building EtherTrust, build Z3.

Get it from https://github.com/Z3Prover/z3.
Build with 

```
python script/mk_make.py --java
cd build; make
```

Place com.microsoft.z3.jar to %project%/lib.

Then build EtherTrust

```
mvn -Dmaven.test.skip=true package
```

## Run

Provide paths to Z3 binary bindings in LD_LIBRARY_PATH (Linux) or DYLD_LIBRARY_PATH (MacOS).

```
LD_LIBRARY_PATH=. java -cp com.microsoft.z3.jar:EtherTrust-1.0-SNAPSHOT.jar secpriv.horst.evm.EvmHorstCompiler $file --json-out-dir $path_to_results -p -b -s $path_to_grammar/evm-abstract-semantics-partial-standard-calls.txt $path_to_grammar/queries-reentrancy.txt
```
