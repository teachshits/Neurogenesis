package neurogenesis.doubleprecision

import neurogenesis.msg._
import neurogenesis.util.ProgressReporter

import scala.actors.Actor
import scala.collection.immutable.Queue
import scala.swing.TextArea
import scala.swing.Label
import java.io.File
//import scala.collection.mutable.LinkedList
import scala.collection.mutable.ArrayOps

class EvolutionSupervisor(tArea:TextArea,fLabel:Label,numThreads:Int,saveWhenExiting:Boolean=false) extends Actor {
  var maxThreads = numThreads
  var runningThreads = 0
  var evolvers = new Array[(NeuralEvolver,Int)](numThreads)
  var numEvolvers = numThreads
  //var messages = List[Queue[String]]()
  //var fitnessTable = List[(Double,NeuralEvolver)]()
  var maxGenerations = 10000L
  var counter = 1L
  val goOn = "Evolve"
  var printInfo = true
  val reporter = new ProgressReporter(tArea)
  reporter.start
  var maxFit = 0.0d
  var saveCounter = 1
  var saveFrequency = 5000L
  var savePath = "./saves/"
  var exitCounter = 0
  var saveOnExit = saveWhenExiting
  def act : Unit = {
    loop {
      react {
        case "Start" => {
          for (i <- 0 until evolvers.length) {
            evolvers(i)._1.start
          }
          var i = 0
          while (runningThreads < maxThreads && i < evolvers.length) {
            evolvers(i)._1 ! UpdateNow(counter)
            runningThreads += 1
            if (printInfo) {
              reporter ! ProgressMessage("Number of running threads: "+runningThreads)
            }
            i += 1
          }
        }
        case UpdateNow(step) => {
          //reporter ! ProgressMessage("Supervisor is updating: "+step.toString+"\n")
          var freeThreads = (runningThreads < maxThreads)
          var i = 0
          while (freeThreads && i < evolvers.length) {
            if (!evolvers(i)._1.isRunning) {
              evolvers(i)._1 ! UpdateNow(counter)
              runningThreads += 1
              if (runningThreads == maxThreads) {
                freeThreads = false
              }
              if (printInfo) {
                reporter ! ProgressMessage("Number of running threads: "+runningThreads)
              }
            }
            /*
            if (saveFrequency > 1 && counter % saveFrequency == 0L) {
              evolvers(i)._1 ! Save2File(new File(savePath+saveCounter+"g"+evolvers(i)._2+".xml"))
            }
            */
            i += 1
          }

        }
        case StatusMessage(bestFitness,from) => {
          //fitnessTable = fitnessTable.+:((bestFitness,from))
          
          var idx = 0
          while (idx < evolvers.length) {
            if (evolvers(idx)._1.myId == from) {
              evolvers(idx) = (evolvers(idx)._1,evolvers(idx)._2+1)
            }
            idx += 1
          }
          if (bestFitness > maxFit) {
            maxFit = bestFitness
            fLabel.text_=("Fitness: "+maxFit.toString.substring(0,7))
          }
          if (printInfo) {
            val fRep = bestFitness.toString
            if (fRep.length > 7) {
              reporter ! ProgressMessage("Latest reported fitness value: "+fRep.substring(0,7)+" (id: "+from+")")
            }
            else {
              reporter ! ProgressMessage("Latest reported fitness value: "+fRep+" (id: "+from+")")
            }
            
          }
          processData
          this ! UpdateNow(counter)
        }
        
        case BirthMessageD(evo) => {
          addEvolver(evo);
          if (printInfo) {
            reporter ! ProgressMessage("Supervisor started another NeuralEvolver")
          }
          this ! UpdateNow(counter)
        }
        
        case "Exit" => {
          for (e <- evolvers) {
            e._1 ! MakeExit(saveOnExit)
          }
          reporter ! "Exit"
        }
        case "Exiting" => {
          exitCounter += 1
          if (exitCounter == evolvers.size) {
            if (printInfo) {
              reporter ! ProgressMessage("Supervisor says Goodbye!")
            }
            exit
          }
        }
      }

    }
  }
  def addEvolver(idx:Int,e:NeuralEvolver) : Unit = {
    evolvers(idx) = (e,0)
    e.setID(idx)
  }
  def addEvolver(e:NeuralEvolver) : Unit = {
    val arr = new Array[(NeuralEvolver,Int)](evolvers.length+1)
    for (i <- 0 until evolvers.length) {
      arr(i) = evolvers(i)
    }
    e.setID(evolvers.length)
    arr(evolvers.length) = (e,0)
    evolvers = arr
    e.setPrintInfo(printInfo)
    e.start()
  }
  def getReporter : ProgressReporter = reporter
  def setThreads(maxT:Int) : Unit = { maxThreads = maxT }
  def setMaximumGenerations(g:Long) : Unit = { maxGenerations = g }
  def setPrintInfo(b:Boolean) : Unit = { printInfo = b }
  def processData : Unit = {
    runningThreads -= 1
    evolvers = evolvers.sortWith(_._2 < _._2) //
    if (evolvers.head._2 == counter) {
      counter += 1
      //println("Counter: "+counter)
      if (counter == maxGenerations) {
        this ! "Exit"
      }
    }
  }
  def reset : Unit = {
    evolvers = new Array[(NeuralEvolver,Int)](numThreads)
  }
  def getSuperStar : RNND = {
    var found = false
    var idx = 4
    var best = evolvers(0)._1.bestNets(idx)
    while (!found) {
    
      var f = 0.0
      if (best != null) {
        f = best.getFitness
      }
      var i = 1
      while (i < evolvers.size) {
        val candidate = evolvers(i)._1.bestNets(idx)
        if (candidate != null) {
          val f2 = candidate.getFitness
          if (f2 > f) {
            f = f2
             best = candidate
          }
        }
        i += 1
      }
      idx -= 1
      if (f > 0) {
        found = true
      }
    }
    best
  }
  def printInfo(b:Boolean) : Unit = {
    printInfo = b
    for ((e,gen) <- evolvers) {
      e.setPrintInfo(b)
    }
  }
  def setSaveOnExit(b:Boolean) : Unit = { saveOnExit = b }
  def setSavePath(path:String) = savePath = path
}