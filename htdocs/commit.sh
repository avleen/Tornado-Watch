#!/bin/bash

srcdir=/www/silverwraith.com/canonical/tw.silverwraith.com/
dstdir=/home/avleen/devel/Tornado-Watch/htdocs
rsync -av ${srcdir} ${dstdir}
pg_dump -s -U postgres tornadowatch > ${dstdir}/db.sql
cd ${dstdir}
#git add -A .
#git commit -am"$*"
#git pull --rebase
#git push
