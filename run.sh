#!/bin/sh

set -x
set -e


hdfs dfs -ls wcplus || hdfs dfs -mkdir wcplus
hdfs dfs -ls wcplus/books || hdfs dfs -copyFromLocal books wcplus

SBT_OPTIONS="-DCASCADING_FABRIC=${CASCADING_FABRIC:-hadoop2-tez}"
if [ ! -z $CASCADING_VERSION ]
then
    SBT_OPTIONS="$SBT_OPTIONS -DCASCADING_VERSION=${CASCADING_VERSION}"
fi

sbt $SBT_OPTIONS assembly

export HADOOP_HEAPSIZE=1024
#export HADOOP_HEAPSIZE=6000

FLAGS="-Xmx${HADOOP_HEAPSIZE}m"
FLAGS="$FLAGS -Dcascading.planner.plan.path=/tmp/plan-wcplus/plan.lst"
FLAGS="$FLAGS -Dcascading.planner.stats.path=/tmp/plan-wcplus/stats.lst"
FLAGS="$FLAGS -Dcascading.planner.plan.transforms.path=/tmp/plan-wcplus/xforms.lst"
#FLAGS="$FLAGS -Dcascading.cascade.maxconcurrentflows=1"
#FLAGS="$FLAGS -Dtest.profile.node=E4DB7E97D232413E91842D31D53B5F19 -Dtest.profile.path=/tmp/jfr/"
FLAGS="$FLAGS -Dorg.slf4j.simpleLogger.log.org.apache.tez.runtime.library.common.writers=DEBUG"
FLAGS="$FLAGS -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG -Dsun.io.serialization.extendedDebugInfo=true "
FLAGS="$FLAGS -Dorg.slf4j.simpleLogger.log.cascading.flow.stream.graph.StreamGraph=DEBUG"

JAR=`ls -t target/scala-2.11/wcplus-*.jar|head -1`

DRIVEN=./`ls -t driven-plugin-*.jar ||echo driven-plugin-__fetchme__.jar |head -1`
if [ ! -f $DRIVEN ] 
then
    echo "Driven plug-in not found here. Suggestion: run "
    echo "     wget -i http://eap.concurrentinc.com/driven/1.3/driven-plugin/latest-jar.txt "
    echo "(check it out http://www.cascading.org/driven/ )"
    echo "and start again. Script will continue in 10 seconds."
    sleep 10
fi    

export HADOOP_CLASSPATH=$DRIVEN:$HADOOP_CLASSPATH

TEZ_PARTITIONS=35


case $CASCADING_FABRIC in
   hadoop)
      FABRIC=--hadoop1
      ;;
   local)
      FABRIC=--local
      ;;
   hadoop2-mr1)
      FABRIC=--hadoop2-mr1
      ;;
   hadoop2-tez)
      FABRIC=--hadoop2-tez
      ;;
   *)
      FABRIC=--hadoop2-tez
      ;;
esac

export HADOOP_OPTS="${FLAGS}"
export HADOOP_CLIENT_OPTS="${FLAGS}"

time hadoop jar $JAR \
    com.twitter.scalding.Tool com.transparencyrights.demo.wcplus.ComputeApp \
    $FABRIC \
    --filter ${FILTER:-true} --manygrams ${NGRAMS:-5} --fakeMedian ${FAKEMEDIAN:-false} --crash ${CRASH:-true} \
    --root wcplus \
    --tez-partitions $TEZ_PARTITIONS \
    --tez.lib.uris hdfs://tpcy-par/apps/tez-0.6/tez-0.6.2-SNAPSHOT-guavafix.tar.gz \
    --queue prod
    
    
rm -rf target/wcplus ||true
hdfs dfs -copyToLocal wcplus target/wcplus

