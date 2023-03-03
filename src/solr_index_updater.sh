#!/bin/bash

# ##########################################################################################################
# Monitors a folder and automatically updates the index of a Solr core for files added, deleted, or moved
# within that folder.
# It uses inotifywait to listen for file system events in the target directory and triggers indexing or
# de-indexing operations accordingly.
# The script also has error logging capabilities and will retry failed indexing or de-indexing operations.
# ##########################################################################################################

#################################################################################################################################
LOGDIR="/home/v/Documents/VirtualBoxShared/.log"                      # absolute path of the folder (as viewed from the host)
                                                                      # containing logs
TARGET_DIR="/home/v/Documents/VirtualBoxShared/SharedDocsSolr"        # absolute path of the folder
                                                                      # (as viewed from the host) to be monitored (to keep
                                                                      # its files constantly updated on the Solr core index)
coreName="files"                                                      # name of the Solr core on which files need to be indexed
absolutePathUseVMscript="/home/v/Documents/VirtualBoxShared/runVM.sh" # absolute path (as viewed from the host)
                                                                      # of the bash script 'runVM.sh'
saveLog=true                                                          # whether or not to write on the log file ('error.log')
#################################################################################################################################


EVENTS="move,create,delete" # types of events to be monitored
# Legend:
# MOVED_FROM -> file moved from the monitored directory
# DELETE -----> file deleted
# CREATE -----> file created
# MOVED_TO ---> file moved to the monitored directory

if [ ! -d $LOGDIR ]; then
	mkdir -p $LOGDIR
fi

ERROR_LOGNAME="$LOGDIR/error.log" # absolute path (as viewed from the host) of the file that contains logs of only errors
                                  # (missed indexing/de-indexing)
absolutePathIncomingFolder="$(readlink -f "$TARGET_DIR")"
sharedFolderAsHost="/home/v/Documents/VirtualBoxShared" # (shared folder as viewed from the host)
sharedFolderAsGuest="/media/sf_VirtualBoxShared" # (shared folder as viewed from the guest)

# Open ERROR_LOGNAME and apply any failed indexing/de-indexing (for any reason)
while IFS='' read -r line || [[ -n "$line" ]]
do
	changeType=$(echo "$line" | cut -d " " -f 1)
	absolutePathFileAsHost=echo "$line" | cut -d ' ' -f 2-
	absolutePathFileAsGuest=$(sed s/${sharedFolderAsHost////\/}/${sharedFolderAsGuest////\/}/ <<< "$absolutePathFileAsHost")

	if [[ "$changeType" == "Failed_De-indexing" ]] # Mancata_Deindicizzazione
	then
		echo -e "\r\nStart de-indexing $absolutePathFileAsGuest ...\r\n"
		sed -i '1d' "$ERROR_LOGNAME"	# remove the first line of ERROR_LOGNAME
		"$absolutePathUseVMscript" "$absolutePathFileAsGuest" "$coreName" deindex
	fi

	if [[ "$changeType" == "Failed_Indexing" ]] # Mancata_Indicizzazione
	then
		echo -e "\r\nStart indexing $absolutePathFileAsGuest ...\r\n"
		sed -i '1d' "$ERROR_LOGNAME"	# remove the first line of ERROR_LOGNAME
		"$absolutePathUseVMscript" "$absolutePathFileAsGuest" "$coreName" index
	fi
	
done < "$ERROR_LOGNAME"


# Start inotify to continuously monitor the folder 'absolutePathIncomingFolder'
echo "Monitoring folder: $absolutePathIncomingFolder"
inotifywait -m -r --format '%T %e %w%f' --timefmt '%F %T' -e "$EVENTS" "$TARGET_DIR" | while read line
do
	# 'line' is composed of 4 strings:
	# year-month-day hour:minutes:seconds typeOfModification absoluteFilePath
	# eg: 2016-09-22 12:16:58 CREATE /home/v/Desktop/aFolder/aFile
	typeOfModification="$(echo "$line" | cut -d " " -f 3)"	# extract typeOfModification
	longFileName=`echo "$line" | cut -d ' ' -f 4-`
	absoluteFilePathAsHost="$(readlink -f "$longFileName")"		# extract absoluteFilePathAsHost
	absoluteFilePathAsGuest=$(sed s/${cartellaCondivisaComeHost//\//\\/}/${cartellaCondivisaComeGuest//\//\\/}/ <<< "$absoluteFilePathAsHost")

	if [ -f "$absoluteFilePathAsHost" ]
	then
		echo -e "\r\nStart deindexing $absoluteFilePathAsGuest ...\r\n"
		"$absolutePathUseVMscript" "$absoluteFilePathAsGuest" "$nomeCore" deindex
	elif [ -d "$absoluteFilePathAsHost" ]
	then
		echo -e "\r\nStart indexing $absoluteFilePathAsGuest ...\r\n"
		"$absolutePathUseVMscript" "$absoluteFilePathAsGuest" "$nomeCore" index
	fi
	if $saveLog; then	# write on the log file
		echo "$line" >> $LOGNAME
	fi
done

exit 0
