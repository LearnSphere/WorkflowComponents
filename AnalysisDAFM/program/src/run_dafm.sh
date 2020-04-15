#!/bin/bash


# python3 src/main.py --dataset Example --dataset_path datasets/Example/example.txt --skill_name skill_name --dafm fine-tuned No --save_model True

# python3 src/main.py --dataset Example --dataset_path datasets/Example/example.txt --skill_name skill_name --dafm fine-tuned No --load_model True sub --puser orig

# Not provided by tigris::
### -dataset Example -dafm fine-tuned No -save_model True

# TensorFlow CPP log levels:
# 0 = all messages are logged (default behavior)
# 1 = INFO messages are not printed
# 2 = INFO and WARNING messages are not printed
# 3 = INFO, WARNING, and ERROR messages are not printed
export TF_CPP_MIN_LOG_LEVEL=2

programDir=""
workingDir=""
dafmType=""

POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"
case $key in
    -workingDir|--workingDir)
    workingDir="$2"
    shift #
    shift #
    ;;
    -programDir|--programDir)
    programDir="$2"
    shift #
    shift #
    ;;
    -node|--node)
    nodeIndex="$2"
    echo "1a $nodeIndex" >> stdout.wfl
    if [ $nodeIndex == "1" ]; then
      echo "1b $3" >> stdout.wfl
      if [ "$3" == "-fileIndex" ]; then
      echo `ls -al $5` >> stdout.wfl
        if [ -f $5 ]; then
        modelParams="-load_model True sub --puser orig"
          unzip $5
          echo `ls -la $workingDir` >> stdout.wfl
          if [ "$?" != "0" ]; then
            echo "Could not unzip model file." 1>&2
            return 1
          fi
        fi
      fi
    fi
    POSITIONAL+=("$1") # save it in an array for later
    shift #
    POSITIONAL+=("$1") # save it in an array for later
    shift #
    ;;
    --default)
    DEFAULT=DEFAULT
    shift #
    ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift #
    ;;
esac
done

set -- "${POSITIONAL[@]}" # restore params
if [ "$modelParams" == "" ]; then
  modelParams="-save_model True"
fi

echo Args: -W ignore $programDir/program/src/main.py -programDir "$programDir" -workingDir "$workingDir" -dataset \
  dataset -dafm fine-tuned No $modelParams $@ >> stdout.wfl

python3 -W ignore $programDir/program/src/main.py -programDir "$programDir" -workingDir "$workingDir" -dataset \
  dataset -dafm fine-tuned No $modelParams $@ >> stdout.wfl
cd $workingDir
zip -r dafm-model.zip datasets Accuracy
rm -R $workingDir/datasets $workingDir/Accuracy
