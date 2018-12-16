#!/usr/bin/env bash

readarray -t gate < ./gateway.txt
ssh -o ConnectTimeout=30 ${gate} './run.sh'