#!/bin/sh

java -classpath "/opt/xnat/pipeline"/lib/commons-email-20030310.165926.jar:"/opt/xnat/pipeline"/lib/commons-lang-2.1.jar:"/opt/xnat/pipeline"/lib/javamail-1.3.2.jar:/opt/jdbc/postgresql-9.0-801.jdbc4.jar:/home/mackest/dev/git_repos/xnat-deidentification/xnat_redaction/bin org.ibcb.xnat.redaction.client.MessagePasser -h $1 -t $2 -f $3
