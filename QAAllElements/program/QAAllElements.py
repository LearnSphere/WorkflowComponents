#C:/Python35/Python QAAllElements.py -programDir . -workingDir . -userId hcheng -dbConnection true -longSelect LongOption1 -selectionType Long -shortSelect ShortOption1 -singleFileSelect_nodeIndex 0 -singleFileSelect_fileIndex 0 -singleFileSelect Row -stringType Default -node 0 -fileIndex 0 C:\datashop\files\workflows\5\Data-1-x725275\output\ds76_student_step_export.txt -node 1 -fileIndex 0 C:\datashop\files\workflows\5\Data-1-x124291\output\ds76_student_step_export.txt
#-singleFileSelect_nodeIndex 0 -singleFileSelect_fileIndex 0 -singleFileSelect Row 
#-stringType Default 
#-node 0 -fileIndex 0 C:\datashop\files\workflows\5\Data-1-x725275\output\ds76_student_step_export.txt 
#-node 1 -fileIndex 0 C:\datashop\files\workflows\5\Data-1-x124291\output\ds76_student_step_export.txt

import sys
import argparse

if __name__ == "__main__":
    args = sys.argv[1:]
    files = {}
    outputStr = ""
    outputStrForInputFile = ""
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
        outputStrForInputFile = outputStrForInputFile + "\nInput node: " + nodeIndex + ";"
        for fileIndex in range(len(files[nodeIndex])):
            outputStrForInputFile = outputStrForInputFile + "\n\tInput file: " + str(fileIndex) + "; name: " + files[nodeIndex][fileIndex] + ";"
            multiFirstTime = True
            singleFirstTime = True
            for x in range(len(args)):
                if args[x] == "-singleFileSelect_nodeIndex" and args[x+1] == nodeIndex and args[x+3] == str(fileIndex):
                    if multiFirstTime:
                        outputStrForInputFile = outputStrForInputFile + "\n\t\tSingle File Selection List: " + args[x+5] + ", "
                        multiFirstTime = False
                    else:
                        outputStrForInputFile = outputStrForInputFile + args[x+5] + ", "
                elif args[x] == "-optionalFileSelect_nodeIndex" and args[x+1] == nodeIndex and args[x+3] == str(fileIndex):
                    if singleFirstTime:
                        outputStrForInputFile = outputStrForInputFile + "\n\t\tOptional File Selection List: " + args[x+5] + ", "
                        singleFirstTime = False
                    else:
                        outputStrForInputFile = outputStrForInputFile + args[x+5] + ", "
                 
    
        #outputStr = outputStr + "\r\n"

    parser = argparse.ArgumentParser(description='Process datashop file.')
    parser.add_argument('-programDir', type=str, help='the component program directory')
    parser.add_argument('-workingDir', type=str, help='the component instance working directory')
    parser.add_argument('-userId', type=str, help='the user executing the component', default='')
    parser.add_argument('-dbConnection', type=str, help='check db connection', default='false')
    parser.add_argument('-stringType', type=str, help='check string type', default='')
    parser.add_argument('-selectionType', type=str, help='check optional selection', default='')
    parser.add_argument('-longSelect', type=str, help='check long selection', default='')
    parser.add_argument('-shortSelect', type=str, help='check short selection', default='')

    args, option_file_index_args = parser.parse_known_args()

    outputStr = outputStr + "Argument programDir: " + args.programDir
    outputStr = outputStr + "\nArgument workingDir: " + args.workingDir
    outputStr = outputStr + "\nArgument userId: " + args.userId

    outputStr = outputStr + "\r\nArgument string type: " + args.stringType
    outputStr = outputStr + "\nArgument optional type: " + args.selectionType
    if args.selectionType == "Long":
        outputStr = outputStr + "\n\tArgument longSelect: " + args.longSelect
    else:
        outputStr = outputStr + "\n\tArgument shortSelect: " + args.shortSelect
    
    outputStr = outputStr + "\r\nDatabase connected: "
    if args.dbConnection == "true":
        outputStr = outputStr + "established"
    else:
        outputStr = outputStr + "not established"

    
    outputStr = outputStr + "\n" + outputStrForInputFile

    
    outputFilePath = args.workingDir + "/all_qa_result.txt"
    outfile = open(outputFilePath, 'w')
    outfile.write(outputStr)
    outfile.close()

    
    



    

    
