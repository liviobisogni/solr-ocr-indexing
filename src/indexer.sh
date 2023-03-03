#!/bin/bash

# ##########################################################################################################
# This script is used to make a pdf file searchable (in case the file is a non-searchable pdf)
# and to index a file on Solr
#
# INPUT:
#     - a string indicating the path of the file to be indexed
#     - an integer: 0 if the file is already searchable, 1 if the file is a pdf that still needs to be made searchable
#     - a string indicating the name of the Solr core to index the file
#     - a string: 'index' to index the file, 'deindex' to de-index the file
# OUTPUT:
#     - (in case of non-searchable pdf file, the script generates) a searchable pdf file
#     - a file indexed on a Solr core
#
# To work, ImageMagick must be installed
# (this can be verified by issuing the "convert -version" command)
#
# A possible example command to use this script
# (assuming that this script is in the working directory) is:
# ./index.sh /absolute/path/to/a/folder/nameOfFile.extensionOfFile 0 myCore
# in case of an already searchable file
# ./index.sh /absolute/path/to/a/folder/nameOfFile.pdf 1 myCore
# in case of non-searchable pdf file
#
# Finally, note that modifying this script by lowering the values of 'density' (e.g. to 150)
# and/or 'quality' (e.g. to 10) has 3 consequences:
#     - reduces conversion time (good)
#     - reduces the disk space required to store the output converted file(s) (good)
#     - reduces the conversion quality (bad)
# ##########################################################################################################


if (($#<4))
then
	echo "Four parameters are needed:"
	echo "1 A string indicating the path of the file to be indexed"
	echo "2 An integer: '0' if the file is already searchable, '1' if the file is a pdf that still needs to be made searchable"
	echo "3 A string indicating the name of the Solr core to index the file"
	echo "4 A string: 'index' to index the file, 'deindex' to de-index the file"
	exit
elif (($#>4))
then
	echo "Too many parameters, only 4 are required:"
	echo "1 A string indicating the path of the file to be indexed"
	echo "2 An integer: '0' if the file is already searchable, '1' if the file is a pdf that still needs to be made searchable"
	echo "3 A string indicating the name of the Solr core to index the file"
	echo "4 A string: 'index' to index the file, 'deindex' to de-index the file"
	exit
fi

#########################################################################################################################
LOGDIR="/home/v/Documents/VirtualBoxShared/.log" # LOGDIR is the absolute path of the folder (as seen from the host)
                                                 # containing the logs
ERROR_LOGNAME="$LOGDIR/error.log"                # name of the file for the error logs (saving the missing
                                                 # indexing/de-indexing files)
#########################################################################################################################
myCore="$3"


if (($2==1)) # if the file is a pdf that needs to be made searchable...
then

	# 1: Convert the pdf file to n image files
	filename="${1}"
	filename_without_ext="${filename%.*}"        # filename without extension (i.e., $1 without the pdf extension)
	file_basename="${filename_without_ext##*/}"  # extracts the file name (without the absolute path)
	output_name="${file_basename}_OUTPUT"        # the name of the output file
	output_filename="${output_name}.jpg"         # the name of the output file with .jpg extension
	temp_dir="/tmp"
	temp_file="${temp_dir}/${output_filename}"
  
	convert -density 300 "${filename}" -quality 100 "$temp_file"
  
  
	# 2: Perform OCR with tesseract on the n image files to create a searchable pdf
	command1='pdftk "$1" dump_data | grep NumberOfPages'
	command2=`eval $command1`
	command3=$(echo "$command2"|tr -d "NumberOfPages:")  # command3 = "number of pages in the pdf file $1"
  
	if ((command3<1)) # if the pdf file has no pages (invalid)
	then
		echo "WARNING: The file passed as an argument is not a valid pdf"
		exit
	fi
  
	if ((command3==1)) # if the pdf file consists of only one page
	then
		tesseract "$temp_file" "$filename_without_ext" pdf
	fi
  
	if ((command3>1)) # if the pdf file consists of more than one page
	then
		filePath=""
		for ((i=0;i<command3;i++))
		do
			temp_output_filename="${temp_dir}/${output_name}-${i}.jpg"
  
			if [ -n "$filePath" ]
			then
				filePath="${filePath}\n${temp_output_filename}"
			else
				filePath="${temp_output_filename}"
			fi
  
		done
		echo "$filePath" > "${temp_dir}/TEMP.txt"
  
		tesseract "${temp_dir}/TEMP.txt" "$filename_without_ext" -l ita pdf
	fi
  
  
	# 3: Remove the n image files created in step 1
	if ((command3==1))
	then
		rm "$temp_file"
	fi
  
	if ((command3>1))
	then
		for ((i=0;i<command3;i++))
		do
			temp_output_filename="${temp_dir}/${output_name}-${i}.jpg"
			rm "$temp_output_filename"
		done
		rm "${temp_dir}/TEMP.txt"
	fi
fi


#4: De-index the file on the 'myCore' core of Solr...
if [[ "$4" == "deindex" ]]
then
	if java -Ddata=args -Dcommit=true -Durl=http://localhost:8983/solr/"$myCore"/update -jar /opt/solr/example/exampledocs/post.jar "<delete><id>$1</id></delete>"
	then
		echo "*** File $1 de-indexed successfully! ***"
	else
		echo "Failed_Deindexing $filename" >> $ERROR_LOGNAME # write the failed de-indexing of file '$filename' to the error log file
		echo "*** File $1 de-indexing unsuccessful! ***"
	fi


#5: ...or index the file on the 'myCore' core of Solr
elif [[ "$4" == "index" ]]
then
	if /opt/solr/bin/post -c "$myCore" "$1"
	then
		echo "*** File $1 indexed successfully! ***"
	else
		echo "Failed_Indexing $filename" >> $ERROR_LOGNAME # write the failed indexing of file '$filename' to the error log file
		echo "*** File $1 indexing unsuccessful! ***"
	fi
fi
