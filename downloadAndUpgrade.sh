#!/bin/bash

set -e

if [ -z "$AWS_ACCESS_KEY_ID" ] ; then
  echo "No AWS credentials provided"
  exit 1
fi

if [ -z "$AWS_SECRET_ACCESS_KEY" ] ; then
  echo "No AWS credentials provided"
  exit 1
fi

function downloadAndUpgrade {
  name=$1
  if [ -n "$2" ] ; then
    dbdir=$2
  else
    dbdir=$name
  fi
  if [ -n "$3" ] ; then
    archives=$3
  else
    archives=${name}.zip
  fi
  mkdir -p $name
  rm -Rf $name/$dbdir
  rm -Rf $name/__MAC*
  for archive in $archives $4 $5 $6 $7; do
    echo "Downloading $archive"
    aws s3 cp s3://customer-datasets.neo4j.org/$archive ./
    if [ ! -f $archive ] ; then
      echo "Failed to download $archive"
      return -1
    fi
    cp $archive $archive.backup_$(date +%Y%m%d%H%M)
    echo "Unpacking $archive to $name/$dbdir"
    (
      cd $name
      unzip -q ../$archive
      if [ ! -d $dbdir ] ; then
        echo "Failed to find unzipped directory $name/$dbdir"
        return -1
      fi
    )
  done
  mvn exec:java -Dexec.mainClass="org.amanzi.neo4j.ForceUpgrade" -Dexec.args="$name/$dbdir"
}

function zipAndUpload {
  name=$1
  if [ -n "$2" ] ; then
    dbdir=$2
  else
    dbdir=$name
  fi
  if [ -n "$3" ] ; then
    archive=$3
  else
    archive=${name}.zip
  fi
  if [ -d $name/$dbdir ] ; then
    rm -f $archive
    echo "Packing $name/$dbdir to $archive"
    (
      cd $name
      zip -q -r ../$archive $dbdir
    )
    if [ -f $archive ] ; then
      echo "Uploading $archive"
      aws s3 cp $archive s3://customer-datasets.neo4j.org/$archive
    fi
  else
    echo "Cannot find $name/$dbdir"
    return -1
  fi
}

# To use this code include this in aother bash script and run commands like:
# source downloadAndUpgrade.sh
# downloadAndUpgrade dbname dbdir
# zipAndUpload dbname dbdir
