#!/bin/bash
path_to_bench=/Users/ilgris/Git/EtherTrust2.0/data/bench/old/
path_to_spec=/Users/ilgris/Git/EtherTrust2.0/grammar/

declare -a bench_files=("${path_to_bench}1f.txt" "${path_to_bench}1e.txt")
declare -a expected=(UNSATISFIABLE SATISFIABLE)
passed=()
failed=()

for index in ${!bench_files[*]}; do
  f=${bench_files[$index]}
  expected_result=${expected[$index]}
  java -cp com.microsoft.z3.jar:ethertrust-1.0-SNAPSHOT.jar secpriv.horst.HorstCompiler --evm $f -vvvv -p -b -l -s ${path_to_spec}stackmachine.txt
  pass=0
  while read -r line; do
    if echo $line | awk '{print $8}' | grep $expected_result; then
      pass=1
    fi
  done < logs/app.log

  if [ "$pass" -eq "0" ]; then
    failed+=$f
  else
    passed+=$f
  fi
  rm -r logs
done
echo Passed:$passed Failed:$failed
