#!/bin/bash 

NAME=okapi
VERSION=`python -c "import xml.etree.ElementTree as ET; \
          print(ET.parse(open('pom.xml')).getroot().find( \
          '{http://maven.apache.org/POM/4.0.0}version').text)"`

if git tag | grep $VERSION > /dev/null; then 
   echo "Git tag $VERSION exists."  
   echo "Source distribution will be tagged v${VERSION}." 
   git archive -o ../${NAME}_${VERSION}.orig.tar.gz --prefix=okapi_${VERSION}/ v${VERSION}
else
   echo "No git tag exists for this version."  
   echo "Source distribution will be HEAD of current branch."
   git archive -o ../${NAME}_${VERSION}.orig.tar.gz --prefix=okapi_${VERSION}/ HEAD
fi

DCH=debian/changelog

if ! git ls-files $DCH --error-unmatch >/dev/null 2>&1; then
    rm -f $DCH
    echo "$NAME (${VERSION}-1) unstable; urgency=medium" >$DCH
    echo '' >>$DCH
    echo '  * Upstream.' >>$DCH
    echo '' >>$DCH
    echo " -- `git config --get user.name` <`git config --get user.email`>  `date -R`" >>$DCH
    echo '' >>$DCH
fi

dpkg-checkbuilddeps
dpkg-buildpackage -rfakeroot -b
