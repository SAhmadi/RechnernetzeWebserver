#!/bin/bash

# title           : curl_tests.sh
# description     : This script will compile a given Java web server source, run it in the background and test it with some use cases.
# author          : Raphael Bialon <bialon@cs.uni-duesseldorf.de>

cyan="\033[01;36m"
red="\033[01;31m"
bold="\033[1m"
restore="\033[0m"

CURL="curl"
CURLOPTS="--http1.0 --include --max-time 2"
HOST="localhost"
PORT="6789"

JAVA="java"
JAVAC="javac"

SOURCE="WebServer.java"
CLASS="$(basename ${SOURCE} .java)"

STARTUP_WAIT=3

NONEXISTING_FILE="notHere.png"
EXISTING_FILE="index.htm"
MIMETYPES_FILE="mime.types"


function info() {
    echo -e "${cyan}${bold}${1}${restore}"
}

function error() {
    echo -e >&2 "${red}${bold}${1}${restore}"
}

function spacer() {
    echo -e "\n"
}

function check_program() {
    command -v $1 >/dev/null 2>&1 || { error "This script requires $1 to run!"; exit 1; }
}

function check_file_exists() {
    if [[ ! -e $1 ]]; then
        error "The file $1 does not exist but is needed for our tests!"
        exit 1
    fi
}


check_program $CURL
check_program $JAVA
check_program $JAVAC

if [[ ! -e $SOURCE ]]; then
    error "Please copy $SOURCE into the folder this script is running from! ($(pwd))"
    exit 1
fi

if [[ -e $NONEXISTING_FILE ]]; then
    error "The file $NONEXISTING_FILE should not exist. Please remove or rename!"
    exit 1
fi

check_file_exists $EXISTING_FILE
check_file_exists $MIMETYPES_FILE


info "[+] Compiling source"
$JAVAC $SOURCE

info "[+] Running program"
$JAVA $CLASS -mime $MIMETYPES_FILE >/dev/null 2>&1 &
BACKGROUND_PID=$!

info "[+] Wait ${STARTUP_WAIT}s for program to start..."
sleep $STARTUP_WAIT


info "[+] Testing HTTP/1.0 GET"

info "  [+] Nonexisting file (Should deliver 404)"
$CURL $CURLOPTS --get "${HOST}:${PORT}/$NONEXISTING_FILE"
spacer

info "  [+] Existing mime.types-file (Should deliver 200)"
$CURL $CURLOPTS --get "${HOST}:${PORT}/$MIMETYPES_FILE"
spacer

info "  [+] Java source (Should deliver 200 and correct MIME type)"
$CURL $CURLOPTS --get "${HOST}:${PORT}/${SOURCE}"
spacer


info "[+] Testing HTTP/1.0 HEAD"

info "  [+] Nonexisting file (Should deliver 404)"
$CURL $CURLOPTS --head "${HOST}:${PORT}/$NONEXISTING_FILE"
spacer

info "  [+] Existing file (Should deliver 200 without content)"
$CURL $CURLOPTS --head "${HOST}:${PORT}/$EXISTING_FILE"
spacer


info "[+] Testing HTTP/1.0 POST"

info "  [+] Nonexisting file (Should deliver 404)"
$CURL $CURLOPTS --request POST "${HOST}:${PORT}/$NONEXISTING_FILE"
spacer

info "  [+] Existing file (Should deliver 200)"
$CURL $CURLOPTS --request POST "${HOST}:${PORT}/$EXISTING_FILE"
spacer

info "  [+] Nonexisting file and invalid request (Should deliver 400)"
$CURL $CURLOPTS --request POST --data "invalid" --header "Content-Length: 42" "${HOST}:${PORT}/$NONEXISTING_FILE"
spacer

info "  [+] Existing file and invalid request (Should deliver 400)"
$CURL $CURLOPTS --request POST --data "invalid" --header "Content-Length: 42" "${HOST}:${PORT}/$EXISTING_FILE"
spacer

info "  [+] Nonexisting file and valid request (Should deliver 404)"
$CURL $CURLOPTS --request POST --data "input=test123" "${HOST}:${PORT}/$NONEXISTING_FILE"
spacer

info "  [+] Existing file and valid request (Should deliver 200 with content OR 204 without content)"
$CURL $CURLOPTS --request POST --data "input=test123" "${HOST}:${PORT}/$EXISTING_FILE"
spacer

info "[+] Stopping program"
kill -15 $BACKGROUND_PID

