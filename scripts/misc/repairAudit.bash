#!/usr/bin/env bash
export REPORT_ID=$1

cd /Users/god08d/Documents/MERIT/FixAuditLogs


grep /report/update/$REPORT_ID fieldcapture-ssl-access.* | while read -r line ; do

    # your code goes here
    echo $line | awk '$9 == 200 {print $NF}' | while read -r cookie ; do
        grep $cookie fieldcapture-ssl-access.* | grep ticket | while read -r ticket ; do

            if [[ $ticket =~ .*ticket=(.*)\ HTTP.* ]]
            then
                grep ${BASH_REMATCH[1]} catalina.out* | grep "for user" | while read -r login ; do
                    if [[ $login =~ catalina.out:(.*)\ INFO.*user\ \[(.*)\].* ]]
                    then
                        echo $line
                        echo $line | awk '{print $4 $5}'
                        echo ${BASH_REMATCH[1]}
                        echo ${BASH_REMATCH[2]}
                    fi
                done
            fi
        done
    done

done


