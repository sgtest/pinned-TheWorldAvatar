#!/bin/bash
# D. Nurkowski (danieln@cmclinnovations.com)
echo "-----------------------------------------------"
echo "--   python entrdfizer installation script  --"
echo "-----------------------------------------------"
echo ""
#

AUTHOR="Daniel Nurkowski <danieln@cmclinnovations.com>"
SPATH="$( cd  "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
CREATE_VENV='n'
VENV_NAME='entityrdfizer_venv'
VENV_DIR=$SPATH
DEV_INSTALL=''
PROJ_NAME='entityrdfizer'

function usage {
    echo "==============================================================================================================="
    echo "python "$PROJ_NAME" installation script."
    echo
    echo "Please run the script with one of the following flags set:"
    echo "--------------------------------------------------------------------------------------------------------"
	echo "  -v              : creates virtual environment for this project in the project directory"
	echo "  -n VENV_NAME    : name of the virtual environment to be created, if not provided,"
	echo "                    default "$VENV_NAME" will be used instead"
	echo "  -d VENV_DIR     : directory specyfing where to craete the virtual environment, if not provided,"
	echo "                    the virtual environment will be created in the root of the project directory"
	echo "  -i              : installs the project in currently active virtual environment in a developer mode"
	echo "  -e              : enables developer mode installation"
	echo "  -h              : print this help message"
	echo
	echo "Example usage:"
	echo "./install_wrapper.sh -i             - this will install the project in the currently active virtual environment"
	echo "./install_wrapper.sh -v -i          - this will create default virtual environment in the project root and"
	echo "                                      install the project in it"
	echo "./install_wrapper.sh -v -i -e       - this will create default virtual environment in the project root and"
	echo "                                      install the project in it in a developer mode"
	echo "==============================================================================================================="
    exit -1
}

function create_env {
	echo "Creating virtual environment for this project"
    echo "-----------------------------------------------"
    echo
        echo "Creating "$VENV_NAME" virtual environment..."
		if [ -d "$SPATH/$VENV_NAME" ]; then
			rm -r $SPATH"/"$VENV_NAME
		fi
		python -m venv $VENV_DIR"/"$VENV_NAME
		if [ $? -eq 0 ]; then
			echo ""
			echo "    INFO: Virtual environment created."
			echo "-----------------------------------------"
		else
			echo ""
			echo "    ERROR: Failed to create virtual environment."
			echo "-----------------------------------------"
            read -n 1 -s -r -p "Press any key to continue"
			exit -1
		fi
		echo ""
}

function get_pip_path {
    if [[ $CREATE_VENV == 'y' ]]
	then        
	    if [ -d "$VENV_DIR/$VENV_NAME/bin/pip3" ]; then
            PIPPATH=$VENV_DIR"/"$VENV_NAME/bin/pip3
		else
		    PIPPATH=$VENV_DIR"/"$VENV_NAME/Scripts/pip3
		fi
	else
        PIPPATH=pip3
	fi
}

function install_project {
	echo "Installing the project"
    echo "-----------------------------------------------"
    echo
    get_pip_path
    $PIPPATH --disable-pip-version-check install $DEV_INSTALL $SPATH
    if [[ "${DEV_INSTALL}" == "-e" ]];
    then
        $PIPPATH --disable-pip-version-check install -r dev_requirements.txt
    fi
    if [ $? -eq 0 ]; then
    	echo ""
    	echo "    INFO: installation complete."
    	echo "-----------------------------------------"
    else
        echo ""
    	echo ""
    	echo "    ERROR: installation failed."
    	echo "-----------------------------------------"
        read -n 1 -s -r -p "Press any key to continue"
        exit -1
    fi

}

# Scan command-line arguments
if [[ $# = 0 ]]
then
   usage
fi
while [[ $# > 0 ]]
do
key="$1"
case $key in
    -h)
     usage;;
    -v) CREATE_VENV='y'; shift;;
	-n) VENV_NAME=$2; shift 2;;
	-d) VENV_DIR=$2; shift 2;;
    -i) INSTALL_PROJ='y'; shift;;
	-e) DEV_INSTALL='-e'; shift;;
    *)
	# otherwise print the usage
    usage
    ;;
esac

done

if [[ $CREATE_VENV == 'y' ]]
then
    create_env
fi
if [[ $INSTALL_PROJ == 'y' ]]
then
    install_project
fi

echo
read -n 1 -s -r -p "Press any key to continue"