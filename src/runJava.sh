#!/bin/bash

############################################################################################################
javaClassName="Indexer"									# This is the name of the Java class that this 
														# Bash script calls
folderContainingJavaClass="/media/sf_VirtualBoxShared"	# This is the absolute path (seen from the host) 
														# of the folder containing the javaClassName.class
############################################################################################################

cd "$folderContainingJavaClass"			# Change directory to the folder containing the desired Java class
java "$javaClassName" "$@"				# Execute the desired Java program
