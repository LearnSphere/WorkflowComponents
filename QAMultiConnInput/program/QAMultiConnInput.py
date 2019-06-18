#C:/Python35/Python QAMultiConnInput.py -programDir . -workingDir . -multiSelect_nodeIndex 0 -multiSelect_fileIndex 0 -multiSelect Sample -multiSelect_nodeIndex 0 -multiSelect_fileIndex 0 -multiSelect "Anon Student Id" -multiSelect_nodeIndex 0 -multiSelect_fileIndex 1 -multiSelect Incorrects -singleSelect_nodeIndex 0 -singleSelect_fileIndex 0 -singleSelect "Anon Student Id" -node 0 -fileIndex 0 C:\WPIDevelopment\dev06_dev\WorkflowComponents\QAMultiConnInput\test\test_data\ds76_student_step_export.txt -node 0 -fileIndex 1 C:\WPIDevelopment\dev06_dev\WorkflowComponents\QAMultiConnInput\test\test_data\ds76_student_step_export.txt
#-multiSelect_nodeIndex 0 -multiSelect_fileIndex 0 -multiSelect Sample 
#-multiSelect_nodeIndex 0 -multiSelect_fileIndex 0 -multiSelect "Anon Student Id" 
#-multiSelect_nodeIndex 0 -multiSelect_fileIndex 1 -multiSelect Incorrects 
#-singleSelect_nodeIndex 0 -singleSelect_fileIndex 0 -singleSelect "Anon Student Id" 
#-node 0 -fileIndex 0 C:\WPIDevelopment\dev06_dev\WorkflowComponents\QAMultiConnInput\test\test_data\ds76_student_step_export.txt
#-node 0 -fileIndex 1 C:\WPIDevelopment\dev06_dev\WorkflowComponents\QAMultiConnInput\test\test_data\ds76_student_step_export.txt

import sys
import argparse

if __name__ == "__main__":
    args = sys.argv[1:]
    files = {}
    outputStr = ""
    for x in range(len(args)):
        if args[x] == "-node":
            nodeIndex = args[x+1]
            fileIndex = int(args[x+3])
            file = args[x+4]
            if nodeIndex not in files.keys():
                files[nodeIndex] = []
            if len(files[nodeIndex]) < (fileIndex+1):
                for y in range(len(files[nodeIndex]), fileIndex+1):
                    files[nodeIndex].append("")
            files[nodeIndex][fileIndex] = file
                            
    for nodeIndex in files.keys():
        outputStr = outputStr + "\nInput node: " + nodeIndex + ";"
        for fileIndex in range(len(files[nodeIndex])):
            outputStr = outputStr + "\n\tInput file: " + str(fileIndex) + "; name: " + files[nodeIndex][fileIndex] + ";"
            multiFirstTime = True
            singleFirstTime = True
            for x in range(len(args)):
                if args[x] == "-multiSelect_nodeIndex" and args[x+1] == nodeIndex and args[x+3] == str(fileIndex):
                    if multiFirstTime:
                        outputStr = outputStr + "\n\t\tMultiple Selection List: " + args[x+5] + ", "
                        multiFirstTime = False
                    else:
                        outputStr = outputStr + args[x+5] + ", "
                elif args[x] == "-singleSelect_nodeIndex" and args[x+1] == nodeIndex and args[x+3] == str(fileIndex):
                    if singleFirstTime:
                        outputStr = outputStr + "\n\t\tSingle Selection List: " + args[x+5] + ", "
                        singleFirstTime = False
                    else:
                        outputStr = outputStr + args[x+5] + ", "
                 
    
        outputStr = outputStr + "\r\n"

    parser = argparse.ArgumentParser(description='Process datashop file.')
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument('-userId', type=str, help='the user executing the component', default='')

    args, option_file_index_args = parser.parse_known_args()

    outputFilePath = args.workingDir + "/multi_conn_qa_result.txt"
    outfile = open(outputFilePath, 'w')
    outfile.write(outputStr)
    outfile.close()
    
    



    

    
