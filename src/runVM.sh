#!/bin/bash

########################################################################################################
vmName="Hortonworks Sandbox with HDP 2.4" # Name of the virtual machine to be executed
guestUsername="root"                      # Guest user name to be logged in with
guestPassword="aPassword"                 # Guest user password to be logged in with
saveState=true                            # Whether or not to save the current state
                                          # of the VM after running the script
########################################################################################################

# If the VM is not already running, then...
if ! vboxmanage showvminfo "$vmName" | grep -c "running (since" &> /dev/null # &> /dev/null is used to "silence" the output of this command
then
# ... start the VM
VBoxManage startvm "$vmName" &> /dev/null
fi

# Wait for VirtualBox to load the machine (necessary so that the indexing scripts can be executed as guest)
until [[ $(VBoxManage guestproperty get "$vmName" "/VirtualBox/GuestInfo/OS/LoggedInUsers") != "No value set!" && $(VBoxManage guestproperty get "$vmName" "/VirtualBox/GuestInfo/OS/LoggedInUsers") != "Value: 0" ]]
do
	sleep 1
done

# Execute the script as "guest"
VBoxManage guestcontrol "$vmName" run --exe "/media/sf_VirtualBoxShared/useJava.sh" anUnseenParameter "$@" --username "$guestUsername" --password "$guestPassword" --wait-stdout

# Save the current state of the VM (Note: This will waste time during the next need of the virtual machine for indexing/deindexing)
#VBoxManage controlvm "$vmName" savestate
if $saveState; then
	VBoxManage controlvm "$vmName" savestate
fi
