#!/usr/bin/env bash

progress-bar() {
  local duration=$((${1} / 10))
    already_done() { for ((done=0; done<$elapsed; done++)); do printf "▇"; done }
    remaining() { for ((remain=$elapsed; remain<$duration; remain++)); do printf " "; done }
    percentage() {
                    sign="-  "
                    if [ $(( ${1} % 2 )) == 0 ]; then
                        sign="|  "
                    fi
                    printf "### %s${sign}" $(( ($duration - $elapsed) * 10 - ${1} ));
                }
    clean_line() { printf "\r"; }

  for (( elapsed=0; elapsed<$duration; elapsed++ )); do
      for i in `seq 0 9`; do
          already_done; remaining; percentage ${i}
          sleep 1
          clean_line
      done
  done
  clean_line
}