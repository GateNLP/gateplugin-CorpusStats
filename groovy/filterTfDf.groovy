#!/usr/bin/env groovy

// Simple groovy script that filters a tfdf file in the following way:
// Only include words where tf is larger than mintf (default 1)
// Only include words where df is larger than mindf (default 1)
// Only include words where tfidf is larger than mintfidf (default is 0.0)
// Only include words where tfdf is smaller than maxtfdf (default is +Inf)

cli = new CliBuilder(usage: 'filterTfDf.groovy [-mintf val] [-mindf val] [-mintfidf val] [-maxtfdf val] < infile > outfile')

cli.with {
  h longOpt: 'help', 'Show usage info'
  t longOpt: 'mintf', args: 1, argName: 'mintf', 'minimum term frequency (default: 1)'
  d longOpt: 'mindf', args: 1, argName: 'mindf', 'minimum document frequency (default: 1)'
  i longOpt: 'mintfidf', args: 1, argName: 'mintfidf', 'minimum tf*idf (default: 0.0)'
  x longOpt: 'maxtfdf', args: 1, argName: 'maxtfdf', 'maximum tf*df (default: no limit)'
  v longOpt: 'verbose', 'Show filtered lines on stderr'
}

options = cli.parse(args)

if(options.h) {
  cli.usage()
  return
}

mintf = 1
mindf = 1
mintfidf = 0.0
maxtfdf = -1

if(options.t) mintf = options.mintf as int
if(options.d) mindf = options.mindf as int
if(options.i) mintfidf = options.mintfidf as double
if(options.x) maxtfdf = options.maxtfdf as int

System.err.println("Using mintf=${mintf}, mindf=${mindf}, mintfidf=${mintfidf}, maxtfdf=${maxtfdf}")

linenr=0
copied=0
indices = [:]
System.in.eachLine() { line ->
  linenr++
  filter=false
  if(linenr==1) {
    System.out.println(line)
    // find the indices of each field
    fieldnames = line.split(/\s+/)
    fieldnames.eachWithIndex() { name,index ->
      indices[name] = index
    }
  } else {
    values = line.split(/\s+/)
    if(options.t) {
      if((values[indices["tf"]] as int)<mintf) filter=true
    } else if(options.d) {
      if((values[indices["df"]] as int)<mindf) filter=true
    } else if(options.i) {
      if((values[indices["tfidf"]] as int)<mintfidf) filter=true
    } else if(options.x) {
      tf=values[indices["tf"]] as int
      df=values[indices["df"]] as int
      tfdf=tf*df
      // System.err.println("tf=${tf}, df=${df}, tfdf=${tfdf}")
      if(tfdf>maxtfdf) filter=true
    }
    if(filter) {
      if(options.v) System.err.println("Filtered: "+values[0])
      return
    }
    System.out.println(line)
  }
  copied++
}
System.err.println("Lines processed: ${linenr}, copied: ${copied}")
